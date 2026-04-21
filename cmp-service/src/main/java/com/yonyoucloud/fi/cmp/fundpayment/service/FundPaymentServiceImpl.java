package com.yonyoucloud.fi.cmp.fundpayment.service;

import com.alibaba.fastjson.JSON;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.basedoc.model.BankVO;
import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.utils.json.GsonHelper;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.ctm.stwb.reqvo.AgentPaymentReqVO;
import com.yonyoucloud.ctm.stwb.reqvo.SettleFailChangeReqVO;
import com.yonyoucloud.fi.basecom.service.FIBillService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.enums.BillNumberEnum;
import com.yonyoucloud.fi.cmp.event.utils.DetermineUtils;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundCommonServiceImpl;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.FundCommonQueryDataVo;
import com.yonyoucloud.fi.cmp.transferaccount.util.AssertUtil;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.tmsp.openapi.ITmspRefRpcService;
import com.yonyoucloud.fi.tmsp.vo.TmspRequestParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class FundPaymentServiceImpl implements FundPaymentService {

    private final FIBillService fiBillService;
    private final ProcessService processService;
    private final FundPaymentCommonService fundPaymentCommonService;
    private final CmpBudgetManagerService cmpBudgetManagerService;
    private final BaseRefRpcService baseRefRpcService;
    private final ReWriteBusCorrDataService reWriteBusCorrDataService;
    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;
    @Autowired
    private FundCommonServiceImpl fundCommonService;
    //票据号fullname
    private static final String BILLNO = "drft.billno.Billno";
    //供应商银行fullname
    private static final String VENDORBANK = "aa.vendor.VendorBank";
    //企业银行账户fullname
    private static final String ORGBANK = "bd.enterprise.OrgFinBankacctVO";
    //银行信息fullname
    private static final String AGENTBANK = "aa.merchant.AgentFinancial";
    //银行类别fullname
    private static final String BANK = "bd.bank.BankVO";
    //银行网点fullname
    private static final String BANKDOT = "bd.bank.BankDotVO";

    /**
     * 整单委托拒绝
     * 主要逻辑 更新子表【是否委托驳回】 更新为是
     * 结算状态更新为结算止付
     *
     * @param jsonObject
     */
    @Override
    public void entrustReject(CtmJSONObject jsonObject) throws Exception {
        String id = getId(jsonObject);
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(id),
                QueryCondition.name(ICmpConstant.SETTLE_STATUS).not_eq(FundSettleStatus.SettleFailed.getValue()));
        QuerySchema schema = QuerySchema.create().addSelect("*");
        schema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(FundPayment_b.ENTITY_NAME, schema);
        check(list);
        List<FundPayment_b> paymentBList = steamTransfer(list);
        long count = paymentBList.stream().filter(FundPayment_b -> FundPayment_b.getEntrustReject() == 1).count();
        if (count == paymentBList.size()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100607"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001E0", "该单已经委托拒绝，不可重复拒绝") /* "该单已经委托拒绝，不可重复拒绝" *//* 该单已经委托拒绝，不可重复拒绝！ */);
        }
        List<FundPayment_b> bills = paymentBList.stream().filter(fundPayment ->
                (null == fundPayment.getEntrustReject() || fundPayment.getEntrustReject() != 1)).collect(Collectors.toList());//过滤已经被委托拒绝的单子
        updateFundPayments(bills);//更新逻辑

        // CZFW-303696:关联后可以进行委托拒绝，委托拒绝后也没有取消关联对账单
        List<CtmJSONObject> cancelAssociationDataList = getCancelAssociationDataMainList(id);
        if (CollectionUtils.isNotEmpty(cancelAssociationDataList)) {
            for (CtmJSONObject ctmJSONObject : cancelAssociationDataList) {
                reWriteBusCorrDataService.resDelData(ctmJSONObject);
            }
        }
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        //调结算工作台
        if (enableSimplify) {
            for (FundPayment_b item : bills) {
                AgentPaymentReqVO agentPaymentReqVO = new AgentPaymentReqVO();
                agentPaymentReqVO.setSettleDetailAId(item.getSettledId());
                agentPaymentReqVO.setStatementdetailstatus(Integer.valueOf(FundSettleStatus.SettleFailed.getValue()));
                fundCommonService.sendEventToSettleBenchDetail(agentPaymentReqVO);
            }
        } else {
            List<Long> settledIds = bills.stream().map(bill -> Long.parseLong(bill.getSettledId())).collect(Collectors.toList());
            SettleFailChangeReqVO paramsJson = new SettleFailChangeReqVO();
            paramsJson.setSettleDetailIdList(settledIds);
            RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).newUpdateSettlementDetailList(paramsJson);
        }
    }

    /**
     * 子表委托拒绝
     *
     * @param jsonObject
     * @throws Exception
     */
    @Override
    public void entrustRejectSub(CtmJSONObject jsonObject) throws Exception {
        String id = getId(jsonObject);//获取子表id
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        QuerySchema schema = QuerySchema.create().addSelect("*");
        schema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(FundPayment_b.ENTITY_NAME, schema);
        check(list);//校验
        List<FundPayment_b> fundPayment_bs = steamTransfer(list);
        FundPayment_b fundPayment_b = fundPayment_bs.get(0);
        if (fundPayment_b.getEntrustReject() == 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100607"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001E0", "该单已经委托拒绝，不可重复拒绝") /* "该单已经委托拒绝，不可重复拒绝" *//* 该单已经委托拒绝，不可重复拒绝！ */);
        }
        //更新子表【是否委托驳回】 更新为是 结算状态更新为结算止付
        fundPayment_b.setEntrustReject(1);
        fundPayment_b.setFundSettlestatus(FundSettleStatus.SettleFailed);
        fundPayment_b.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundPayment_b.getFundSettlestatus()));
        fundPayment_b.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPayment_b);

        // CZFW-303696:关联后可以进行委托拒绝，委托拒绝后也没有取消关联对账单
        List<CtmJSONObject> cancelAssociationDataList = getCancelAssociationDataSubList(fundPayment_b);
        if (CollectionUtils.isNotEmpty(cancelAssociationDataList)) {
            for (CtmJSONObject ctmJSONObject : cancelAssociationDataList) {
                reWriteBusCorrDataService.resDelData(ctmJSONObject);
            }
        }
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        //调结算工作台
        if (enableSimplify) {
            AgentPaymentReqVO agentPaymentReqVO = new AgentPaymentReqVO();
            agentPaymentReqVO.setSettleDetailAId(fundPayment_b.getSettledId());
            agentPaymentReqVO.setStatementdetailstatus(Integer.valueOf(FundSettleStatus.SettleFailed.getValue()));
            fundCommonService.sendEventToSettleBenchDetail(agentPaymentReqVO);
        } else {
            //调结算工作台
            CtmJSONObject params = new CtmJSONObject();
            long settledId = Long.parseLong(fundPayment_b.getSettledId());
            List<Long> settledIds = new ArrayList<>();
            settledIds.add(settledId);
            params.put("settleDetailIdList", settledIds);
            SettleFailChangeReqVO paramsJson = new SettleFailChangeReqVO();
            paramsJson.setSettleDetailIdList(settledIds);
            RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).newUpdateSettlementDetailList(paramsJson);
        }
    }

    /**
     * <h2>退票重付：生成退票的资金付款单</h2>
     *
     * @param json : 入参
     * @return java.lang.String
     * @author Sun GuoCai
     * @since 2023/11/18 8:12
     */
    @Override
    public String refundAndRepayment(CtmJSONObject json) {
        try {
            Object id = json.get("id");
            BizObject bizObject = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id, 2);
            BizObject fundPayment = new BizObject(bizObject);
            List<Long> fundPaymentSubIdsList = new ArrayList<>();
            // 构建退票重付的资金付款单
            processRefundFundPayment(fundPayment, fundPaymentSubIdsList);

            // 保存退票重付的资金付款单
            RuleExecuteResult ruleExecuteResult = executeSaveFundPaymentRule(fundPayment);

            BizObject resultData = (BizObject) ruleExecuteResult.getData();

            // 回写原始单据的是否已退票重付、退票关联付款单id
            Object dataId = resultData.getId();
            List<BizObject> fundPaymentSubUpdateList = new ArrayList<>();
            List<Map<String, Object>> bizObjects = MetaDaoHelper.queryByIds(FundPayment_b.ENTITY_NAME, "*", fundPaymentSubIdsList.toArray(new Long[]{}));
            for (Map<String, Object> map : bizObjects) {
                BizObject bizObj = new BizObject(map);
                bizObj.set("whetherRefundAndRepayment", 1);
                bizObj.set("refundAssociatedPaymentId", dataId);
                fundPaymentSubUpdateList.add(bizObj);
            }
            EntityTool.setUpdateStatus(fundPaymentSubUpdateList);
            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPaymentSubUpdateList);
        } catch (Exception e) {
            log.error("refundAndRepayment generator bill fail! json={}, errorMag={}", json.toString(), e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100608"),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800AD", "退票重付生单失败！请检查！") /* "退票重付生单失败！请检查！" */ + e.getMessage());
        }
        return "success";
    }

    /**
     * <h2>第三方服务更新结算状态以及过账</h2>
     *
     * @param jsonObject : 入参
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2024/1/11 16:52
     */
    @Override
    public CtmJSONObject updateThirdPartyBillSettlementStatus(CtmJSONObject jsonObject) throws Exception {
        log.error("updateThirdPartyBillSettlementStatus params!, data={}", jsonObject);
        CtmJSONObject result = new CtmJSONObject();
        String billNum = jsonObject.getString("billNum");
        CtmJSONArray array = jsonObject.getJSONArray("data");
        try {
            if (IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
                List<FundPayment_b> fundPaymentBList = new ArrayList<>();
                fundPaymentCommonService.updateSubSettleStatusAndSettleAmountForFundPayment(array, fundPaymentBList);
                fundPaymentCommonService.updateSettleSuccessTimeAndGeneratorVoucherForFundPayment(fundPaymentBList);
            } else {
                List<FundCollection_b> fundCollectionBList = new ArrayList<>();
                fundPaymentCommonService.updateSubSettleStatusAndSettleAmountForFundCollection(array, fundCollectionBList);
                fundPaymentCommonService.updateSettleSuccessTimeAndGeneratorVoucherForFundCollection(fundCollectionBList);
            }
        } catch (Exception e) {
            log.error("updateThirdPartyBillSettlementStatus fail!, data={}, errorMsg={}", jsonObject, e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100609"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800AC", "处理数据异常，请检查参数！errorMsg:") /* "处理数据异常，请检查参数！errorMsg:" */ + e.getMessage());
        }
        result.put("code", 200);
        result.put("data", array);
        result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070D", "操作成功！") /* "操作成功！" */);
        return result;
    }

    private void processRefundFundPayment(BizObject fundPayment, List<Long> fundPaymentSubIdsList) {
        List<FundPayment_b> fundPaymentSubList = fundPayment.getBizObjects("FundPayment_b", FundPayment_b.class);
        List<FundPayment_b> fundPaymentSubReFundList = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal lineno = new BigDecimal(ICmpConstant.CONSTANT_TEN);
        Object currency = fundPayment.get(ICmpConstant.CURRENCY);
        Object natCurrency = fundPayment.get(ICmpConstant.NATCURRENCY);
        boolean isCurrencyFlag = currency.equals(natCurrency);

        for (FundPayment_b fundPaymentB : fundPaymentSubList) {
            if (fundPaymentB.getFundSettlestatus().getValue() == FundSettleStatus.Refund.getValue()
                    && BigDecimal.ZERO.compareTo(fundPaymentB.getRefundSum()) < 0
                    && !(Objects.nonNull(fundPaymentB.getWhetherRefundAndRepayment()) && fundPaymentB.getWhetherRefundAndRepayment() == 1)) {
                fundPaymentSubIdsList.add(Long.parseLong(fundPaymentB.getId().toString()));
                fundPaymentB.setOriSum(fundPaymentB.getRefundSum());
                BigDecimal refAmt = fundPaymentB.getRefundSum();
                fundPaymentB.setOriSum(refAmt);
                if (isCurrencyFlag) {
                    fundPaymentB.setNatSum(refAmt);
                } else {
                    BigDecimal natRefAmt =BigDecimalUtils.safeMultiply(new BigDecimal(fundPayment.get(ICmpConstant.EXCHRATE).toString()), refAmt);
                    fundPaymentB.setNatSum(natRefAmt);
                }
                fundPaymentB.setAssociationStatus(AssociationStatus.NoAssociated.getValue());
                fundPaymentB.setFundSettlestatus(FundSettleStatus.WaitSettle);
                fundPaymentB.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundPaymentB.getFundSettlestatus()));
                fundPaymentB.put("srcBillRowId", fundPaymentB.getId());
                fundPaymentB.setSrcbillid(fundPayment.getId());
                fundPaymentB.setSrcbillitemno(fundPaymentB.getLineno().toString());
                fundPaymentB.setSrcbillno(fundPayment.get(ICmpConstant.CODE));
                sum = computeBigDecimalSum(fundPayment, sum, fundPaymentB);
                fundPaymentB.put(ICmpConstant.LINE_NO, lineno);
                lineno = BigDecimalUtils.safeAdd(lineno, new BigDecimal(ICmpConstant.CONSTANT_TEN));
                clearFundPaymentBFieldValue(fundPaymentB);
                fundPaymentB.setEntityStatus(EntityStatus.Insert);
                fundPaymentSubReFundList.add(fundPaymentB);
            }
        }
        if (CollectionUtils.isEmpty(fundPaymentSubReFundList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100610"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800AE", "没有满足退票重付的明细行数据！") /* "没有满足退票重付的明细行数据！" */);
        }
        setupOtherAttributes(fundPayment);
        fundPayment.setId(null);
        CmpCommonUtil.organizeAmountData(fundPayment, sum, isCurrencyFlag);
        fundPayment.setBizObjects("FundPayment_b", fundPaymentSubReFundList);
        fundPayment.setEntityStatus(EntityStatus.Insert);
    }

    private void clearFundPaymentBFieldValue(FundPayment_b fundPaymentB) {
        fundPaymentB.setRefundSum(BigDecimal.ZERO);
        fundPaymentB.setSettlesuccessSum(BigDecimal.ZERO);
        fundPaymentB.setSettleerrorSum(BigDecimal.ZERO);
        fundPaymentB.set("auditorId", null);
        fundPaymentB.set("auditor", null);
        fundPaymentB.set("auditDate", null);
        fundPaymentB.set("auditTime", null);
        fundPaymentB.set("createTime", null);
        fundPaymentB.set("createDate", null);
        fundPaymentB.set("modifyTime", null);
        fundPaymentB.set("modifyDate", null);
        fundPaymentB.set("creator", null);
        fundPaymentB.set("modifier", null);
        fundPaymentB.set("creatorId", null);
        fundPaymentB.set("modifierId", null);
        fundPaymentB.set("actualSettlementExchangeRate", null);
        fundPaymentB.set("actualSettlementAmount", null);
        fundPaymentB.set("actualSettlementExchangeRateType", null);
        fundPaymentB.set("transNumber", null);
        fundPaymentB.set(ICmpConstant.SETTLE_SUCCESS_TIME, null);
        fundPaymentB.setId(null);
        fundPaymentB.setSmartcheckno(null);
        fundPaymentB.setBankReconciliationId(null);
        fundPaymentB.setBillClaimId(null);
        fundPaymentB.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
    }


    public BigDecimal computeBigDecimalSum(BizObject fundPayment, BigDecimal sum, FundPayment_b fundPaymentB) {
        fundPayment.set(ICmpConstant.WHETHER_SETTLE, ICmpConstant.CONSTANT_ONE);
        sum = BigDecimalUtils.safeAdd(sum, fundPaymentB.getRefundSum());
        return sum;
    }

    private RuleExecuteResult executeSaveFundPaymentRule(BizObject fundPayment) throws Exception {
        try {
            BillContext billContext = CmpCommonUtil.getBillContextByFundPayment();
            boolean isWfControlled = processService.bpmControl(billContext, fundPayment);
            fundPayment.put(ICmpConstant.IS_WFCONTROLLED, isWfControlled);
        } catch (Exception e) {
            fundPayment.put(ICmpConstant.IS_WFCONTROLLED, false);
        }
        BillDataDto dataDto = new BillDataDto();
        dataDto.setBillnum(IBillNumConstant.FUND_PAYMENT);
        dataDto.setData(CtmJSONObject.toJSONString(fundPayment));
        return fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), dataDto);
    }


    private void setupOtherAttributes(BizObject fundPayment) {
        // 事项来源
        fundPayment.set(ICmpConstant.SRC_ITEM, EventSource.Cmpchase.getValue());
        // 事项类型
        fundPayment.set(ICmpConstant.BILLTYPE, EventType.refund_and_reissue.getValue());
        // 审批状态
        fundPayment.set(ICmpConstant.AUDIT_STATUS, AuditStatus.Incomplete.getValue());
        // 审批流状态
        fundPayment.set(ICmpConstant.VERIFY_STATE, VerifyState.INIT_NEW_OPEN.getValue());
        // 凭证状态
        fundPayment.set(ICmpConstant.VOUCHER_STATUS, VoucherStatus.Empty.getValue());
        // 凭证号
        fundPayment.set(ICmpConstant.VOUCHERNO, null);
        // 凭证id
        fundPayment.set(ICmpConstant.VOUCHERID, null);
        // 凭证期间
        fundPayment.set(ICmpConstant.VOUCHERPERIOD, null);
        // 结算成功时间
        fundPayment.set(ICmpConstant.SETTLE_SUCCESS_TIME, null);
    }

    /**
     * 整单更新
     *
     * @param paymentBList
     * @throws Exception
     */
    public void updateFundPayments(List<FundPayment_b> paymentBList) throws Exception {
        List<FundPayment_b> updateList = paymentBList.stream().peek(_b -> {
            _b.setFundSettlestatus(FundSettleStatus.SettleFailed); //结算止付
            _b.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(_b.getFundSettlestatus()));
            _b.setEntrustReject(1); //是否驳回
            _b.setEntityStatus(EntityStatus.Update);
        }).collect(Collectors.toList());
        MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, updateList);
    }

    public List<FundPayment_b> steamTransfer(List<Map<String, Object>> list) {
        return list.stream().map(map_ -> {
            FundPayment_b fundPayment_b = new FundPayment_b();
            fundPayment_b.init(map_);
            return fundPayment_b;
        }).collect(Collectors.toList());
    }

    private String getId(CtmJSONObject jsonObject) {
        String id = jsonObject.getString("id");
        AssertUtil.isNotBlank(id, () -> new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800FA", "单据不存在，请刷新后重试！") /* "单据不存在，请刷新后重试！" */)); /*单据不存在*/
        return id;
    }

    private void check(List<Map<String, Object>> list) {
        if (CollectionUtils.isEmpty(list)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100611"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800FA", "单据不存在，请刷新后重试！") /* "单据不存在，请刷新后重试！" */);
        }
    }

    /**
     * <h2>资金收付款单提供RPC查询接口</h2>
     *
     * @param fundCommonQueryDataVo : 入参实体
     * @return java.lang.Object
     * @author Sun GuoCai
     * @since 2024/1/20 22:13
     */
    @Override
    public Object queryFundBillDataByParams(FundCommonQueryDataVo fundCommonQueryDataVo) throws Exception {
        log.error("dataVerify, input parameter fundCommonQueryDataVo={}", CtmJSONObject.toJSONString(fundCommonQueryDataVo));
        // 返给事项平台的参数校验
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(fundCommonQueryDataVo)).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400706", "资金收付款单查询接口，请求参数实体为空！") /* "资金收付款单查询接口，请求参数实体为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(fundCommonQueryDataVo.getAccentityList())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400708", "资金收付款单查询接口，会计主体为空！") /* "资金收付款单查询接口，会计主体为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(fundCommonQueryDataVo.getStartVouchDate())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400709", "资金收付款单查询接口，起始日期为空！") /* "资金收付款单查询接口，起始日期为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(fundCommonQueryDataVo.getEndVouchDate())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070A", "资金收付款单查询接口，截止日期为空！") /* "资金收付款单查询接口，截止日期为空！" */);


        QuerySchema querySchema = new QuerySchema();
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        String entityUri = fundCommonQueryDataVo.getEntityUri();
        queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY)
                .in(fundCommonQueryDataVo.getAccentityList())));

        queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.VOUCHDATE)
                .between(DateUtils.dateParse(fundCommonQueryDataVo.getStartVouchDate(), DateUtils.DATE_TIME_PATTERN),
                        DateUtils.dateParse(fundCommonQueryDataVo.getEndVouchDate(), DateUtils.DATE_TIME_PATTERN))));
        Short billtype = fundCommonQueryDataVo.getBilltype();
        if (ValueUtils.isNotEmptyObj(billtype)) {
            queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.BILLTYPE)
                    .eq(fundCommonQueryDataVo.getBilltype())));
        }
        String mainProject = fundCommonQueryDataVo.getMainProject();
        if (ValueUtils.isNotEmptyObj(mainProject)) {
            queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.PROJECT)
                    .eq(fundCommonQueryDataVo.getMainProject())));
        }
        Short settleStatus = fundCommonQueryDataVo.getSettleStatus();
        String subProject = fundCommonQueryDataVo.getSubProject();
        String expenseitem = fundCommonQueryDataVo.getExpenseitem();
        String selectField = "*";
        if (entityUri.equals(FundPayment.ENTITY_NAME)) {
            selectField += ", FundPayment_b.*";
            if (ValueUtils.isNotEmptyObj(settleStatus)) {
                queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("FundPayment_b.settlestatus")
                        .eq(settleStatus)));
            }
            if (ValueUtils.isNotEmptyObj(subProject)) {
                queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("FundPayment_b.project")
                        .eq(subProject)));
            }
            if (ValueUtils.isNotEmptyObj(expenseitem)) {
                queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("FundPayment_b.expenseitem")
                        .eq(expenseitem)));
            }
        } else {
            selectField += ", FundCollection_b.*";
            if (ValueUtils.isNotEmptyObj(settleStatus)) {
                queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("FundCollection_b.settlestatus")
                        .eq(settleStatus)));
            }
            if (ValueUtils.isNotEmptyObj(subProject)) {
                queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("FundCollection_b.project")
                        .eq(subProject)));
            }
            if (ValueUtils.isNotEmptyObj(expenseitem)) {
                queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("FundCollection_b.expenseitem")
                        .eq(expenseitem)));
            }
        }
        querySchema.addCondition(queryConditionGroup);
        querySchema.addSelect(selectField);

        List<BizObject> bizObjectNewList = new ArrayList<>();
        List<BizObject> bizObjectList = MetaDaoHelper.queryObject(fundCommonQueryDataVo.getEntityUri(), querySchema, null);
        if (CollectionUtils.isEmpty(bizObjectList)) {
            return bizObjectNewList;
        }
        Map<Object, List<BizObject>> listMap = bizObjectList.stream().collect(Collectors.groupingBy(BizObject::getId));
        for (List<BizObject> objectList : listMap.values()) {
            bizObjectNewList.add(initMainData(objectList, entityUri));
        }
        return bizObjectNewList;
    }

    private BizObject initMainData(List<BizObject> list, String entityUri) {
        BizObject bizObj = new BizObject();
        List<BizObject> subList = new ArrayList<>();
        for (BizObject data : list) {
            BizObject object = new BizObject();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                assignValuesToBizObject(bizObj, object, entry, entityUri);
            }
            subList.add(object);
        }
        if (entityUri.equals(FundPayment.ENTITY_NAME)) {
            bizObj.put("FundPayment_b", subList);
        } else {
            bizObj.put("FundCollection_b", subList);
        }
        return bizObj;
    }

    private void assignValuesToBizObject(BizObject mainBizObject, BizObject subBizObject, Map.Entry<String, Object> entry, String entityUri) {
        String key = entry.getKey();
        if (entityUri.equals(FundPayment.ENTITY_NAME)) {
            if (key.contains("FundPayment_b_")) {
                subBizObject.put(key.substring(key.indexOf("_") + 3), entry.getValue());
            } else {
                mainBizObject.put(key, entry.getValue());
            }
        } else {
            if (key.contains("FundCollection_b_")) {
                subBizObject.put(key.substring(key.indexOf("_") + 3), entry.getValue());
            } else {
                mainBizObject.put(key, entry.getValue());
            }
        }
    }

    @Override
    public Map<String, Object> findBillNoById(CtmJSONObject jsonObject) throws Exception {
        String noteno = jsonObject.getString("noteno");
        String currency = jsonObject.getString("currency");
        QuerySchema schema = QuerySchema.create().addSelect("receiveroles", "receiverbysupp", "receiverbyorg",
                "receiverbycust", "showreceiver", "receiverbankacc", "receiverbankaccbysupp", "receiverbankaccbycust",
                "receiverbankaccbyorg", "showreceiverbankacc", "receiveraccbyobject", "receiverbyfundobject",
                "showReceiveOpenbankname", "elecreceiveaccname", "receiverbankacc");
        schema.appendQueryCondition(QueryCondition.name("id").eq(noteno));
        List<Map<String, Object>> billNoList = MetaDaoHelper.query("drft.drftnoteinformation.DrftNoteInformation", schema, "drft");
        Map<String, Object> map = new HashMap<>();
        if (billNoList.size() > 0) {
            BizObject billno = new BizObject(billNoList.get(0));
            String receiveroles = billno.getString("receiveroles");
            if ("3".equals(receiveroles)) {
                receiveroles = String.valueOf(CaObject.Other.getValue());
            } else if ("4".equals(receiveroles)) {
                receiveroles = String.valueOf(CaObject.InnerUnit.getValue());
            }
            map.put("receiveroles", receiveroles);
            map.put("oppositeaccountno", billno.getString("showreceiverbankacc"));
            if ("4".equals(receiveroles)) {
                map.put("receiverbankaccName", billno.getString("elecreceiveaccname"));
                map.put("acctname", billno.getString("showReceiveOpenbankname"));
                String oppositeobjectid = billno.getString("receiverbankacc");
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setAccount(oppositeobjectid);
                List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
                if (!bankAccounts.isEmpty()) {
                    EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccounts.get(0);
                    map.put("bankname", enterpriseBankAcctVO.getBankName());
                    map.put("lineNumber", enterpriseBankAcctVO.getLineNumber());
                }
            } else if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(billno.getString("receiverbysupp"))) {
                //收款人供应商
                map.put("receiver", billno.getString("receiverbysupp"));
                QuerySchema suppSchema = QuerySchema.create().addSelect("accountname", "correspondentcode", "bank", "openaccountbank");
                suppSchema.appendQueryCondition(QueryCondition.name("id").eq(billno.getString("receiverbankaccbysupp")));
                List<Map<String, Object>> suppList = MetaDaoHelper.query(VENDORBANK, suppSchema, "yssupplier");
                if (suppList.size() > 0) {
                    map.put("receiverbankacc", billno.getString("receiverbankaccbysupp"));
                    map.put("receiverbankaccName", suppList.get(0).get("accountname"));
                    //联行号
                    map.put("lineNumber", suppList.get(0).get("correspondentcode"));
                    QuerySchema orgBankSchema = QuerySchema.create().addSelect("name");
                    orgBankSchema.appendQueryCondition(QueryCondition.name("id").eq(suppList.get(0).get("bank")));
                    List<Map<String, Object>> bankList = MetaDaoHelper.query(BANK, orgBankSchema, "ucfbasedoc");
                    if (bankList.size() > 0) {
                        //银行类别
                        map.put("bank", suppList.get(0).get("bank"));
                        map.put("bankname", bankList.get(0).get("name"));
                    }
                    QuerySchema acctSchema = QuerySchema.create().addSelect("name");
                    acctSchema.appendQueryCondition(QueryCondition.name("id").eq(suppList.get(0).get("openaccountbank")));
                    List<Map<String, Object>> acctList = MetaDaoHelper.query(BANKDOT, acctSchema, "ucfbasedoc");
                    if (acctList.size() > 0) {
                        //开户行
                        map.put("acct", suppList.get(0).get("openaccountbank"));
                        map.put("acctname", acctList.get(0).get("name"));
                    }
                }
            } else if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(billno.getString("receiverbyorg"))) {
                //收款人会计主体
                map.put("receiver", billno.getString("receiverbyorg"));
                QuerySchema orgSchema = QuerySchema.create().addSelect("name", "lineNumber", "bank", "bankNumber", "acctName");
                orgSchema.appendQueryCondition(QueryCondition.name("account").eq(billno.getString("showreceiverbankacc")),
                        QueryCondition.name("currencyList.currency").eq(currency));
                List<Map<String, Object>> orgList = MetaDaoHelper.query(ORGBANK, orgSchema, "ucfbasedoc");
                if (orgList.size() > 0) {
                    map.put("receiverbankacc", billno.getString("receiverbankaccbyorg"));
                    map.put("receiverbankaccName", orgList.get(0).get("name"));
                    //联行号
                    map.put("lineNumber", orgList.get(0).get("lineNumber"));
                    //开户行
                    map.put("acct", orgList.get(0).get("bankNumber"));
                    map.put("acctname", orgList.get(0).get("acctName"));
                    QuerySchema orgBankSchema = QuerySchema.create().addSelect("name");
                    orgBankSchema.appendQueryCondition(QueryCondition.name("id").eq(orgList.get(0).get("bank")));
                    List<Map<String, Object>> orgBankList = MetaDaoHelper.query(BANK, orgBankSchema, "ucfbasedoc");
                    if (orgBankList.size() > 0) {
                        //银行类别
                        map.put("bank", orgList.get(0).get("bank"));
                        map.put("bankname", orgBankList.get(0).get("name"));
                    }
                }
            } else if (!StringUtils.isEmpty(billno.getString("receiverbycust"))) {
                //收款人客户
                map.put("receiver", billno.getString("receiverbycust"));
                QuerySchema custSchema = QuerySchema.create().addSelect("bankAccountName", "jointLineNo", "bank", "openBank");
                custSchema.appendQueryCondition(QueryCondition.name("id").eq(billno.getString("receiverbankaccbycust")));
                List<Map<String, Object>> custList = MetaDaoHelper.query(AGENTBANK, custSchema, "productcenter");
                if (custList.size() > 0) {
                    map.put("receiverbankacc", billno.getString("receiverbankaccbycust"));
                    map.put("receiverbankaccName", custList.get(0).get("bankAccountName"));
                    //联行号
                    map.put("lineNumber", custList.get(0).get("jointLineNo"));
                    QuerySchema orgBankSchema = QuerySchema.create().addSelect("name");
                    orgBankSchema.appendQueryCondition(QueryCondition.name("id").eq(custList.get(0).get("bank")));
                    List<Map<String, Object>> bankList = MetaDaoHelper.query(BANK, orgBankSchema, "ucfbasedoc");
                    if (bankList.size() > 0) {
                        //银行类别
                        map.put("bank", custList.get(0).get("bank"));
                        map.put("bankname", bankList.get(0).get("name"));
                    }
                    QuerySchema acctSchema = QuerySchema.create().addSelect("name");
                    acctSchema.appendQueryCondition(QueryCondition.name("id").eq(custList.get(0).get("openBank")));
                    List<Map<String, Object>> acctList = MetaDaoHelper.query(BANKDOT, acctSchema, "ucfbasedoc");
                    if (acctList.size() > 0) {
                        //开户行
                        map.put("acct", custList.get(0).get("openBank"));
                        map.put("acctname", acctList.get(0).get("name"));
                    }
                }
            } else if ("5".equals(receiveroles)) {
                // 资金业务对象id
                String receiverbyfundobject = billno.getString("receiverbyfundobject");
                map.put("receiver", receiverbyfundobject);
                // 资金业务对象账号id
                String receiveraccbyobject = billno.getString("receiveraccbyobject");
                TmspRequestParams tmspRequestParams = new TmspRequestParams();
                tmspRequestParams.setId(receiverbyfundobject);
                tmspRequestParams.setCurrency(currency);
                tmspRequestParams.setFundBusinObjArchivesId(receiveraccbyobject);
                //获取账户信息对象
                List result = RemoteDubbo.get(ITmspRefRpcService.class, IDomainConstant.YONBIP_FI_CTMPUB).queryFundBusinObjArchives(tmspRequestParams);
                if (CollectionUtils.isNotEmpty(result)) {
                    //获取数据实体
                    CtmJSONObject reslutMap = (CtmJSONObject) GsonHelper.FromJSon(JSON.toJSONString(result.get(0)), CtmJSONObject.class);
                    map.put("receiver", reslutMap.get("id"));
                    map.put("receiverName", reslutMap.get("fundbusinobjtypename"));
                    //获取数据实体
                    if (reslutMap.get("fundBusinObjArchivesItemDTO") != null && ((List) reslutMap.get("fundBusinObjArchivesItemDTO")).size() > 0) {
                        List<Object> fundBusinObjArchivesItemDTOList = (List) reslutMap.get("fundBusinObjArchivesItemDTO");

                        if (CollectionUtils.isNotEmpty(fundBusinObjArchivesItemDTOList)) {
                            // 将 List 转换为 Stream 并过滤
                            Optional<CtmJSONObject> targetItemOpt = fundBusinObjArchivesItemDTOList.stream()
                                    .map(item -> (CtmJSONObject) GsonHelper.FromJSon(JSON.toJSONString(item), CtmJSONObject.class))
                                    .filter(item -> {
                                        if (item == null) {
                                            return false;
                                        }
                                        // 判断 id 是否匹配
                                        boolean idMatch = receiveraccbyobject != null && receiveraccbyobject.equals(item.getString("id"));
                                        // 判断币种是否匹配
                                        boolean currencyMatch = currency != null && currency.equals(item.getString("currency"));
                                        return idMatch && currencyMatch;
                                    })
                                    .findFirst();

                            if (targetItemOpt.isPresent()) {
                                CtmJSONObject fundBusinObjArchivesItem = targetItemOpt.get();

                                map.put("fundbusinobjtypeid", reslutMap.get("fundbusinobjtypeid"));
                                if (ValueUtils.isNotEmptyObj(receiveraccbyobject)) {
                                    //查询银行类别
                                    String bbankid = fundBusinObjArchivesItem.getString("bbankid");
                                    BankVO bankVO = baseRefRpcService.queryBankTypeById(bbankid);
                                    map.put("bankname", bankVO.getName());

                                    map.put("receiverbankacc", fundBusinObjArchivesItem.get("id"));
                                    map.put("oppositeaccountno", fundBusinObjArchivesItem.get("bankaccount"));
                                    map.put("receiverbankaccName", fundBusinObjArchivesItem.get("accountname"));
                                    map.put("lineNumber", fundBusinObjArchivesItem.get("linenumber"));

                                    map.put("bbankAccountId", fundBusinObjArchivesItem.get("bbankAccountId"));
                                    //查询银行网点
                                    String bopenaccountbankid = fundBusinObjArchivesItem.getString("bopenaccountbankid");
                                    BankdotVO bankdotVO = baseRefRpcService.queryBankdotVOByBanddotId(bopenaccountbankid);
                                    map.put("acctname", bankdotVO.getName());
                                } else {
                                    map.put("oppositeaccountno", null);
                                }
                            }
                        }
                    }
                }


            }
            map.put("receiverName", billno.getString("showreceiver"));
        }
        return map;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_PAYMENT)) {
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, true);
            return ResultMessage.data(resultBack);
        }
        String billnum = cmpBudgetVO.getBillno();

        String entityname = BillNumberEnum.find(billnum);
        if (StringUtils.isEmpty(entityname)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"), InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070C", "请求参数缺失") /* "请求参数缺失" */));
        }

        //TODO 变更单据返回来源单据信息，ids 为便跟单据自己信息
        List<String> ids = cmpBudgetVO.getIds();
        List<BizObject> bizObjects = new ArrayList<>();
        if (ValueUtils.isNotEmptyObj(ids)) {
            bizObjects = queryBizObjsWarpParentInfo(ids);
        } else if (ValueUtils.isNotEmptyObj(cmpBudgetVO.getBizObj())) {
            BizObject bizObject = CtmJSONObject.parseObject(cmpBudgetVO.getBizObj(), BizObject.class);
            bizObjects.add(bizObject);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"), InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070C", "请求参数缺失") /* "请求参数缺失" */));
        }
        //变更单据
        String changeBillno = cmpBudgetVO.getChangeBillno();
        if (!StringUtils.isEmpty(changeBillno)) {
            if (CollectionUtils.isEmpty(bizObjects)) {
                CtmJSONObject resultBack = new CtmJSONObject();
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000D0", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400707", "变更金额小于原金额,不需要校验!") /* "变更金额小于原金额,不需要校验!" */));
                return ResultMessage.data(resultBack);
            }
            //变更单据获取（融资登记单据类型）
            billnum = changeBillno;
        } else {
            //非变更单据 自己单据
            if (CollectionUtils.isEmpty(bizObjects)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"), InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070C", "请求参数缺失") /* "请求参数缺失" */));
            }
        }
        return cmpBudgetManagerService.budgetCheckNew(bizObjects, billnum, BudgetUtils.SUBMIT);
    }

    public List<BizObject> queryBizObjsWarpParentInfo(List<String> ids) throws Exception {
        // 根据id批量查询数据
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        // 只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        // 查询子表信息
        QuerySchema detailSchema = QuerySchema.create().name("FundPayment_b").addSelect("*");
        schema.addCompositionSchema(detailSchema);
        return MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, schema, null);
    }

    public List<CtmJSONObject> getCancelAssociationDataMainList(Object busid) throws Exception {
        List<CtmJSONObject> listreq = new ArrayList<CtmJSONObject>();
        FundPayment payment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, busid);
        Short associationcount = payment.getAssociationcount();
        List<FundPayment_b> fundPaymentList = new ArrayList<FundPayment_b>();
        QuerySchema querySchema = QuerySchema.create().addSelect("id,billClaimId,bankReconciliationId,associationStatus,pubts");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(busid));
        querySchema.addCondition(group);
        fundPaymentList = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isNotEmpty(fundPaymentList)) {
            for (FundPayment_b paymentB : fundPaymentList) {
                if (paymentB.getAssociationStatus() != null && AssociationStatus.Associated.getValue() == paymentB.getAssociationStatus().shortValue()) {
                    CtmJSONObject jsonReq = new CtmJSONObject();
                    if (org.imeta.core.lang.StringUtils.isNotEmpty(paymentB.getBankReconciliationId())) {
                        jsonReq.put("busid", Long.parseLong(paymentB.getBankReconciliationId()));
                        if (associationcount != null && AssociationCount.First.getValue() == associationcount) {
                            BankReconciliation byId = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, paymentB.getBankReconciliationId(), 3);
                            if (byId == null) {
                                continue;
                            }
                            Short associationcount1 = byId.getAssociationcount();
                            if (associationcount1 != null && AssociationCount.Second.getValue() == associationcount1) {
                                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankReconciliationId").eq(paymentB.getBankReconciliationId()), QueryCondition.name("id").not_eq(paymentB.getId()));
                                querySchema1.addCondition(group1);
                                List<BizObject> bizObjects = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema1, null);
                                if (bizObjects != null && bizObjects.size() > 0) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100007"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180831", "本单据对应的银行对账单已生成实际业务单据，不允许删除") /* "本单据对应的银行对账单已生成实际业务单据，不允许删除" */);
                                }
                            }
                        }
                    }
                    if (org.imeta.core.lang.StringUtils.isNotEmpty(paymentB.getBillClaimId())) {
                        jsonReq.put("claimid", Long.parseLong(paymentB.getBillClaimId()));
                    }
                    jsonReq.put("stwbbusid", paymentB.getId());
                    listreq.add(jsonReq);
                }
            }
        }
        return listreq;
    }

    private static List<CtmJSONObject> getCancelAssociationDataSubList(FundPayment_b paymentB) throws Exception {
        List<CtmJSONObject> listreq = new ArrayList<>();
        if (paymentB.getAssociationStatus() != null && AssociationStatus.Associated.getValue() == paymentB.getAssociationStatus().shortValue()) {
            CtmJSONObject jsonReq = new CtmJSONObject();
            if (org.imeta.core.lang.StringUtils.isNotEmpty(paymentB.getBankReconciliationId())) {
                jsonReq.put("busid", Long.parseLong(paymentB.getBankReconciliationId()));
                BankReconciliation byId = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, paymentB.getBankReconciliationId(), 3);
                if (byId == null) {
                    return Collections.emptyList();
                }
                Short associationcount1 = byId.getAssociationcount();
                if (associationcount1 != null && AssociationCount.Second.getValue() == associationcount1) {
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankReconciliationId").eq(paymentB.getBankReconciliationId()), QueryCondition.name("id").not_eq(paymentB.getId()));
                    querySchema1.addCondition(group1);
                    List<BizObject> bizObjects = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema1, null);
                    if (bizObjects != null && bizObjects.size() > 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100007"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180831", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070B", "本单据对应的银行对账单已生成实际业务单据，不允许删除") /* "本单据对应的银行对账单已生成实际业务单据，不允许删除" */) /* "本单据对应的银行对账单已生成实际业务单据，不允许删除" */);
                    }
                }
            }
            if (org.imeta.core.lang.StringUtils.isNotEmpty(paymentB.getBillClaimId())) {
                jsonReq.put("claimid", Long.parseLong(paymentB.getBillClaimId()));
            }
            jsonReq.put("stwbbusid", paymentB.getId());
            listreq.add(jsonReq);
        }
        return listreq;
    }

}
