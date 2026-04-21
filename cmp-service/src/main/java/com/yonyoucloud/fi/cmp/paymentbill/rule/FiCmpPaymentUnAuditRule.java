package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 付款单远程弃审规则
 * @author liuttm
 * @version V1.0
 * @date 2021/4/20 16:00
 * @Copyright yonyou
 */
@Slf4j
@Component
public class FiCmpPaymentUnAuditRule extends FiCmpPaymentBaseRule implements ISagaRule {

    @Autowired
    PaymentService paymentService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103017"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2583004E00003", "在财务新架构环境下，不允许弃审付款单。") /* "在财务新架构环境下，不允许弃审付款单。" */);
        }
        log.error("executing  FiCmpPaymentUnAuditRule , request paramMap = {}", JsonUtils.toJson(paramMap));
        List<PayBill> payBillList =  this.getPaymentFromRequest(billContext,paramMap);
        // 应收单据在现金不存在，直接结束流程。
        if(payBillList.size() == 0){
            return new RuleExecuteResult();
        }
        Long payBillId = payBillList.get(0).getId();
        PayStatus payStatus = payBillList.get(0).getPaystatus();
        if (payStatus != null && payStatus != PayStatus.NoPay && payStatus != PayStatus.PreFail && payStatus != PayStatus.Fail) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102009"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418011D","单据【") /* "单据【" */ + payBillList.get(0).get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418011C","】支付状态不能弃审") /* "】支付状态不能弃审" */);
        }
        YmsLock ymsLock = null;
        try {
            ymsLock = JedisLockUtils.lockBillWithOutTrace(payBillId.toString());
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102010"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418011B","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
            }
            unaudit(payBillList.get(0));
            YtsContext.setYtsContext("payBillId",payBillId);
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }catch (Exception e){
            log.error(e.getMessage());
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102011"),e.getMessage());
        }
        return new RuleExecuteResult();
    }

    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("FiCmpPaymentUnAuditRule cancel, request paramMap = {}", new Object[]{JsonUtils.toJson(paramMap)});
        // 取消审核失败，调用审核流程重置单据状态
        if(YtsContext.getYtsContext("payBillId") == null){
            return new RuleExecuteResult();
        }
        Long payBillId =  (Long) YtsContext.getYtsContext("payBillId");
        PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, payBillId);
        if(payBill != null){
            audit(payBill);
        }
        return new RuleExecuteResult();
    }

}
