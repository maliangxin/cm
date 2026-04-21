package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 付款单复制规则
 *
 * @author lidchn
 * @version V1.0
 * @date 2021年5月26日11:24:16
 * @Copyright yonyou
 */
@Slf4j
@Component
public class PaymentCopyRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103022"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C258FE04A00007", "在财务新架构环境下，不允许复制付款单。") /* "在财务新架构环境下，不允许复制付款单。" */);
        }
        List<BizObject> bills = getBills(billContext, paramMap);
        log.info("===========进入付款工作台整单复制规则类PaymentCopyRule中===========");
        for (BizObject detail : bills) {
            //需清空字段与需重新赋值字段
            detail.set("auditor", null);//审批人
            detail.set("auditTime", null);//审批时间
            detail.set("auditDate", null);//审批日期
            detail.set("auditstatus", "2");//审批状态，设置为未审核（2）
            detail.set("settleuser", null);//结算人
            detail.set("settledate", null);//结算时间
            detail.set("settlestatus", "1");//结算状态,未结算（1）
            detail.set("voucherstatus", VoucherStatus.Empty);
            PayBill payBill = (PayBill)detail;
            payBill.setTranseqno(null);
        }
        return new RuleExecuteResult();
    }

}
