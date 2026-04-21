package com.yonyoucloud.fi.cmp.initdata.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
public class PreNextMoveRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        // 执行dao
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = null;
        if (billDataDto.getCondition() == null) {
            filterVO = new FilterVO();
        } else {
            // 若原先有过滤条件，则要在原来的基础上追加条件
            filterVO = billDataDto.getCondition();
        }
        //单据类型判断
        String billNum = billDataDto.getBillnum();
        switch (billNum) {
            case "cmp_initdatayh":
                filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "moneyform", "eq", "1");
                filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "qzbz", "eq", "1");
                break;
            case "cmp_initdataxj":
                filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "moneyform", "eq", "0");
                filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "qzbz", "eq", "1");
                break;
            default:
                break;
        }
        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
