package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ClaimCompleteType;
import com.yonyoucloud.fi.cmp.cmpentity.EntryType;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author guoxh
 * 银行流水处理在新增/编辑数据后如果数据的疑重标识状态为非0时,会导致查询不到数据,所以此处增加一个规则进行处理,在查询条件中增加isrepeat
 * com.yonyoucloud.fi.cmp.modifyschema.CmpQuerySchemaExecutorPlugin
 */
@Component("bankreconciliationBeforeDetailRule")
@Slf4j
public class BankreconciliationBeforeDetailRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) paramMap.get("param");
        String id = billDataDto.getId();
        RuleExecuteResult ruleResult = new RuleExecuteResult();
        Object p = this.getParam(paramMap);
        BillDataDto billDto = null;
        if (p instanceof BillDataDto) {
            billDto = (BillDataDto) this.getParam(paramMap);
            id = billDto.getId();
        } else {
            billDto = new BillDataDto();
            billDto.setId(p.toString());
        }

        if (null == id) {
            return ruleResult;
        } else {
            FilterVO filterVO = ValueUtils.isNotEmptyObj(billDto.getCondition()) ? billDto.getCondition() : new FilterVO();
            QueryConditionGroup group = filterVO.getQueryConditionGroup() == null ? new QueryConditionGroup() : filterVO.getQueryConditionGroup();

            QueryConditionGroup repeatGroup = new QueryConditionGroup(ConditionOperator.or);
            repeatGroup.addCondition(QueryCondition.name("isrepeat").is_null());
            repeatGroup.addCondition(QueryCondition.name("isrepeat").is_not_null());

            group.appendCondition(repeatGroup);
            filterVO.setQueryConditionGroup(group);
            billDto.setCondition(filterVO);
            return ruleResult;
        }
    }
}
