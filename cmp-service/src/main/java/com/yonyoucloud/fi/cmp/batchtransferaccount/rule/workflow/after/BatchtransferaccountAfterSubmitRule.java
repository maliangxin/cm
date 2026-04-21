package com.yonyoucloud.fi.cmp.batchtransferaccount.rule.workflow.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * @author xuxbo
 * @date 2025/6/5 16:12
 */
// @Component
public class BatchtransferaccountAfterSubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        return new RuleExecuteResult();
    }
}
