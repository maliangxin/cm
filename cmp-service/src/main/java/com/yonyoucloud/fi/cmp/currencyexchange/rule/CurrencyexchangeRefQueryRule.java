package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component("currencyexchangeRefQueryRule")
public class CurrencyexchangeRefQueryRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto)getParam(map);

        FilterVO filterVO = new FilterVO();
        if(billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
        }

        SimpleFilterVO settlemode = new SimpleFilterVO("settlemode.serviceAttr", "eq", '0');
        filterVO.appendCondition(ConditionOperator.and, settlemode);

        billDataDto.setCondition(filterVO);
        putParam(map, billDataDto);
        return new RuleExecuteResult();
    }
}
