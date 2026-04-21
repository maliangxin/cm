package com.yonyoucloud.fi.cmp.journalbill.rule.save;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.EnterpriseCashVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill_b;
import com.yonyoucloud.fi.cmp.journalbill.service.JournalBillServiceImpl;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CustomerQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.MerchantDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorVO;
import io.edap.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JournalBillBeforeSaveRule extends AbstractCommonRule {
    public static final String CUSTOMER_EXCHANGE_RATE_TYPE = "02";
    public static final String PAR_TRADE_TYPE = "parTradeType";
    public static final String EXTEND_ATTRS_JSON = "extend_attrs_json";

    @Autowired
    private SettlementService settlementService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    CustomerQueryService customerQueryService;

    @Autowired
    VendorQueryService vendorQueryService;

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            boolean importFlag = ICmpConstant.IMPORT_FLAG.equals(billDataDto.getRequestAction());
            boolean apiFlag = bizObject.containsKey(ICmpConstant.FROM_API) && bizObject.get(ICmpConstant.FROM_API).equals(true) || billDataDto.getFromApi();
            JournalBill journalBill = (JournalBill) bizObject;
            setGenerateType(bizObject, billDataDto, journalBill);
            setDefaultStatus(journalBill);
            setAccentityIfSingleOrg(bizObject, journalBill);
            if (importFlag || apiFlag) {
                fillInDataForImportOrApi(journalBill, billContext);
            }
            checkBankAccountAndCashAccount(journalBill);
            checkPeriodDate(journalBill);
            checkDebitSumAndCreditSum(journalBill);
            checkBusinessDateAndDzdate(journalBill);
            checkIsSettledForAccentity(journalBill);
        }
        return result;
    }

    private void setAccentityIfSingleOrg(BizObject bizObject, JournalBill journalBill) throws Exception {
        if (FIDubboUtils.isSingleOrg()) {
            BizObject singleOrg = FIDubboUtils.getSingleOrg();
            if (singleOrg != null) {
                journalBill.setAccentity(singleOrg.get("id"));
                bizObject.set("accentity_name", singleOrg.get("name"));
            }
        }
    }

    private void setDefaultStatus(JournalBill journalBill) {
        if (ObjectUtils.isEmpty(journalBill.getVerifystate())) {
            journalBill.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
        }
        for (JournalBill_b journalBillB : journalBill.JournalBill_b()) {
            if (ObjectUtils.isEmpty(journalBillB.getVoucherstatus())) {
                journalBillB.setVoucherstatus(VoucherStatus.Empty.getValue());
            }
        }
    }

    private void checkPeriodDate(JournalBill journalBill) {
        Date periodFirstDate = null;
        try {
            periodFirstDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(journalBill.getAccentity());
            if(periodFirstDate == null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102283"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00029", "模块未启用") /* "模块未启用" */);
            }
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102283"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00029", "模块未启用") /* "模块未启用" */ + e.getMessage(), e);
        }
    }

    private void checkBankAccountAndCashAccount(JournalBill journalBill) throws Exception {
        String parentTradeType = getParentTradeType(journalBill);
        checkBankAccountAndCashAccountRequired(journalBill, parentTradeType);
        checkBankAccountAndCashAccountCurrencySame(journalBill, parentTradeType);
    }

    private void checkBankAccountAndCashAccountCurrencySame(JournalBill journalBill, String parentTradeType) throws Exception {
        if (ICmpConstant.JOURNAL_BILL_TRADE_TYPE_BANK.equals(parentTradeType)) {
            checkBankAccountCurrencySame(journalBill);
        } else if (ICmpConstant.JOURNAL_BILL_TRADE_TYPE_CASH.equals(parentTradeType)) {
            checkCashAccountCurrencySame(journalBill);
        }
    }

    private void checkCashAccountCurrencySame(JournalBill journalBill) throws Exception {
        List<String> cashAccountIdList = journalBill.JournalBill_b().stream().map(JournalBill_b::getCashaccount).filter(StringUtils::isNotEmpty).distinct().collect(Collectors.toList());
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setIdList(cashAccountIdList);
        enterpriseParams.setPageSize(5000);
        List<EnterpriseCashVO> enterpriseCashVOS = baseRefRpcService.queryEnterpriseCashAcctByCondition(enterpriseParams);
        Map<String, String> cashAccountIdToCurrencyIdMap = enterpriseCashVOS.stream().collect(Collectors.toMap(EnterpriseCashVO::getId, EnterpriseCashVO::getCurrency));
        journalBill.JournalBill_b().forEach(item -> {
            if (cashAccountIdToCurrencyIdMap.containsKey(item.getCashaccount()) && !Objects.equals(cashAccountIdToCurrencyIdMap.get(item.getCashaccount()), item.getCurrency())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105045"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21CC2C2A05A00009","现金账户币种与日记账录入明细币种不符") /* "现金账户币种与日记账录入明细币种不符" */);
            }
        });
    }

    private void checkBankAccountCurrencySame(JournalBill journalBill) throws Exception {
        List<String> bankAccountIdList = journalBill.JournalBill_b().stream().map(JournalBill_b::getBankaccount).filter(StringUtils::isNotEmpty).distinct().collect(Collectors.toList());
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setPageSize(5000);
        enterpriseParams.setIdList(bankAccountIdList);
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOWithRanges = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParams);
        Map<String, Set<String>> bankAccountIdToCurrencyIdListMap = enterpriseBankAcctVOWithRanges.stream().collect(Collectors.toMap(EnterpriseBankAcctVOWithRange::getId, item -> item.getCurrencyList().stream().map(BankAcctCurrencyVO::getCurrency).collect(Collectors.toSet())));
        journalBill.JournalBill_b().forEach(item -> {
            if (bankAccountIdToCurrencyIdListMap.containsKey(item.getBankaccount()) && !bankAccountIdToCurrencyIdListMap.get(item.getBankaccount()).contains(item.getCurrency())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105044"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21CC2BDE04D80003","银行账户币种与日记账录入明细币种不符") /* "银行账户币种与日记账录入明细币种不符" */);
            }
        });
    }

    private void checkBankAccountAndCashAccountRequired(JournalBill journalBill, String parentTradeType) throws Exception {
        for (JournalBill_b journalBillB : journalBill.JournalBill_b()) {
            if (StringUtils.isEmpty(journalBillB.getBankaccount()) && StringUtils.isEmpty(journalBillB.getCashaccount())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105019"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218CFF6804B80009", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E6", "银行账户和现金账户不能同时为空") /* "银行账户和现金账户不能同时为空" */) /* "银行账户和现金账户不能同时为空" */);
            }
            if (StringUtils.isNotEmpty(journalBillB.getBankaccount()) && StringUtils.isNotEmpty(journalBillB.getCashaccount())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105020"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218CFFEA04300003", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E9", "银行账户和现金账户应其中一个有值") /* "银行账户和现金账户应其中一个有值" */) /* "银行账户和现金账户应其中一个有值" */);
            }
            if (ICmpConstant.JOURNAL_BILL_TRADE_TYPE_BANK.equals(parentTradeType) && StringUtils.isEmpty(journalBillB.getBankaccount())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105033"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_219F692A04980007", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005EB", "银行账户不能为空") /* "银行账户不能为空" */) /* "银行账户不能为空" */);
            }
            if (ICmpConstant.JOURNAL_BILL_TRADE_TYPE_CASH.equals(parentTradeType) && StringUtils.isEmpty(journalBillB.getCashaccount())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105034"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_219F68EC04980006", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005EE", "现金账户不能为空") /* "现金账户不能为空" */) /* "现金账户不能为空" */);
            }
        }
    }

    private String getParentTradeType(JournalBill journalBill) throws Exception {
        Map<String, Object> tradeCodeById = QueryBaseDocUtils.queryTransTypeById(journalBill.getTradetype());
        if (CollectionUtils.isEmpty(tradeCodeById)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105032"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_219F671804C80004", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E3", "交易编码未匹配到！请检查！") /* "交易编码未匹配到！请检查！" */) /* "交易编码未匹配到！请检查！" */);
        }
        return CtmJSONObject.parseObject(tradeCodeById.get(EXTEND_ATTRS_JSON).toString()).getString(PAR_TRADE_TYPE);
    }

    private void fillInDataForImportOrApi(JournalBill journalBill, BillContext billContext) throws Exception {
        fillAmountDataForImportOrApi(journalBill);
        fillOppositeInfoForImportOrApi(journalBill, billContext);
        fillDzTimeForImportOrApi(journalBill);
    }

    private void fillDzTimeForImportOrApi(JournalBill journalBill) throws ParseException {
        for (JournalBill_b journalBillB : journalBill.JournalBill_b()) {
            if (journalBillB.getDzdate() != null && journalBillB.getDztime() == null) {
                journalBillB.setDztime(DateUtils.dateTimeToDate(journalBillB.getDzdate()));
            }
        }
    }

    private void fillOppositeInfoForImportOrApi(JournalBill journalBill, BillContext billContext) throws Exception {
        for (JournalBill_b journalBillB : journalBill.JournalBill_b()) {
            if (ObjectUtils.isEmpty(journalBillB.getOppositetype())) { // 对方信息非必填，如果对方类型为空，则不去校验和填充对方信息
                continue;
            }
            if (journalBillB.getOppositetype() == CaObject.Other.getValue()) {
                continue;
            }
            boolean needTranslateOppositeInfo = true;
            if (StringUtils.isEmpty(journalBillB.getOppositecode()) && StringUtils.isEmpty(journalBillB.getOppositeid())) {
                needTranslateOppositeInfo = false;
            }
            boolean needTranslateOppsoiteAccountInfo = true;
            // 对方账号编码只有对方类型是内部单位时有值
            if (StringUtils.isEmpty(journalBillB.getOppositebankaccountno()) && StringUtils.isEmpty(journalBillB.<String>get(JournalBillServiceImpl.OPPOSITE_ACCOUNT_CODE)) && StringUtils.isEmpty(journalBillB.getOppositeaccountid())) {
                needTranslateOppsoiteAccountInfo = false;
            }
            if (journalBillB.getOppositetype() == CaObject.Customer.getValue()) {
                fillOppositeInfoForCustomer(journalBillB, needTranslateOppositeInfo, needTranslateOppsoiteAccountInfo);
            } else if (journalBillB.getOppositetype() == CaObject.Supplier.getValue()) {
                fillOppsoiteInfoForSupplier(journalBillB, needTranslateOppositeInfo, needTranslateOppsoiteAccountInfo);
            } else if (journalBillB.getOppositetype() == CaObject.Employee.getValue()) {
                fillOppositeInfoForEmployee(journalBillB, needTranslateOppositeInfo, needTranslateOppsoiteAccountInfo);
            } else if (journalBillB.getOppositetype() == CaObject.InnerUnit.getValue()) {
                fillOppositeInfoForInnerUnit(billContext, journalBillB, needTranslateOppositeInfo, needTranslateOppsoiteAccountInfo);
            } else if (journalBillB.getOppositetype() == CaObject.CapBizObj.getValue()) {
                fillOppositeInfoForCapBizObj(journalBillB, needTranslateOppositeInfo, needTranslateOppsoiteAccountInfo);
            }
        }
    }

    private void fillOppositeInfoForCapBizObj(JournalBill_b journalBillB, boolean needTranslateOppositeInfo, boolean needTranslateOppsoiteAccountInfo) throws Exception {
        if (needTranslateOppositeInfo) {
            fillOppositeForCapBizObj(journalBillB);
        }
        if (needTranslateOppsoiteAccountInfo) {
            fillOppositeAccountForCapBizObj(journalBillB);
        }
    }

    private void fillOppositeInfoForInnerUnit(BillContext billContext, JournalBill_b journalBillB, boolean needTranslateOppositeInfo, boolean needTranslateOppsoiteAccountInfo) throws Exception {
        if (needTranslateOppositeInfo) {
            fillOppositeForInnerOrg(journalBillB);
        }
        if (needTranslateOppsoiteAccountInfo) {
            fillOppositeAccountForInnerOrg(billContext, journalBillB);
        }
    }

    private void fillOppositeInfoForEmployee(JournalBill_b journalBillB, boolean needTranslateOppositeInfo, boolean needTranslateOppsoiteAccountInfo) throws Exception {
        if (needTranslateOppositeInfo) {
            fillOppositeForEmployee(journalBillB);
        }
        if (needTranslateOppsoiteAccountInfo) {
            fillOppositeAccountForEmployee(journalBillB);
        }
    }

    private void fillOppsoiteInfoForSupplier(JournalBill_b journalBillB, boolean needTranslateOppositeInfo, boolean needTranslateOppsoiteAccountInfo) throws Exception {
        if (needTranslateOppositeInfo) {
            fillOppositeForSupplier(journalBillB);
        }
        if (needTranslateOppsoiteAccountInfo) {
            fillOppositeAccountForSupplier(journalBillB);
        }
    }

    private void fillOppositeInfoForCustomer(JournalBill_b journalBillB, boolean needTranslateOppositeInfo, boolean needTranslateOppsoiteAccountInfo) throws Exception {
        if (needTranslateOppositeInfo) {
            fillOppositeForCustomer(journalBillB);
        }
        if (needTranslateOppsoiteAccountInfo) {
            fillOppositeAccountForCustomer(journalBillB);
        }
    }

    private void fillOppositeAccountForCapBizObj(JournalBill_b journalBillB) throws Exception {
        if (StringUtils.isEmpty(journalBillB.getOppositeaccountid())) {
            if (StringUtils.isNotEmpty(journalBillB.getOppositebankaccountno())) {
                List<Map<String, Object>> capBizObjbankaccountByIdOrCode = QueryBaseDocUtils.getCapBizObjbankaccountByIdOrCode(journalBillB.getOppositebankaccountno());
                if (CollectionUtils.isEmpty(capBizObjbankaccountByIdOrCode)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105030"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D76B604400001", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F1", "没有查到资金业务对象银行账户信息") /* "没有查到资金业务对象银行账户信息" */) /* "没有查到资金业务对象银行账户信息" */);
                }
                journalBillB.setOppositeaccountid(capBizObjbankaccountByIdOrCode.get(0).get("id").toString());
                journalBillB.setOppositeaccountname(capBizObjbankaccountByIdOrCode.get(0).get("accountname").toString());
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105030"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D76B604400001", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F1", "没有查到资金业务对象银行账户信息") /* "没有查到资金业务对象银行账户信息" */) /* "没有查到资金业务对象银行账户信息" */);
            }
        } else {
            if (StringUtils.isEmpty(journalBillB.getOppositeaccountname())) {
                List<Map<String, Object>> capBizObjbankaccountByIdOrCode = QueryBaseDocUtils.getCapBizObjbankaccountByIdOrCode(journalBillB.getOppositeaccountid());
                if (CollectionUtils.isEmpty(capBizObjbankaccountByIdOrCode)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105030"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D76B604400001", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F1", "没有查到资金业务对象银行账户信息") /* "没有查到资金业务对象银行账户信息" */) /* "没有查到资金业务对象银行账户信息" */);
                }
                journalBillB.setOppositeaccountname(capBizObjbankaccountByIdOrCode.get(0).get("accountname").toString());
            }
        }
    }

    private void fillOppositeForCapBizObj(JournalBill_b journalBillB) throws Exception {
        if (StringUtils.isEmpty(journalBillB.getOppositeid())) {
            List<Map<String, Object>> capBizObj = QueryBaseDocUtils.getCapBizObj(journalBillB.getOppositecode());
            if (CollectionUtils.isEmpty(capBizObj)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105029"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D767C04400009", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005DD", "没有查到资金业务对象信息") /* "没有查到资金业务对象信息" */) /* "没有查到资金业务对象信息" */);
            }
            journalBillB.setOppositename(capBizObj.get(0).get("name").toString());
            journalBillB.setOppositeid(capBizObj.get(0).get("id").toString());
        } else {
            List<Map<String, Object>> capBizObj = QueryBaseDocUtils.getCapBizObj(journalBillB.getOppositeid());
            if (CollectionUtils.isEmpty(capBizObj)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105029"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D767C04400009", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005DD", "没有查到资金业务对象信息") /* "没有查到资金业务对象信息" */) /* "没有查到资金业务对象信息" */);
            }
            journalBillB.setOppositename(capBizObj.get(0).get("name").toString());
        }
    }

    private void fillOppositeAccountForInnerOrg(BillContext billContext, JournalBill_b journalBillB) throws Exception {
        if (StringUtils.isEmpty(journalBillB.getOppositeaccountid())) {
            if (StringUtils.isNotEmpty(journalBillB.getOppositebankaccountno())) {
                Map<String, Object> condition = new HashMap<>();
                condition.put(ICmpConstant.ACCOUNT, journalBillB.getOppositebankaccountno());
                condition.put(ICmpConstant.ENABLE, 1);
                List<Map<String, Object>> enterpriseBankAccountByCondition = QueryBaseDocUtils.queryEnterpriseBankAccountByCondition(condition, billContext);
                if (CollectionUtils.isEmpty(enterpriseBankAccountByCondition)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105028"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D764204400007", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E8", "没有查到内部单位银行账户信息") /* "没有查到内部单位银行账户信息" */) /* "没有查到内部单位银行账户信息" */);
                }
                journalBillB.setOppositeaccountname(enterpriseBankAccountByCondition.get(0).get("name").toString());
                journalBillB.setOppositeaccountid(enterpriseBankAccountByCondition.get(0).get("id").toString());
            } else if (StringUtils.isNotEmpty(journalBillB.get(JournalBillServiceImpl.OPPOSITE_ACCOUNT_CODE))) {
                Map<String, Object> condition = new HashMap<>();
                condition.put(ICmpConstant.CODE, journalBillB.get(JournalBillServiceImpl.OPPOSITE_ACCOUNT_CODE));
                condition.put(ICmpConstant.ENABLE, 1);
                List<Map<String, Object>> enterpriseBankAccountByCondition = QueryBaseDocUtils.queryEnterpriseBankAccountByCondition(condition, billContext);
                if (CollectionUtils.isEmpty(enterpriseBankAccountByCondition)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105028"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D764204400007", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E8", "没有查到内部单位银行账户信息") /* "没有查到内部单位银行账户信息" */) /* "没有查到内部单位银行账户信息" */);
                }
                journalBillB.setOppositeaccountname(enterpriseBankAccountByCondition.get(0).get("name").toString());
                journalBillB.setOppositeaccountid(enterpriseBankAccountByCondition.get(0).get("id").toString());
            }
        } else {
            if (StringUtils.isEmpty(journalBillB.getOppositeaccountname())) {
                Map<String, Object> condition = new HashMap<>();
                condition.put(ICmpConstant.ID, journalBillB.getOppositeaccountid());
                condition.put(ICmpConstant.ENABLE, 1);
                List<Map<String, Object>> enterpriseBankAccountByCondition = QueryBaseDocUtils.queryEnterpriseBankAccountByCondition(condition, billContext);
                if (CollectionUtils.isEmpty(enterpriseBankAccountByCondition)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105028"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D764204400007", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E8", "没有查到内部单位银行账户信息") /* "没有查到内部单位银行账户信息" */) /* "没有查到内部单位银行账户信息" */);
                }
                journalBillB.setOppositeaccountname(enterpriseBankAccountByCondition.get(0).get("name").toString());
            }
        }
    }

    private void fillOppositeForInnerOrg(JournalBill_b journalBillB) throws Exception {
        if (StringUtils.isEmpty(journalBillB.getOppositeid())) {
            List<Map<String, Object>> orgMVByIdOrCode = QueryBaseDocUtils.getOrgMVByIdOrCode(journalBillB.getOppositecode());
            if (CollectionUtils.isEmpty(orgMVByIdOrCode)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105027"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D75EE05080009", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005DE", "没有查到内部单位信息") /* "没有查到内部单位信息" */) /* "没有查到内部单位信息" */);
            }
            journalBillB.setOppositeid(orgMVByIdOrCode.get(0).get("id").toString());
            journalBillB.setOppositename(orgMVByIdOrCode.get(0).get("name").toString());
        } else {
            List<Map<String, Object>> orgMVByIdOrCode = QueryBaseDocUtils.getOrgMVByIdOrCode(journalBillB.getOppositeid());
            if (CollectionUtils.isEmpty(orgMVByIdOrCode)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105027"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D75EE05080009", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005DE", "没有查到内部单位信息") /* "没有查到内部单位信息" */) /* "没有查到内部单位信息" */);
            }
            journalBillB.setOppositename(orgMVByIdOrCode.get(0).get("name").toString());
        }
    }

    private void fillOppositeAccountForEmployee(JournalBill_b journalBillB) throws Exception {
        if (StringUtils.isEmpty(journalBillB.getOppositeaccountid())) {
            if (StringUtils.isNotEmpty(journalBillB.getOppositebankaccountno())) {
                Map<String, Object> queryCondition = new HashMap<String, Object>();
                queryCondition.put("account", journalBillB.getOppositebankaccountno());
                List<Map<String, Object>> employeeAccList = QueryBaseDocUtils.queryStaffBankAccountByCondition(queryCondition);
                if (CollectionUtils.isEmpty(employeeAccList)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105026"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D75BC04400002", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E4", "没有查到员工银行账户信息") /* "没有查到员工银行账户信息" */) /* "没有查到员工银行账户信息" */);
                }
                journalBillB.setOppositeaccountid(employeeAccList.get(0).get("account").toString());
                journalBillB.setOppositeaccountname(employeeAccList.get(0).get("accountname").toString());
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105026"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D75BC04400002", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E4", "没有查到员工银行账户信息") /* "没有查到员工银行账户信息" */) /* "没有查到员工银行账户信息" */);
            }
        } else {
            if (StringUtils.isEmpty(journalBillB.getOppositeaccountname())) {
                Map<String, Object> queryCondition = new HashMap<String, Object>();
                queryCondition.put("id", journalBillB.getOppositeaccountid());
                List<Map<String, Object>> employeeAccList = QueryBaseDocUtils.queryStaffBankAccountByCondition(queryCondition);
                if (CollectionUtils.isEmpty(employeeAccList)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105026"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D75BC04400002", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E4", "没有查到员工银行账户信息") /* "没有查到员工银行账户信息" */) /* "没有查到员工银行账户信息" */);
                }
                journalBillB.setOppositeaccountname(employeeAccList.get(0).get("accountname").toString());
            }
        }
    }

    private void fillOppositeForEmployee(JournalBill_b journalBillB) throws Exception {
        if (StringUtils.isEmpty(journalBillB.getOppositeid())) {
            List<Map<String, Object>> employeeList = QueryBaseDocUtils.queryEmployeeByIdOrCode(journalBillB.getOppositecode());
            if (CollectionUtils.isEmpty(employeeList)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105025"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D758804400006", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F2", "没有查到员工信息") /* "没有查到员工信息" */) /* "没有查到员工信息" */);
            }
            journalBillB.setOppositeid(employeeList.get(0).get("id").toString());
            journalBillB.setOppositename(employeeList.get(0).get("name").toString());
        } else {
            List<Map<String, Object>> employeeList = QueryBaseDocUtils.queryEmployeeByIdOrCode(journalBillB.getOppositeid());
            if (CollectionUtils.isEmpty(employeeList)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105025"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D758804400006", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F2", "没有查到员工信息") /* "没有查到员工信息" */) /* "没有查到员工信息" */);
            }
            journalBillB.setOppositename(employeeList.get(0).get("name").toString());
        }
    }

    private void fillOppositeAccountForSupplier(JournalBill_b journalBillB) throws Exception {
        if (StringUtils.isEmpty(journalBillB.getOppositeaccountid())) {
            if (StringUtils.isNotEmpty(journalBillB.getOppositebankaccountno())) {
                List<VendorBankVO> vendorBanksByAccountList = vendorQueryService.getVendorBanksByAccountList(Collections.singletonList(Long.valueOf(journalBillB.getOppositebankaccountno())), "id", "account", "accountname");
                if (CollectionUtils.isEmpty(vendorBanksByAccountList)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105024"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D754C04400008", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005DF", "没有查到供应商银行账户信息") /* "没有查到供应商银行账户信息" */) /* "没有查到供应商银行账户信息" */);
                }
                journalBillB.setOppositeaccountid(vendorBanksByAccountList.get(0).getId().toString());
                journalBillB.setOppositeaccountname(vendorBanksByAccountList.get(0).getAccountname());
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105024"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D754C04400008", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005DF", "没有查到供应商银行账户信息") /* "没有查到供应商银行账户信息" */) /* "没有查到供应商银行账户信息" */);
            }
        } else {
            if (StringUtils.isEmpty(journalBillB.getOppositeaccountname())) {
                List<VendorBankVO> vendorBanksByAccountIdList = vendorQueryService.getVendorBanksByVendorId(Long.valueOf(journalBillB.getOppositeaccountid()), null);
                if (CollectionUtils.isEmpty(vendorBanksByAccountIdList)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105024"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D754C04400008", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005DF", "没有查到供应商银行账户信息") /* "没有查到供应商银行账户信息" */) /* "没有查到供应商银行账户信息" */);
                }
                journalBillB.setOppositeaccountname(vendorBanksByAccountIdList.get(0).getAccountname());
            }
        }
    }

    private void fillOppositeForSupplier(JournalBill_b journalBillB) throws Exception {
        if (StringUtils.isEmpty(journalBillB.getOppositeid())) {
            List<VendorVO> vendorFieldByCode = vendorQueryService.getVendorFieldByCodeList(Collections.singletonList(journalBillB.getOppositecode()));
            if (CollectionUtils.isEmpty(vendorFieldByCode)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105023"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D74F205080009", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E7", "没有查到供应商信息") /* "没有查到供应商信息" */) /* "没有查到供应商信息" */);
            }
            journalBillB.setOppositeid(vendorFieldByCode.get(0).getId().toString());
            journalBillB.setOppositename(vendorFieldByCode.get(0).getName());
        } else {
            List<VendorVO> vendorFieldByIdList = vendorQueryService.getVendorFieldByIdList(Collections.singletonList(Long.valueOf(journalBillB.getOppositeid())));
            if (CollectionUtils.isEmpty(vendorFieldByIdList)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105023"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D74F205080009", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E7", "没有查到供应商信息") /* "没有查到供应商信息" */) /* "没有查到供应商信息" */);
            }
            journalBillB.setOppositename(vendorFieldByIdList.get(0).getName());
        }
    }

    private void fillOppositeAccountForCustomer(JournalBill_b journalBillB) throws Exception {
        if (StringUtils.isEmpty(journalBillB.getOppositeaccountid())) {
            if (StringUtils.isNotEmpty(journalBillB.getOppositebankaccountno())) {
                AgentFinancialDTO customerAccountByAccountNo = customerQueryService.getCustomerAccountByAccountNo(journalBillB.getOppositebankaccountno());
                if (customerAccountByAccountNo == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105022"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D746A04400008", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F3", "没有查到客户银行账户信息") /* "没有查到客户银行账户信息" */) /* "没有查到客户银行账户信息" */);
                }
                journalBillB.setOppositeaccountid(customerAccountByAccountNo.getId().toString());
                journalBillB.setOppositeaccountname(customerAccountByAccountNo.getBankAccountName());
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105022"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D746A04400008", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F3", "没有查到客户银行账户信息") /* "没有查到客户银行账户信息" */) /* "没有查到客户银行账户信息" */);
            }
        } else {
            if (StringUtils.isEmpty(journalBillB.getOppositeaccountname())) {
                AgentFinancialDTO customerAccountByAccountId = customerQueryService.getCustomerAccountByAccountId(Long.valueOf(journalBillB.getOppositeaccountid()));
                if (customerAccountByAccountId == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105022"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D746A04400008", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F3", "没有查到客户银行账户信息") /* "没有查到客户银行账户信息" */) /* "没有查到客户银行账户信息" */);
                }
                journalBillB.setOppositeaccountname(customerAccountByAccountId.getBankAccountName());
            }
        }
    }

    private void fillOppositeForCustomer(JournalBill_b journalBillB) throws Exception {
        if (StringUtils.isEmpty(journalBillB.getOppositeid())) {
            List<MerchantDTO> merchantDTOS = QueryBaseDocUtils.queryMerchantByCode(journalBillB.getOppositecode());
            if (CollectionUtils.isEmpty(merchantDTOS)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105021"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D740604400006", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E1", "没有查到客户信息") /* "没有查到客户信息" */) /* "没有查到客户信息" */);
            }
            journalBillB.setOppositeid(merchantDTOS.get(0).getId().toString());
            journalBillB.setOppositename(merchantDTOS.get(0).getName());
        } else {
            if (StringUtils.isEmpty(journalBillB.getOppositename())) {
                MerchantDTO merchantById = QueryBaseDocUtils.getMerchantById(Long.valueOf(journalBillB.getOppositeid()));
                if (merchantById == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105021"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218D740604400006", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E1", "没有查到客户信息") /* "没有查到客户信息" */) /* "没有查到客户信息" */);
                }
                journalBillB.setOppositename(merchantById.getName());
            }
        }
    }

    private void fillAmountDataForImportOrApi(JournalBill journalBill) throws Exception {
        String accEntityId = journalBill.getAccentity();
        FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accEntityId);
        String natCurrencyId;
        Integer natCurrencyMoneyDigit;
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
        if (currencyTenantDTO != null) {
            natCurrencyId = currencyTenantDTO.getId();
            journalBill.setNatCurrency(natCurrencyId);
            natCurrencyMoneyDigit = currencyTenantDTO.getMoneydigit();
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105018"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218B583804300004", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005EC", "未取到本位币信息") /* "未取到本位币信息" */) /* "未取到本位币信息" */);
        }
        for (JournalBill_b journalBillB : journalBill.JournalBill_b()) {
            String exchangeRateType = journalBillB.getExchangeRateType();
            List<Map<String, Object>> exchangeRateTypeCodeOrId = QueryBaseDocUtils.queryExchangeRateTypeByIdOrCode(exchangeRateType);
            if (CUSTOMER_EXCHANGE_RATE_TYPE.equals(exchangeRateTypeCodeOrId.get(0).get(ICmpConstant.CODE))) {
                if (Objects.equals(journalBillB.getCurrency(), journalBill.getNatCurrency())) {
                    journalBillB.setExchangeRateOps(ICmpConstant.EXCHANGE_RATE_OPS_MULTIPLY);
                    journalBillB.setExchangeRate(BigDecimal.ONE);
                } else {
                    journalBillB.setExchangeRateOps(ObjectUtils.isEmpty(journalBillB.getExchangeRateOps()) ? ICmpConstant.EXCHANGE_RATE_OPS_MULTIPLY : journalBillB.getExchangeRateOps());
                }
            } else {
                CmpExchangeRateVO exchangeRateWithMode = CmpExchangeRateUtils.getNewExchangeRateWithMode(journalBillB.getCurrency(), natCurrencyId, journalBill.getBillDate(), exchangeRateType);
                Short exchangeRateOps = exchangeRateWithMode.getExchangeRateOps();
                BigDecimal exchangeRate = exchangeRateWithMode.getExchangeRate();
                journalBillB.setExchangeRate(exchangeRate);
                journalBillB.setExchangeRateOps(exchangeRateOps);
            }
            if (journalBillB.getDebitnatSum() == null && journalBillB.getDebitoriSum() != null) {
                journalBillB.setDebitnatSum(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(journalBillB.getExchangeRateOps(), journalBillB.getExchangeRate(), journalBillB.getDebitoriSum(), natCurrencyMoneyDigit));
            }
            if (journalBillB.getCreditnatSum() == null && journalBillB.getCreditoriSum() != null) {
                journalBillB.setCreditnatSum(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(journalBillB.getExchangeRateOps(), journalBillB.getExchangeRate(), journalBillB.getCreditoriSum(), natCurrencyMoneyDigit));
            }
        }
    }

    private void checkBusinessDateAndDzdate(JournalBill journalBill) {
        if (CollectionUtils.isEmpty(journalBill.JournalBill_b())) {
            return;
        }
        journalBill.JournalBill_b().forEach(journalBillB -> {
            if (journalBillB.getDzdate().before(journalBillB.getBusinessDate())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105010"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218B473604B80002", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F4", "登账日期不可小于业务日期") /* "登账日期不可小于业务日期" */) /* "登账日期不可小于业务日期" */);
            }
        });
    }

    private void checkIsSettledForAccentity(JournalBill journalBill) throws Exception {
        Date maxSettleDate = settlementService.getMaxSettleDate(journalBill.getAccentity());
        if (maxSettleDate == null) {
            return;
        }
        Date billDate = journalBill.getBillDate();
        if (billDate.before(maxSettleDate)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100454"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00162", "单据日期已日结，不能保存单据！") /* "单据日期已日结，不能保存单据！" */);
        }
        if (CollectionUtils.isEmpty(journalBill.JournalBill_b())) {
            return;
        }
        journalBill.JournalBill_b().forEach(journalBillB -> {
            Date dzdate = journalBillB.getDzdate();
            if (dzdate.before(maxSettleDate)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100455"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00165", "登账日期已日结，不能保存单据！") /* "登账日期已日结，不能保存单据！" */);
            }
        });
    }

    private void checkDebitSumAndCreditSum(JournalBill journalBill) {
        if (CollectionUtils.isEmpty(journalBill.JournalBill_b())) {
            return;
        }
        journalBill.JournalBill_b().forEach(journalBillB -> {
            if (ObjectUtils.isEmpty(journalBillB.getPaymenttype())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105011"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21A5CEE404C00004", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005E5", "收付类型不能为空") /* "收付类型不能为空" */) /* "收付类型不能为空" */);
            }
            if (journalBillB.getPaymenttype() == JournalBillPaymentType.DEBIT.getValue()) {
                if (journalBillB.getDebitoriSum() == null || journalBillB.getDebitnatSum() == null
                    || BigDecimal.ZERO.compareTo(journalBillB.getDebitoriSum()) == 0
                        || BigDecimal.ZERO.compareTo(journalBillB.getDebitnatSum()) == 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105012"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218AC23C04380002", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005EA", "付款类型为收入时，收入本币以及原币金额不能为空或者0") /* "付款类型为收入时，收入本币以及原币金额不能为空或者0" */) /* "付款类型为收入时，收入本币以及原币金额不能为空或者0" */);
                }
                if (journalBillB.getCreditoriSum() != null || journalBillB.getCreditnatSum() != null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105013"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218AC32605680000", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005ED", "付款类型为收入时，支出本币以及原币金额不能有值") /* "付款类型为收入时，支出本币以及原币金额不能有值" */) /* "付款类型为收入时，支出本币以及原币金额不能有值" */);
                }
            } else if (journalBillB.getPaymenttype() == JournalBillPaymentType.CREDIT.getValue()) {
                if (journalBillB.getCreditoriSum() == null || journalBillB.getCreditnatSum() == null
                        || BigDecimal.ZERO.compareTo(journalBillB.getCreditoriSum()) == 0
                            || BigDecimal.ZERO.compareTo(journalBillB.getCreditnatSum()) == 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105014"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218AC37605680002", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005EF", "付款类型为支出时，支出本币以及原币金额不能为空或者0") /* "付款类型为支出时，支出本币以及原币金额不能为空或者0" */) /* "付款类型为收入时，收入本币以及原币金额不能为空或者0" */);
                }
                if (journalBillB.getDebitoriSum() != null || journalBillB.getDebitnatSum() != null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105015"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218AC46C05680004", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F0", "付款类型为支出时，收入本币以及原币金额不能有值") /* "付款类型为支出时，收入本币以及原币金额不能有值" */) /* "付款类型为支出时，收入本币以及原币金额不能有值" */);
                }
            }
        });
    }

    private void setGenerateType(BizObject bizObject, BillDataDto billDataDto, JournalBill journalBill) {
        if (ObjectUtils.isNotEmpty(journalBill.getGenerateType())) {
            return;
        }
        boolean importFlag =  ICmpConstant.IMPORT_FLAG.equals(billDataDto.getRequestAction());
        boolean apiFlag = bizObject.containsKey(ICmpConstant.FROM_API) && bizObject.get(ICmpConstant.FROM_API).equals(true) || billDataDto.getFromApi();
        journalBill.setGenerateType(JournalBillGenerateType.MANUAL_INPUT.getValue());
        if (importFlag) {
            journalBill.setGenerateType(JournalBillGenerateType.IMPORT.getValue());
        }
        if (apiFlag) {
            journalBill.setGenerateType(JournalBillGenerateType.API.getValue());
        }
    }
}
