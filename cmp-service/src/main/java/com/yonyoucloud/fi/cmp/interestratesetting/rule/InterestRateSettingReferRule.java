package com.yonyoucloud.fi.cmp.interestratesetting.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 预提，参照前规则 - 10
 */
@Slf4j
@Component("interestRateSettingReferRule")
public class InterestRateSettingReferRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);

        if ("ucfbasedoc.bd_currencytenantref".equals(billDataDto.getrefCode())) {
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.ENABLE, ICmpConstant.QUERY_EQ, "1"));
                billDataDto.setCondition(conditon);
            } else {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.ENABLE, ICmpConstant.QUERY_EQ, "1"));
            }
        }
        return new RuleExecuteResult();
    }
}
