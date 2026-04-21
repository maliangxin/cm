package com.yonyoucloud.fi.cmp.fundexpense.rule.audit;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.ExpenseAuditStatus;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("beforeFundexpenseAuditBillRule")
public class BeforeFundexpenseAuditBillRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            //获取前端传过来的值对象
            Fundexpense fundexpense = (Fundexpense) bills.get(0);
            fundexpense.setAuditstatus(ExpenseAuditStatus.passed.getValue());
        }
        return new RuleExecuteResult();
    }
}
