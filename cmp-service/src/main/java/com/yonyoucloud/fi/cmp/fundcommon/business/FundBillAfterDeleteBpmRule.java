package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.Relatedinterest;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.PushCsplStatusEnum;
import com.yonyoucloud.fi.cmp.enums.SystemIntegrationParamsEnum;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanService;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <h1>资金收付款单审批流为驳回制单或终止：释放资资金计划项目；撤销驳回到制单：占用资金计划项目</h1>
 *
 * "deleteAll":删除流程实例或撤回流程到初始状态
 * "withdraw":(终审)撤销审核
 * "REJECTTOSTART":驳回制单
 * "WITHDRAWREJECTTOSTART":撤销驳回制单
 * "ACTIVITI_DELETED":终止
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023/3/11 18:40
 */
@Component("fundBillAfterDeleteBpmRule")
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FundBillAfterDeleteBpmRule extends AbstractCommonRule {
    private final CmCommonService commonService;
    private final IFundCommonService fundCommonService;
    private final CmpBudgetManagerService cmpBudgetManagerService;
    private final FundBillAdaptationFundPlanService fundBillAdaptationFundPlanService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        String deleteReason = billContext.getDeleteReason();
        List<BizObject> bills = getBills(billContext, map);
        String fullName = billContext.getFullname();
        String billnum = billContext.getBillnum();
        for (BizObject bizobject : bills) {
            BizObject currentBill = MetaDaoHelper.findById(fullName, bizobject.getId(),ICmpConstant.CONSTANT_TWO);
            currentBill.set("auditDate", null);
            currentBill.set("auditTime", null);
            currentBill.setEntityStatus(EntityStatus.Update);
            // 驳回制单或终止：释放资资金计划项目
            if(BooleanUtils.b(currentBill.get(ICmpConstant.IS_WFCONTROLLED)) && ("REJECTTOSTART".equals(deleteReason) || "ACTIVITI_DELETED".equals(deleteReason))){
                List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
                if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                    List<FundPayment_b> billbs = currentBill.getBizObjects(ICmpConstant.FUND_PAYMENT_B, FundPayment_b.class);
                    List<FundPayment_b> updateBillbs = new ArrayList<>();
                    for (FundPayment_b billb : billbs) {
                        Object isToPushCspl = billb.get(ICmpConstant.IS_TO_PUSH_CSPL);
                        if (ValueUtils.isNotEmptyObj(isToPushCspl) && ICmpConstant.CONSTANT_ONE == Integer.parseInt(isToPushCspl.toString())) {
                            billb.set(ICmpConstant.IS_TO_PUSH_CSPL, PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                            releaseFundBillForFundPlanProjectList.add(billb);
                        }
                    }
                    // 预算占用，释放
                    if (cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_PAYMENT)) {
                        List<FundPayment_b> updateList = new ArrayList();
                        for (FundPayment_b fundPayment_b : billbs) {
                            if (fundPayment_b.getIsOccupyBudget() != null && fundPayment_b.getIsOccupyBudget() == OccupyBudget.PreSuccess.getValue()) {
                                updateList.add(fundPayment_b);
                            }
                        }
                        if (updateList.size() > 0) {
                            ResultBudget resultBudget = cmpBudgetManagerService.gcExecuteBatchUnSubmit(currentBill,updateList, IBillNumConstant.FUND_PAYMENT, BillAction.CANCEL_APPROVE_BACK);
                            if (resultBudget != null && resultBudget.isSuccess()) {
                                updateList.stream().forEach(fundPayment_b -> {
                                    fundPayment_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                                    fundPayment_b.setEntityStatus(EntityStatus.Update);
                                });
                            }
                        }
                    }
                    // 更新审批时间
                    MetaDaoHelper.update(FundPayment.ENTITY_NAME, currentBill);
                    // 更新预算占用标志
                    MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, updateBillbs);
                } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
                    List<FundCollection_b> billbs = currentBill.getBizObjects(ICmpConstant.FUND_COLLECTION_B, FundCollection_b.class);
                    for (FundCollection_b billb : billbs) {
                        Object isToPushCspl = billb.get(ICmpConstant.IS_TO_PUSH_CSPL);
                        if (ValueUtils.isNotEmptyObj(isToPushCspl) && ICmpConstant.CONSTANT_ONE == Integer.parseInt(isToPushCspl.toString())) {
                            billb.set(ICmpConstant.IS_TO_PUSH_CSPL, PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                            releaseFundBillForFundPlanProjectList.add(billb);
                        }
                    }
                    // 更新审批时间
                    MetaDaoHelper.update(FundCollection.ENTITY_NAME, currentBill);
                }
                // 资金计划额度释放
                if (CollectionUtils.isNotEmpty(releaseFundBillForFundPlanProjectList)) {
                    fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(billnum, currentBill, releaseFundBillForFundPlanProjectList, null, null, "act");
                }
                if ("ACTIVITI_DELETED".equals(deleteReason)){
                    if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                        List<BizObject> billbs = currentBill.get(ICmpConstant.FUND_PAYMENT_B);
                        for (BizObject bizObjSub : billbs) {
                            // 判断款项类型是否为利息
                            if (fundCommonService.isInterestWithQuickType(bizObjSub.get(ICmpConstant.QUICK_TYPE))){
                                List<BizObject> fundPaymentSubWithholdingRelation = bizObjSub.get(ICmpConstant.FUND_PAYMENT_SUB_WITHHOLDING_RELATION);
                                // 是否有关联了预提单
                                if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                    List<Map<String, Object>> accrualsWithholdingMap = MetaDaoHelper.queryByIds(AccrualsWithholding.ENTITY_NAME, "*",
                                            fundPaymentSubWithholdingRelation.stream().map(e -> Long.parseLong(e.get(ICmpConstant.WITHHOLDING_ID).toString())).toArray(Long[]::new));
                                    List<AccrualsWithholding> accrualsWithholdingList = new ArrayList<>();
                                    for (Map<String, Object> maps : accrualsWithholdingMap) {
                                        AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
                                        accrualsWithholding.init(maps);
                                        // 更新结息单的【关联结息单】字段状态为未关联
                                        accrualsWithholding.setRelatedinterest(Relatedinterest.relatedUnAssociated.getValue());
                                        accrualsWithholding.setSrcbillmainid(null);
                                        accrualsWithholding.setSrcbillnum(null);
                                        accrualsWithholding.setSrcbilltype(null);
                                        accrualsWithholdingList.add(accrualsWithholding);
                                    }
                                    EntityTool.setUpdateStatus(accrualsWithholdingList);
                                    MetaDaoHelper.update(AccrualsWithholding.ENTITY_NAME, accrualsWithholdingList);
                                }
                            }
                        }
                    } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
                        List<BizObject> billbs = currentBill.get(ICmpConstant.FUND_COLLECTION_B);
                        for (BizObject bizObjSub : billbs) {
                            // 判断款项类型是否为利息
                            if (fundCommonService.isInterestWithQuickType(bizObjSub.get(ICmpConstant.QUICK_TYPE))){
                                List<BizObject> fundPaymentSubWithholdingRelation = bizObjSub.get(ICmpConstant.FUND_COLLECTION_SUB_WITHHOLDING_RELATION);
                                // 是否有关联了预提单
                                if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                    List<Map<String, Object>> accrualsWithholdingMap = MetaDaoHelper.queryByIds(AccrualsWithholding.ENTITY_NAME, "*",
                                            fundPaymentSubWithholdingRelation.stream().map(e -> Long.parseLong(e.get(ICmpConstant.WITHHOLDING_ID).toString())).toArray(Long[]::new));
                                    List<AccrualsWithholding> accrualsWithholdingList = new ArrayList<>();
                                    for (Map<String, Object> maps : accrualsWithholdingMap) {
                                        AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
                                        accrualsWithholding.init(maps);
                                        // 更新结息单的【关联结息单】字段状态为未关联
                                        accrualsWithholding.setRelatedinterest(Relatedinterest.relatedUnAssociated.getValue());
                                        accrualsWithholding.setSrcbillmainid(null);
                                        accrualsWithholding.setSrcbillnum(null);
                                        accrualsWithholding.setSrcbilltype(null);
                                        accrualsWithholdingList.add(accrualsWithholding);
                                    }
                                    EntityTool.setUpdateStatus(accrualsWithholdingList);
                                    MetaDaoHelper.update(AccrualsWithholding.ENTITY_NAME, accrualsWithholdingList);
                                }
                            }
                        }
                    }
                }
            // 撤销驳回到制单：占用资金计划项目
            } else if (BooleanUtils.b(currentBill.get(ICmpConstant.IS_WFCONTROLLED)) && "WITHDRAWREJECTTOSTART".equals(deleteReason)) {
                List<BizObject> employFundBillForFundPlanProjectList = new ArrayList<>();
                boolean checkFundPlanIsEnabled = false;
                if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                    // 撤销驳回到制单，等同于提交或审批通过，走预算的预占逻辑
                    if (cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_PAYMENT)) {
                        List<FundPayment_b> updateBillbs = new ArrayList<>();
                        List<FundPayment_b> billbs = currentBill.getBizObjects(ICmpConstant.FUND_PAYMENT_B, FundPayment_b.class);
                        for (FundPayment_b billb : billbs) {
                            Short budgeted = billb.getIsOccupyBudget();
                            // 已经预占仍要预占或者已经实占，直接跳过不执行了
                            if (budgeted != null && ((budgeted==OccupyBudget.PreSuccess.getValue()))) {
                                continue;
                            }
                            // 是否委托驳回，为是的时候，无需占用预算，
                            if (billb.getEntrustReject()!=null && billb.getEntrustReject()==1) {
                                continue;
                            }
                            updateBillbs.add(billb);
                        }
                        if (updateBillbs.size() > 0) {
                            ResultBudget resultBudget = cmpBudgetManagerService.gcExecuteBatchSubmit(currentBill,updateBillbs, IBillNumConstant.FUND_PAYMENT, BillAction.SUBMIT);
                            if (resultBudget.isSuccess()) {
                                if (resultBudget.getIds() != null && !resultBudget.getIds().isEmpty()) {
                                    updateBillbs.stream().forEach( fundPayment_b -> {
                                        if (resultBudget.getIds().contains(fundPayment_b.getId().toString())) {
                                            fundPayment_b.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                                            fundPayment_b.setEntityStatus(EntityStatus.Update);
                                        }
                                    });
                                } else {
                                    updateBillbs.stream().forEach( fundPayment_b -> {
                                        fundPayment_b.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                                        fundPayment_b.setEntityStatus(EntityStatus.Update);
                                    });
                                }
                            }
                            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, updateBillbs);
                        }
                    }
                    checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundPayment.getValue());
                } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
                    checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundCollection.getValue());
                }
                if (checkFundPlanIsEnabled) {
                    if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                        List<FundPayment_b> billbs = currentBill.getBizObjects(ICmpConstant.FUND_PAYMENT_B, FundPayment_b.class);
                        for (FundPayment_b billb : billbs) {
                            Integer isToPushCspl = billb.getIsToPushCspl();
                            boolean isToPushCsplFlag = ValueUtils.isNotEmptyObj(isToPushCspl)
                                    && PushCsplStatusEnum.PRE_OCCUPIED.getValue().equals(isToPushCspl);
                            Integer entrustReject = billb.getEntrustReject();
                            boolean entrustRejectFlag = ValueUtils.isNotEmptyObj(entrustReject)
                                    && !ICmpConstant.CONSTANT_ONE.equals(entrustReject);
                            if (isToPushCsplFlag && entrustRejectFlag && ValueUtils.isNotEmptyObj(billb.get("fundPlanProject"))) {
                                billb.set(ICmpConstant.IS_TO_PUSH_CSPL, PushCsplStatusEnum.ALREADY_OCCUPIED.getValue());
                                employFundBillForFundPlanProjectList.add(billb);
                            }
                        }
                    } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
                        List<FundCollection_b> billbs = currentBill.getBizObjects("FundCollection_b", FundCollection_b.class);
                        for (FundCollection_b billb : billbs) {
                            Integer isToPushCspl = billb.getIsToPushCspl();
                            boolean isToPushCsplFlag = ValueUtils.isNotEmptyObj(isToPushCspl)
                                    && PushCsplStatusEnum.PRE_OCCUPIED.getValue().equals(isToPushCspl);
                            Integer entrustReject = billb.getEntrustReject();
                            boolean entrustRejectFlag = ValueUtils.isNotEmptyObj(entrustReject)
                                    && !ICmpConstant.CONSTANT_ONE.equals(entrustReject);
                            if (isToPushCsplFlag && entrustRejectFlag && ValueUtils.isNotEmptyObj(billb.get("fundPlanProject"))) {
                                billb.set(ICmpConstant.IS_TO_PUSH_CSPL, PushCsplStatusEnum.ALREADY_OCCUPIED.getValue());
                                employFundBillForFundPlanProjectList.add(billb);
                            }
                        }

                    }
                }
                // 资金计划额度占用
                if (CollectionUtils.isNotEmpty(employFundBillForFundPlanProjectList)) {
                    fundBillAdaptationFundPlanService.fundBillEmployFundPlan(billnum, currentBill, employFundBillForFundPlanProjectList, "act");
                }
            }
        }
        return new RuleExecuteResult();
    }
}



