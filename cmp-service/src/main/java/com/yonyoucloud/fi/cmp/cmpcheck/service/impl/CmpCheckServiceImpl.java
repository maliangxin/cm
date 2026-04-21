package com.yonyoucloud.fi.cmp.cmpcheck.service.impl;

import com.yonyou.cloud.middleware.rpc.RPCStubBeanFactory;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting_b;
import com.yonyoucloud.fi.cmp.cmpcheck.service.CmpCheckService;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.ctmrpc.CtmCmpBankReconciliationSettingRpcServiceImpl;
import com.yonyoucloud.fi.cmp.event.constant.IEventCenterConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BankReconciliationSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;
import com.yonyoucloud.fi.cmp.reconciliate.vo.BankReceiptInfoVO;
import com.yonyoucloud.fi.cmp.reconciliate.vo.BankReceiptToVoucherVO;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.SendEventMessageUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.uretail.sys.auth.DataPermissionRequestDto;
import com.yonyoucloud.uretail.sys.auth.DataPermissionResponseDto;
import com.yonyoucloud.uretail.sys.auth.DataPermissionResultDto;
import com.yonyoucloud.uretail.sys.pubItf.IDataPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @description: 银企对账后台查询接口
 * @author: wanxbo@yonyou.com
 * @date: 2024/1/26 15:13
 */

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class CmpCheckServiceImpl implements CmpCheckService {

    @Autowired
    CtmCmpBankReconciliationSettingRpcServiceImpl cmpBankReconciliationSettingRpcService;

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Override
    public CtmJSONObject querySettingDetailInfo(CtmJSONObject  param) throws Exception {
        CtmJSONObject jsonObject = new CtmJSONObject();
        PlanParam planParam = new PlanParam(param.getString("accountId"),param.getString("currencyId"),param.getString("planId"));
        List<BankReconciliationSettingVO> infoList = findUseOrg(planParam);
        log.error(" findUseOrg, param = {},result = {}",
                CtmJSONObject.toJSONString(param),CtmJSONObject.toJSONString(infoList));
        jsonObject.put("data",infoList);

        //资金组织改造，增加银企对账设置数据源的返回；1凭证2银行日记账
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, param.getString("planId"));
        if (bankReconciliationSetting != null ){
            jsonObject.put("reconciliationdatasource",bankReconciliationSetting.getReconciliationdatasource().getValue());
        }

        //凭证数据，要查询对账方案账簿下对应勾兑或未勾兑数据
        if ("1".equals(param.getString("reconciliationdatasource"))){
            boolean checkflag = param.getBoolean("checkflag");
            try {
                List<CtmJSONObject> accbookinfoList = queryTotalCountByAccbooks(infoList,checkflag,param);
                jsonObject.put("accbookinfoList",accbookinfoList);
            }catch (Exception e){
                log.error("批量按账簿查询数量异常！, e = {}",e.getMessage());
            }
        }
        return jsonObject;
    }

    @Override
    public CtmJSONObject queryOpenOutstandingInfo(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        QuerySchema schema;
        //银行对账单期初未达和日记账期初未达不需要查询对账账簿
        if ("bank".equals(param.getString("resourceType"))){
            schema = QuerySchema.create().addSelect("id","useorg","useorg.name as useorg_name","currency");
        }else {
            schema = QuerySchema.create().addSelect("id","useorg","useorg.name as useorg_name","currency","accbook_b as accbook","accbook_b.name as accbookname");
        }
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(
                QueryCondition.name("mainid").eq(param.getString("bankreconciliationscheme")),
                QueryCondition.name("bankaccount").eq(param.getString("bankaccount")),
                QueryCondition.name("currency").eq(param.getString("currency")),
                QueryCondition.name("enableStatus_b").eq(EnableStatus.Enabled.getValue())
        );
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> itemList = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        List<CtmJSONObject> infoList = new ArrayList<>();
        if(itemList != null && itemList.size() > 0){
            for (Map<String, Object> map : itemList){
                CtmJSONObject c = new CtmJSONObject();
                c.put("itemid",map.get("id"));
                c.put("useorg",map.get("useorg"));
                c.put("useorg_name",map.get("useorg_name"));
                c.put("accbook",map.get("accbook"));
                c.put("accbookname",map.get("accbookname"));
                infoList.add(c);
            }
        }
        result.put("infoList",infoList);
        return result;
    }

    /**
     * 查询银行账户的授权使用组织信息
     * @param param
     * bankaccountid 银行账户id
     * @return rangeOrgId:授权使用组织id; rangeOrgIdName授权使用组织名称
     * @throws Exception
     */
    @Override
    public CtmJSONObject queryBankAccountUseOrgInfo(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //不包含银行账户id时
        if (!param.containsKey("bankaccountid") || StringUtils.isEmpty(param.getString("bankaccountid"))){
            result.put("count",0);
            return result;
        }
        //查询授权使用组织
        EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(param.getString("bankaccountid"));
        List<OrgRangeVO> orgRangeVOList = enterpriseBankAcctVoWithRange.getAccountApplyRange();

        List<CtmJSONObject> orgList = new ArrayList<>();
        if (orgRangeVOList !=null ){
            CtmJSONObject orgInfo = new CtmJSONObject();
            for (OrgRangeVO orgRangeVO : orgRangeVOList){
                List<Map<String, Object>> accEntityes = AccentityUtil.getFundMapLIstById(orgRangeVO.getRangeOrgId());
                if (accEntityes !=null && accEntityes.size() > 0){
                    orgInfo.put("orgid",orgRangeVO.getRangeOrgId());
                    orgInfo.put("orgname",accEntityes.get(0).get("name"));
                    orgList.add(orgInfo);
                }
            }
        }
        result.put("count",orgList.size());
        result.put("orgList",orgList);
        return result;
    }

    /**
     * 根据对账账簿id和勾兑状态查询总账凭证账簿勾兑数
     * @param infoList 对账方案信息集合，包含账簿信息
     * @param checkflag 勾兑状态
     * @return {"accbookId": "账簿ID1", "totalCount": 86},
     */
    private List<CtmJSONObject> queryTotalCountByAccbooks( List<BankReconciliationSettingVO> infoList,boolean checkflag,CtmJSONObject  param){
        Set<String> bookids = new HashSet<>();
        Set<String> enableBookIds = new HashSet<>();
        //账簿要去重复
        List<CtmJSONObject> allBankAccountInfo = new ArrayList<>();
        List<CtmJSONObject> enableBankAccountInfo = new ArrayList<>();
        for (BankReconciliationSettingVO settingVO : infoList){
            bookids.add(settingVO.getAccBook());
            CtmJSONObject bankAccountInfo = new CtmJSONObject();
            bankAccountInfo.put("bankAccount",settingVO.getBankAccount());
            bankAccountInfo.put("currency",settingVO.getCurrency());
            bankAccountInfo.put("accbook",settingVO.getAccBook());
            if (settingVO.getEnableStatus() == EnableStatus.Enabled.getValue() ){
                enableBankAccountInfo.add(bankAccountInfo);
                enableBookIds.add(settingVO.getAccBook());
            }
            allBankAccountInfo.add(bankAccountInfo);
        }
        if (param.get("bankaccount") != null){
            String bankaccounts = param.getString("bankaccount");
            CtmJSONArray bankAccountArray = CtmJSONArray.parseArray(bankaccounts);
            // 转换为Set便于快速查找
            Set<String> filterAccounts = new HashSet<>();
            for (int i = 0; i < bankAccountArray.size(); i++) {
                filterAccounts.add(bankAccountArray.getString(i));
            }
            // 过滤allBankAccountInfo
            List<CtmJSONObject> filteredAllBankAccountInfo = new ArrayList<>();
            Set<String> filteredAllBookIds = new HashSet<>();
            for (CtmJSONObject accountInfo : allBankAccountInfo) {
                if (filterAccounts.contains(accountInfo.getString("bankAccount"))) {
                    filteredAllBankAccountInfo.add(accountInfo);
                    filteredAllBookIds.add(accountInfo.getString("accbook"));
                }
            }
            allBankAccountInfo = filteredAllBankAccountInfo;
            bookids = filteredAllBookIds;
            // 过滤enableBankAccountInfo
            List<CtmJSONObject> filteredEnableBankAccountInfo = new ArrayList<>();
            Set<String> filteredEnableBookIds = new HashSet<>();
            for (CtmJSONObject accountInfo : enableBankAccountInfo) {
                if (filterAccounts.contains(accountInfo.getString("bankAccount"))) {
                    filteredEnableBankAccountInfo.add(accountInfo);
                    filteredEnableBookIds.add(accountInfo.getString("accbook"));
                }
            }
            enableBankAccountInfo = filteredEnableBankAccountInfo;
            enableBookIds = filteredEnableBookIds;
        }
        //若为空则默认一个不存在的数据
        CtmJSONObject bankAccountInfo = new CtmJSONObject();
        bankAccountInfo.put("bankAccount","0");
        bankAccountInfo.put("currency","0");
        if (enableBankAccountInfo.size() == 0){
            enableBankAccountInfo.add(bankAccountInfo);
        }
        if (allBankAccountInfo.size() == 0){
            allBankAccountInfo.add(bankAccountInfo);
        }
        //对账方案id
        String bankreconciliationscheme = param.getString("planId");
        //解析需要查询的账簿id
        String[] accbookIds = bookids.toArray(new String[0]);
        String[] enableBookIdsArray = enableBookIds.toArray(new String[0]);
        //调用总账接口查询账簿勾兑数量
        CtmJSONObject jsonobject = new CtmJSONObject();
        jsonobject.put("checkflag",checkflag);
        jsonobject.put("bankreconciliationscheme",bankreconciliationscheme);
        try {
            //业务日期
            if (param.get("startMakeTime") != null){
                jsonobject.put("startMakeTime",param.getString("startMakeTime"));
            }
            if (param.get("endMakeTime") != null){
                jsonobject.put("endMakeTime",param.getString("endMakeTime"));
            }
            //金额
            if (param.get("lowAmount") != null){
                jsonobject.put("lowAmount", param.getBigDecimal("lowAmount"));
            }
            if (param.get("highAmount") != null){
                jsonobject.put("highAmount", param.getBigDecimal("highAmount"));
            }
            //只有在已勾对数据时才传递以下参数
            if (checkflag){
                //勾对日期
                if (param.get("startCheckDate") != null){
                    jsonobject.put("startCheckDate",param.getString("startCheckDate"));
                }
                if (param.get("endCheckDate") != null){
                    jsonobject.put("endCheckDate",param.getString("endCheckDate"));
                }
                //勾对号
                if (param.get("checkNo") != null){
                    jsonobject.put("checkNo", param.getString("checkNo"));
                }
                //是否封存
                if (param.get("sealflag") != null){
                    jsonobject.put("sealflag", param.getBoolean("sealflag"));
                }
                //银行账户
                jsonobject.put("bankAccounts", allBankAccountInfo);
                jsonobject.put("accbookIds",accbookIds);
            }else {
                jsonobject.put("bankAccounts", enableBankAccountInfo);
                jsonobject.put("accbookIds",enableBookIdsArray);
            }

        }catch (Exception e){
            log.error("账簿信息查询参数组装异常：" + e.getMessage());
        }

        String serverUrl = AppContext.getEnvConfig("yzb.base.url");
        String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/cash/queryTotalCountByAccbooks";
        String thd_userId = AppContext.getCurrentUser().getYhtUserId();
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("thd_userId", thd_userId);
        String str = HttpTookit.
                doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(jsonobject), header,"UTF-8");
        CtmJSONObject result = CtmJSONObject.parseObject(str);

        //解析并拼装返回数据
        CtmJSONArray countInfoArray = result.getJSONArray("data");
        List<String> checkedbookids = new ArrayList<>();
        List<CtmJSONObject> countInfoList = new ArrayList<>();
        if (countInfoArray !=null && countInfoArray.size()> 0) {
            for (BankReconciliationSettingVO settingVO : infoList){
                //已停用的对账方案不展示
                if (!checkflag && settingVO.getEnableStatus() == EnableStatus.Disabled.getValue()){
                    continue;
                }
                //一个账簿只查询一次
                if (checkedbookids.contains(settingVO.getAccBook())){
                    continue;
                }else {
                    checkedbookids.add(settingVO.getAccBook());
                }
                boolean isCotain = false;
                for (int j = 0; j < countInfoArray.size(); j++) {
                    CtmJSONObject c = countInfoArray.getJSONObject(j);
                    //根据对账账簿id匹配
                    if (settingVO.getAccBook().equals(c.getString("accbookId"))){
                        CtmJSONObject count = new CtmJSONObject();
                        count.put("accbookId",c.getString("accbookId"));
                        count.put("totalCount",c.getInteger("totalCount"));
                        count.put("accbookName",settingVO.getAccBookName());
                        countInfoList.add(count);
                        isCotain = true;
                    }
                }
                if (!isCotain){
                    CtmJSONObject count = new CtmJSONObject();
                    count.put("accbookId",settingVO.getAccBook());
                    count.put("totalCount",0);
                    count.put("accbookName",settingVO.getAccBookName());
                    countInfoList.add(count);
                }
            }
        }else {
            for (BankReconciliationSettingVO settingVO : infoList){
                //已停用的对账方案不展示
                if (!checkflag && settingVO.getEnableStatus() == EnableStatus.Disabled.getValue()){
                    continue;
                }
                //一个账簿只查询一次
                if (checkedbookids.contains(settingVO.getAccBook())){
                    continue;
                }else {
                    checkedbookids.add(settingVO.getAccBook());
                }
                CtmJSONObject count = new CtmJSONObject();
                count.put("accbookId",settingVO.getAccBook());
                count.put("totalCount",0);
                count.put("accbookName",settingVO.getAccBookName());
                countInfoList.add(count);
            }
        }

        return countInfoList;
    }

    /**
     * 银企对账，银企对账设置查询专用接口，返回的是资金组织
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public List<BankReconciliationSettingVO> findUseOrg(PlanParam param) throws Exception {

        String accountId = param.getAccountId();
        String currencyId = param.getCurrencyId();

        //资金组织适配 ，授权使用组织替换为accentityRaw核算会计主体
        QuerySchema schema = QuerySchema.create().addSelect("id", "doctype", "assistaccountingtype", "assistaccounting", "subject", "useorg", "useorg.name", "bankaccount", "currency", "accbook_b", "accbook_b.name",
                "mainid", "pubts", "tenant", "enableDate", "enableStatus_b","accentityRaw","accentityRaw.name as accentityRaw_name");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("mainid").eq(param.getPlanId()));
        if (StringUtil.isNotEmpty(currencyId)) {
            conditionGroup.appendCondition(QueryCondition.name("currency").eq(currencyId));
        }
        if (StringUtil.isNotEmpty(accountId)) {
            conditionGroup.appendCondition(QueryCondition.name("bankaccount").eq(accountId));
        }
        //银行流水对账银行账户数据权限适配
        String[] bankAccountPermissions = this.getBankAccountDataPermission(IServicecodeConstant.BANKRECONCILIATION);
        if(bankAccountPermissions != null && bankAccountPermissions.length > 0){
            conditionGroup.appendCondition(QueryCondition.name("bankaccount").in(Arrays.asList(bankAccountPermissions)));
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> itemList = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        if (itemList.isEmpty()) {
            return new ArrayList<>();
        }
        return toVos(itemList);
    }

    /**
     * 现金管理 银企对账对账方案详情专用接口， UseOrg传递的是资金组织
     * @param itemMapList
     * @return
     */
    private List<BankReconciliationSettingVO> toVos(List<Map<String, Object>> itemMapList) {
        ArrayList<BankReconciliationSettingVO> list = new ArrayList<>();
        for (Map<String, Object> item : itemMapList) {
            BankReconciliationSettingVO bankReconciliationSettingVO = new BankReconciliationSettingVO();
            bankReconciliationSettingVO.setDocType((String) item.get("doctype"));
            bankReconciliationSettingVO.setMainId((Long) item.get("mainid"));
            bankReconciliationSettingVO.setSubject((String) item.get("subject"));
            bankReconciliationSettingVO.setAssistAccountingType((String) item.get("assistaccountingtype"));
            bankReconciliationSettingVO.setAssistAccounting((String) item.get("assistaccounting"));
            bankReconciliationSettingVO.setCurrency((String) item.get("currency"));
            bankReconciliationSettingVO.setBankAccount((String) item.get("bankaccount"));
            bankReconciliationSettingVO.setUseOrg((String) item.get("useorg"));
            bankReconciliationSettingVO.setUseOrgName((String) item.get("useorg_name"));
            bankReconciliationSettingVO.setAccBook((String) item.get("accbook_b"));
            bankReconciliationSettingVO.setAccBookName((String) item.get("accbook_b_name"));
            bankReconciliationSettingVO.setEnableDate((Date) item.get("enableDate"));
            bankReconciliationSettingVO.setEnableStatus(item.get("enableStatus_b") == null ? (short)1:Short.parseShort(item.get("enableStatus_b").toString()));
            list.add(bankReconciliationSettingVO);
        }
        return list;
    }

    /**
     * 处理凭证回单关联事件发送
     * @param bankMap 勾兑的银行对账单
     * @param journalMap 勾兑的凭证数据
     */
    @Override
    public void handleBankReceiptEvent(Map<String,List<BankReconciliation>> bankMap, Map<String,List<Journal>> journalMap) throws Exception {
        List<BankReceiptToVoucherVO> bankReceiptToVoucherVOList = new ArrayList<>();
        if (CollectionUtils.isEmpty(bankMap) || CollectionUtils.isEmpty(journalMap)){
            return;
        }

        //遍历查询对账单是否关联了电子回单
        for(Map.Entry<String,List<BankReconciliation>> entry : bankMap.entrySet()){
            String checkno = entry.getKey();
            List<BankReconciliation> bankReconciliationList = entry.getValue();
            if (CollectionUtils.isEmpty(bankReconciliationList)){
                continue;
            }

            //事件发送信息实体
            BankReceiptToVoucherVO bankReceiptToVoucherVO = new BankReceiptToVoucherVO();
            bankReceiptToVoucherVO.setActionType("1");
            bankReceiptToVoucherVO.setCheckNo(checkno);
            //封装凭证id集合
            List<String> voucherbidList = new ArrayList<>();
            List<Journal> journalList = journalMap.get(checkno);
            for (Journal journal : journalList){
                voucherbidList.add(journal.getSrcbillitemid());
            }
            //勾兑的银行对账单信息
            List<BankReceiptInfoVO> bankReceiptInfoVOList = new ArrayList<>();

            boolean isNeedPush = false;
            for (BankReconciliation bankReconciliation : bankReconciliationList){
                QuerySchema querySchema = QuerySchema.create().addSelect("id,extendss,bankreconciliationid");
                QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(
                        QueryCondition.name("bankreconciliationid").eq(bankReconciliation.getId().toString()),
                        QueryCondition.name("isdown").eq(true)
                );
                querySchema.addCondition(queryConditionGroup);
                List<BankElectronicReceipt> list = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, querySchema, null);
                if (list != null && list.size() >0){
                    //标记数据需要推送
                    isNeedPush = true;
                    BankElectronicReceipt bankElectronicReceipt = list.get(0);
                    BankReceiptInfoVO bankReceiptInfoVO = new BankReceiptInfoVO();
                    //电子回单扩展信息=电子回单文件id
                    bankReceiptInfoVO.setExtendss(bankElectronicReceipt.getExtendss());
                    //对账单id
                    bankReceiptInfoVO.setBankReconciliationId(bankReconciliation.getId().toString());
                    //电子回单id
                    bankReceiptInfoVO.setBankElectronicReceiptId(bankElectronicReceipt.getId().toString());
                    bankReceiptInfoVOList.add(bankReceiptInfoVO);
                }
            }
            bankReceiptToVoucherVO.setVoucherbidList(voucherbidList);
            bankReceiptToVoucherVO.setBankReceiptInfoVOList(bankReceiptInfoVOList);
            if (isNeedPush){
                //记录业务日志
                CtmJSONObject logparam = new CtmJSONObject();
                logparam.put("bankReceiptToVoucherVO",bankReceiptToVoucherVO);
                ctmcmpBusinessLogService.saveBusinessLog(logparam, checkno, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540069D", "银企对账和凭证勾对时，发送回单和凭证关联消息") /* "银企对账和凭证勾对时，发送回单和凭证关联消息" */, IServicecodeConstant.BANKRECONCILIATION, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540069B", "银企对账") /* "银企对账" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540069C", "发送银行回单和凭证关联事件") /* "发送银行回单和凭证关联事件" */);
                bankReceiptToVoucherVOList.add(bankReceiptToVoucherVO);
            }
        }

        //发送事件中心
        if (bankReceiptToVoucherVOList.size() > 0){
            //事件中心，发送的数据包装类
            BizObject userObject = new BizObject();
            //数据类型，凭证回单关联
            userObject.put("datatype","voucher");
            //对账单生单数据
            userObject.put("datalist",bankReceiptToVoucherVOList);
            //记录业务日志
            CtmJSONObject logparam = new CtmJSONObject();
            logparam.put("userObject",userObject);
            ctmcmpBusinessLogService.saveBusinessLog(logparam, "", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540069E", "发送回单和凭证关联消息完全消息体") /* "发送回单和凭证关联消息完全消息体" */, IServicecodeConstant.BANKRECONCILIATION, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540069B", "银企对账") /* "银企对账" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540069C", "发送银行回单和凭证关联事件") /* "发送银行回单和凭证关联事件" */);
            //发送消息到事件中心
            SendEventMessageUtils.sendEventMessageEos(userObject, IEventCenterConstant.CMP_BANKRECONCILIATION, IEventCenterConstant.CMP_BANKELECTRONICRECEIPTURL_GL);
        }
    }

    @Override
    public String[] getBankAccountDataPermission(String serviceCode) throws Exception{
        //目前只处理银行流水对账
        if (!"ficmp0014".equals(serviceCode)){
            return new String[0];
        }
        DataPermissionRequestDto requestDto = new DataPermissionRequestDto();
        requestDto.setSysCode(ICmpConstant.CMP_MODUAL_NAME);
        requestDto.setYxyUserId(AppContext.getUserId().toString());
        requestDto.setYhtTenantId(InvocationInfoProxy.getTenantid());
        requestDto.setYxyTenantId(AppContext.getTenantId().toString());
        requestDto.setHaveDetail(true);
        RPCStubBeanFactory rpChainBeanFactory = new RPCStubBeanFactory("iuap-apcom-auth", "c87e2267-1001-4c70-bb2a-ab41f3b81aa3", null, IDataPermissionService.class);
        rpChainBeanFactory.afterPropertiesSet();
        if (rpChainBeanFactory.getObject() == null){
            return new String[0];
        }
        IDataPermissionService remoteBean = (IDataPermissionService) rpChainBeanFactory.getObject();
        //指定查询银行账户的数据权限
        requestDto.setEntityUri("cmp.bankvourchercheck.BankvourchercheckWorkbench");
        requestDto.setServiceCode(serviceCode);
        requestDto.setFieldNameArgs(new String[]{ICmpConstant.BANK_ACCOUNT});
        DataPermissionResponseDto dataPermission = remoteBean.getDataPermission(requestDto);
        List<DataPermissionResultDto> dataPermissionResultDtos = dataPermission.getResultDtos();
        // 计算所有values的并集
        Set<String> union = new HashSet<>();
        for (DataPermissionResultDto resultDto : dataPermissionResultDtos) {
            String[] values = resultDto.getValues();
            if (values != null) {
                union.addAll(Arrays.asList(values));
            }
        }
        return union.toArray(new String[0]);
    }

}
