package com.yonyoucloud.fi.cmp.fundcollection.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.migrade.CmpNewFiPreCheckService;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.BillAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * <h1>资金付款单弃审规则</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-13 10:16
 */
@Slf4j
@Component
public class FundCollectionUnAuditRule extends AbstractCommonRule {

    @Qualifier("stwbCollectionBillServiceImpl")
    @Autowired
    StwbBillService stwbBillService;

    @Autowired
    private IFundCommonService fundCommonService;

    @Autowired
    private CmpNewFiPreCheckService cmpNewFiPreCheckService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        // 启用审批流时，最后一个审批通过的操作人，撤回时校验
        fundCommonService.statementUnSubmitVerificationByFundCollection(bills);
        for (BizObject bizobject : bills) {
            log.info("FundCollectionAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
            FundCollection currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizobject.getId());
            log.info("FundCollectionAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(),
                    currentBill.getPubts());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100871"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EE", "单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }
//            // 如果是委托收款单或者委托付款单、并且是已结算支付状态，不能撤回
//            List<FundCollection_b> fundCollection_bList = currentBill.get("FundCollection_b");
//            List<FundCollection_b> filterList =
//                    fundCollection_bList.stream().filter(item -> item.get("settlestatus").equals(7)).collect(Collectors.toList());
//            if (currentBill.get("billtype").equals(100) && filterList.size() > 0) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100872"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID" );
//                        ":P_CM-BE_1811A04805B0003D", "已结算补单场景下不允许委托单弃审") /* "已结算补单场景下不允许委托单弃审" */);
//            }
            if (String.valueOf(VerifyState.INIT_NEW_OPEN.getValue()).equals(currentBill.get("verifystate"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100873"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EF", "单据未提交，不能进行撤回！") /* "单据未提交，不能进行撤回！" */);
            }
            // begin yangjn 20240130 老架构收付款工作台升级数据 如果来源单据已经结算完成 则资金收付不能逆操作
            if (currentBill.getMigradeid() != null) {
                cmpNewFiPreCheckService.checkUpgradeDataBack(ReceiveBill.ENTITY_NAME, currentBill.getMigradeid());
            }
            // end yangjn 20240130
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100874"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802F1", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            Date date = BillInfoUtils.getBusinessDate();
            if (null != billContext.getDeleteReason()) {
                if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {// 删除流程实例
                    return new RuleExecuteResult();
                }
            }
            Date currentDate = BillInfoUtils.getBusinessDate();
            if (currentDate.compareTo(date) < 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100875"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802F0", "审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
            }

            //暂存调用结算之前的对象，调用预算的时候必须是原始对象，修改完的对象调用预算会释放不了实占
            FundCollection originFundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, currentBill.getId());;

            String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
            List<BizObject> fundCollectionList = currentBill.get(childrenFieldCheck);
            Integer settleflag = currentBill.getInteger("settleflag");
            if (settleflag == 1 && CollectionUtils.isNotEmpty(fundCollectionList) && fundCollectionList.size() > 0) {
                // 删除单据
                List<BizObject> currentBillList = new ArrayList<>();
                currentBillList.add(currentBill);
                stwbBillService.deleteBill(currentBillList);
            }
            FundCollection afterStwb = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizobject.getId());
            // 子表预算，实占成功，弃审时，需要释放实占，重新预占
            if (cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_COLLECTION)) {
                // TODO 此处子表结算成功时间不能清空，FundCollection_b__settleSuccessTime、settlesuccessSum字段;释放完预算实占之后才能清空
                fundCollectionExecuteAuditDelete(originFundCollection);
                afterStwb.FundCollection_b().forEach(
                        (FundCollection_b sub) -> {
                            FundCollection_b fundCollectionB = new FundCollection_b();
                            fundCollectionB.setId(sub.getId());
                            fundCollectionB.set("transNumber", sub.getTransNumber());
                            fundCollectionB.set("settleSuccessTime", sub.getSettleSuccessTime());
                            fundCollectionB.set("settlesuccessSum", sub.getSettlesuccessSum());
                            fundCollectionB.set("settlestatus", sub.get("settlestatus"));
                            fundCollectionB.set("stwbSettleStatus", sub.get("stwbSettleStatus"));
                            fundCollectionB.set("actualSettlementExchangeRate", sub.getActualSettlementExchangeRate());
                            fundCollectionB.set("actualSettlementExchangeRateType", sub.getActualSettlementExchangeRateType());
                            fundCollectionB.set("actualSettlementAmount", sub.getActualSettlementAmount());
                            fundCollectionB.setEntityStatus(EntityStatus.Update);
                            try {
                                MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollectionB);
                            } catch (Exception e) {
                                log.error("FundCollectionUnAuditRule update sub error" + e.getMessage());
                            }
                        }
                );
                //把rule中用的数据赋值给上下文
                copySubFieidValue(originFundCollection, (FundCollection) bizobject,new String[]{"isOccupyBudget"});
                copySubFieidValue(afterStwb, (FundCollection) bizobject,new String[]{"transNumber", "settleSuccessTime", "settlesuccessSum", "settlestatus", "stwbSettleStatus",
                        "actualSettlementExchangeRate", "actualSettlementExchangeRateType", "actualSettlementAmount"});
            }

            // 更新该币种银行结息账号预提规则的上次结息结束日=本次结息结束日
            List<BizObject> fundCollectionBList = currentBill.get(ICmpConstant.FUND_COLLECTION_B);
            fundCommonService.updateWithholdingRuleSettingLastInterestSettlementDate(fundCollectionBList,
                    ICmpConstant.CONSTANT_ONE);

            // 不推待结算数据，弃审直接修改结算状态为待结算
            if (settleflag == 0) {
                currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizobject.getId());
                List<FundCollection_b> list = currentBill.FundCollection_b();
                list.forEach(item -> {
                    if (item.getFundSettlestatus().getValue() == FundSettleStatus.SettleSuccess.getValue()
                            || item.getFundSettlestatus().getValue() == FundSettleStatus.PartSuccess.getValue()
                            || item.getFundSettlestatus().getValue() == FundSettleStatus.SettleFailed.getValue()) {
                        item.setFundSettlestatus(FundSettleStatus.WaitSettle);
                        item.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(item.getFundSettlestatus()));
                        item.setSettleerrorSum(BigDecimal.ZERO);
                        item.setSettleSuccessTime(null);
                        item.setSettleerrorSum(BigDecimal.ZERO);
                        item.setEntityStatus(EntityStatus.Update);
                    }
                });
                MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, list);
            }
            // 已审核
            bizobject.set("auditstatus", AuditStatus.Incomplete.getValue());
            bizobject.set("voucherstatus", VoucherStatus.Empty.getValue());
            bizobject.set("voucherNo", null);
            bizobject.set("voucherPeriod", null);
            bizobject.set("voucherId", null);
            bizobject.set("voucherstatus_original", currentBill.getVoucherstatus().getValue());
            bizobject.set("auditorId", AppContext.getCurrentUser().getId());
            bizobject.set("auditor", AppContext.getCurrentUser().getName());
            bizobject.set("auditDate", new Date());
            bizobject.set("auditTime", new Date());
            bizobject.set("settleSuccessTime", null);
            bizobject.set("fiEventDataVersion", null);
            bizobject.set("voucherVersion", null);
            bizobject.set("postingMsg", null);
        }
        return new RuleExecuteResult();
    }

    /**
     * 更新预算占用状态
     *
     * @param originFundCollection
     * @throws Exception
     */
    private void fundCollectionExecuteAuditDelete(FundCollection originFundCollection) throws Exception {
        // 数据被更新过了，需重新查询一次
        //FundCollection currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, fundCollection.getId());
//        FundCollection currentBill = originFundCollection;
        List<FundCollection_b> list = originFundCollection.FundCollection_b();
        List<FundCollection_b> updateList = new ArrayList();
        for (FundCollection_b fundCollection_b : list) {
            Short budgeted = fundCollection_b.getIsOccupyBudget();
            // 除非状态为实占成功，其他子表单据全部跳过
            if (budgeted == null || ((budgeted != OccupyBudget.ActualSuccess.getValue()))) {
                continue;
            }
            updateList.add(fundCollection_b);
        }
        if (!updateList.isEmpty()) {
            // 释放实占
            ResultBudget resultBudgetDelActual =
                    cmpBudgetManagerService.fundCollectionReleaseActualOccupySuccessUnAudit(originFundCollection, updateList,
                            IBillNumConstant.FUND_COLLECTION, BillAction.CANCEL_AUDIT);
            if (resultBudgetDelActual.isSuccess()) {
                updateList.stream().forEach(fundCollection_b -> {
                    // 实占成功，弃审后变为预占成功
                    fundCollection_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    fundCollection_b.setEntityStatus(EntityStatus.Update);
                });
                MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, updateList);
            }
        }
    }

    private void copySubFieidValue(FundCollection original, FundCollection target,String[] fields) throws Exception {
        if(original == null || target==null || fields == null || fields.length == 0 ){
            return;
        }
        List<FundCollection_b> oriSubList = original.FundCollection_b();
        if(oriSubList == null || oriSubList.size() == 0){
            return;
        }
        List<FundCollection_b> tarSubList = target.FundCollection_b();
        if(tarSubList == null || tarSubList.size() == 0){
            return;
        }
        HashMap<String, FundCollection_b> oriSubMap = new HashMap<>();
        for (int i = 0; i < oriSubList.size(); i++) {
            oriSubMap.put(oriSubList.get(i).getId().toString(), oriSubList.get(i));
        }
        HashMap<String, FundCollection_b> tarSubMap = new HashMap<>();
        for (int i = 0; i < tarSubList.size(); i++) {
            tarSubMap.put(tarSubList.get(i).getId().toString(), tarSubList.get(i));
        }
        oriSubMap.keySet().forEach(key -> {
            FundCollection_b oriSub = oriSubMap.get(key);
            FundCollection_b tarSub = tarSubMap.get(key);
            if(tarSub == null){
                return;
            }
            copyFieidValue(oriSub, tarSub, fields);
        });
    }

    private void copyFieidValue(FundCollection_b original, FundCollection_b target,String[] fields) {
        if(original == null || target==null || fields == null || fields.length == 0){
            return;
        }
        for(String field : fields){
            Object value = original.get(field);
            if(value != null){
                target.set(field, value);
            }
        }
    }
}
