package com.yonyoucloud.fi.cmp.fundexpense.rule.query;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("beforeFundExpenseQueryRule")
public class BeforeFundExpenseQueryRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        String billnum = billContext.getBillnum();
        FilterVO filterVO = new FilterVO();
        if (billDataDto.getCondition() != null) {
            // 若原先有过滤条件，则要在原来的基础上追加条件
            filterVO = billDataDto.getCondition();
        }
        if("Initialfundexpenselist".equals(billnum)){
            filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "isinit", "eq", 1);
        } else if("fundexpenselist".equals(billnum)){
            filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "isinit", "eq", 0);
        }

        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
