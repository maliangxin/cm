package com.yonyoucloud.fi.cmp.bankcapitalvirtual;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.virtualFlowRuleConfig.VirtualFlowRuleConfig;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @description: 页面，删除前规则
 * @author:
 * @date:
 */

@Slf4j
@Component
public class VirtualFlowRuleConfigBatchDeleteRule extends AbstractCommonRule {


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (int i = 0; i < bills.size(); i++) {
            VirtualFlowRuleConfig contextData = (VirtualFlowRuleConfig) bills.get(i);
            Long id = contextData.get("id");
            if (id != null) {
                deleteVirtualFlowRuleConfig(id);
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 删除规则
     *
     * @param id
     */
    private void deleteVirtualFlowRuleConfig(Long id) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        querySchema.addCondition(group);
        List<VirtualFlowRuleConfig> virtualflowrules = MetaDaoHelper.queryObject(VirtualFlowRuleConfig.ENTITY_NAME, querySchema, null);
        MetaDaoHelper.delete(VirtualFlowRuleConfig.ENTITY_NAME, virtualflowrules);
    }
}
