package com.yonyoucloud.fi.cmp.payapplicationbill.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.payapplicationbill.CloseStatus;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayBillStatus;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.PAYBILLSTATUS;

/**
 * <h1>付款申请单提交审批流前置规则</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-25 16:28
 */
@Slf4j
@Component("payApplicationBillSubmitRule")
public class PayApplicationBillSubmitRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400704", "在财务新架构环境下，不允许提交付款申请单。") /* "在财务新架构环境下，不允许提交付款申请单。" */);
        }
        bills.forEach(e -> {
            log.info("PayApplicationBillRuleToPayment, PayApplicationBill = {}", e);
            short payApplyClosed = Short.parseShort(e.get(PAYBILLSTATUS).toString());
            if (CloseStatus.Closed.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101952"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418038A","单据已关闭，不能再提交审批流！") /* "单据已关闭，不能再提交审批流！" */);
            }
            if (PayBillStatus.PendingPayment.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101953"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418038B","单据状态为待付款，不能再提交审批流！") /* "单据状态为待付款，不能再提交审批流！" */);
            }
            if (PayBillStatus.PartialPayment.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101954"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418038C","单据状态为部分付款，不能再提交审批流！") /* "单据状态为部分付款，不能再提交审批流！" */);
            }
            if (PayBillStatus.PaymentCompleted.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101955"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418038D","单据状态为付款完成，不能再提交审批流！") /* "单据状态为付款完成，不能再提交审批流！" */);
            }
            short verifyState = ValueUtils.isNotEmptyObj(e.get("verifystate")) ? Short.parseShort(e.get("verifystate").toString()) : null;
            if (VerifyState.TERMINATED.getValue() == verifyState) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101956"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180389","流程已终止，不允许提交单据！") /* "流程已终止，不允许提交单据！" */);
            }
        });
        return new RuleExecuteResult();
    }
}
