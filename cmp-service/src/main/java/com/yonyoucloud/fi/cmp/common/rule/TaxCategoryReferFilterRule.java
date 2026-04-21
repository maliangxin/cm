package com.yonyoucloud.fi.cmp.common.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <h1>资金收付款单税种过滤</h1>
 *
 * 只参照增值税税种的税率档案 (税种档案字段：taxCategoryArchiveId=10000)
 *
 * @author duanhj12
 * @version 1.0
 * @since 2023-06-08 9:30
 */
@Component
public class TaxCategoryReferFilterRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        return new RuleExecuteResult();
    }
}
