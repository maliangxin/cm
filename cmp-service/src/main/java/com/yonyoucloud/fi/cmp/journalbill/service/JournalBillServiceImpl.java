package com.yonyoucloud.fi.cmp.journalbill.service;

import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.bizdoc.service.settlemethod.ISettleMethodQueryService;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.EnterpriseCashVO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.BdRequestParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.basedoc.service.itf.IExchangeRateTypeService;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.common.ResultList;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.orgs.FinOrgQueryServiceComponent;
import com.yonyoucloud.fi.basecom.service.FIBillService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.JournalBillPaymentType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IFieldConstant;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill_b;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class JournalBillServiceImpl implements JournalBillService {

    /**
     * 保存态
     */
    private static final int SAVE_ACTION = 0;

    /**
     * 提交态
     */
    private static final int SUBMIT_ACTION = 1;

    private static final String JOURNAL_BILL_B_KEY = "journalBill_b";
    private static final String BILL_STATE = "billState";
    private static final String BILL_DATE = "billDate";
    private static final String TRADE_TYPE = "tradeType";
    private static final String CASH_ACCOUNT = "cashAccount";
    private static final String BANK_ACCOUNT = "bankAccount";
    public static final String CODE_LIST = "codeList";
    public static final String OPPOSITE_ID = "oppositeId";
    public static final String OPPOSITE_CODE = "oppositeCode";
    public static final String OPPOSITE_ACCOUNT_CODE = "oppositeAccountCode";
    public static final String OPPOSITE_ACCOUNT_ID = "oppositeAccountId";
    public static final String OPPOSITE_ACCOUNT_NO = "oppositeAccountNo";
    public static final String OPPOSITE_TYPE = "oppositeType";
    public static final String OPPOSITE_NAME = "oppositeName";
    public static final String OPPOSITE_ACCOUNT_NAME = "oppositeAccountName";
    public static final String NOTE_DATE = "noteDate";
    public static final String NOTE_NO = "noteNo";
    public static final String PROJECT = "project";
    public static final String COST_PROJECT = "costProject";
    public static final String UNIRECONCILIATIONCODE = "unireconciliationcode";
    public static final String RESULT = "result";
    public static final String SUCCESS_COUNT = "successCount";
    public static final String MSG = "msg";
    public static final String NOTE_TYPE = "noteType";
    public static final String EXCHANGE_RATE = "exchangeRate";
    public static final String EXCHANGE_RATE_OPS = "exchangeRateOps";
    public static final String SETTLE_MODE = "settleMode";
    public static final String INDEX = "index";
    public static final String IS_ERROR = "isError";

    @Autowired
    private FinOrgQueryServiceComponent finOrgQueryService;

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Autowired
    FIBillService fiBillService;

    @Autowired
    private IExchangeRateTypeService exchangeRateTypeService;

    @Autowired
    ISettleMethodQueryService settleMethodQueryService;

    @Autowired
    private TransTypeQueryService transTypeQueryService;

    @Override
    public CtmJSONObject batchSaveOrUpdateForOpenApi(CtmJSONObject param) throws Exception {
        CtmJSONArray arrayData = param.getJSONArray("data");
        if (param.isEmpty()) {
            CtmJSONObject resmap = new CtmJSONObject();
            resmap.put("code", 999);
            resmap.put(MSG, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540068A", "参数不能为空！") /* "参数不能为空！" */);
            return resmap;
        }
        List<CtmJSONObject> paramList = arrayData.toJavaList(CtmJSONObject.class);
        List<List<JournalBill>> journalBillList = null;
        try {
            journalBillList = convertJsonToJournalBill(paramList);
        } catch (CtmException e) {
            log.error("process param failed, ", e);
            throw e;
        } catch (Exception e) {
            log.error("process param failed, ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105035"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_219F655804C80008", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400690", "入参错误") /* "入参错误" */) /* "入参错误" */, e);
        }
        List<JournalBill> journalBillSaveList = journalBillList.get(0);
        List<JournalBill> journalBillSubmitList = journalBillList.get(1);
        List<JournalBill> journalBillAllList = Stream.concat(journalBillSaveList.stream(), journalBillSubmitList.stream()).collect(Collectors.toList());
        ResultList saveResultList = new ResultList();
        if (CollectionUtils.isNotEmpty(journalBillAllList)) {
            BillDataDto billSave = new BillDataDto();
            billSave.setBillnum(IBillNumConstant.JOURNAL_BILL);
            billSave.setData(journalBillAllList);
            billSave.setAction(OperationTypeEnum.SAVE.getValue());
            saveResultList = fiBillService.batchSave(billSave);
        }
        Map<Integer, CtmJSONObject> saveIndexToMsgObjMap = getIndexToMsgObjMap(journalBillAllList, saveResultList);
        Set<Integer> saveSuccessIndexSet = saveIndexToMsgObjMap.entrySet().stream()
                .filter(item -> !item.getValue().getBoolean(IS_ERROR))
                .map(item -> item.getKey()).collect(Collectors.toSet());
        List<JournalBill> successSaveBillIdForSubmitList = journalBillSubmitList.stream().filter(item -> saveSuccessIndexSet.contains(item.get(INDEX))).collect(Collectors.toList());
        // 重新查询最新数据
        List<JournalBill> saveSuccessForSubmitList = new ArrayList<>();
        for (JournalBill journalBill :successSaveBillIdForSubmitList) {
            Integer index = journalBill.get(INDEX);
            Long id = saveIndexToMsgObjMap.get(index).getLong(ICmpConstant.ID);
            JournalBill newJournalBill = MetaDaoHelper.findById(JournalBill.ENTITY_NAME, id, 2);
            newJournalBill.set(INDEX, index);
            saveSuccessForSubmitList.add(newJournalBill);
        }

        ResultList submitResultList = new ResultList();
        if (CollectionUtils.isNotEmpty(saveSuccessForSubmitList)) {
            BillDataDto billSubmit = new BillDataDto();
            billSubmit.setBillnum(IBillNumConstant.JOURNAL_BILL);
            billSubmit.setData(saveSuccessForSubmitList);
            billSubmit.setAction(OperationTypeEnum.SUBMIT.getValue());
            submitResultList = fiBillService.batchsubmit(billSubmit);
        }
        Map<Integer, CtmJSONObject> submitIndexToMsgObjMap = getIndexToMsgObjMap(saveSuccessForSubmitList, submitResultList);

        Map<Integer, CtmJSONObject> allIndexToMsgObjMap = new HashMap<>();
        allIndexToMsgObjMap.putAll(saveIndexToMsgObjMap);
        allIndexToMsgObjMap.putAll(submitIndexToMsgObjMap);

        CtmJSONObject resmap = new CtmJSONObject();
        resmap.put(ICmpConstant.CODE, 200);
        List<CtmJSONObject> resultList = new ArrayList<>();
        for (int i = 0; i < paramList.size(); i++) {
            CtmJSONObject jsonObject = allIndexToMsgObjMap.get(i);
            jsonObject.remove(IS_ERROR);
            resultList.add(jsonObject);
        }
        resmap.put(RESULT, resultList);
        return resmap;
    }

    private Map<Integer, CtmJSONObject> getIndexToMsgObjMap(List<JournalBill> journalBillList, ResultList resultList) {
        Map<Integer, CtmJSONObject> idToMessageObjMap = new HashMap<>();
        Set<Integer> successIndexSet = resultList.getInfos().stream().map(item -> Integer.valueOf(((Map<String, Object>) item).get(INDEX).toString())).collect(Collectors.toSet());
        int successPos = 0, failPos = 0;
        for (int i = 0; i < journalBillList.size(); i++) {
            Integer index = journalBillList.get(i).get(INDEX);
            CtmJSONObject msg = new CtmJSONObject();
            if (successIndexSet.contains(index)) {
                Map<String, Object> infoObj = (Map<String, Object>) resultList.getInfos().get(successPos++);
                msg.put(ICmpConstant.CODE, infoObj.get(ICmpConstant.CODE));
                msg.put(ICmpConstant.ID, infoObj.get(ICmpConstant.ID));
                msg.put(IS_ERROR, Boolean.FALSE);
            } else {
                String errorMsg = resultList.getMessages().get(failPos++).toString();
                msg.put("errorMessage", errorMsg);
                msg.put(IS_ERROR, Boolean.TRUE);
            }
            idToMessageObjMap.put(index, msg);
        }
        return idToMessageObjMap;
    }

    private List<List<JournalBill>> convertJsonToJournalBill(List<CtmJSONObject> journalBillJsonList) throws Exception {
        List<JournalBill> journalBillSaveList = new ArrayList<>();
        List<JournalBill> journalBillSubmitList = new ArrayList<>();

        checkJournalBillBExists(journalBillJsonList);
        List<String> bankAcctCodeOrAccountOrIdList = getBankAcctCodeOrAccountOrIdList(journalBillJsonList);
        List<String> cashAcctCodeOrIdList = getCashAcctCodeOrIdList(journalBillJsonList);
        List<String> currencyCodeList = getCurrencyCodeOrIdList(journalBillJsonList);
        List<String> exchangeRateTypeCodeList = getExchangeRateTypeCodeList(journalBillJsonList);
        List<String> deptCodeList = getDeptCodeList(journalBillJsonList);
        List<String> settleModeCodeList = getSettleModeCodeList(journalBillJsonList);
        Map<String, String> bankAcctCodeToIdMap = getBankAcctCodeToIdMap(bankAcctCodeOrAccountOrIdList);
        Map<String, String> bankAcctToIdMap = getBankAcctToIdMap(bankAcctCodeOrAccountOrIdList);
        Map<String, String> cashAcctToIdMap = getCashAcctCodeToIdMap(cashAcctCodeOrIdList);
        Map<String, String> currencyCodeToIdMap = getCurrencyCodeToId(currencyCodeList);
        Map<String, String> exchangeRateTypeCodeToIdNap = getExchangeRateTypeCodeToIdMap(exchangeRateTypeCodeList);
        String defaultExchangeRateType = QueryBaseDocUtils.queryExchangeRateTypeByIdOrCode("01").get(0).get("id").toString();
        Map<String, String> deptCodeToIdMap = getDeptCodeToIdMap(deptCodeList);
        Map<String, String> settleModeCodeToIdMap = getSettleModeCodeToIdMap(settleModeCodeList);

        int index = 0;
        for (CtmJSONObject journalBillJson : journalBillJsonList) {
            JournalBill journalBill = new JournalBill();
            journalBill.set(INDEX, index++);
            journalBill.setId(journalBillJson.getLong(ICmpConstant.ID));
            journalBill.setCode(journalBillJson.getString(ICmpConstant.CODE));
            setEntityStatus(journalBill);
            processBillState(journalBillJson, journalBillSaveList, journalBill, journalBillSubmitList);
            setAccentyAndAccentyRaw(journalBillJson, journalBill);
            journalBill.setBillDate(DateUtils.parseDate(journalBillJson.getString(BILL_DATE)));
            processTradeType(journalBillJson, journalBill);
            setDept(journalBillJson, journalBill, deptCodeToIdMap);
            CtmJSONArray journalBillBArrayJson = journalBillJson.getJSONArray(JOURNAL_BILL_B_KEY);
            List<CtmJSONObject> journalBillBJsonList = journalBillBArrayJson.toJavaList(CtmJSONObject.class);
            List<JournalBill_b> journalBillBList = new ArrayList<>();
            journalBill.setJournalBill_b(journalBillBList);
            for (CtmJSONObject journalBillBJson : journalBillBJsonList) {
                JournalBill_b journalBillB = new JournalBill_b();
                journalBillB.setDebitoriSum(journalBillBJson.getBigDecimal("debitoriSum"));
                journalBillB.setCreditoriSum(journalBillBJson.getBigDecimal("creditoriSum"));
                setAccount(journalBillBJson, bankAcctCodeToIdMap, journalBillB, bankAcctToIdMap, cashAcctToIdMap);
                setCurrency(journalBillBJson, journalBillB, currencyCodeToIdMap);
                setExchangeRateType(journalBillBJson, journalBillB, exchangeRateTypeCodeToIdNap, defaultExchangeRateType);
                journalBillB.setExchangeRate(journalBillBJson.getBigDecimal(EXCHANGE_RATE));
                journalBillB.setExchangeRateOps(journalBillBJson.getShort(EXCHANGE_RATE_OPS));
                setDate(journalBillBJson, journalBillB);
                journalBillB.setDescription(journalBillBJson.getString(ICmpConstant.DESCRIPTION));
                setSettleMode(journalBillBJson, journalBillB, settleModeCodeToIdMap);
                setPaymentType(journalBillBJson, journalBillB);
                journalBillB.setOppositetype(journalBillJson.getShort(OPPOSITE_TYPE));
                setOppositeInfo(journalBillBJson, journalBillB);
                journalBillB.setNotedate(DateUtils.parseDate(journalBillBJson.getString(NOTE_DATE)));
                journalBillB.setNotetype(journalBillBJson.getString(NOTE_TYPE)); // TODO
                journalBillB.setNoteno(journalBillBJson.getString(NOTE_NO));
                setProject(journalBillBJson, journalBillB);
                setCostProject(journalBillBJson, journalBillB);
                journalBillB.setUnireconciliationcode(journalBillBJson.getString(UNIRECONCILIATIONCODE));
                journalBillBList.add(journalBillB);
            }
        }

        return Arrays.asList(journalBillSaveList, journalBillSubmitList);
    }

    private void setPaymentType(CtmJSONObject journalBillBJson, JournalBill_b journalBillB) {
        Short paymentType = journalBillBJson.getShort(ICmpConstant.PAYMENT_TYPE);
        if (ObjectUtils.isEmpty(paymentType)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105052"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_220165DE05000006", "收付类型值不能为空" /* "收付类型值不能为空" */));
        }
        if (Objects.equals(paymentType, JournalBillPaymentType.CREDIT.getValue())
                || Objects.equals(paymentType, JournalBillPaymentType.DEBIT.getValue())) {
            journalBillB.setPaymenttype(paymentType);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105051"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21F1E01204680002", "收付类型值不正确" /* "收付类型值不正确" */));
        }
    }

    private void checkJournalBillBExists(List<CtmJSONObject> journalBillJsonList) {
        journalBillJsonList.forEach(journalBillJson -> {
            CtmJSONArray journalBillBArrayJson = journalBillJson.getJSONArray(JOURNAL_BILL_B_KEY);
            if (journalBillBArrayJson == null || journalBillBArrayJson.isEmpty()) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006C5", "明细不能为空！") /* "明细不能为空！" */);
            }
        });
    }

    private void setEntityStatus(JournalBill journalBill) {
        if (ObjectUtils.isNotEmpty(journalBill.getId())) {
            journalBill.setEntityStatus(EntityStatus.Update);
        } else {
            journalBill.setEntityStatus(EntityStatus.Insert);
        }
    }

    private void setCostProject(CtmJSONObject journalBillBJson, JournalBill_b journalBillB) throws Exception {
        String projectIdOrCode = journalBillBJson.getString(COST_PROJECT);
        if (StringUtils.isEmpty(projectIdOrCode)) {
            return;
        }
        List<Map<String, Object>> projectList = QueryBaseDocUtils.queryExpenseItemByIdOrCode(projectIdOrCode);
        if (CollectionUtils.isEmpty(projectList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105036"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_219F65C604C80000", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540068F", "费用项目未匹配到！请检查！") /* "费用项目未匹配到！请检查！" */) /* "费用项目编码未匹配到！请检查！" */);
        }
        journalBillB.setProjectId(projectList.get(0).get(ICmpConstant.ID).toString());
    }

    private void setProject(CtmJSONObject journalBillBJson, JournalBill_b journalBillB) throws Exception {
        String projectIdOrCode = journalBillBJson.getString(PROJECT);
        if (StringUtils.isEmpty(projectIdOrCode)) {
            return;
        }
        List<Map<String, Object>> projectList = QueryBaseDocUtils.queryProjectByIdOrCode(projectIdOrCode);
        if (CollectionUtils.isEmpty(projectList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105037"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_219F660804C80003", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540068B", "项目未匹配到！请检查！") /* "项目未匹配到！请检查！" */) /* "项目编码未匹配到！请检查！" */);
        }
        journalBillB.setProjectId(projectList.get(0).get(ICmpConstant.ID).toString());
    }

    private void setOppositeInfo(CtmJSONObject journalBillBJson, JournalBill_b journalBillB) {
        if (ObjectUtils.isEmpty(journalBillBJson.getShort(OPPOSITE_TYPE))) {
            return;
        }
        String oppositeId = journalBillBJson.getString(OPPOSITE_ID);
        String oppositeCode = journalBillBJson.getString(OPPOSITE_CODE);
        if (StringUtils.isNotEmpty(oppositeId)) {
            journalBillB.setOppositeid(oppositeId);
        } else if (StringUtils.isNotEmpty(oppositeCode)) {
            journalBillB.setOppositecode(oppositeCode);
        }  else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105038"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_219F667204C80006", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540068C", "对方账户不能为空") /* "对方账户不能为空" */) /* "对方账户不能为空" */);
        }
        String oppositeAccountCode = journalBillBJson.getString(OPPOSITE_ACCOUNT_CODE);
        String oppositeAccountId = journalBillBJson.getString(OPPOSITE_ACCOUNT_ID);
        String oppositeAccountNo = journalBillBJson.getString(OPPOSITE_ACCOUNT_NO);
        if (StringUtils.isNotEmpty(oppositeAccountId)) {
            journalBillB.setOppositeaccountid(oppositeAccountId);
        } else if (StringUtils.isNotEmpty(oppositeAccountCode)) {
            journalBillB.put(OPPOSITE_ACCOUNT_CODE, oppositeAccountCode);
        } else if (StringUtils.isNotEmpty(oppositeAccountNo)) {
            journalBillB.setOppositebankaccountno(oppositeAccountNo);
        }
        if (journalBillB.getOppositetype() == CaObject.Other.getValue()) {
            journalBillB.setOppositename(journalBillBJson.getString(OPPOSITE_NAME));
            journalBillB.setOppositebankaccountno(journalBillBJson.getString(OPPOSITE_ACCOUNT_NO));
            journalBillB.setOppositeaccountname(journalBillBJson.getString(OPPOSITE_ACCOUNT_NAME));
        }
    }

    private void setSettleMode(CtmJSONObject journalBillBJson, JournalBill_b journalBillB, Map<String, String> settleModeCodeToIdMap) {
        String settleModeCode = journalBillBJson.getString(SETTLE_MODE);
        if (StringUtils.isEmpty(settleModeCode)) {
            return;
        }
        if (settleModeCodeToIdMap.containsKey(settleModeCode)) {
            journalBillB.setSettlemode(Long.valueOf(settleModeCodeToIdMap.get(settleModeCode)));
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105039"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_219F66D604C80004", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400691", "结算方式未匹配到") /* "结算方式未匹配到" */) /* "结算方式未匹配到" */);
        }
    }

    private Map<String, String> getSettleModeCodeToIdMap(List<String> settleModeCodeList) {
        if (CollectionUtils.isEmpty(settleModeCodeList)) {
            return Collections.emptyMap();
        }
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setCodes(settleModeCodeList);
        settleMethodQueryParam.setIsEnabled(ICmpConstant.CONSTANT_ONE);
        List<SettleMethodModel> settleMethodModels = settleMethodQueryService.querySettleMethods(settleMethodQueryParam);
        return settleMethodModels.stream().collect(Collectors.toMap(SettleMethodModel::getCode, item -> String.valueOf(item.getId())));
    }

    private List<String> getSettleModeCodeList(List<CtmJSONObject> journalBillJsonList) {
        return journalBillJsonList.stream().flatMap(journalBillJson -> {
            CtmJSONArray journalBillBArrayJson = journalBillJson.getJSONArray(JOURNAL_BILL_B_KEY);
            List<CtmJSONObject> journalBillBList = journalBillBArrayJson.toJavaList(CtmJSONObject.class);
            List<String> currentSettleModeCodeList = new ArrayList<>();
            for (CtmJSONObject journalBillBJson : journalBillBList) {
                currentSettleModeCodeList.add(journalBillBJson.getString(SETTLE_MODE));
            }
            return currentSettleModeCodeList.stream();
        }).distinct().collect(Collectors.toList());
    }

    private void setDate(CtmJSONObject journalBillBJson, JournalBill_b journalBillB) throws Exception {
        journalBillB.setBusinessDate(DateUtils.parseDate(journalBillBJson.get(IFieldConstant.BUSINESS_DATE).toString()));
        journalBillB.setDzdate(DateUtils.parseDate(journalBillBJson.get(IFieldConstant.DZ_DATE).toString()));
    }

    private void setDept(CtmJSONObject journalBillJson, JournalBill journalBill, Map<String, String> deptCodeToIdMap) {
        String deptCodeOrId = journalBillJson.getString(ICmpConstant.DEPT);
        if (deptCodeToIdMap.containsKey(deptCodeOrId)) {
            journalBill.setDept(deptCodeToIdMap.get(deptCodeOrId));
        } else {
            journalBill.setDept(deptCodeOrId);
        }
    }

    private Map<String, String> getDeptCodeToIdMap(List<String> deptCodeList) throws Exception {
        if (CollectionUtils.isEmpty(deptCodeList)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> deptList = QueryBaseDocUtils.queryDeptByCodeList(deptCodeList);
        return deptList.stream().collect(Collectors.toMap(item -> item.get(ICmpConstant.CODE).toString(),
                item -> item.get(ICmpConstant.ID).toString()));
    }

    private List<String> getDeptCodeList(List<CtmJSONObject> journalBillJsonList) {
        return journalBillJsonList.stream().map(journalBillJson -> journalBillJson.getString(ICmpConstant.DEPT))
                .filter(ObjectUtils::isNotEmpty).distinct().collect(Collectors.toList());
    }

    private Map<String, String> getExchangeRateTypeCodeToIdMap(List<String> exchangeRateTypeCodeList) throws Exception {
        if (CollectionUtils.isEmpty(exchangeRateTypeCodeList)) {
            return Collections.emptyMap();
        }
        BdRequestParams requestParams = new BdRequestParams();
        requestParams.setCodeList(exchangeRateTypeCodeList);
        List<ExchangeRateTypeVO> exchangeRateTypeVOS = exchangeRateTypeService.queryListByCondition(requestParams);
        return exchangeRateTypeVOS.stream().collect(Collectors.toMap(ExchangeRateTypeVO::getCode, ExchangeRateTypeVO::getId));
    }

    private List<String> getExchangeRateTypeCodeList(List<CtmJSONObject> journalBillJsonList) {
        return journalBillJsonList.stream().flatMap(journalBillJson -> {
            CtmJSONArray journalBillBArrayJson = journalBillJson.getJSONArray(JOURNAL_BILL_B_KEY);
            List<CtmJSONObject> journalBillBList = journalBillBArrayJson.toJavaList(CtmJSONObject.class);
            List<String> currentExchangeRateTypeCodeList = new ArrayList<>();
            for (CtmJSONObject journalBillBJson : journalBillBList) {
                currentExchangeRateTypeCodeList.add(journalBillBJson.getString(ICmpConstant.EXCHANGE_RATE_TYPE));
            }
            return currentExchangeRateTypeCodeList.stream();
        }).distinct().collect(Collectors.toList());
    }

    private void setExchangeRateType(CtmJSONObject journalBillBJson, JournalBill_b journalBillB, Map<String, String> exchangeRateTypeCodeToIdNap, String defaultExchangeRateType) {
        String exchangeRateTypeCodeOrId = journalBillBJson.get(ICmpConstant.EXCHANGE_RATE_TYPE).toString();
        if (exchangeRateTypeCodeToIdNap.containsKey(exchangeRateTypeCodeOrId)) {
            journalBillB.setExchangeRateType(exchangeRateTypeCodeToIdNap.get(exchangeRateTypeCodeOrId));
        } else {
            journalBillB.setExchangeRateType(defaultExchangeRateType);
        }
    }

    private Map<String, String> getCurrencyCodeToId(List<String> currencyCodeList) throws Exception {
        List<Map<String, Object>> currencyList = QueryBaseDocUtils.queryCurrencyByCodes(currencyCodeList);
        return currencyList.stream().collect(Collectors.toMap(item -> item.get(ICmpConstant.CODE).toString()
                , item -> item.get(ICmpConstant.ID).toString()));
    }

    private List<String> getCurrencyCodeOrIdList(List<CtmJSONObject> journalBillJsonList) {
        return journalBillJsonList.stream().flatMap(journalBillJson -> {
            CtmJSONArray journalBillBArrayJson = journalBillJson.getJSONArray(JOURNAL_BILL_B_KEY);
            List<CtmJSONObject> journalBillBList = journalBillBArrayJson.toJavaList(CtmJSONObject.class);
            List<String> currentCurrencyCodeList = new ArrayList<>();
            for (CtmJSONObject journalBillBJson : journalBillBList) {
                currentCurrencyCodeList.add(journalBillBJson.getString(ICmpConstant.CURRENCY));
            }
            return currentCurrencyCodeList.stream();
        }).distinct().collect(Collectors.toList());
    }

    private void setCurrency(CtmJSONObject journalBillBJson, JournalBill_b journalBillB, Map<String, String> currencyCodeToIdMap) {
        String currencyCodeOrId = journalBillBJson.getString(ICmpConstant.CURRENCY);
        if (currencyCodeToIdMap.containsKey(currencyCodeOrId)) {
            journalBillB.setCurrency(currencyCodeToIdMap.get(currencyCodeOrId));
        } else {
            journalBillB.setCurrency(currencyCodeOrId);
        }
    }

    private void setAccount(CtmJSONObject journalBillBJson, Map<String, String> bankAcctCodeToIdMap, JournalBill_b journalBillB, Map<String, String> bankAcctToIdMap, Map<String, String> cashAcctToIdMap) {
        String cashAccount = journalBillBJson.getString(CASH_ACCOUNT);
        String bankAccount = journalBillBJson.getString(BANK_ACCOUNT);
        if (bankAcctCodeToIdMap.containsKey(bankAccount)) {
            journalBillB.setBankaccount(bankAcctCodeToIdMap.get(bankAccount));
        } else if (bankAcctToIdMap.containsKey(bankAccount)) {
            journalBillB.setBankaccount(bankAcctToIdMap.get(bankAccount));
        } else {
            journalBillB.setBankaccount(bankAccount);
        }
        if (cashAcctToIdMap.containsKey(cashAccount)) {
            journalBillB.setCashaccount(cashAcctToIdMap.get(cashAccount));
        } else {
            journalBillB.setCashaccount(cashAccount);
        }
    }

    private void processTradeType(CtmJSONObject journalBillJson, JournalBill journalBill) throws Exception {
        BdTransType bdTransType = transTypeQueryService.queryTransTypes(journalBillJson.get(TRADE_TYPE).toString());
        if (ObjectUtils.isNotEmpty(bdTransType)) {
            journalBill.setTradetype(bdTransType.getId());
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105032"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_219F671804C80004", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540068D", "交易编码未匹配到！请检查！") /* "交易编码未匹配到！请检查！" */) /* "交易编码未匹配到！请检查！" */);
        }
    }

    private void processBillState(CtmJSONObject journalBillJson, List<JournalBill> journalBillSaveList, JournalBill journalBill, List<JournalBill> journalBillSubmitList) {
        int billState = journalBillJson.getInteger(BILL_STATE);
        if (billState == SAVE_ACTION) {
            journalBillSaveList.add(journalBill);
        } else if (billState == SUBMIT_ACTION) {
            journalBillSubmitList.add(journalBill);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105040"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_219F676804C80001", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400692", "单据状态不能为空") /* "单据状态不能为空" */) /* "单据状态不能为空" */);
        }
    }

    private void setAccentyAndAccentyRaw(CtmJSONObject journalBillJson, JournalBill journalBill) throws Exception {
        List<Map<String, Object>> accentity = QueryBaseDocUtils.getOrgMVByIdOrCode(journalBillJson.getString(IFieldConstant.ACCENTITY));
        if (CollectionUtils.isEmpty(accentity)) {
            throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0005E", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540068E", "资金组织未匹配到！请检查！") /* "资金组织未匹配到！请检查！" */) /* "资金组织未匹配到！请检查！" */);
        } else {
            journalBill.setAccentity(accentity.get(0).get(IFieldConstant.ID).toString());
        }
        if (StringUtils.isNotEmpty(journalBillJson.getString(IFieldConstant.ACCENTITYRAW))) {
            List<Map<String, Object>> accentityRaw = QueryBaseDocUtils.getOrgMVByIdOrCode(journalBillJson.getString(IFieldConstant.ACCENTITYRAW));
            if (CollectionUtils.isEmpty(accentityRaw)) {
                throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0005E", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540068E", "资金组织未匹配到！请检查！") /* "资金组织未匹配到！请检查！" */) /* "资金组织未匹配到！请检查！" */);
            } else {
                journalBill.setAccentity(accentityRaw.get(0).get(IFieldConstant.ID).toString());
            }
        } else {
            FinOrgDTO accentyRaw = finOrgQueryService.getById(journalBill.getAccentity());
            if (accentyRaw != null) {
                journalBill.setAccentityRaw(journalBill.getAccentity());
            } else {
                journalBill.setAccentityRaw(accentity.get(0).get(IFieldConstant.FIN_ORG_ID).toString());
            }
        }
    }

    private Map<String, String> getCashAcctCodeToIdMap(List<String> cashAcctCodeOrIdList) throws Exception {
        if (CollectionUtils.isEmpty(cashAcctCodeOrIdList)) {
            return Collections.emptyMap();
        }
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setCodeList(cashAcctCodeOrIdList);
        enterpriseParams.setPageSize(5000);
        // TODO：校验资金组织是否有银行的权限
        List<EnterpriseCashVO> enterpriseCashVOS = baseRefRpcService.queryEnterpriseCashAcctByCondition(enterpriseParams);
        return enterpriseCashVOS.stream().collect(Collectors.toMap(EnterpriseCashVO::getCode, EnterpriseCashVO::getId));
    }

    private Map<String, String> getBankAcctToIdMap(List<String> bankAcctCodeOrAccountOrIdList) throws Exception {
        if (CollectionUtils.isEmpty(bankAcctCodeOrAccountOrIdList)) {
            return Collections.emptyMap();
        }
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setPageSize(5000);
        enterpriseParams.setAccountList(bankAcctCodeOrAccountOrIdList);
        // TODO：校验资金组织是否有银行的权限
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOWithRanges = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParams);
        return enterpriseBankAcctVOWithRanges.stream().collect(Collectors.toMap(EnterpriseBankAcctVOWithRange::getAccount, EnterpriseBankAcctVOWithRange::getId));
    }

    private Map<String, String> getBankAcctCodeToIdMap(List<String> bankAcctCodeOrAccountOrIdList) throws Exception {
        if (CollectionUtils.isEmpty(bankAcctCodeOrAccountOrIdList)) {
            return Collections.emptyMap();
        }
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setPageSize(5000);
        enterpriseParams.setCodeList(bankAcctCodeOrAccountOrIdList);
        // TODO：校验资金组织是否有银行的权限
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOWithRanges = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParams);
        return enterpriseBankAcctVOWithRanges.stream().collect(Collectors.toMap(EnterpriseBankAcctVOWithRange::getCode, EnterpriseBankAcctVOWithRange::getId));
    }

    private List<String> getCashAcctCodeOrIdList(List<CtmJSONObject> journalBillJsonList) {
        return journalBillJsonList.stream().flatMap(journalBillJson -> {
            CtmJSONArray journalBillBArrayJson = journalBillJson.getJSONArray(JOURNAL_BILL_B_KEY);
            List<CtmJSONObject> journalBillBList = journalBillBArrayJson.toJavaList(CtmJSONObject.class);
            List<String> currentBanlAcctCodeList = new ArrayList<>();
            for (CtmJSONObject journalBillBJson : journalBillBList) {
                currentBanlAcctCodeList.add(journalBillBJson.getString(CASH_ACCOUNT));
            }
            return currentBanlAcctCodeList.stream();
        }).filter(ObjectUtils::isNotEmpty).distinct().collect(Collectors.toList());
    }

    private List<String> getBankAcctCodeOrAccountOrIdList(List<CtmJSONObject> journalBillJsonList) {
        return journalBillJsonList.stream().flatMap(journalBillJson -> {
            CtmJSONArray journalBillBArrayJson = journalBillJson.getJSONArray(JOURNAL_BILL_B_KEY);
            List<CtmJSONObject> journalBillBList = journalBillBArrayJson.toJavaList(CtmJSONObject.class);
            List<String> currentBanlAcctCodeList = new ArrayList<>();
            for (CtmJSONObject journalBillBJson : journalBillBList) {
                currentBanlAcctCodeList.add(journalBillBJson.getString(BANK_ACCOUNT));
            }
            return currentBanlAcctCodeList.stream();
        }).filter(ObjectUtils::isNotEmpty).distinct().collect(Collectors.toList());
    }

    @Override
    public String batchDeleteForOpenApi(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray(ICmpConstant.DATA);
        List<String> idDelete = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            String id = rowData.getString(ICmpConstant.ID);
            idDelete.add(id);
        }
        if (CollectionUtils.isEmpty(idDelete)) {
            CtmJSONObject resmap = new CtmJSONObject();
            resmap.put(ICmpConstant.CODE, 999);
            resmap.put(MSG, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540068A", "参数不能为空！") /* "参数不能为空！" */);
            return CtmJSONObject.toJSONString(resmap);
        }
        List<Map<String, Object>> list = MetaDaoHelper.queryByIds(JournalBill.ENTITY_NAME, "*", idDelete);
        if (Objects.isNull(list) || list.isEmpty()) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418048A", "单据不存在 id:") /* "单据不存在 id:" */ + idDelete);
        }
        BillDataDto billDataDto = new BillDataDto();
        billDataDto.setBillnum(IBillNumConstant.JOURNAL_BILL);
        billDataDto.setData(list);
        billDataDto.setAction(OperationTypeEnum.DELETE.getValue());
        return CtmJSONObject.toJSONString(fiBillService.batchdelete(billDataDto));
    }

    @Override
    public CtmJSONObject checkAddTransType(CtmJSONObject params) throws Exception {
        String serviceCode = params.getString("serviceCode");
        String addType = params.getString("addType");
        String tradeType = serviceCode.split("_")[0];
        BdTransType bdTransType = transTypeQueryService.findById(tradeType);
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
        boolean addTransTypeFlag = false;
        if ("btnBankJournalAdd".equals(addType)) {
            if ("cmp_journalbill_bank".equals(jsonObject.getString("parTradeType"))) {
                addTransTypeFlag = true;
            }
        } else if ("btnCashJournalAdd".equals(addType)) {
            if ("cmp_journalbill_cash".equals(jsonObject.getString("parTradeType"))) {
                addTransTypeFlag = true;
            }
        }
        params.put("addTransTypeFlag", addTransTypeFlag);
        return params;
    }
}
