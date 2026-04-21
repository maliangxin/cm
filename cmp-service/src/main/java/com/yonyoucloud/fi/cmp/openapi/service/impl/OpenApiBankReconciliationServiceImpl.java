package com.yonyoucloud.fi.cmp.openapi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationSupportWayEnum;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.api.openapi.OpenApiBankReconciliationService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CmpBankReconciliationCharacterDef;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.BillClaimStatus;
import com.yonyoucloud.fi.cmp.cmpentity.BillProcessFlag;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataServiceImpl;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.enums.OrgConfirmBillEnum;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 银行对账单导入
 */
@Service
@Slf4j
public class OpenApiBankReconciliationServiceImpl implements OpenApiBankReconciliationService {

    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Autowired
    private BankAccountSettingService bankAccountSettingService;

    @Autowired
    private YmsOidGenerator ymsOidGenerator;

    @Autowired
    ICmpSendEventService cmpSendEventService;

    @Autowired
    CtmCmpCheckRepeatDataServiceImpl checkRepeatDataService;

    

    @Override
    public CtmJSONObject batchInsert(CtmJSONArray jsonArray) throws Exception {
        // 支持部分成功部分失败
        // 总条目数
        int total = jsonArray.size();
        Map<String, Object> cacheMap = new HashMap<>();
        Map<String, List<String>> accountCashMap = new HashMap<>();
        // 正常添加数据
        List<BankReconciliation> bankReconciliationlist = new ArrayList<>();
        // 异常数据
        List<CtmJSONObject> errormsgList = new ArrayList<>();
        // 成功数据
        List<CtmJSONObject> successInsertList = new ArrayList<>();
        // json转对象
        List<CtmJSONObject> paramList = jsonArray.toJavaList(CtmJSONObject.class);
        // Map<生成对象id,传入参数对象> 用于区分哪些成功哪些失败
        Map<String,CtmJSONObject> idParamMap = new HashMap<>();
        HashMap<String,BankReconciliation> bankReconciliationKeyMap = new HashMap<String,BankReconciliation>();
        for (CtmJSONObject ctmJSONObject : paramList) {
            BankReconciliation bankReconciliation;
            try {
                bankReconciliation = dealBankReconcliation(ctmJSONObject, cacheMap,accountCashMap);
                AccentityUtil.setAccentityRawToDtofromCtmJSONObject(ctmJSONObject, bankReconciliation);
                AccentityUtil.setAccentityRawToDtofromCtmJSONObject(ctmJSONObject, bankReconciliation);
                if(bankReconciliationKeyMap.containsKey(bankReconciliation.getConcat_info())){
                    continue;
                }else{
                    bankReconciliationKeyMap.put(bankReconciliation.getConcat_info(), bankReconciliation);
                }
                // 设置id
                long id = ymsOidGenerator.nextId();
                bankReconciliation.setId(id);
                ctmJSONObject.put("id",id);
                ctmJSONObject.put("smartcheckno",bankReconciliation.getSmartcheckno());
                idParamMap.put(id+"",ctmJSONObject);
                bankReconciliationlist.add(bankReconciliation);
            } catch (Exception e) {
                String msg = e.getMessage();
                ctmJSONObject.put("msg", msg);
                errormsgList.add(ctmJSONObject);
            }
        }
        // 校验是否重复
        // 交易流水号、会计主体、收付方向、本方银行账号、金额
        List<BankReconciliation> insertList = new ArrayList<>();
        List<String> errorIdList = new ArrayList<>();
        removeDuplicateData(insertList,errorIdList,bankReconciliationKeyMap);
        if (insertList.size() > 0) {
            CommonSaveUtils.saveBankReconciliation(insertList);
        }
        List<Object> insertidList = insertList.stream().map(bankReconciliation -> bankReconciliation.getId()+"").collect(Collectors.toList());
        for (Map.Entry<String, CtmJSONObject> entry : idParamMap.entrySet()) {
            String id = entry.getKey();
            CtmJSONObject value = entry.getValue();
            if (errorIdList.contains(id)){
                value.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F00069", "重复数据不允许新增！") /* "重复数据不允许新增！" */);
                errormsgList.add(value);
            }
            if (insertidList.contains(id)){
                successInsertList.add(value);
            }
        }
        int errornum = errormsgList.size();
        int successnum = total - errornum;
        CtmJSONObject result = new CtmJSONObject();
        result.put("total", total);
        result.put("errornum", errornum);
        result.put("successnum", successnum);
        result.put("faild", errormsgList);
        // 新增成功
        result.put("success", successInsertList);

        // 银行流水支持发送事件消息
        try {
            List<BankReconciliation> bizObjects = new ArrayList<>();
            for (CtmJSONObject ctmJSONObject : successInsertList) {
                BankReconciliation bizObject = new BankReconciliation();
                bizObject.init(ctmJSONObject);
                bizObjects.add(bizObject);
            }
            cmpSendEventService.sendEventByBankClaimBatch(bizObjects, EntityStatus.Insert.name());
        }catch (Exception e){
            log.error("流水发布事件中心错误！",e);
        }
        return result;
    }

//    private void removeDuplicateData(List<BankReconciliation> bankReconciliationlist,
//                                                         List<BankReconciliation> addList,List<String> errorList) throws Exception {
//        if (bankReconciliationlist.size() <= 0){
//            return;
//        }
//        Set<String> accentityarr = new HashSet<>();
//        Set<String> bank_seq_noarr = new HashSet<>();
//        Set<String> bankaccountarr = new HashSet<>();
//        Set<String> tran_datearr = new HashSet<>();
//        for (int i = 0; i < bankReconciliationlist.size(); i++) {
//            BankReconciliation bankReconciliation = bankReconciliationlist.get(i);
//            accentityarr.add(bankReconciliation.getAccentity());
//            bank_seq_noarr.add(bankReconciliation.getBank_seq_no());
//            bankaccountarr.add( bankReconciliation.getBankaccount());
//            String date = DateUtil.format(bankReconciliation.getTran_date(), "yyyy-MM-dd");
//            tran_datearr.add(date);
//        }
//        QuerySchema querySchema = QuerySchema.create().addSelect("*");
//        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
//        group.addCondition(QueryCondition.name("accentity").in(accentityarr));
//        group.addCondition(QueryCondition.name("bank_seq_no").in(bank_seq_noarr));
//        group.addCondition(QueryCondition.name("bankaccount").in(bankaccountarr));
//        group.addCondition(QueryCondition.name("tran_date").in(tran_datearr));
//        querySchema.addCondition(group);
//        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
//        Map<String, BankReconciliation> bankReconciliationMap = new HashMap<>();
//        if (!bankReconciliations.isEmpty()) {
//            bankReconciliations.forEach(bankReconciliation -> {
//                // key  交易流水号 + 会计主体 + 收付方向 + 本方银行账号 + 金额
//                String key = getKey(bankReconciliation);
//                bankReconciliationMap.put(key, bankReconciliation);
//            });
//        }
//        bankReconciliationlist.forEach(bankReconciliation -> {
//            String key = getKey(bankReconciliation);
//            if (bankReconciliationMap.get(key) == null) {
//                addList.add(bankReconciliation);
//            } else {
//                errorList.add(bankReconciliation.getId()+"");
//            }
//        });
//    }
    private void removeDuplicateData(List<BankReconciliation> addList,List<String> errorList, HashMap<String,BankReconciliation> bankReconciliationKeyMap) throws Exception {
        if (bankReconciliationKeyMap.size() <= 0){
            return;
        }
        Set<String> bankReconciliationKeySet = bankReconciliationKeyMap.keySet();
        QuerySchema schema = QuerySchema.create();
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        schema.addCondition(conditionGroup);
        String smartClassify = AppContext.getEnvConfig("cmp.smartClassify", "1");
        if ("1".equals(smartClassify)) {
            conditionGroup.addCondition(QueryConditionGroup.and(
                    QueryConditionGroup.or(QueryCondition.name("unique_no").in(bankReconciliationKeySet),
                            QueryCondition.name("concat_info").in(bankReconciliationKeySet))));
        } else {
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("concat_info").in(bankReconciliationKeySet)));
        }
        schema.addSelect(" id,bank_seq_no,tran_date,tran_time,tran_amt,dc_flag,bankaccount,acct_bal,unique_no,concat_info,to_acct_no,to_acct_name ");
        // 数据库中存在的数据
        List<BizObject> bizObjects = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        Map<String, BizObject> bizObjectMap = new HashMap<>();
        for (BizObject bizObject : bizObjects) {
            BankReconciliation bankReconciliation = (BankReconciliation) bizObject;
            if (!StringUtils.isEmpty(bankReconciliation.getUnique_no())) {
                bizObjectMap.put(bankReconciliation.getUnique_no(), bankReconciliation);
            }
            if (!StringUtils.isEmpty(bankReconciliation.getConcat_info())) {
                bizObjectMap.put(bankReconciliation.getConcat_info(), bankReconciliation);
            }
        }
        bankReconciliationKeySet.forEach(bankReconciliationKey -> {
            if (bizObjectMap.get(bankReconciliationKey) == null) {
                addList.add(bankReconciliationKeyMap.get(bankReconciliationKey));
            } else {
                errorList.add("" + bankReconciliationKeyMap.get(bankReconciliationKey).getId());
            }
        });
    }

    /**
     * 获取币种
     * @param currency
     * @return
     * @throws Exception
     */
    private CurrencyTenantDTO findCurrency(String currency) {
        try {
            CurrencyTenantDTO currencyDTO = null;
            String locale = InvocationInfoProxy.getLocale();
            String currencyKey = currency.concat(InvocationInfoProxy.getTenantid()).concat("DEPOSITINTERESTWITHHOLDINGAFTERQUERYRULE").concat(locale);
            if (null != AppContext.cache().getObject(currencyKey)) {
                currencyDTO = AppContext.cache().getObject(currencyKey);
            } else {
                currencyDTO = baseRefRpcService.queryCurrencyById(currency);
                if (null != currencyDTO) {
                    AppContext.cache().setObject(currencyKey, currencyDTO);
                }
            }
            return currencyDTO;
        }catch (Exception e){
            log.error("查询币种失败:",e);
        }
        return null;
    }

    private String getKey(BankReconciliation bankReconciliation) {
        String currency = bankReconciliation.getCurrency();
        CurrencyTenantDTO currencyTenantDTO = findCurrency(currency);
        BigDecimal tran_amt = bankReconciliation.getTran_amt();
        if (tran_amt != null && currencyTenantDTO != null){
            tran_amt = tran_amt.setScale(currencyTenantDTO.getMoneydigit());
        }
        return bankReconciliation.getBank_seq_no() +
                "," +
                bankReconciliation.getAccentity() +
                "," +
                bankReconciliation.getDc_flag() +
                "," +
                bankReconciliation.getBankaccount() +
                "," +
                tran_amt +
                "," +
                DateUtil.format(bankReconciliation.getTran_date(), "yyyy-MM-dd");
    }

    private BankReconciliation dealBankReconcliation(CtmJSONObject param, Map<String, Object> cacheMap,Map<String, List<String>> accountCashMap) throws Exception {
        /** 获取字段属性 */
        String bankaccount = param.getString("bankaccount");
        String statementno = param.getString("statementno");
        String bank_seq_no = param.getString("bank_seq_no");
        String thirdserialno = param.getString("thirdserialno");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date tran_date = null;
        try {
            tran_date = simpleDateFormat.parse(simpleDateFormat.format(param.getDate("tran_date")));
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101349"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180841", "对账单日期转化异常！") /* "对账单日期转化异常！" */);
        }
        Short dc_flag = param.getShort("dc_flag");
        BigDecimal tran_amt = param.getBigDecimal("tran_amt");
        BigDecimal acct_bal = param.getBigDecimal("acct_bal");
        String to_acct_no = param.getString("to_acct_no");
        String to_acct_name = param.getString("to_acct_name");
        String to_acct_bank = param.getString("to_acct_bank");
        String to_acct_bank_name = param.getString("to_acct_bank_name");
        String use_name = param.getString("use_name");
        String remark = param.getString("remark");
        String remark01 = param.getString("remark01");
        String bankcheckno = param.getString("bankcheckno");
        String accentity = param.getString("accentity");
        String currency = param.getString("currency");
        /** 新加的字段 */
        Short billprocessflag = param.getShort("billprocessflag");
        String enteraccounttype = param.getString("enteraccounttype");
        String enteraccountcode = param.getString("enteraccountcode");
        String enteraccountname = param.getString("enteraccountname");
        String note = param.getString("note");
        Date tran_time = null;
        if(StringUtils.isNotEmpty(String.valueOf(param.get("tran_time"))) ){
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            tran_time = param.getDate("tran_time");
            if (tran_time != null) {
                try {
                    tran_time = dateFormat.parse(dateFormat.format(tran_time));
                } catch (Exception e) {
                    log.error("对账单交易时间转化异常:{}", e.getMessage(), e);
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540054A", "对账单交易时间转化异常！") /* "对账单交易时间转化异常！" */);
                }
            }
        }

        ///** 判空操作 */
        //if (accentity == null) {
        //    throw new CtmException("账户使用组织编码不能为空！");
        //}
        if (bankaccount == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101351"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180848", "银行账户不能为空！") /* "银行账户不能为空！" */);
        }
//        if (bank_seq_no == null) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101352"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180849", "银行交易流水号不能为空！") /* "银行交易流水号不能为空！" */);
//        }
        if (tran_date == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101353"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418084C", "交易日期不能为空！") /* "交易日期不能为空！" */);
        }
        if (dc_flag == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101354"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418084D", "借贷标不能为空！") /* "借贷标不能为空！" */);
        }
        if (currency == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101355"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418083E", "币种不能为空！") /* "币种不能为空！" */);
        }

        BankReconciliation bankReconciliation = new BankReconciliation();
        /** 字段赋对应固定值 */
        String detailReceiptRelationCode = param.getString("detailReceiptRelationCode");
        bankReconciliation.setDetailReceiptRelationCode(detailReceiptRelationCode);
        bankReconciliation.setInitflag(false);//是否期初：否
        bankReconciliation.setLibraryflag(false);//是否来自事项库：否
        bankReconciliation.setCheckflag(false);//是否勾对：否
        bankReconciliation.setDataOrigin(DateOrigin.Created);//事项来源：外部导入
        bankReconciliation.setOther_checkflag(false);//其他模块是否已勾对：否
        bankReconciliation.setAutobill(false);//是否已自动生单：否
        bankReconciliation.setBillclaimstatus((short) 0);//认领状态：否
        bankReconciliation.setAutoassociation(false);//自动关联标志
        //mark by lichaor 20240408 和马良沟通过了，openApi进来的给什么参数就设置什么状态，这个例外处理，不去掉
        bankReconciliation.setAssociationstatus((short) 0);//业务关联状态：未关联
        bankReconciliation.setIsautocreatebill(false);//是否自动生单：否
        bankReconciliation.setIschoosebill(false);//是否选择生单单据：否
        bankReconciliation.setCreateDate(new Date());
        bankReconciliation.setCreateTime(new Date());
        bankReconciliation.setCreatorId(AppContext.getCurrentUser().getId());
        bankReconciliation.setCreator(AppContext.getCurrentUser().getName());
        bankReconciliation.setTran_time(tran_time);
        String uniqueNo = param.getString("unique_no");
        if (StringUtils.isNotEmpty(uniqueNo)) {
            bankReconciliation.setUnique_no(uniqueNo);
        }
        /** 新加的字段 */
        if (billprocessflag == null) {
            bankReconciliation.setBillprocessflag(BillProcessFlag.NeedDeal.getValue());
        } else if (billprocessflag.equals((short) 0) || billprocessflag.equals((short) 1) || billprocessflag.equals((short) 2)) {
            bankReconciliation.setBillprocessflag(billprocessflag);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101356"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180843", "回单处理标识错误！") /* "回单处理标识错误！" */);
        }

        /** 币种名称转成ID */
        if (cacheMap.get(currency) != null) {
            bankReconciliation.setCurrency(cacheMap.get(currency).toString());
        } else {
            CurrencyBdParams currencyBdParams = new CurrencyBdParams();
            currencyBdParams.setCode(currency);
            List<CurrencyTenantDTO> currencylist = baseRefRpcService.queryCurrencyByParams(currencyBdParams);
            if (currencylist == null || currencylist.size() == 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101357"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180846", "币种名称不存在！") /* "币种名称不存在！" */);
            }
            for (CurrencyTenantDTO currencyTenantDTO : currencylist) {
                if (currency.equals(currencyTenantDTO.getCode())) {
                    bankReconciliation.setCurrency(currencyTenantDTO.getId());//设置币种ID
                    cacheMap.put(currency, currencyTenantDTO.getId());
                    break;
                }
            }
        }

        if (bankReconciliation.getCurrency() == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101357"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180846", "币种名称不存在！") /* "币种名称不存在！" */);
        }

        //if (cacheMap.get(accentity) != null) {
        //    bankReconciliation.setAccentity(cacheMap.get(accentity).toString());
        //} else {
        //    /** 会计主体编码转ID */
        //    QuerySchema querySchema = QuerySchema.create().addSelect("id,code");
        //    querySchema.appendQueryCondition(QueryCondition.name("code").eq(accentity));
        //    List<Map<String, Object>> finList = MetaDaoHelper.query("aa.baseorg.OrgMV", querySchema);//会计主体的fullname
        //    if (finList == null || finList.size() == 0) {
        //        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101358"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418084A", "会计主体编码不存在！") /* "会计主体编码不存在！" */);
        //    }
        //    for (Map<String, Object> finOrg : finList) {
        //        if (accentity.equals(finOrg.get("code").toString())) {
        //            bankReconciliation.setAccentity(finOrg.get("id").toString());
        //            cacheMap.put(accentity, finOrg.get("id").toString());
        //            break;
        //        }
        //    }
        //}
        //if (bankReconciliation.getAccentity() == null) {
        //    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101358"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418084A", "会计主体编码不存在！") /* "会计主体编码不存在！" */);
        //}

        /** 银行账户名称转成ID */
        if (cacheMap.get(bankaccount) != null) {
            EnterpriseBankAcctVO tmpBankAcctVO = (EnterpriseBankAcctVO)cacheMap.get(bankaccount);
            bankReconciliation.setBankaccount(tmpBankAcctVO.getId());
            // 所属组织
            bankReconciliation.setOrgid(tmpBankAcctVO.getOrgid());
            //// 确认组织节点 银行对账单
            //bankReconciliation.setConfirmbill(OrgConfirmBillEnum.CMP_BANKRECONCILIATION.getIndex());
            //// 确认状态
            //bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
        } else {
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setAccount(bankaccount);
            List<EnterpriseBankAcctVO> enterpriselist = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
            if (enterpriselist == null || enterpriselist.size() <= 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101359"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418083D", "银行账户名称不存在！") /* "银行账户名称不存在！" */);
            }
            if (enterpriselist.size() > 1) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FD7C4DA04700005", "依据银行账号匹配到多个满足条件的银行账户档案，保存失败，请检查！") /* "依据银行账号匹配到多个满足条件的银行账户档案，保存失败，请检查！:" */);
            }
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriselist.get(0);
            //for (EnterpriseBankAcctVO enterpriseBankAcctVO : enterpriselist) {
            //    if (bankaccount.equals(enterpriseBankAcctVO.getAccount())) {
            bankReconciliation.setBankaccount(enterpriseBankAcctVO.getId());//设置银行账户ID
            // 账户名称 账户id
            cacheMap.put(bankaccount, enterpriseBankAcctVO);
            // 账户id 币种集合
            List<String> currencyList = enterpriseBankAcctVO.getCurrencyList().stream().map(c -> c.getCurrency()).collect(Collectors.toList());
            accountCashMap.put(enterpriseBankAcctVO.getId(),currencyList);
            // 所属组织
            bankReconciliation.setOrgid(enterpriseBankAcctVO.getOrgid());
                    //// 确认组织节点 银行对账单
                    //bankReconciliation.setConfirmbill(OrgConfirmBillEnum.CMP_BANKRECONCILIATION.getIndex());
                    //// 确认状态
                    //bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
                    //break;
            //    }
            //}
        }

        String accentityId = null;
        if (!StringUtils.isEmpty(accentity)) {
            accentityId = QueryBaseDocUtils.getAccentityIdByCode(accentity);
        }
        BankreconciliationUtils.getAndSetAuthoruseaccentityRelationCol(accentityId, bankReconciliation.getBankaccount(), bankReconciliation);

        //if (bankReconciliation.getBankaccount() == null) {
        //    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101360"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418083F", "银行账户名称不存在或银行账户与会计主体不匹配！") /* "银行账户名称不存在或银行账户与会计主体不匹配！" */);
        //}

        // 如果当前币种不在账户币种列表里报错
        if (!accountCashMap.get(bankReconciliation.getBankaccount()).contains(bankReconciliation.getCurrency())){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101361"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508002B", "币种不在账户中！") /* "币种不在账户中！" */);
        }


        //if (bankAccountSettingService.getOpenFlagByBankAccountIdOfQuery(bankReconciliation.getBankaccount())) {
        //    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101362"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508002C", "直联账户不允许导入银行对账单！") /* "直联账户不允许导入银行对账单！" */);
        //}
        /** 其他必填信息 */
        bankReconciliation.setBank_seq_no(bank_seq_no);//银行交易流水号
        bankReconciliation.setThirdserialno(thirdserialno);//第三方流水号
        bankReconciliation.setDzdate(tran_date);
        bankReconciliation.setTran_date(tran_date);//交易日期
        if (dc_flag == (short) 1) {
            bankReconciliation.setDc_flag(Direction.Debit);//借
            bankReconciliation.setDebitamount(tran_amt);//借方金额等于交易金额
        } else if (dc_flag == (short) 2) {
            bankReconciliation.setDc_flag(Direction.Credit);//贷
            bankReconciliation.setCreditamount(tran_amt);//贷方金额等于交易金额
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101363"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180844", "借贷标错误，只能为(short)1或(short)2！") /* "借贷标错误，只能为(short)1或(short)2！" */);
        }
        bankReconciliation.setTran_amt(tran_amt);//交易金额
        bankReconciliation.setAcct_bal(acct_bal);//余额

        /** 额外非必填信息 */
        if (statementno != null) {
            bankReconciliation.setStatementno(statementno);//对账单行号
        }
        if (to_acct_no != null) {
            bankReconciliation.setTo_acct_no(to_acct_no);//对方账号
        }
        if (to_acct_name != null) {
            bankReconciliation.setTo_acct_name(to_acct_name);//对方户名
        }
        if (to_acct_bank != null) {
            bankReconciliation.setTo_acct_bank(to_acct_bank);//对方开户行
        }
        if (to_acct_bank_name != null) {
            bankReconciliation.setTo_acct_bank_name(to_acct_bank_name);//对方开户行名
        }
        if (use_name != null) {
            bankReconciliation.setUse_name(use_name);//用途
        }
        if (remark != null) {
            bankReconciliation.setRemark(remark);//摘要
        }

        if (remark01 != null) {
            bankReconciliation.setRemark01(remark01);//摘要
        }

        if (bankcheckno != null) {
            bankReconciliation.setBankcheckno(bankcheckno);//银行对账编号
        }
        /** 新加的字段 */
        if (enteraccounttype != null) {
            bankReconciliation.setEnteraccounttype(enteraccounttype);//入账方类型
        }
        if (enteraccountcode != null) {
            bankReconciliation.setEnteraccountcode(enteraccountcode);//入账方编码
        }
        if (enteraccountname != null) {
            bankReconciliation.setEnteraccountname(enteraccountname);//入账方名称
        }
        if (note != null) {
            bankReconciliation.setNote(note);//备注
        }
        // 未解析出财资统一码，生成财资统一码并进行设置
        bankReconciliation.setSmartcheckno(RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate());
        bankReconciliation.setIsparsesmartcheckno(BooleanUtils.toBoolean(ReconciliationSupportWayEnum.GENERATION_OR_ASSOCIATION.getValue()));
        /** 按照平台保存前后规则进行保存 */
        bankReconciliation.setEntityStatus(EntityStatus.Insert);
        bankReconciliation.set("_fromApi", true);//从openApi导入
        //特征
        if (param.getObject("characterDef", LinkedHashMap.class) != null) {
            BizObject bizObject = Objectlizer.convert(param.getObject("characterDef", LinkedHashMap.class), CmpBankReconciliationCharacterDef.ENTITY_NAME);
            bizObject.put("id", String.valueOf(IdCreator.getInstance().nextId()));
            //设置特征状态
            bizObject.setEntityStatus(EntityStatus.Insert);
            bankReconciliation.put("characterDef", bizObject);
        }
        String concatInfo = checkRepeatDataService.formatConctaInfoBankReconciliation(bankReconciliation);
        bankReconciliation.fillConcatInfo(concatInfo);
        return bankReconciliation;
    }

    @Override
    public CtmJSONObject queryBankReconciliation(CtmJSONObject param) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id,accentity,accentity.code as accentity_code,accentity.name as accentity_name,tran_amt , " +
                "bankaccount,bankaccount.acctName as bankaccount_acctName,bankaccount.account as bankaccount_account,tran_date,tran_time,dc_flag," + "currency,currency.name as currency_name," +
                "amounttobeclaimed,to_acct_no,to_acct_name,to_acct_bank,to_acct_bank_name,remark ,bankcheckno, " +
                "note, orgid, bank_seq_no, use_name, remark01, banktype.name as banktype_name, bankaccount.bankNumber.name as bankaccount_bankNumber_name ");

        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        //根据api查询条件拼装
        //会计主体
        if (param.getString("accentity") != null) {
            group.addCondition(QueryCondition.name("accentity").eq(param.getString("accentity")));
        } else { //未传id，则根据会计主体编码查询
            if (param.getString("accentity_code") != null) {
                group.addCondition(QueryCondition.name("accentity.code").eq(param.getString("accentity_code")));
            }
        }
        //本方银行账户
        if (param.getString("bankaccount") != null) {
            group.addCondition(QueryCondition.name("bankaccount").eq(param.getString("bankaccount")));
        } else { //未传id，则根据本行银行账户的账号
            if (param.getString("bankaccount_account") != null) {
                group.addCondition(QueryCondition.name("bankaccount.account").eq(param.getString("bankaccount_account")));
            }
        }

        //交易日期
        if (param.getString("tran_date") != null) {
            group.addCondition(QueryCondition.name("tran_date").eq(param.getString("tran_date")));
        }
        if (param.getString("tran_date") == null) {
            String begindate = param.getString("begindate");
            String enddate = param.getString("enddate");
            group.addCondition(QueryCondition.name("tran_date").between(begindate, enddate));
        }
        //借贷方向
        if (param.get("dc_flag") != null) {
            group.addCondition(QueryCondition.name("dc_flag").eq(param.getInteger("dc_flag")));
        }
        //默认查待认领的
        if (param.get("status") == null) {
            group.addCondition(QueryCondition.name("billclaimstatus").eq(BillClaimStatus.ToBeClaim.getValue()));
            group.addCondition(QueryCondition.name("ispublish").eq(true));
        } else if (param.get("status") != null && param.get("status").equals("all")){
            //查询所有,不加条件
        }
        querySchema.addCondition(group);
        //对账单日期倒序
        querySchema.addOrderBy(new QueryOrderby("tran_date", "desc"));
        querySchema.addPager(param.getInteger("pageIndex"), param.getInteger("pageSize"));
        List<Map<String, Object>> infoMapList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchema);

        //解析查询数据
        List<CtmJSONObject> resultList = new ArrayList<>();
        for (Map<String, Object> b : infoMapList) {
            QuerySchema queryElecSchema = QuerySchema.create().addSelect(" remark01 ");
            QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name("bankreconciliationid").eq(b.get("id").toString()));
            queryElecSchema.addCondition(queryConditionGroup);
            List<BankElectronicReceipt> bankElectronicReceiptlist = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, queryElecSchema, null);
            if (null != bankElectronicReceiptlist && bankElectronicReceiptlist.size() > 0) {
                b.put("remak01", bankElectronicReceiptlist.get(0).getRemark01());
            }
            CtmJSONObject r = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(b));
            // 处理tran_date
            Object tran_time = r.get("tran_time");
            String tran_date = null;
            if (tran_time != null) {
                tran_date = tran_time.toString();
            } else {
                if (r.get("tran_date") != null) {
                    tran_date = r.get("tran_date").toString() + " 00:00:00";
                }
            }
            r.put("tran_date", tran_date);
            r.remove("tran_time");
            resultList.add(r);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("code", 200);
        result.put("message", "success");
        CtmJSONObject recordList = new CtmJSONObject();
        recordList.put("recordList", resultList);
        result.put("data", recordList);
        return result;
    }


}
