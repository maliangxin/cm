package com.yonyoucloud.fi.cmp.paymentbill.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 付款单撤回规则（提交的逆向操作）
 * 审批流相关
 *
 * @author lidchn
 * 2021年8月3日20:38:05
 */
@Component
public class PaymentUnSubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103026"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2599C04A00002", "在财务新架构环境下，不允许撤回付款单。") /* "在财务新架构环境下，不允许撤回付款单。" */);        }
        List<BizObject> paymentBillList = getBills(billContext, paramMap);
        if (paymentBillList == null || paymentBillList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101242"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180232","请选择单据！") /* "请选择单据！" */);
        }
        for (BizObject bizObject : paymentBillList) {
            Date currentPubts = bizObject.getPubts();
            PayBill currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizObject.getId());
            short verifystate = Short.parseShort(bizObject.get("verifystate").toString());
            if (verifystate == VerifyState.INIT_NEW_OPEN.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101243"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180234","单据未提交，不能进行撤回！") /* "单据未提交，不能进行撤回！" */);
            }
            if (verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101244"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180235","单据已终止流程，不能进行撤回！") /* "单据已终止流程，不能进行撤回！" */);
            }
            if (currentBill.getAuditstatus().getValue() == AuditStatus.Complete.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101245"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180236","单据已审批") /* "单据已审批" */);
            }
//            if (currentPubts != null) {
//                if (currentPubts.compareTo(currentBill.getPubts()) != 0) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101246"),MessageUtils.getMessage("P_YS_FI_AR_0000059097") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
//                }
//            }
        }
        return new RuleExecuteResult();
    }

}
