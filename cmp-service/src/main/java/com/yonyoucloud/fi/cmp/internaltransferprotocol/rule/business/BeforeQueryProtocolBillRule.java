package com.yonyoucloud.fi.cmp.internaltransferprotocol.rule.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <h1>内转协议查询前置规则</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-05-18 7:44
 */
@Component("beforeQueryProtocolBillRule")
public class BeforeQueryProtocolBillRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = new FilterVO();
        if (billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
        }

        SimpleFilterVO filterAndAuth = new SimpleFilterVO(ConditionOperator.and);
        filterAndAuth.addCondition(new SimpleFilterVO("isParent", ICmpConstant.QUERY_EQ, 1));
        filterVO.appendCondition(ConditionOperator.and, filterAndAuth);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
