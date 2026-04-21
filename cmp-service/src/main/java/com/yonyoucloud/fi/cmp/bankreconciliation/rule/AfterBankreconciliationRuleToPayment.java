package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <h1>去掉左树的ID值</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-30 16:41
 */
@Component
public class AfterBankreconciliationRuleToPayment extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<Map<String, Object>> dimensionKeysList = (List) paramMap.get("dimensionKeys");
        if (dimensionKeysList.size() < 2) {
            return new RuleExecuteResult(paramMap);
        }
        Iterator<Map<String, Object>> iterator = dimensionKeysList.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> map = iterator.next();
            String name = (String) map.get("name");
            map.put("name", name.substring(name.indexOf("+")+1));
        }
        return new RuleExecuteResult(paramMap);
    }
}
