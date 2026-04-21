package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationQueryService;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting_b;
import com.yonyoucloud.fi.cmp.cmpcheck.service.CmpCheckService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmCmpBankReconciliationSettingRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.*;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.converter.BankReconciliationSettingConverter;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.BatchCheckParam;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.ReconciliationPlanParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 银企对账设置
 *
 * @author xuwei
 * @date 2024/01/26
 */
@Service
@Slf4j
public class CtmCmpBankReconciliationSettingRpcServiceImpl implements CtmCmpBankReconciliationSettingRpcService {

    @Autowired
    private BankReconciliationSettingConverter bankReconciliationSettingConverter;
    @Autowired
    private BankReconciliationQueryService bankReconciliationQueryService;
    @Autowired
    private ApplicationContext applicationContext;
    private CmpCheckService getCmpCheckService() {
        return applicationContext.getBean(CmpCheckService.class);
    }

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
        try {
            String[] bankAccountPermissions =  getCmpCheckService().getBankAccountDataPermission(IServicecodeConstant.BANKRECONCILIATION);
            if(bankAccountPermissions != null && bankAccountPermissions.length > 0){
                conditionGroup.appendCondition(QueryCondition.name("bankaccount").in(Arrays.asList(bankAccountPermissions)));
            }
        }catch (Exception e){
            log.error("获取数据权限时错误" + e);
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> itemList = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        if (itemList.isEmpty()) {
            return new ArrayList<>();
        }
        return toVos(itemList);
    }

    @Override
    public BankReconciliationPlanVO find(ReconciliationPlanParam param) throws Exception {
        String useOrg = param.getUseOrg();
        String bankAccount = param.getBankAccount();
        String currency = param.getCurrency();
        if (StringUtils.isEmpty(useOrg)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "useOrg不能为空！"));
        }
        if (StringUtils.isEmpty(bankAccount)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "bankAccount不能为空！"));
        }
        if (StringUtils.isEmpty(currency)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "currency不能为空！"));
        }

        QuerySchema schema = QuerySchema.create().addSelect("mainid");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("useorg").eq(useOrg));
        conditionGroup.appendCondition(QueryCondition.name("bankaccount").eq(bankAccount));
        conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
        conditionGroup.appendCondition(QueryCondition.name("enableStatus_b").eq(1));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> itemList = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        if (itemList.isEmpty()) {
            return null;
        }
        Long mainId = (Long) itemList.get(0).get("mainid");
        BizObject byId = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, mainId);
        Integer reconciliationdatasource = byId.get("reconciliationdatasource");
        if (reconciliationdatasource != 1) {
            return null;
        }
        return toMainVo(byId);
    }

    @Override
    public CheckResultVO batchCheck(BatchCheckParam batchCheckParam) {
        CheckResultVO checkResultVO = new CheckResultVO(0, 0, 0, null);

        //勾兑号总数
        Integer totalCheckNo = getTotalCheckNo(batchCheckParam);
        checkResultVO.setTotal(totalCheckNo);
        //过滤出有效数据
        Map<String, Object> validData = filterValidData(batchCheckParam);
        //无效的勾兑号
        Set<String> invalidCheckNo = (Set<String>) validData.get("invalidCheckNo");
        //无效勾兑号的数量
        Integer invalidNo = invalidCheckNo.size();
        checkResultVO.setErrorNum(invalidNo);
        //成功勾兑的数量
        checkResultVO.setSuccessNum(totalCheckNo - invalidNo);
        //无效勾兑号列表
        List<CheckFailVO> errList = new ArrayList<>();
        for (String s : invalidCheckNo) {
            CheckFailVO checkFailVO = new CheckFailVO(s,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540056E", "未查询到银行对账单数据！") /* "未查询到银行对账单数据！" */);
            errList.add(checkFailVO);
        }
        checkResultVO.setErrorList(errList);

        //可以勾兑的数据
        BatchCheckParam validCheckData = (BatchCheckParam) validData.get("batchCheckParam");
        if (validCheckData == null) {
            return checkResultVO;
        }
        //开始勾兑数据
        checkData(validCheckData);
        return checkResultVO;
    }

    private Integer getTotalCheckNo(BatchCheckParam batchCheckParam) {
        List<VoucherVO> voucherList = batchCheckParam.getVoucherList();
        HashSet<String> checkNoSet = new HashSet<>();
        for (VoucherVO voucherVO : voucherList) {
            String checkNo = voucherVO.getCheckNo();
            checkNoSet.add(checkNo);
        }
        Integer size = checkNoSet.size();
        return size;
    }

    /**
     * 勾兑数据
     *
     * @param validData 有效数据
     */
    private void checkData(BatchCheckParam validData) {
        List<BankBillVO> bankBillVOList = validData.getBankBillList();
        Boolean checkFlag = validData.getCheckFlag();
        //更新凭证
        updateVoucher(validData, checkFlag);
        //更新银行对账单
        updateBankBill(bankBillVOList, checkFlag,validData.getReconciliationPlanId());
    }

    private void updateBankBill(List<BankBillVO> bankBillVOList, Boolean checkFlag,String reconciliationPlanId) {
        if(bankBillVOList.isEmpty()){
            return;
        }
        //更新对账单
        try {
            List<BankReconciliation> list = new ArrayList<>();
            for (BankBillVO bankBillVO : bankBillVOList) {
                BankReconciliation bankReconciliation = new BankReconciliation();
                bankReconciliation.setId(Long.valueOf(bankBillVO.getBankBillId()));
                bankReconciliation.setOther_checkdate(bankBillVO.getCheckDate());
                bankReconciliation.setOther_checkno(bankBillVO.getCheckNo());
                bankReconciliation.setOther_checkflag(checkFlag);
                bankReconciliation.setGl_bankreconciliationsettingid(reconciliationPlanId);
                bankReconciliation.setEntityStatus(EntityStatus.Update);
                list.add(bankReconciliation);
            }
            // 修改完成
            CommonSaveUtils.updateBankReconciliation(list);
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102572"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "更新对账单异常！"));
        }
    }

    private void updateVoucher(BatchCheckParam batchCheckParam, Boolean checkFlag) {
        //勾兑凭证
        List<VoucherVO> voucherList = batchCheckParam.getVoucherList();
        if(voucherList.isEmpty()){
            return;
        }
        CtmJSONObject jsonobject = new CtmJSONObject();
        List<Map<String, Object>> dataList = new ArrayList<>();
        jsonobject.put("checkflag", checkFlag);
        for (VoucherVO voucher : voucherList) {
            Map<String, Object> data = new HashMap<>();
            data.put("voucherbid", voucher.getVoucherbId());
            data.put("tradetype", voucher.getTradeType());
            data.put("checkno", voucher.getCheckNo());
            data.put("bankreconciliationsettingid", batchCheckParam.getReconciliationPlanId());
            data.put("ts", voucher.getVoucherts());
            dataList.add(data);
        }
        jsonobject.put("checkinfo", dataList);
        String serverUrl = AppContext.getEnvConfig("yzb.base.url");
        String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/cash/updatecheckflag";
        String thd_userId = AppContext.getCurrentUser().getYhtUserId();
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("thd_userId", thd_userId);
        String str = HttpTookit.doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(jsonobject), header, "UTF-8");
        CtmJSONObject result = CtmJSONObject.parseObject(str);
        Boolean successFlag = (Boolean) result.get("success");
        if (!successFlag) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101330"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A5", "更新总账凭证勾兑状态失败，总账报错：") + result.get("message"));
        }
    }

    /**
     * 过滤有效数据
     *
     * @param batchCheckParam 入参
     * @return 有效数据
     */
    private Map<String, Object> filterValidData(BatchCheckParam batchCheckParam) {
        List<VoucherVO> voucherVOList = batchCheckParam.getVoucherList();
        List<BankBillVO> bankBillVOList = batchCheckParam.getBankBillList();
        if (bankBillVOList.isEmpty() || voucherVOList.isEmpty()) {
            return null;
        }
        //过滤可以查询到的银行对账单数据
        List<BankReconciliation> validBankReconciliations = filterBankBillData(batchCheckParam);

        //对比过滤出，不能勾兑的勾兑号
        HashSet<String> invalidCheckNo = filterInvalidCheckNo(bankBillVOList, validBankReconciliations);

        //两边列表分别移除不能响应勾兑号的数据，得到真正勾兑的数据
        List<VoucherVO> finalVoucherList = new ArrayList<>();
        List<BankBillVO> finalBankBillList = new ArrayList<>();
        if (!invalidCheckNo.isEmpty()) {
            //如果不在无效的checkNo中，就提取出来
            for (VoucherVO voucherVO : voucherVOList) {
                String checkNo = voucherVO.getCheckNo();
                if (!invalidCheckNo.contains(checkNo)) {
                    finalVoucherList.add(voucherVO);
                }
            }
            //同上
            for (BankBillVO bankBillVO : bankBillVOList) {
                String checkNo = bankBillVO.getCheckNo();
                if (!invalidCheckNo.contains(checkNo)) {
                    finalBankBillList.add(bankBillVO);
                }
            }
            batchCheckParam.setBankBillList(finalBankBillList);
            batchCheckParam.setVoucherList(finalVoucherList);
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("batchCheckParam", batchCheckParam);
        map.put("invalidCheckNo", invalidCheckNo);
        return map;
    }

    private HashSet<String> filterInvalidCheckNo(List<BankBillVO> bankBillVOList, List<BankReconciliation> validBankReconciliations) {
        //查询到的数据和入参的数据对比，最后得出不能勾兑的数据的勾兑号
        HashSet<String> bankBillIdSet = new HashSet<>();
        for (BankReconciliation validBankReconciliation : validBankReconciliations) {
            Long id = validBankReconciliation.getId();
            bankBillIdSet.add(id.toString());
        }

        HashSet<String> checkNoSet = new HashSet<>();
        for (BankBillVO bankBillVO : bankBillVOList) {
            String bankBillId = bankBillVO.getBankBillId();
            if (!bankBillIdSet.contains(bankBillId)) {
                checkNoSet.add(bankBillVO.getCheckNo());
            }
        }
        return checkNoSet;
    }

    /**
     * 查询银行对账单数据
     *
     * @param batchCheckParam 银行对账单
     */
    private List<BankReconciliation> filterBankBillData(BatchCheckParam batchCheckParam) {

        List<BankBillVO> bankBillList = batchCheckParam.getBankBillList();
        List<String> ids = bankBillList.stream().map(BankBillVO::getBankBillId).collect(Collectors.toList());

        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("id").in(ids)));
        //勾兑的需要过滤未勾兑的，未勾兑的需要过滤勾兑的
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("other_checkflag").eq(!batchCheckParam.getCheckFlag())));
        schema.addCondition(conditionGroup);

        //查询银行对账单
        List<BankReconciliation> bankReconciliations;
        try {
            bankReconciliations = bankReconciliationQueryService.queryBySchema(schema);
            return bankReconciliations;
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102573"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "查询银行对账单异常！"));
        }
    }

    /**
     * 设置为私有方法，之前放在api包里，位置不正确
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
//            bankReconciliationSettingVO.setUseOrg((String) item.get("useorg"));
//            bankReconciliationSettingVO.setUseOrgName((String) item.get("useorg_name"));
            //资金组织适配 ，授权使用组织useorg替换为accentityRaw核算会计主体
            bankReconciliationSettingVO.setUseOrg((String) item.get("accentityRaw"));
            bankReconciliationSettingVO.setUseOrgName((String) item.get("accentityRaw_name"));
            bankReconciliationSettingVO.setAccBook((String) item.get("accbook_b"));
            bankReconciliationSettingVO.setAccBookName((String) item.get("accbook_b_name"));
            bankReconciliationSettingVO.setEnableDate((Date) item.get("enableDate"));
            bankReconciliationSettingVO.setEnableStatus(item.get("enableStatus_b") == null ? (short)1:Short.parseShort(item.get("enableStatus_b").toString()));
            list.add(bankReconciliationSettingVO);
        }
        return list;
    }

    private BankReconciliationPlanVO toMainVo(Map<String, Object> objectMap) {
        BankReconciliationPlanVO bankReconciliationPlanVO = new BankReconciliationPlanVO();
        //主表字段
        Long id = (Long) objectMap.get("id");
        bankReconciliationPlanVO.setReconciliationPlanId(id);
        String accentity = (String) objectMap.get("accentity");
        bankReconciliationPlanVO.setAccentity(accentity);
        //子表字段
        List<Map<String, Object>> itemList = (List<Map<String, Object>>) objectMap.get("bankReconciliationSetting_b");
        List<BankReconciliationSettingVO> itemVos = toVos(itemList);
        bankReconciliationPlanVO.setCheckInfo(itemVos);
        return bankReconciliationPlanVO;
    }

}
