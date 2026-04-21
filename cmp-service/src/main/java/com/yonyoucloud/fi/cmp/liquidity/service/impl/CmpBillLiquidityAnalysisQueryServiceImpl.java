package com.yonyoucloud.fi.cmp.liquidity.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.yonyou.ucf.mdd.common.utils.json.GsonHelper;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.liquidity.entity.dto.LiquidityCashQueryConditionDTO;
import com.yonyou.yonbip.ctm.liquidity.entity.dto.LiquidityItemDTO;
import com.yonyou.yonbip.ctm.liquidity.entity.dto.LiquidityQueryConditionDTO;
import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquidityAuditStatusEnum;
import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquidityOppTypeEnum;
import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquidityPayStatusEnum;
import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquiditySettleStatusEnum;
import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquiditySourceBusinessSystemEnum;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.PaymentStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.liquidity.service.CmpBillLiquidityAnalysisQueryService;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <h1>CmpBillLiquidityAnalysisQueryServiceImpl</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-05-18 10:42
 */
@Service
@Slf4j
public class CmpBillLiquidityAnalysisQueryServiceImpl implements CmpBillLiquidityAnalysisQueryService {
    @Override
    public List<LiquidityItemDTO> queryFundPaymentLiquidityAnalysisData(LiquidityQueryConditionDTO conditionDTO) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect(
                "accentity as accentity, " +
                        "tradetype, " +
                        "settlemode, " +
                        "FundPayment_b.fundPlanProject as planProgram, " +
                        "org as orgId, " +
                        "FundPayment_b.dept as deptId, " +
                        "FundPayment_b.project as projectId, " +
                        "id as businessID, " +
                        "FundPayment_b.enterprisebankaccount as ourAccId, " +
                        "FundPayment_b.cashaccount as ourCashAccId, " +
                        "vouchdate as businessDate, " +
                        "accentity.name as accentityName, " +
                        "code as businessNum ," +
                        "FundPayment_b.quickType as paymentType, " +
                        "FundPayment_b.noteno as noteno, " +
                        "FundPayment_b.expenseitem as expenseItem, " +
                        "FundPayment_b.caobject as caobject, currency, " +
                        "FundPayment_b.oppositeobjectname as oppName, " +
                        "FundPayment_b.oppositeobjectid as oppDocId, " +
                        "FundPayment_b.oppositeaccountno as oppAcc, " +
                        "FundPayment_b.oppositeaccountname as oppAccName, " +
                        "FundPayment_b.oppositebankType as oppAccType, " +
                        "FundPayment_b.oppositebankaddr as oppAccOpenName, " +
                        "FundPayment_b.oriSum as oriSum, " +
                        "FundPayment_b.exchangeRateType as exchangeRateType, " +
                        "settleflag, " +
                        "verifystate, " +
                        "FundPayment_b.settlestatus as settlestatus, " +
                        "FundPayment_b.description as description, " +
                        "FundPayment_b.id as businessDetailNum"
        );
        List<String> accentitys = conditionDTO.getAccentitys();
        if (ValueUtils.isNotEmpty(accentitys)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.ACCENTITY).in((Object) accentitys));
        }
        List<String> transType = conditionDTO.getTransType();
        if (CollectionUtils.isNotEmpty(transType)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.TRADE_TYPE).in((Object) transType));
        }

        String currencyId = conditionDTO.getCurrencyId();
        if (ValueUtils.isNotEmpty(currencyId)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.CURRENCY).eq(currencyId));
        }
        String currencyCode = conditionDTO.getCurrencyCode();
        if (ValueUtils.isNotEmpty(currencyCode)) {
            querySchema.appendQueryCondition(QueryCondition.name("currency.code").eq(currencyCode));
        }
        List<String> bankAccountIds = conditionDTO.getBankAccountIds();
        if (ValueUtils.isNotEmpty(bankAccountIds)) {
            querySchema.appendQueryCondition(QueryCondition.name("FundPayment_b.enterprisebankaccount").in((Object) bankAccountIds));
        }

        QueryConditionGroup queryConditionGroup7 = new QueryConditionGroup(ConditionOperator.or);
        QueryConditionGroup queryConditionGroup9 = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup9.addCondition(QueryCondition.name("settleflag").eq(0));
        queryConditionGroup7.addCondition(queryConditionGroup9);
        QueryConditionGroup queryConditionGroup8 = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup8.addCondition(QueryCondition.name("settleflag").eq(1));
        queryConditionGroup8.addCondition(QueryCondition.name("verifystate").in((Object) new Integer[]{0, 1, 4}));
        queryConditionGroup7.addCondition(queryConditionGroup8);
        querySchema.appendQueryCondition(queryConditionGroup7);

        querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.VOUCHDATE).between(conditionDTO.getBeginDate(), conditionDTO.getEndDate()));
        List<BizObject> bizObjects = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, querySchema, null);
        List<LiquidityItemDTO> liquidityItemDTOS = new ArrayList<>();
        for (BizObject bizObject : bizObjects) {
            LiquidityItemDTO liquidityItemDTO = new LiquidityItemDTO();
            liquidityItemDTO.setAccentity(bizObject.getString("accentity"));
            liquidityItemDTO.setAccentityName(bizObject.getString("accentityName"));
            liquidityItemDTO.setPaymentType(bizObject.getString("paymentType"));
            liquidityItemDTO.setBillId(bizObject.getString("businessID"));
            liquidityItemDTO.setBillDetailNum(bizObject.getString("businessDetailNum"));
            liquidityItemDTO.setBillNum(bizObject.getString("businessNum"));
            liquidityItemDTO.setServiceCode(IServicecodeConstant.FUNDPAYMENT);
            liquidityItemDTO.setSourceBusinessSystem(LiquiditySourceBusinessSystemEnum.CashSystem);
            liquidityItemDTO.setPlanProgram(bizObject.getString("planProgram"));
            liquidityItemDTO.setOrgId(bizObject.getString("orgId"));
            liquidityItemDTO.setDeptId(bizObject.getString("deptId"));
            liquidityItemDTO.setProjectId(bizObject.getString("projectId"));
            liquidityItemDTO.setBusinessDate(bizObject.getDate("businessDate"));
            liquidityItemDTO.setTransType(bizObject.getString("tradetype"));
            liquidityItemDTO.setSettlementMethod(bizObject.getString("settlemode"));
            liquidityItemDTO.setOurAccId(bizObject.getString("ourAccId"));
            liquidityItemDTO.setOurCashAccId(bizObject.getString("ourCashAccId"));
            liquidityItemDTO.setNoteId(bizObject.getString("noteno"));
            liquidityItemDTO.setExpenseItem(bizObject.getString("expenseItem"));
            liquidityItemDTO.setOppType(getOppType(bizObject));
            liquidityItemDTO.setOppName(bizObject.getString("oppName"));
            liquidityItemDTO.setOppDocId(bizObject.getString("oppDocId"));
            liquidityItemDTO.setOppAcc(bizObject.getString("oppAcc"));
            liquidityItemDTO.setOppAccName(bizObject.getString("oppAccName"));
            liquidityItemDTO.setOppAccType(bizObject.getString("oppAccType"));
            liquidityItemDTO.setOppAccOpenName(bizObject.getString("oppAccOpenName"));
            liquidityItemDTO.setReceiptType(2);
            liquidityItemDTO.setOriginCurrency(bizObject.getString("currency"));
            liquidityItemDTO.setOriginAmount(bizObject.getBigDecimal(ICmpConstant.ORISUM));
            liquidityItemDTO.setPlanSettlementDate(bizObject.getDate("businessDate"));
            liquidityItemDTO.setExchangeRateType(bizObject.getString("exchangeRateType"));
            liquidityItemDTO.setRemark(bizObject.getString("description"));
            liquidityItemDTO.setIsIntegrateSettlementPlatform(bizObject.getInteger("settleflag") == 1);
            liquidityItemDTO.setBusinessStatus(getLiquidityAuditStatusEnum(bizObject));
            liquidityItemDTO.setSettleStatus(getLiquiditySettleStatusEnum(bizObject));
            liquidityItemDTOS.add(liquidityItemDTO);
        }
        return liquidityItemDTOS;
    }

    private static Set<Short> getSettleStatusParams(List<LiquiditySettleStatusEnum> settleStatusParamsList) {
        Set<Short> settleStatusList = new HashSet<>();
        for (LiquiditySettleStatusEnum liquiditySettleStatusEnum : settleStatusParamsList) {
            if (liquiditySettleStatusEnum == LiquiditySettleStatusEnum.UNSETTLE) {
                settleStatusList.add(FundSettleStatus.WaitSettle.getValue());
            } else if (liquiditySettleStatusEnum == LiquiditySettleStatusEnum.INSETTLE) {
                settleStatusList.add(FundSettleStatus.SettleProssing.getValue());
            } else if (liquiditySettleStatusEnum == LiquiditySettleStatusEnum.SETTLESUCCESS) {
                settleStatusList.addAll(Arrays.asList(FundSettleStatus.Refund.getValue(),
                        FundSettleStatus.SettleSuccess.getValue(),
                        FundSettleStatus.SettlementSupplement.getValue()));
            } else if (liquiditySettleStatusEnum == LiquiditySettleStatusEnum.SETTLEFAIL) {
                settleStatusList.add(FundSettleStatus.SettleFailed.getValue());
            }
        }
        return settleStatusList;
    }

    private static @NotNull Set<Short> getVerifyStateList(List<LiquidityAuditStatusEnum> businessStatusList) {
        Set<Short> verifyStateList = new HashSet<>();
        for (LiquidityAuditStatusEnum liquidityAuditStatusEnum : businessStatusList) {
            if (liquidityAuditStatusEnum == LiquidityAuditStatusEnum.UnSubmit) {
                verifyStateList.add(VerifyState.INIT_NEW_OPEN.getValue());
            } else if (liquidityAuditStatusEnum == LiquidityAuditStatusEnum.Approval) {
                verifyStateList.add(VerifyState.SUBMITED.getValue());
            } else if (liquidityAuditStatusEnum == LiquidityAuditStatusEnum.Retured) {
                verifyStateList.add(VerifyState.REJECTED_TO_MAKEBILL.getValue());
            } else if (liquidityAuditStatusEnum == LiquidityAuditStatusEnum.Passed) {
                verifyStateList.add(VerifyState.COMPLETED.getValue());
            } else if (liquidityAuditStatusEnum == LiquidityAuditStatusEnum.Stopped) {
                verifyStateList.add(VerifyState.REJECTED_TO_MAKEBILL.getValue());
            }
        }
        return verifyStateList;
    }

    private @NotNull LiquiditySettleStatusEnum getLiquiditySettleStatusEnum(BizObject bizObject) {
        LiquiditySettleStatusEnum settleStatus = null;
        Short settlestatus = bizObject.getShort("settlestatus");
        if (settlestatus == FundSettleStatus.WaitSettle.getValue()) {
            settleStatus = LiquiditySettleStatusEnum.UNSETTLE;
        } else if (settlestatus == FundSettleStatus.SettleProssing.getValue()) {
            settleStatus = LiquiditySettleStatusEnum.INSETTLE;
        } else if (settlestatus == FundSettleStatus.SettleSuccess.getValue()) {
            settleStatus = LiquiditySettleStatusEnum.SETTLESUCCESS;
        } else if (settlestatus == FundSettleStatus.SettleFailed.getValue()) {
            settleStatus = LiquiditySettleStatusEnum.SETTLEFAIL;
        } else {
            settleStatus = LiquiditySettleStatusEnum.SETTLESUCCESS;
        }
        return settleStatus;
    }

    private @NotNull LiquidityAuditStatusEnum getLiquidityAuditStatusEnum(BizObject bizObject) {
        LiquidityAuditStatusEnum businessStatus = null;
        Short verifystate = bizObject.getShort("verifystate");
        if (verifystate == VerifyState.INIT_NEW_OPEN.getValue()) {
            businessStatus = LiquidityAuditStatusEnum.UnSubmit;
        } else if (verifystate == VerifyState.SUBMITED.getValue()) {
            businessStatus = LiquidityAuditStatusEnum.Approval;
        } else if (verifystate == VerifyState.COMPLETED.getValue()) {
            businessStatus = LiquidityAuditStatusEnum.Passed;
        } else if (verifystate == VerifyState.TERMINATED.getValue()) {
            businessStatus = LiquidityAuditStatusEnum.Stopped;
        } else if (verifystate == VerifyState.REJECTED_TO_MAKEBILL.getValue()) {
            businessStatus = LiquidityAuditStatusEnum.Retured;
        } else {
            businessStatus = LiquidityAuditStatusEnum.UnSubmit;
        }
        return businessStatus;
    }

    private LiquidityOppTypeEnum getOppType(BizObject bizObject) {
        Short caObject = bizObject.getShort(ICmpConstant.CA_OBJECT);
        LiquidityOppTypeEnum oppType = null;
        if (caObject == 1) {
            oppType = LiquidityOppTypeEnum.Customer;
        } else if (caObject == 2) {
            oppType = LiquidityOppTypeEnum.Supplier;
        } else if (caObject == 3) {
            oppType = LiquidityOppTypeEnum.Employee;
        } else if (caObject == 4) {
            oppType = LiquidityOppTypeEnum.Other;
        } else if (caObject == 5) {
            oppType = LiquidityOppTypeEnum.FundsBusinessObject;
        } else if (caObject == 6) {
            oppType = LiquidityOppTypeEnum.InnerUnit;
        }
        return oppType;
    }

    @Override
    public List<LiquidityItemDTO> queryFundCollectionLiquidityAnalysisData(LiquidityQueryConditionDTO conditionDTO) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect(
                "accentity as accentity, " +
                        "tradetype, " +
                        "settlemode, " +
                        "FundCollection_b.fundPlanProject as planProgram, " +
                        "org as orgId, " +
                        "FundCollection_b.dept as deptId, " +
                        "FundCollection_b.project as projectId, " +
                        "id as businessID, " +
                        "enterprisebankaccount as ourAccId, " +
                        "cashaccount as ourCashAccId, " +
                        "vouchdate as businessDate, " +
                        "accentity.name as accentityName, " +
                        "code as businessNum ," +
                        "FundCollection_b.quickType as paymentType, " +
                        "FundCollection_b.noteno as noteno, " +
                        "FundCollection_b.expenseitem as expenseItem, " +
                        "FundCollection_b.caobject as caobject, currency, " +
                        "FundCollection_b.oppositeobjectname as oppName, " +
                        "FundCollection_b.oppositeobjectid as oppDocId, " +
                        "FundCollection_b.oppositeaccountno as oppAcc, " +
                        "FundCollection_b.oppositeaccountname as oppAccName, " +
                        "FundCollection_b.oppositebankType as oppAccType, " +
                        "FundCollection_b.oppositebankaddr as oppAccOpenName, " +
                        "FundCollection_b.oriSum as oriSum, " +
                        "FundCollection_b.exchangeRateType as exchangeRateType, " +
                        "settleflag, " +
                        "verifystate, " +
                        "FundCollection_b.settlestatus as settlestatus, " +
                        "FundCollection_b.description as description, " +
                        "FundCollection_b.id as businessDetailNum"
        );

        List<String> accentitys = conditionDTO.getAccentitys();
        if (ValueUtils.isNotEmpty(accentitys)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.ACCENTITY).in((Object) accentitys));
        }
        List<String> transType = conditionDTO.getTransType();
        if (CollectionUtils.isNotEmpty(transType)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.TRADE_TYPE).in((Object) transType));
        }

        String currencyId = conditionDTO.getCurrencyId();
        if (ValueUtils.isNotEmpty(currencyId)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.CURRENCY).eq(currencyId));
        }
        String currencyCode = conditionDTO.getCurrencyCode();
        if (ValueUtils.isNotEmpty(currencyCode)) {
            querySchema.appendQueryCondition(QueryCondition.name("currency.code").eq(currencyCode));
        }
        List<String> bankAccountIds = conditionDTO.getBankAccountIds();
        if (ValueUtils.isNotEmpty(bankAccountIds)) {
            querySchema.appendQueryCondition(QueryCondition.name("FundCollection_b.enterprisebankaccount").in((Object) bankAccountIds));
        }

        QueryConditionGroup queryConditionGroup7 = new QueryConditionGroup(ConditionOperator.or);
        QueryConditionGroup queryConditionGroup9 = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup9.addCondition(QueryCondition.name("settleflag").eq(0));
        queryConditionGroup7.addCondition(queryConditionGroup9);
        QueryConditionGroup queryConditionGroup8 = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup8.addCondition(QueryCondition.name("settleflag").eq(1));
        queryConditionGroup8.addCondition(QueryCondition.name("verifystate").in((Object) new Integer[]{0, 1, 4}));
        queryConditionGroup7.addCondition(queryConditionGroup8);
        querySchema.appendQueryCondition(queryConditionGroup7);

        querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.VOUCHDATE).between(conditionDTO.getBeginDate(), conditionDTO.getEndDate()));
        List<BizObject> bizObjects = MetaDaoHelper.queryObject(FundCollection.ENTITY_NAME, querySchema, null);
        List<LiquidityItemDTO> liquidityItemDTOS = new ArrayList<>();
        for (BizObject bizObject : bizObjects) {
            LiquidityItemDTO liquidityItemDTO = new LiquidityItemDTO();
            liquidityItemDTO.setAccentity(bizObject.getString("accentity"));
            liquidityItemDTO.setAccentityName(bizObject.getString("accentityName"));
            liquidityItemDTO.setPaymentType(bizObject.getString("paymentType"));
            liquidityItemDTO.setBillId(bizObject.getString("businessID"));
            liquidityItemDTO.setBillDetailNum(bizObject.getString("businessDetailNum"));
            liquidityItemDTO.setBillNum(bizObject.getString("businessNum"));
            liquidityItemDTO.setServiceCode(IServicecodeConstant.FUNDCOLLECTION);
            liquidityItemDTO.setSourceBusinessSystem(LiquiditySourceBusinessSystemEnum.CashSystem);
            liquidityItemDTO.setPlanProgram(bizObject.getString("planProgram"));
            liquidityItemDTO.setOrgId(bizObject.getString("orgId"));
            liquidityItemDTO.setDeptId(bizObject.getString("deptId"));
            liquidityItemDTO.setProjectId(bizObject.getString("projectId"));
            liquidityItemDTO.setBusinessDate(bizObject.getDate("businessDate"));
            // liquidityItemDTO.setBusinessType(LiquidityBusinessBillTypeEnum.InvestmentPayBill);
            liquidityItemDTO.setTransType(bizObject.getString("tradetype"));
            liquidityItemDTO.setTransType(bizObject.getString("tradetype"));
            liquidityItemDTO.setSettlementMethod(bizObject.getString("settlemode"));
            liquidityItemDTO.setOurAccId(bizObject.getString("ourAccId"));
            liquidityItemDTO.setOurCashAccId(bizObject.getString("ourCashAccId"));
            liquidityItemDTO.setNoteId(bizObject.getString("noteno"));
            liquidityItemDTO.setExpenseItem(bizObject.getString("expenseItem"));
            liquidityItemDTO.setOppType(getOppType(bizObject));
            liquidityItemDTO.setOppName(bizObject.getString("oppName"));
            liquidityItemDTO.setOppDocId(bizObject.getString("oppDocId"));
            liquidityItemDTO.setOppAcc(bizObject.getString("oppAcc"));
            liquidityItemDTO.setOppAccName(bizObject.getString("oppAccName"));
            liquidityItemDTO.setOppAccType(bizObject.getString("oppAccType"));
            liquidityItemDTO.setOppAccOpenName(bizObject.getString("oppAccOpenName"));
            liquidityItemDTO.setReceiptType(1);
            liquidityItemDTO.setOriginCurrency(bizObject.getString("currency"));
            liquidityItemDTO.setOriginAmount(bizObject.getBigDecimal(ICmpConstant.ORISUM));
            liquidityItemDTO.setPlanSettlementDate(bizObject.getDate("businessDate"));
            liquidityItemDTO.setExchangeRateType(bizObject.getString("exchangeRateType"));
            liquidityItemDTO.setRemark(bizObject.getString("description"));
            liquidityItemDTO.setIsIntegrateSettlementPlatform(bizObject.getInteger("settleflag") == 1);
            liquidityItemDTO.setBusinessStatus(getLiquidityAuditStatusEnum(bizObject));
            liquidityItemDTO.setSettleStatus(getLiquiditySettleStatusEnum(bizObject));
            liquidityItemDTOS.add(liquidityItemDTO);
        }
        return liquidityItemDTOS;
    }

    @Override
    public List<LiquidityItemDTO> querySalaryPayLiquidityAnalysisData(LiquidityQueryConditionDTO conditionDTO) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect(
                "accentity as accentity, " +
                        "tradetype, " +
                        "settlemode, " +
                        "fundPlanProject as planProgram, " +
                        "org as orgId, " +
                        "dept as deptId, " +
                        "project as projectId, " +
                        "id as businessID, " +
                        "payBankAccount as ourAccId, " +
                        "vouchdate as businessDate, " +
                        "accentity.name as accentityName, " +
                        "code as businessNum ," +
                        "expenseitem as expenseItem, " +
                        "currency, " +
                        "Salarypay_b.crtacc as oppAcc, " +
                        "Salarypay_b.crtaccname as oppAccName, " +
                        "Salarypay_b.crtbank as oppAccOpenName, " +
                        "Salarypay_b.amount as oriSum, " +
                        "exchangeRateType as exchangeRateType, " +
                        "verifystate, " +
                        "settlestatus, " +
                        "Salarypay_b.postscript as note, " +
                        "Salarypay_b.tradestatus as tradestatus, " +
                        "Salarypay_b.id as businessDetailNum"
        );


        List<String> accentitys = conditionDTO.getAccentitys();
        if (ValueUtils.isNotEmpty(accentitys)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.ACCENTITY).in((Object) accentitys));
        }
        List<String> transType = conditionDTO.getTransType();
        if (CollectionUtils.isNotEmpty(transType)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.TRADE_TYPE).in((Object) transType));
        }

        String currencyId = conditionDTO.getCurrencyId();
        if (ValueUtils.isNotEmpty(currencyId)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.CURRENCY).eq(currencyId));
        }
        String currencyCode = conditionDTO.getCurrencyCode();
        if (ValueUtils.isNotEmpty(currencyCode)) {
            querySchema.appendQueryCondition(QueryCondition.name("currency.code").eq(currencyCode));
        }
        List<String> bankAccountIds = conditionDTO.getBankAccountIds();
        if (ValueUtils.isNotEmpty(bankAccountIds)) {
            querySchema.appendQueryCondition(QueryCondition.name("payBankAccount").in((Object) bankAccountIds));
        }
        List<LiquidityCashQueryConditionDTO> cashLiquidityQuerys = conditionDTO.getCashLiquidityQuerys();
        for (LiquidityCashQueryConditionDTO cashLiquidityQuery : cashLiquidityQuerys) {
            List<LiquidityPayStatusEnum> payStatusParamsList = cashLiquidityQuery.getPayStatus();
            if (ValueUtils.isNotEmpty(payStatusParamsList)) {
                Set<Short> payStatusList = new HashSet<>();
                for (LiquidityPayStatusEnum liquidityPayStatusEnum : payStatusParamsList) {
                    if (liquidityPayStatusEnum == LiquidityPayStatusEnum.NoPay) {
                        payStatusList.add(PaymentStatus.NoPay.getValue());
                    } else if (liquidityPayStatusEnum == LiquidityPayStatusEnum.PayUnknown) {
                        payStatusList.add(PaymentStatus.PayDone.getValue());
                    } else if (liquidityPayStatusEnum == LiquidityPayStatusEnum.Fail) {
                        payStatusList.add(PaymentStatus.PayFail.getValue());
                    } else if (liquidityPayStatusEnum == LiquidityPayStatusEnum.Success) {
                        payStatusList.add(PaymentStatus.UnkownPay.getValue());
                    }
                }
                if (CollectionUtils.isNotEmpty(payStatusList)) {
                    querySchema.appendQueryCondition(QueryCondition.name("Salarypay_b.tradestatus").in((Object) payStatusList));
                }
            }
            List<LiquidityAuditStatusEnum> businessStatusList = cashLiquidityQuery.getBusinessStatus();
            if (ValueUtils.isNotEmpty(businessStatusList)) {
                Set<Short> verifyStateList = getVerifyStateList(businessStatusList);
                querySchema.appendQueryCondition(QueryCondition.name("verifystate").in((Object) verifyStateList));
            }
        }

        querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.VOUCHDATE).between(conditionDTO.getBeginDate(), conditionDTO.getEndDate()));
        List<BizObject> bizObjects = MetaDaoHelper.queryObject(Salarypay.ENTITY_NAME, querySchema, null);
        List<LiquidityItemDTO> liquidityItemDTOS = new ArrayList<>();
        for (BizObject bizObject : bizObjects) {
            LiquidityItemDTO liquidityItemDTO = new LiquidityItemDTO();
            liquidityItemDTO.setAccentity(bizObject.getString("accentity"));
            liquidityItemDTO.setAccentityName(bizObject.getString("accentityName"));
            liquidityItemDTO.setBillId(bizObject.getString("businessID"));
            liquidityItemDTO.setBillDetailNum(bizObject.getString("businessDetailNum"));
            liquidityItemDTO.setBillNum(bizObject.getString("businessNum"));
            liquidityItemDTO.setServiceCode(IServicecodeConstant.SALARYPAY);
            liquidityItemDTO.setSourceBusinessSystem(LiquiditySourceBusinessSystemEnum.CashSystem);
            liquidityItemDTO.setPlanProgram(bizObject.getString("planProgram"));
            liquidityItemDTO.setOrgId(bizObject.getString("orgId"));
            liquidityItemDTO.setDeptId(bizObject.getString("deptId"));
            liquidityItemDTO.setProjectId(bizObject.getString("projectId"));
            liquidityItemDTO.setBusinessDate(bizObject.getDate("businessDate"));
            // liquidityItemDTO.setBusinessType(LiquidityBusinessBillTypeEnum.InvestmentPayBill);
            liquidityItemDTO.setTransType(bizObject.getString("tradetype"));
            liquidityItemDTO.setSettlementMethod(bizObject.getString("settlemode"));
            liquidityItemDTO.setOurAccId(bizObject.getString("ourAccId"));
            liquidityItemDTO.setNoteId(bizObject.getString("noteno"));
            liquidityItemDTO.setExpenseItem(bizObject.getString("expenseItem"));
            liquidityItemDTO.setOppType(LiquidityOppTypeEnum.Other);
            liquidityItemDTO.setOppAcc(bizObject.getString("oppAcc"));
            liquidityItemDTO.setOppAccName(bizObject.getString("oppAccName"));
            liquidityItemDTO.setOppAccOpenName(bizObject.getString("oppAccOpenName"));
            liquidityItemDTO.setReceiptType(2);
            liquidityItemDTO.setOriginCurrency(bizObject.getString("currency"));
            liquidityItemDTO.setOriginAmount(bizObject.getBigDecimal(ICmpConstant.ORISUM));
            liquidityItemDTO.setPlanSettlementDate(bizObject.getDate("businessDate"));
            liquidityItemDTO.setExchangeRateType(bizObject.getString("exchangeRateType"));
            liquidityItemDTO.setRemark(bizObject.getString("note"));
            liquidityItemDTO.setIsIntegrateSettlementPlatform(Boolean.FALSE);
            Short tradeStatus = bizObject.getShort("tradestatus");
            LiquidityPayStatusEnum payStatus = null;
            if (tradeStatus == 1) {
                payStatus = LiquidityPayStatusEnum.NoPay;
            } else if (tradeStatus == 2) {
                payStatus = LiquidityPayStatusEnum.PayUnknown;
            } else if (tradeStatus == 3) {
                payStatus = LiquidityPayStatusEnum.Fail;
            } else if (tradeStatus == 4) {
                payStatus = LiquidityPayStatusEnum.Success;
            }
            liquidityItemDTO.setPayStatus(payStatus);
            liquidityItemDTO.setBusinessStatus(getLiquidityAuditStatusEnum(bizObject));
            liquidityItemDTOS.add(liquidityItemDTO);
        }
        return liquidityItemDTOS;
    }

    @Override
    public List<LiquidityItemDTO> queryTransferAccountLiquidityAnalysisData(LiquidityQueryConditionDTO conditionDTO) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect(
                "accentity, " +
                        "tradetype, " +
                        "settlemode, isSettlement, collectsettlemode, " +
                        "dept as deptId, " +
                        "project as projectId, " +
                        "id as businessID, " +
                        "payBankAccount as ourAccId, " +
                        "payCashAccount as ourCashAccId, " +
                        "vouchdate as businessDate, " +
                        "accentity.name as accentityName, " +
                        "code as businessNum ," +
                        "currency, " +
                        "recBankAccount as recBankAccount, " +
                        "recCashAccount as recCashAccount, " +
                        "oriSum, " +
                        "exchangeRateType as exchangeRateType, " +
                        "verifystate, " +
                        "settlestatus, " +
                        "description as note, " +
                        "settlestatus, paystatus"
        );


        List<String> accentitys = conditionDTO.getAccentitys();
        if (ValueUtils.isNotEmpty(accentitys)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.ACCENTITY).in((Object) accentitys));
        }
        List<String> transType = conditionDTO.getTransType();
        if (CollectionUtils.isNotEmpty(transType)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.TRADE_TYPE).in((Object) transType));
        }

        String currencyId = conditionDTO.getCurrencyId();
        if (ValueUtils.isNotEmpty(currencyId)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.CURRENCY).eq(currencyId));
        }
        String currencyCode = conditionDTO.getCurrencyCode();
        if (ValueUtils.isNotEmpty(currencyCode)) {
            querySchema.appendQueryCondition(QueryCondition.name("currency.code").eq(currencyCode));
        }
        List<String> bankAccountIds = conditionDTO.getBankAccountIds();
        if (ValueUtils.isNotEmpty(bankAccountIds)) {
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.or);
            queryConditionGroup.addCondition(QueryCondition.name("payBankAccount").in((Object) bankAccountIds));
            queryConditionGroup.addCondition(QueryCondition.name("payCashAccount").in((Object) bankAccountIds));
            querySchema.appendQueryCondition(queryConditionGroup);
        }



        QueryConditionGroup queryConditionGroup7 = new QueryConditionGroup(ConditionOperator.or);
        QueryConditionGroup queryConditionGroup9 = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup9.addCondition(QueryCondition.name("isSettlement").eq(0));
        queryConditionGroup7.addCondition(queryConditionGroup9);
        QueryConditionGroup queryConditionGroup8 = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup8.addCondition(QueryCondition.name("isSettlement").eq(1));
        queryConditionGroup8.addCondition(QueryCondition.name("verifystate").in((Object) new Integer[]{0, 1, 4}));
        queryConditionGroup7.addCondition(queryConditionGroup8);
        querySchema.appendQueryCondition(queryConditionGroup7);

        querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.VOUCHDATE).between(conditionDTO.getBeginDate(), conditionDTO.getEndDate()));
        List<BizObject> bizObjects = MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, querySchema, null);
        List<LiquidityItemDTO> liquidityItemDTOS = new ArrayList<>();
        Gson gson = new Gson();
        for (BizObject bizObject : bizObjects) {
            LiquidityItemDTO liquidityItemDTOReceipt = new LiquidityItemDTO();
            liquidityItemDTOReceipt.setAccentity(bizObject.getString("accentity"));
            liquidityItemDTOReceipt.setAccentityName(bizObject.getString("accentityName"));
            liquidityItemDTOReceipt.setBillId(bizObject.getString("businessID"));
            liquidityItemDTOReceipt.setBillDetailNum(bizObject.getString("businessDetailNum"));
            liquidityItemDTOReceipt.setBillNum(bizObject.getString("businessNum"));
            liquidityItemDTOReceipt.setServiceCode(IServicecodeConstant.TRANSFERACCOUNT);
            liquidityItemDTOReceipt.setSourceBusinessSystem(LiquiditySourceBusinessSystemEnum.CashSystem);
            liquidityItemDTOReceipt.setDeptId(bizObject.getString("deptId"));
            liquidityItemDTOReceipt.setProjectId(bizObject.getString("projectId"));
            liquidityItemDTOReceipt.setTransType(bizObject.getString("tradetype"));
            liquidityItemDTOReceipt.setSettlementMethod(bizObject.getString("settlemode"));
            liquidityItemDTOReceipt.setOurAccId(bizObject.getString("ourAccId"));
            liquidityItemDTOReceipt.setOurCashAccId(bizObject.getString("ourCashAccId"));
            liquidityItemDTOReceipt.setOppType(LiquidityOppTypeEnum.InnerUnit);
            liquidityItemDTOReceipt.setOppDocId(bizObject.getString("accentity"));
            liquidityItemDTOReceipt.setOppName(bizObject.getString("accentityName"));

            String oppAcc = ValueUtils.isEmpty(bizObject.getString("recBankAccount"))
                    ? bizObject.getString("recCashAccount") : bizObject.getString("recBankAccount");
            liquidityItemDTOReceipt.setOppAcc(oppAcc);
            liquidityItemDTOReceipt.setReceiptType(2);
            liquidityItemDTOReceipt.setOriginCurrency(bizObject.getString("currency"));
            liquidityItemDTOReceipt.setOriginAmount(bizObject.getBigDecimal(ICmpConstant.ORISUM));
            liquidityItemDTOReceipt.setExchangeRateType(bizObject.getString("exchangeRateType"));
            liquidityItemDTOReceipt.setRemark(bizObject.getString("note"));
            liquidityItemDTOReceipt.setBusinessStatus(getLiquidityAuditStatusEnum(bizObject));
            liquidityItemDTOReceipt.setIsIntegrateSettlementPlatform(ValueUtils.isNotEmptyObj(bizObject.getBoolean("isSettlement"))
                    ? bizObject.getBoolean("isSettlement") : false);

            if (liquidityItemDTOReceipt.getIsIntegrateSettlementPlatform()) {
                Short settleStatus = bizObject.getShort("settlestatus");
                LiquiditySettleStatusEnum settleStatusOutParam = null;
                if (SettleStatus.noSettlement.getValue() == settleStatus) {
                    settleStatusOutParam = LiquiditySettleStatusEnum.UNSETTLE;
                } else if (SettleStatus.SettleProssing.getValue() == settleStatus) {
                    settleStatusOutParam = LiquiditySettleStatusEnum.INSETTLE;
                } else if (SettleStatus.alreadySettled.getValue() == settleStatus) {
                    settleStatusOutParam = LiquiditySettleStatusEnum.SETTLESUCCESS;
                } else if (SettleStatus.SettleFailed.getValue() == settleStatus) {
                    settleStatusOutParam = LiquiditySettleStatusEnum.SETTLEFAIL;
                } else if (SettleStatus.SettledRep.getValue() == settleStatus) {
                    settleStatusOutParam = LiquiditySettleStatusEnum.SETTLESUCCESS;
                }
                liquidityItemDTOReceipt.setSettleStatus(settleStatusOutParam);

            } else {
                Short paystatus = bizObject.getShort("paystatus");
                LiquidityPayStatusEnum payStatus = null;
                if (paystatus == 1) {
                    payStatus = LiquidityPayStatusEnum.NoPay;
                } else if (paystatus == 2) {
                    payStatus = LiquidityPayStatusEnum.PayUnknown;
                } else if (paystatus == 3) {
                    payStatus = LiquidityPayStatusEnum.Fail;
                } else if (paystatus == 4) {
                    payStatus = LiquidityPayStatusEnum.Success;
                }
                liquidityItemDTOReceipt.setPayStatus(payStatus);
            }

            //todo 315不合release
            LiquidityItemDTO liquidityItemDTOPay = (LiquidityItemDTO) GsonHelper.FromJSon(JSON.toJSONString(liquidityItemDTOReceipt), LiquidityItemDTO.class);
            liquidityItemDTOPay.setSettlementMethod(bizObject.getString("collectsettlemode"));
            liquidityItemDTOPay.setReceiptType(1);

            String oppAccPay = ValueUtils.isEmpty(liquidityItemDTOReceipt.getOurAccId())
                    ? liquidityItemDTOReceipt.getOurCashAccId() : liquidityItemDTOReceipt.getOurAccId();
            liquidityItemDTOPay.setOppAcc(oppAccPay);
            liquidityItemDTOPay.setOurAccId(bizObject.getString("recBankAccount"));
            liquidityItemDTOPay.setOurCashAccId(bizObject.getString("recCashAccount"));
            Date businessDate = bizObject.getDate("businessDate");

            liquidityItemDTOPay.setPlanSettlementDate(businessDate);
            liquidityItemDTOReceipt.setPlanSettlementDate(businessDate);
            liquidityItemDTOPay.setBusinessDate(businessDate);
            liquidityItemDTOS.add(liquidityItemDTOPay);
            liquidityItemDTOReceipt.setBusinessDate(businessDate);
            liquidityItemDTOS.add(liquidityItemDTOReceipt);
        }
        return liquidityItemDTOS;
    }

    @Override
    public List<LiquidityItemDTO> queryForeignPaymentLiquidityAnalysisData(LiquidityQueryConditionDTO conditionDTO) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect(
                "accentity, " +
                        "tradetype, " +
                        "settlemode, " +
                        "org as orgId, " +
                        "dept as deptId, " +
                        "project as projectId, " +
                        "id as businessID, " +
                        "paymenterprisebankaccount as ourAccId, " +
                        "expectedsettlementdate as businessDate, " +
                        "accentity.name as accentityName, " +
                        "code as businessNum ," +
                        "expenseitem as expenseItem, " +
                        "currency, " +
                        "receivenameid, " +
                        "receivebankaccountid, " +
                        "amount, receivebankaddress, " +
                        "currencyexchangeratetype as exchangeRateType, " +
                        "verifystate, " +
                        "settlestatus, " +
                        "description as note, " +
                        "receivetype as caobject, " +
                        "settlestatus"
        );
        List<String> accentitys = conditionDTO.getAccentitys();
        if (ValueUtils.isNotEmpty(accentitys)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.ACCENTITY).in((Object) accentitys));
        }
        List<String> transType = conditionDTO.getTransType();
        if (CollectionUtils.isNotEmpty(transType)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.TRADE_TYPE).in((Object) transType));
        }

        String currencyId = conditionDTO.getCurrencyId();
        if (ValueUtils.isNotEmpty(currencyId)) {
            querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.CURRENCY).eq(currencyId));
        }
        String currencyCode = conditionDTO.getCurrencyCode();
        if (ValueUtils.isNotEmpty(currencyCode)) {
            querySchema.appendQueryCondition(QueryCondition.name("currency.code").eq(currencyCode));
        }
        List<String> bankAccountIds = conditionDTO.getBankAccountIds();
        if (ValueUtils.isNotEmpty(bankAccountIds)) {
            querySchema.appendQueryCondition(QueryCondition.name("receivebankaccountid").in((Object) bankAccountIds));
        }
        List<LiquidityCashQueryConditionDTO> cashLiquidityQuerys = conditionDTO.getCashLiquidityQuerys();
        for (LiquidityCashQueryConditionDTO cashLiquidityQuery : cashLiquidityQuerys) {
            List<LiquidityAuditStatusEnum> businessStatusList = cashLiquidityQuery.getBusinessStatus();
            if (ValueUtils.isNotEmpty(businessStatusList)) {
                Set<Short> verifyStateList = getVerifyStateList(businessStatusList);
                querySchema.appendQueryCondition(QueryCondition.name("verifystate").in((Object) verifyStateList));
            }
            List<LiquiditySettleStatusEnum> settleStatusParamsList = cashLiquidityQuery.getSettleStatus();
            if (ValueUtils.isNotEmpty(settleStatusParamsList)) {
                Set<Short> settleStatusList = getSettleStatusParams(settleStatusParamsList);
                querySchema.appendQueryCondition(QueryCondition.name("settlestatus").in((Object) settleStatusList));

            }
        }
        querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.VOUCHDATE).between(conditionDTO.getBeginDate(), conditionDTO.getEndDate()));
        List<BizObject> bizObjects = MetaDaoHelper.queryObject(ForeignPayment.ENTITY_NAME, querySchema, null);
        List<LiquidityItemDTO> liquidityItemDTOS = new ArrayList<>();
        for (BizObject bizObject : bizObjects) {
            LiquidityItemDTO liquidityItemDTO = new LiquidityItemDTO();
            liquidityItemDTO.setAccentity(bizObject.getString("accentity"));
            liquidityItemDTO.setAccentityName(bizObject.getString("accentityName"));
            liquidityItemDTO.setBillId(bizObject.getString("businessID"));
            liquidityItemDTO.setBillDetailNum(bizObject.getString("businessDetailNum"));
            liquidityItemDTO.setBillNum(bizObject.getString("businessNum"));
            liquidityItemDTO.setServiceCode(IServicecodeConstant.FOREIGNPAYMENT);
            liquidityItemDTO.setSourceBusinessSystem(LiquiditySourceBusinessSystemEnum.CashSystem);
            liquidityItemDTO.setOrgId(bizObject.getString("orgId"));
            liquidityItemDTO.setDeptId(bizObject.getString("deptId"));
            liquidityItemDTO.setProjectId(bizObject.getString("projectId"));
            liquidityItemDTO.setBusinessDate(bizObject.getDate("businessDate"));
            liquidityItemDTO.setTransType(bizObject.getString("tradetype"));
            liquidityItemDTO.setSettlementMethod(bizObject.getString("settlemode"));
            liquidityItemDTO.setOurAccId(bizObject.getString("ourAccId"));
            liquidityItemDTO.setOurCashAccId(bizObject.getString("ourCashAccId"));
            liquidityItemDTO.setNoteId(bizObject.getString("noteno"));
            liquidityItemDTO.setExpenseItem(bizObject.getString("expenseItem"));
            liquidityItemDTO.setOppType(getOppType(bizObject));
            liquidityItemDTO.setOppDocId(bizObject.getString("receivenameid"));
            liquidityItemDTO.setOppName(bizObject.getString("receivename"));


            liquidityItemDTO.setOppAcc(bizObject.getString("receivebankaccountid"));
            liquidityItemDTO.setOppAccOpenName(bizObject.getString("receivebankaddress"));

            // 枚举值：1 收入、2 支出
            liquidityItemDTO.setReceiptType(2);
            liquidityItemDTO.setOriginCurrency(bizObject.getString("currency"));
            liquidityItemDTO.setOriginAmount(bizObject.getBigDecimal("amount"));
            liquidityItemDTO.setPlanSettlementDate(bizObject.getDate("businessDate"));
            liquidityItemDTO.setExchangeRateType(bizObject.getString("exchangeRateType"));
            liquidityItemDTO.setRemark(bizObject.getString("note"));
            liquidityItemDTO.setBusinessStatus(getLiquidityAuditStatusEnum(bizObject));
            liquidityItemDTO.setIsIntegrateSettlementPlatform(true);
            liquidityItemDTO.setSettleStatus(getLiquiditySettleStatusEnum(bizObject));
            liquidityItemDTOS.add(liquidityItemDTO);

        }
        return liquidityItemDTOS;
    }
}
