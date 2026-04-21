package com.yonyoucloud.fi.cmp.paymentbill.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 付款单提交规则
 * 审批流相关
 *
 * @author lidchn
 * 2021-8-3 20:37:49
 */
@Component
public class PaymentSubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103025"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2597C04E00008", "在财务新架构环境下，不允许提交付款单。") /* "在财务新架构环境下，不允许提交付款单。" */);
        }
        List<BizObject> paymentBillList = getBills(billContext, paramMap);
        if (CollectionUtils.isEmpty(paymentBillList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101618"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180486","请选择单据！") /* "请选择单据！" */);
        }
        for (BizObject bizObject : paymentBillList) {
            PayBill currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizObject.getId());
            Date currentPubts = bizObject.getPubts();
            if (currentBill.getAuditstatus().getValue() == AuditStatus.Complete.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101619"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180487","单据已审批") /* "单据已审批" */);
            }
            if (currentPubts != null && currentPubts.compareTo(currentBill.getPubts()) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101620"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180488","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
            }
            short verifyState = ValueUtils.isNotEmptyObj(bizObject.get("verifystate")) ? Short.parseShort(bizObject.get("verifystate").toString()) : (short) -1;
            if (VerifyState.TERMINATED.getValue() == verifyState) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101621"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180485","流程已终止，不允许提交单据！") /* "流程已终止，不允许提交单据！" */);
            }
        }
        return new RuleExecuteResult();
    }

}
