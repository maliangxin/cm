package com.yonyoucloud.fi.cmp.foreignpayment.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import lombok.RequiredArgsConstructor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 外汇付款 撤回规则
 * *
 *
 * @author xuxbo
 * @date 2023/8/3 15:15
 */

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ForeignPaymentUnSubmitRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            ForeignPayment foreignPayment = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, bizObject.getId(), null);
            short verifystate = Short.parseShort(bizObject.get(ICmpConstant.VERIFY_STATE).toString());
            if (verifystate == VerifyState.INIT_NEW_OPEN.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101385"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418038F", "单据未提交，不能进行撤回！") /* "单据未提交，不能进行撤回！" */);
            }
            if (verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101386"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180390", "单据已终止流程，不能进行撤回！") /* "单据已终止流程，不能进行撤回！" */);
            }
            short voucherstatus = foreignPayment.getVoucherstatus();
            if (voucherstatus == VoucherStatus.POSTING.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101387"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418038E", "过账中的单据，不能进行撤回！") /* "过账中的单据，不能进行撤回！" */);
            }
            if (!foreignPayment.getIsWfControlled()) {
                // 未启动审批流，单据直接弃审通过
                result = BillBiz.executeRule(ICmpConstant.UN_AUDIT, billContext, paramMap);
                result.setCancel(true);
            }
        }
        return result;
    }
}
