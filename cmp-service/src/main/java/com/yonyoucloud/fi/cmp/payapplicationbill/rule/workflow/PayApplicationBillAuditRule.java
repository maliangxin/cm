package com.yonyoucloud.fi.cmp.payapplicationbill.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
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

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <h1>付款申请审核规则</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021/7/9 13:57
 */
@Slf4j
@Component("payApplicationBillAuditRule")
public class PayApplicationBillAuditRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400696", "在财务新架构环境下，不允许审批付款申请单。") /* "在财务新架构环境下，不允许审批付款申请单。" */);
        }
        for (BizObject bizobject : bills) {
            log.info("PayApplicationBillAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
            BizObject currentBill = MetaDaoHelper.findById(bizobject.getEntityName(), bizobject.getId());
            log.info("PayApplicationBillAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102000"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180093","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102001"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180096","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            short payApplyClosed = Short.parseShort(currentBill.get("payBillStatus").toString());
            if (CloseStatus.Closed.getValue() == Short.parseShort(currentBill.get("closeStatus").toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102002"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180090","单据已关闭，不能再审核！") /* "单据已关闭，不能再审核！" */);
            }
            if (PayBillStatus.PendingPayment.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102003"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180091","单据状态为待付款，不能再审核！") /* "单据状态为待付款，不能再审核！" */);
            }
            if (PayBillStatus.PartialPayment.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102004"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180094","单据状态为部分付款，不能再审核！") /* "单据状态为部分付款，不能再审核！" */);
            }
            if (PayBillStatus.PaymentCompleted.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102005"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180095","单据状态为付款完成，不能再审核！") /* "单据状态为付款完成，不能再审核！" */);
            }
            Date date = BillInfoUtils.getBusinessDate();
            if (null != billContext.getDeleteReason()) {
                if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                    return new RuleExecuteResult();
                }
            }
            Date currentDate = BillInfoUtils.getBusinessDate();
            if (currentDate.compareTo(date) < 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102006"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E0", "审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
            }
            // 已审核
            bizobject.set("payBillStatus", PayBillStatus.PendingApproval.getValue());
            bizobject.set("approvalStatus", ApprovalStatus.ApprovedPass.getValue());
            bizobject.set("auditorId", AppContext.getCurrentUser().getId());
            bizobject.set("auditor", AppContext.getCurrentUser().getName());
            bizobject.set("auditDate", AppContext.getCurrentUser().getName());
            bizobject.set("auditTime", new Date());
        }
        return new RuleExecuteResult();
    }
}
