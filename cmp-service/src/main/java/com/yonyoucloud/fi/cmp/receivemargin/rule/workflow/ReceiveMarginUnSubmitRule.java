package com.yonyoucloud.fi.cmp.receivemargin.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetReceivemarginManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.margincommon.service.MarginCommonService;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.receivemargin.service.ReceiveMarginService;
import com.yonyoucloud.fi.cmp.util.BillAction;
import lombok.RequiredArgsConstructor;
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
 *  *
 * @author xuxbo
 * @date 2023/8/3 15:15
 */

@RequiredArgsConstructor
@Component
public class ReceiveMarginUnSubmitRule extends AbstractCommonRule {
    @Autowired
    private ReceiveMarginService receiveMarginService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetReceivemarginManagerService cmpBudgetReceivemarginManagerService;
    @Autowired
    MarginCommonService marginCommonService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            ReceiveMargin receiveMargin = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId(), null);
            short verifystate = Short.parseShort(bizObject.get(ICmpConstant.VERIFY_STATE).toString());
            if (verifystate == VerifyState.INIT_NEW_OPEN.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101385"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418038F","单据未提交，不能进行撤回！") /* "单据未提交，不能进行撤回！" */);
            }
            if (verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101386"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180390","单据已终止流程，不能进行撤回！") /* "单据已终止流程，不能进行撤回！" */);
            }
            short voucherstatus = receiveMargin.getVoucherstatus();
            if (voucherstatus == VoucherStatus.POSTING.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101387"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418038E","过账中的单据，不能进行撤回！") /* "过账中的单据，不能进行撤回！" */);
            }

            // 结算状态为已结算补单 并且 推送结算 并且 凭证状态是为生成的  不允许撤回
            Short settlestatus = receiveMargin.getSettlestatus();
            Short settleflag = receiveMargin.getSettleflag();
            if (ObjectUtils.isNotEmpty(settlestatus)){
                if (settlestatus.equals(FundSettleStatus.SettlementSupplement.getValue()) && settleflag == 1 && voucherstatus == VoucherStatus.Empty.getValue()
                        && verifystate == VerifyState.COMPLETED.getValue()) {
                    marginCommonService.checkHasSettlementBill(receiveMargin.getId().toString());
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
            if(!receiveMargin.getIsWfControlled()){
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_RECEIVEMARGIN)) {
                    receiveMarginExecuteAuditDelete(receiveMargin);
                    //刷新pubts
                    ReceiveMargin receiveMarginNew = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId(), null);
                    bizObject.setPubts(receiveMarginNew.getPubts());
                }
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule(ICmpConstant.UN_AUDIT, billContext, paramMap);
                result.setCancel(true);
            } else {
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_RECEIVEMARGIN)) {
                    receiveMarginExecuteSubmitDelete(receiveMargin);
                }
            }
            receiveMarginService.changeSettleFlagAfterUnAudit(receiveMargin);
            //刷新pubts
            ReceiveMargin receiveMarginNew = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId(), null);
            bizObject.setPubts(receiveMarginNew.getPubts());
            ReceiveMargin receiveMarginObj = (ReceiveMargin) bizObject;
            receiveMarginObj.setSettlestatus(receiveMarginNew.getSettlestatus());
            receiveMarginObj.setSettlesuccesstime(receiveMarginNew.getSettlesuccesstime());
            receiveMarginObj.setAuditTime(receiveMarginNew.getAuditTime());
            receiveMarginObj.setIsOccupyBudget(receiveMarginNew.getIsOccupyBudget());
        }
        return result;
    }
    /**
     * 是否占预算为预占成功时，删除预占；
     * @param receiveMargin
     * @throws Exception
     */
    private void receiveMarginExecuteSubmitDelete(ReceiveMargin receiveMargin) throws Exception {
        ReceiveMargin receiveMarginNew = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, receiveMargin.getId());
        Short budgeted = receiveMarginNew.getIsOccupyBudget();
        ReceiveMargin update = new ReceiveMargin();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted != null && budgeted == OccupyBudget.PreSuccess.getValue()) {
            ResultBudget resultBudget = cmpBudgetReceivemarginManagerService.releaseBudget(receiveMargin, receiveMarginNew, IBillNumConstant.CMP_RECEIVEMARGIN, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(receiveMargin.getId());
                update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, update);
            }
        } else if (budgeted != null && budgeted == OccupyBudget.ActualSuccess.getValue()) {
            ResultBudget resultBudget = cmpBudgetReceivemarginManagerService.gcExecuteTrueUnAudit(receiveMargin, receiveMarginNew, IBillNumConstant.CMP_RECEIVEMARGIN, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(receiveMargin.getId());
                update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, update);
            }
        }
    }

    /**
     * 是否占预算为实占成功时，删除实占；
     * 是否占预算为预占成功时，删除预占；
     * @param receiveMargin
     * @throws Exception
     */
    private void receiveMarginExecuteAuditDelete(ReceiveMargin receiveMargin) throws Exception {
        ReceiveMargin receiveMarginNew = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, receiveMargin.getId());
        Short budgeted = receiveMarginNew.getIsOccupyBudget();
        ReceiveMargin update = new ReceiveMargin();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return;
        } else if (OccupyBudget.PreSuccess.getValue() == budgeted) {//是否占预算为预占成功时，删除预占；
            ResultBudget resultBudget = cmpBudgetReceivemarginManagerService.releaseBudget(receiveMargin, receiveMarginNew, IBillNumConstant.CMP_RECEIVEMARGIN, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(receiveMargin.getId());
                update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, update);
            }
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            ResultBudget resultBudget = cmpBudgetReceivemarginManagerService.gcExecuteTrueUnAudit(receiveMargin, receiveMarginNew, IBillNumConstant.CMP_RECEIVEMARGIN, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(receiveMargin.getId());
                update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, update);
            }
        }
    }
}
