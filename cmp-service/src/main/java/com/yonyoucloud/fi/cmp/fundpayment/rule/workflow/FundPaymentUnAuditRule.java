package com.yonyoucloud.fi.cmp.fundpayment.rule.workflow;

import com.yonyou.epmp.busi.service.BusiSystemConfigService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.migrade.CmpNewFiPreCheckService;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.BudgetUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <h1>资金付款单弃审规则</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-13 10:16
 */
@Slf4j
@Component
@Transactional(rollbackFor = Exception.class)
public class FundPaymentUnAuditRule extends AbstractCommonRule {
    @Autowired
    @Qualifier("stwbPaymentBillServiceImpl")
    StwbBillService stwbBillService;
    @Autowired
    private IFundCommonService fundCommonService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpNewFiPreCheckService cmpNewFiPreCheckService;
    // 预占
    public static final String PRE = "pre";

    @Autowired
    private BusiSystemConfigService busiSystemConfigService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        // 启用审批流时，最后一个审批通过的操作人，撤回时校验
        fundCommonService.statementUnSubmitVerificationByFundPayment(bills);
        for (BizObject bizobject : bills) {
            log.info("FundPaymentAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
            FundPayment currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizobject.getId());
            log.info("FundPaymentAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100698"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180383","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }
//            //如果是委托收款单或者委托付款单、并且是已结算支付状态，不能撤回
//            List<FundPayment_b> fundPayment_bList = currentBill.get("FundPayment_b");
//            List<FundPayment_b> filterList = fundPayment_bList.stream().filter(item -> item.get("settlestatus").equals(7)).collect(Collectors.toList());
//            if(currentBill.get("billtype").equals(100) && filterList.size() > 0 ){
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100699"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B0003D", "已结算补单场景下不允许委托单弃审") /* "已结算补单场景下不允许委托单弃审" */);
//            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100700"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180385","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            Date date = BillInfoUtils.getBusinessDate();
            if (null != billContext.getDeleteReason()) {
                if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                    return new RuleExecuteResult();
                }
            }
            Date currentDate = BillInfoUtils.getBusinessDate();
            if (currentDate.compareTo(date) < 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100701"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180384","审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
            }
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(bizobject.getId()));
            querySchema.addCondition(group);
            List<FundPayment_b> sublist = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null);
            for (FundPayment_b payment_b : sublist){
                //关联协同资金收款单编码不为空，则有协同生单数据
                if (!StringUtils.isEmpty(payment_b.getSynergybillno())){
                    FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME,payment_b.getSynergybillid());
                    if (fundCollection != null){
                        //单审批流状态为初始开立/驳回到制单
                        if (VerifyState.INIT_NEW_OPEN.getValue() != fundCollection.getVerifystate() && VerifyState.REJECTED_TO_MAKEBILL.getValue() != fundCollection.getVerifystate() ){
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101248"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1982586404D0000A","协同生成的资金收款单[%s]已处理，不允许撤回") /* "协同生成的资金收款单[%s]已处理，不允许撤回" */,fundCollection.getCode()));
                        }
                    }
                }
            }
            //begin yangjn 20240130 老架构收付款工作台升级数据 如果来源单据已经结算完成 则资金收付不能逆操作
            if(currentBill.getMigradeid()!=null){
                cmpNewFiPreCheckService.checkUpgradeDataBack(PayBill.ENTITY_NAME,currentBill.getMigradeid());
            }

            //暂存调用结算之前的对象，调用预算的时候必须是原始对象，修改完的对象调用预算会释放不了实占
            FundPayment originFundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, currentBill.getId());;
            //end yangjn 20240130
            String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
            List<BizObject> fundPaymentList = currentBill.get(childrenFieldCheck);
            Short settleflag = currentBill.getShort("settleflag");
            if (settleflag ==1 && CollectionUtils.isNotEmpty(fundPaymentList) && fundPaymentList.size() > 0) {
                // 删除单据
                List<BizObject> currentBillList = new ArrayList<>();
                currentBillList.add(currentBill);
                stwbBillService.deleteBill(currentBillList);
            }
            // 子表预算，实占成功，弃审时，需要释放实占，重新预占
            if (cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_PAYMENT)) {
                // TODO 此处子表结算成功时间不能清空，FundPayment_b__settleSuccessTime、settlesuccessSum字段;释放完预算实占之后才能清空
                fundPaymentExecuteAuditDelete(originFundPayment);
            }
            // 更新该币种银行结息账号预提规则的上次结息结束日=本次结息结束日
            List<BizObject> fundPaymentBList = currentBill.get(ICmpConstant.FUND_PAYMENT_B);
            fundCommonService.updateWithholdingRuleSettingLastInterestSettlementDate(fundPaymentBList, ICmpConstant.CONSTANT_ONE);

            // 不推待结算数据，弃审直接修改结算状态为待结算
            if (settleflag ==0) {
                // 单据数据可能被修改，重新查询数据库
                currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizobject.getId());
                List<FundPayment_b> list = currentBill.FundPayment_b();
                list.forEach(item -> {
                    if (item.getFundSettlestatus().getValue() == FundSettleStatus.SettleSuccess.getValue()
                            || item.getFundSettlestatus().getValue() == FundSettleStatus.PartSuccess.getValue()
                            || item.getFundSettlestatus().getValue() == FundSettleStatus.SettleFailed.getValue()) {
                        item.setFundSettlestatus(FundSettleStatus.WaitSettle);
                        item.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(item.getFundSettlestatus()));
                        item.setSettlesuccessSum(BigDecimal.ZERO);
                        item.setSettleSuccessTime(null);
                        item.setSettleerrorSum(BigDecimal.ZERO);
                        item.setEntityStatus(EntityStatus.Update);
                    }
                });
                MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, list);
            }
            // 单据撤回后不管是结算简强还是非结算简强，结算成功时间都应该是空的，结算成功金额都是0，清空上下文的数据，防止又被更新回数据库
            List<FundPayment_b> listb = ((FundPayment)bizobject).FundPayment_b();
            if(listb != null && listb.size() > 0){
                for (FundPayment_b bill_b : listb) {
                    bill_b.setSettleSuccessTime(null);
                    bill_b.setSettlesuccessSum(BigDecimal.ZERO);
                }
            }
            // 已审核
            bizobject.set("auditstatus", AuditStatus.Incomplete.getValue());
            bizobject.set("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
            bizobject.set("voucherstatus", VoucherStatus.Empty.getValue());
            bizobject.set("voucherNo", null);
            bizobject.set("voucherPeriod", null);
            bizobject.set("voucherId", null);
            bizobject.set("voucherstatus_original", currentBill.getVoucherstatus().getValue());
            bizobject.set("auditorId", null);
            bizobject.set("auditor", null);
            bizobject.set("auditDate", null);
            bizobject.set("auditTime", null);
            bizobject.set("settleSuccessTime", null);
            bizobject.set("fiEventDataVersion", null);
            bizobject.set("voucherVersion", null);
            bizobject.set("postingMsg", null);
        }
        return new RuleExecuteResult();
    }

    /**
     * 更新预算占用状态
     * @param originFundPayment
     * @throws Exception
     */
    private void fundPaymentExecuteAuditDelete(FundPayment originFundPayment) throws Exception {
        // 数据被更新过了，需重新查询一次
        //FundPayment currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, fundPayment.getId());
        FundPayment currentBill = originFundPayment;
        List<FundPayment_b> list = currentBill.FundPayment_b();
        List<FundPayment_b> updateList = new ArrayList();
        for (FundPayment_b fundPayment_b : list) {
            Short budgeted = fundPayment_b.getIsOccupyBudget();
            // 除非状态为实占成功，其他子表单据全部跳过
            if (budgeted == null || ((budgeted != OccupyBudget.ActualSuccess.getValue()))) {
                continue;
            }
            updateList.add(fundPayment_b);
        }
        if (!updateList.isEmpty()) {
            // 释放实占
            ResultBudget resultBudgetDelActual = cmpBudgetManagerService.gcExecuteTrueUnAudit(currentBill, updateList, IBillNumConstant.FUND_PAYMENT, BillAction.CANCEL_AUDIT);
            if (resultBudgetDelActual.isSuccess()) {
                List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, IBillNumConstant.FUND_PAYMENT);
                //获取平台配置
                String preAction = null;
                if (com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(objects)) {
                    preAction = (String) objects.get(0).get(PRE);
                }
                if (!org.apache.commons.lang3.StringUtils.isEmpty(preAction)) {
                    updateList.stream().forEach(fundPayment_b -> {
                        // 实占成功，弃审后变为预占成功
                        fundPayment_b.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                    });
                } else {//没有配置预占动作
                    updateList.stream().forEach(fundPayment_b -> {
                        // 实占成功，弃审后变为未占用
                        fundPayment_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    });
                }
            } else{//没有配置预占动作
                updateList.stream().forEach(fundPayment_b -> {
                    // 实占成功，弃审后变为未占用
                    fundPayment_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                        fundPayment_b.setEntityStatus(EntityStatus.Update);
                });
            }
            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, updateList);
        }
    }

}
