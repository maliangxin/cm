package com.yonyoucloud.fi.cmp.journalbill.rule.submit;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JournalBillSubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            JournalBill journalBill = (JournalBill) bizObject;
            short verifystate = Short.parseShort(bizObject.get(ICmpConstant.VERIFY_STATE).toString());
            if (verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105017"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21A7E1DE05380003", "单据不是状态为初始开立/驳回到制单的单据，不允许提交") /* "单据不是状态为初始开立/驳回到制单的单据，不允许提交" */);
            }
            if (verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100758"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00140", "单据已终止流程，不能进行提交！") /* "单据已终止流程，不能进行提交！" */);
            }
            //未开启审批流则直接审核通过
            if (null == journalBill.getIsWfControlled() || !journalBill.getIsWfControlled()) {
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule(ICmpConstant.AUDIT, billContext, paramMap);
                result.setCancel(true);
            }
        }
        return result;
    }
}
