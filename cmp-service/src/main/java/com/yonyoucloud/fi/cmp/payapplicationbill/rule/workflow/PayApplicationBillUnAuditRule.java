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
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <h1>付款申请弃审规则</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021/7/9 13:53
 */
@Slf4j
@Component("payApplicationBillUnAuditRule")
public class PayApplicationBillUnAuditRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400588", "在财务新架构环境下，不允许弃审付款申请单。") /* "在财务新架构环境下，不允许弃审付款申请单。" */);
        }
        for (BizObject bizobject : bills) {
            log.info("PayApplicationBillUnAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
            BizObject currentBill = MetaDaoHelper.findById(bizobject.getEntityName(), bizobject.getId());
            log.info("PayApplicationBillUnAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100332"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180573","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100333"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180576","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            short payApplyClosed = Short.parseShort(currentBill.get("payBillStatus").toString());
            if (null != currentBill.get("paymentPreemptAmountSum")
                    && new BigDecimal(currentBill.get("paymentPreemptAmountSum").toString()).compareTo(BigDecimal.ZERO) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100334"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180570","单据已经拉单了，不能再弃审或撤回！") /* "单据已经拉单了，不能再弃审或撤回！" */);
            }
            if (CloseStatus.Closed.getValue() == Short.parseShort(currentBill.get("closeStatus").toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100335"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180572","单据已关闭，不能再弃审或撤回！") /* "单据已关闭，不能再弃审或撤回！" */);
            }
            if (PayBillStatus.PendingPayment.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100336"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180574","单据状态为待付款，不能再弃审或撤回！") /* "单据状态为待付款，不能再弃审或撤回！" */);
            }
            if (PayBillStatus.PartialPayment.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100337"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180575","单据状态为部分付款，不能再弃审或撤回！") /* "单据状态为部分付款，不能再弃审或撤回！" */);
            }
            if (PayBillStatus.PaymentCompleted.getValue() == payApplyClosed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100338"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180577","单据状态为付款完成，不能再弃审或撤回！") /* "单据状态为付款完成，不能再弃审或撤回！" */);
            }
            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100339"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180571","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            bizobject.set("payBillStatus", PayBillStatus.Auditing.getValue());
            bizobject.set("approvalStatus", ApprovalStatus.Approving.getValue());
            bizobject.set("auditorId", null);
            bizobject.set("auditor", null);
            bizobject.set("auditDate", null);
            bizobject.set("auditTime", null);
        }
        return new RuleExecuteResult();
    }
}
