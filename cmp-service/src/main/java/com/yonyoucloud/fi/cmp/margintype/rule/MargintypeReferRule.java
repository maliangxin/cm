package com.yonyoucloud.fi.cmp.margintype.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.utils.MddBaseUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * 支付保证金参照过滤
 */
@Component("margintypeReferRule")
public class MargintypeReferRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        if ("cmp_margintyperefer".equals(billDataDto.getrefCode())) {
            FilterVO filterVO = new FilterVO();
            if (billDataDto.getCondition() != null) {
                filterVO = billDataDto.getCondition();
            }
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "isEnabledType", ICmpConstant.QUERY_EQ, true));
            putParam(paramMap, billDataDto);
        }
        return new RuleExecuteResult();
    }
}
