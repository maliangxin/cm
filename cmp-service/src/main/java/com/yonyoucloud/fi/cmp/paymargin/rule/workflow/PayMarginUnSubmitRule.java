package com.yonyoucloud.fi.cmp.paymargin.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.ctm.stwb.api.vo.reqvo.QueryDataSettledVO;
import com.yonyoucloud.ctm.stwb.api.vo.respvo.DataSettledDetailVO;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDetail;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.openapi.QuerySettledDetailModel;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.stwbentity.WSettleStatus;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetPaymarginManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.margincommon.service.MarginCommonService;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.paymargin.service.PayMarginService;
import com.yonyoucloud.fi.cmp.util.BillAction;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 支付保证金撤回规则
 * *
 *
 * @author xuxbo
 * @date 2023/8/3 15:15
 */

@RequiredArgsConstructor
@Component
public class PayMarginUnSubmitRule extends AbstractCommonRule {
    @Autowired
    private PayMarginService payMarginService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetPaymarginManagerService cmpBudgetPaymarginManagerService;
    @Autowired
    MarginCommonService marginCommonService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            PayMargin payMargin = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizObject.getId(), null);
            short verifystate = Short.parseShort(bizObject.get(ICmpConstant.VERIFY_STATE).toString());
            if (verifystate == VerifyState.INIT_NEW_OPEN.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101385"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418038F", "单据未提交，不能进行撤回！") /* "单据未提交，不能进行撤回！" */);
            }
            if (verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101386"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180390", "单据已终止流程，不能进行撤回！") /* "单据已终止流程，不能进行撤回！" */);
            }
            short voucherstatus = payMargin.getVoucherstatus();
            if (voucherstatus == VoucherStatus.POSTING.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101387"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418038E", "过账中的单据，不能进行撤回！") /* "过账中的单据，不能进行撤回！" */);
            }

            // 2024.10.59 保证金需求 当 同名账户划转”字段为：是 结算状态为已结算补单 并且 推送结算为是 并且 审批状态是已完成
            // 并且 内部单位银行账户名称 字段不为空时， 此时应该去调用结算的查询接口  只有返回的数据是两条的时候 才让撤回
            short settlestatus = payMargin.getSettlestatus();
            short settleflag = payMargin.getSettleflag();
            short samenametransferflag = payMargin.getSamenametransferflag();
            String ourbankaccount = payMargin.getOurbankaccount();
            if (ObjectUtils.isNotEmpty(settlestatus)) {
                if (settlestatus == FundSettleStatus.SettlementSupplement.getValue() && settleflag == 1
                        && verifystate == VerifyState.COMPLETED.getValue() && samenametransferflag == 1 && ObjectUtils.isNotEmpty(ourbankaccount)) {
                    //调用结算的查询接口
                    QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
                    querySettledDetailModel.setWdataorigin(8);// 来源业务系统 8-现金管理
                    querySettledDetailModel.setBusinessDetailsId(payMargin.getId().toString());//业务单据明细ID -> 转账单-ID
                    List<DataSettledDetail> dataSettledDetailList = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).querySettledDetails(querySettledDetailModel);
                    if (ObjectUtils.isNotEmpty(dataSettledDetailList) && dataSettledDetailList.size() != 2 && ArrayUtils.contains(new short[] {WSettleStatus.SettleDone.getValue(), WSettleStatus.SettleProssing.getValue()}, Short.parseShort(dataSettledDetailList.get(0).getWsettleStatus()))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100172"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D70EA3405000007", "待结算数据推送处理中，不可撤回！") /* "待结算数据推送处理中，不可撤回！" */);
                    }
                }
            }

            // 结算状态为已结算补单 并且 推送结算 并且 凭证状态是为生成的  不允许撤回
//            Short settlestatus = payMargin.getSettlestatus();
//            Short settleflag = payMargin.getSettleflag();
            if (ObjectUtils.isNotEmpty(settlestatus)){
                if (settlestatus == FundSettleStatus.SettlementSupplement.getValue() && settleflag == 1 && voucherstatus == VoucherStatus.Empty.getValue()
                        && verifystate == VerifyState.COMPLETED.getValue()) {
                    marginCommonService.checkHasSettlementBill(payMargin.getId().toString());
                }
            }
            //期初为是的允许撤回 现金的允许撤回
//            Boolean initflag = bizObject.getBoolean(ICmpConstant.INITFLAG);
//            Short srcitem = bizObject.getShort(ICmpConstant.SRC_ITEM);
//            if (initflag.equals(false) && !srcitem.equals(EventSource.Cmpchase.getValue())) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101430"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC591605080009", "单据不是来源于现金管理或者是否期初为是的单据，不允许撤回！") /* "单据不是来源于现金管理或者是否期初为是的单据，不允许撤回" */);
//            }
//            if (!srcitem.equals(EventSource.Cmpchase.getValue())) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101430"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC591605080009", "单据不是来源于现金管理或者是否期初为是的单据，不允许撤回！") /* "单据不是来源于现金管理或者是否期初为是的单据，不允许撤回" */);
//            }
            if (!payMargin.getIsWfControlled()) {
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_PAYMARGIN)) {
                    payMarginExecuteAuditDelete(payMargin);
                    //刷新pubts
                    PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizObject.getId(), null);
                    bizObject.setPubts(payMarginNew.getPubts());
                }
                // 未启动审批流，单据直接弃审通过
                result = BillBiz.executeRule(ICmpConstant.UN_AUDIT, billContext, paramMap);
                result.setCancel(true);
            } else {
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_PAYMARGIN)) {
                    payMarginExecuteSubmitDelete(payMargin);
                }
            }
            payMarginService.changeSettleFlagAfterUnAudit(payMargin);
            //刷新pubts
            PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizObject.getId(), null);
            bizObject.setPubts(payMarginNew.getPubts());
            PayMargin payMarginObj = (PayMargin) bizObject;
            payMarginObj.setSettlestatus(payMarginNew.getSettlestatus());
            payMarginObj.setSettlesuccesstime(payMarginNew.getSettlesuccesstime());
            payMarginObj.setAuditTime(payMarginNew.getAuditTime());
            payMarginObj.setIsOccupyBudget(payMarginNew.getIsOccupyBudget());
        }
        return result;
    }

    /**
     * 是否占预算为预占成功时，删除预占；
     * @param payMargin
     * @throws Exception
     */
    private void payMarginExecuteSubmitDelete(PayMargin payMargin) throws Exception {
        PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, payMargin.getId());
        Short budgeted = payMarginNew.getIsOccupyBudget();
        PayMargin update = new PayMargin();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted != null && budgeted == OccupyBudget.PreSuccess.getValue()) {
            ResultBudget resultBudget = cmpBudgetPaymarginManagerService.releaseBudget(payMargin, payMargin, IBillNumConstant.CMP_PAYMARGIN, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(payMargin.getId());
                update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(PayMargin.ENTITY_NAME, update);
            }
        } else if (budgeted != null && budgeted == OccupyBudget.ActualSuccess.getValue()) {
            ResultBudget resultBudget = cmpBudgetPaymarginManagerService.gcExecuteTrueUnAudit(payMargin, payMargin, IBillNumConstant.CMP_PAYMARGIN, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(payMargin.getId());
                update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(PayMargin.ENTITY_NAME, update);
            }
        }
    }

    /**
     * 是否占预算为实占成功时，删除实占；
     * 是否占预算为预占成功时，删除预占；
     * @param payMargin
     * @throws Exception
     */
    private void payMarginExecuteAuditDelete(PayMargin payMargin) throws Exception {
        PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, payMargin.getId());
        Short budgeted = payMarginNew.getIsOccupyBudget();
        PayMargin update = new PayMargin();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return;
        } else if (OccupyBudget.PreSuccess.getValue() == budgeted) {//是否占预算为预占成功时，删除预占；
            ResultBudget resultBudget = cmpBudgetPaymarginManagerService.releaseBudget(payMargin, payMarginNew, IBillNumConstant.CMP_PAYMARGIN, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(payMargin.getId());
                update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(PayMargin.ENTITY_NAME, update);
            }
            MetaDaoHelper.update(PayMargin.ENTITY_NAME, payMarginNew);
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            ResultBudget resultBudget = cmpBudgetPaymarginManagerService.gcExecuteTrueUnAudit(payMargin, payMarginNew, IBillNumConstant.CMP_PAYMARGIN, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(payMargin.getId());
                update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(PayMargin.ENTITY_NAME, update);
            }
        }
    }

}
