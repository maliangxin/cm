package com.yonyoucloud.fi.cmp.paymentbill.workflow;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paymentbill.rule.FiCmpPaymentBaseRule;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentServiceUtil;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 付款单弃审规则（审核的逆向操作）
 * 审批流相关
 *
 * @author lidchn
 * 2021年8月3日19:58:20
 */
@Slf4j
@Component
public class PaymentBillUnAuditRule extends FiCmpPaymentBaseRule {

    @Autowired
    private JournalService journalService;
    @Autowired
    private PaymentServiceUtil paymentServiceUtil;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103017"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2583004E00003", "在财务新架构环境下，不允许弃审付款单。") /* "在财务新架构环境下，不允许弃审付款单。" */);
        }
        if(log.isInfoEnabled()) {
            log.info("##########   executing  PaymentBillUnAuditRule , request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        List<PayBill> paymentBillList = this.getBills(billContext, paramMap);
        if (CollectionUtils.isEmpty(paymentBillList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101134"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180833","请选择单据！") /* "请选择单据！" */);
        }
        PayBill payBill = paymentBillList.get(0);//此处只会有一条数据进来，因为审批流仅会传入一条单据
        PayBill currentBill = MetaDaoHelper.findById(billContext.getFullname(), paymentBillList.get(0).getId());
        YmsLock ymsLock = null;
        try{
            //和线下支付并发问题，添加pk锁，FIBillController中会解锁
            ymsLock = JedisLockUtils.lockBillWithOutTrace(currentBill.getId().toString());
            PayStatus payStatus = currentBill.getPaystatus();
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101135"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180835","单据【") /* "单据【" */ + currentBill.getCode() +
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180839","】已锁定，请勿操作") /* "】已锁定，请勿操作" */);
            }
            Date currentPubts = payBill.getPubts();
            if (currentPubts != null && currentPubts.compareTo(currentBill.getPubts()) != 0) {//Pubts改变，出现多线程问题
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100704"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418083B","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
            }
            if (payStatus != null && payStatus != PayStatus.NoPay && payStatus != PayStatus.Fail && payStatus != PayStatus.PreFail) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101135"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180835","单据【") /* "单据【" */ + currentBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180834","】支付状态不能进行取消审批！") /* "】支付状态不能进行取消审批！" */);
            }
            AuditStatus auditStatus = currentBill.getAuditstatus();
            if (auditStatus != null && auditStatus.getValue() == AuditStatus.Incomplete.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101135"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180835","单据【") /* "单据【" */ + currentBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180837","】未审批，不能进行取消审批！") /* "】未审批，不能进行取消审批！" */);
            }
            EventSource eventSource = currentBill.getSrcitem();
            if (eventSource != null && eventSource != EventSource.Cmpchase) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101135"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180835","单据【") /* "单据【" */ + currentBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180838","】不是现金自制单据，不能进行取消审批！") /* "】不是现金自制单据，不能进行取消审批！" */);
            }
            //勾对完成后不能取消审批
            if (journalService.checkJournal(currentBill.getId())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101135"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180835","单据【") /* "单据【" */ + currentBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418083A","】已勾对，不能取消审批！") /* "】已勾对，不能取消审批！" */);
            }
            //关联回单后不能取消审批
            Boolean matchJournal = journalService.matchJournal(currentBill.getId());
            if (matchJournal) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101136"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180836","单据已经关联匹配银行交易回单，不能取消审批") /* "单据已经关联匹配银行交易回单，不能取消审批" */);
            }
            //清空审批信息
            payBill.setEntityStatus(EntityStatus.Update);
            payBill.setAuditstatus(AuditStatus.Incomplete);
            payBill.setAuditorId(null);
            payBill.setAuditor(null);
            payBill.setAuditDate(null);
            payBill.setAuditTime(null);
            currentBill.setAuditstatus(AuditStatus.Incomplete);
            currentBill.setAuditor(null);
            currentBill.setAuditorId(null);
            currentBill.setAuditDate(null);
            currentBill.setAuditTime(null);
            journalService.updateJournal(currentBill);
            // 当付款单拉取的是付款申请时，单据状态改为已审核。
            paymentServiceUtil.auditPayBillPullPayApplyBillUpdatePayBillStatus(paymentBillList, "unaudit");
        }catch (Exception e){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101137"),e.getMessage());
        }finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return new RuleExecuteResult();
    }

}
