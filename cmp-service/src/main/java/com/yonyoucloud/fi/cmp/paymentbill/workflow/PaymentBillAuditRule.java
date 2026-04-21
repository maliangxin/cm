package com.yonyoucloud.fi.cmp.paymentbill.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paymentbill.rule.FiCmpPaymentBaseRule;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentServiceUtil;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 付款单审核规则
 * 审批流相关
 *
 * @author lidchn
 * 2021年8月3日20:37:05
 */
@Slf4j
@Component
public class PaymentBillAuditRule extends FiCmpPaymentBaseRule {

    @Autowired
    private JournalService journalService;
    @Autowired
    private PaymentServiceUtil paymentServiceUtil;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103015"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2575C04E00004", "在财务新架构环境下，不允许审批付款单。") /* "在财务新架构环境下，不允许审批付款单。" */);
        }
        if(log.isInfoEnabled()) {
            log.info("##########   executing  PaymentBillAuditRule , request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        List<PayBill> payBillList = this.getBills(billContext, paramMap);
        if (payBillList == null || payBillList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100637"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418062B","请选择单据！") /* "请选择单据！" */);
        }
        PayBill payBill = payBillList.get(0);
        PayBill currentBill = MetaDaoHelper.findById(billContext.getFullname(), payBill.getId());
        Date date = BillInfoUtils.getBusinessDate();
        PayStatus payStatus = currentBill.getPaystatus();
        AuditStatus auditStatus = currentBill.getAuditstatus();
        Date currentPubts = payBillList.get(0).getPubts();

        if (payBill.getVouchdate() != null && date != null && date.compareTo(payBill.getVouchdate()) < 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100638"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418062F","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180630","】审批日期小于单据日期，不能审批！") /* "】审批日期小于单据日期，不能审批！" */);
        }
        if (payStatus != null && payStatus != PayStatus.NoPay && payStatus != PayStatus.PreFail && payStatus != PayStatus.Fail) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100639"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418062A","该单据支付状态不能进行审批！") /* "该单据支付状态不能进行审批！" */);
        }
        if (auditStatus == AuditStatus.Complete) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100640"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418062C","该单据已审批，不能进行重复审批！") /* "该单据已审批，不能进行重复审批！" */);
        }
        EventSource eventSource = currentBill.getSrcitem();
        if (eventSource != null && eventSource != EventSource.Cmpchase) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100641"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418062D","该单据不是现金自制单据，不能进行审批！") /* "该单据不是现金自制单据，不能进行审批！" */);
        }
        if (currentPubts != null && currentPubts.compareTo(currentBill.getPubts()) != 0) {//Pubts改变，出现多线程问题
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100642"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418062E","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }
        payBill.setAuditstatus(AuditStatus.Complete);
        payBill.setEntityStatus(EntityStatus.Update);
        payBill.setAuditDate(BillInfoUtils.getBusinessDate());
        payBill.setAuditTime(new Date());
        payBill.setAuditorId(AppContext.getCurrentUser().getId());
        payBill.setAuditor(AppContext.getCurrentUser().getName());
        currentBill.setAuditstatus(AuditStatus.Complete);
        currentBill.setAuditor(AppContext.getCurrentUser().getName());
        currentBill.setAuditorId(AppContext.getCurrentUser().getId());
        currentBill.setAuditDate(BillInfoUtils.getBusinessDate());
        currentBill.setAuditTime(new Date());
        journalService.updateJournal(currentBill);
        // 当付款单拉取的是付款申请时，单据状态改为待付款。
        paymentServiceUtil.auditPayBillPullPayApplyBillUpdatePayBillStatus(payBillList, "audit");
        return new RuleExecuteResult();
    }

}
