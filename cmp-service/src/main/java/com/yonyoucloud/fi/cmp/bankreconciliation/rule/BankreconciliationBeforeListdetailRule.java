package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 银行对账单listdetail查询逻辑
 */
@Component("bankreconciliationBeforeListdetailRule")
@Slf4j
public class BankreconciliationBeforeListdetailRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = ValueUtils.isNotEmptyObj(billDataDto.getCondition()) ? billDataDto.getCondition() : new FilterVO();

        if(IBillNumConstant.BANKRECONCILIATION.equals(billDataDto.getBillnum()) && "export".equalsIgnoreCase(billDataDto.getRequestAction())){
            //导出逻辑
            if(billDataDto.getExternalData() != null) {
                Map<String, Object> extendData = (Map<String, Object>) billDataDto.getExternalData();
                if (extendData.containsKey("use4Export") && "true".equalsIgnoreCase(extendData.get("use4Export").toString())) {
                    QueryConditionGroup repeatGroupFather = new QueryConditionGroup(ConditionOperator.and);
                    QueryConditionGroup repeatGroup = new QueryConditionGroup(ConditionOperator.or);
                    repeatGroup.addCondition(QueryCondition.name("isrepeat").is_not_null());
                    repeatGroup.addCondition(QueryCondition.name("isrepeat").is_null());
                    repeatGroupFather.addCondition(repeatGroup);
                    if(filterVO.getQueryConditionGroup() == null) {
                        filterVO.setQueryConditionGroup(repeatGroupFather);
                    }else{
                        filterVO.getQueryConditionGroup().appendCondition(repeatGroupFather);
                    }
                    billDataDto.setCondition(filterVO);
                }
            }
        }
        return new RuleExecuteResult();
    }
}
