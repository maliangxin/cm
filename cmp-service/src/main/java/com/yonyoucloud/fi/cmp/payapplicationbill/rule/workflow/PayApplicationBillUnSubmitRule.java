package com.yonyoucloud.fi.cmp.payapplicationbill.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.payapplicationbill.ApprovalStatus;
import com.yonyoucloud.fi.cmp.payapplicationbill.CloseStatus;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayBillStatus;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <h1>付款申请单提交审批流前置规则</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-25 16:28
 */
@Slf4j
@Component("payApplicationBillUnSubmitRule")
public class PayApplicationBillUnSubmitRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400715", "在财务新架构环境下，不允许撤回付款申请单。") /* "在财务新架构环境下，不允许撤回付款申请单。" */);
        }
        for (BizObject bizobject : bills) {
            log.info("PayApplicationBillRuleToPayment, bizobject = {}", bizobject);
            BizObject currentBill = MetaDaoHelper.findById(bizobject.getEntityName(), bizobject.getId());
            short payApplyClosed = Short.parseShort(currentBill.get("payBillStatus").toString());
            if (null != bizobject.get("paymentPreemptAmountSum")
                    && ! (new BigDecimal(currentBill.get("paymentPreemptAmountSum").toString()).compareTo(BigDecimal.ZERO) == 0)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102636"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180509","单据已经拉单了，不能再撤回审批流！") /* "单据已经拉单了，不能再撤回审批流！" */);
            }
            if (CloseStatus.Closed.getValue() == Short.parseShort(currentBill.get("closeStatus").toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102637"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418050A","单据已关闭，不能再撤回审批流！") /* "单据已关闭，不能再撤回审批流！" */);
            }
            if (PayBillStatus.PendingPayment.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102638"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418050B","单据状态为待付款，不能再撤回审批流！") /* "单据状态为待付款，不能再撤回审批流！" */);
            }
            if (PayBillStatus.PartialPayment.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102639"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180507","单据状态为部分付款，不能再撤回审批流！") /* "单据状态为部分付款，不能再撤回审批流！" */);
            }
            if (PayBillStatus.PaymentCompleted.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102640"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180508","单据状态为付款完成，不能再撤回审批流！") /* "单据状态为付款完成，不能再撤回审批流！" */);
            }
            bizobject.set("payBillStatus", PayBillStatus.Auditing.getValue());
            bizobject.set("approvalStatus", ApprovalStatus.Approving.getValue());
        }
        return new RuleExecuteResult();
    }
}
