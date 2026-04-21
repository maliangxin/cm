package com.yonyoucloud.fi.cmp.fundexpense.rule.audit;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.cmpentity.ExpenseAuditStatus;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

@Component("beforeDischargeUnAuditBillRule")
public class BeforeFundexpenseUnAuditBillRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            //获取前端传过来的值对象
            Fundexpense fundexpense = (Fundexpense) bills.get(0);
            Short auditstatus = fundexpense.getAuditstatus();
            if(auditstatus.equals(ExpenseAuditStatus.passed.getValue())){
                if(!BooleanUtils.b(fundexpense.getIsWfControlled())){
                    fundexpense.setAuditstatus(ExpenseAuditStatus.unsubmit.getValue());
                }else{
                    fundexpense.setAuditstatus(ExpenseAuditStatus.approval.getValue());
                }
            }else{
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101131"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800F5", "当前单据{0}，单据状态不允许撤回审批") /* "当前单据{0}，单据状态不允许撤回审批" */, fundexpense.getCode()));
            }
        }
        return new RuleExecuteResult();
    }
}
