package com.yonyoucloud.fi.cmp.bankbillcheck.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author: liaojbo
 * @Date: 2025年04月28日 17:55
 * @Description:日期查询条件处理，转为区间查询
 */
@Slf4j
@Component("bankBillCheckBeforeQueryRule")
public class BankBillCheckBeforeQueryRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = billDataDto.getCondition();
        String beginDateStr = null;
        String endDatestr = null;
        if (filterVO != null) {
            FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
            if (commonVOs != null && commonVOs.length > 0) {
                for (FilterCommonVO vo : commonVOs) {
                    String beginDateItemName = "beginDate";
                    String endDateItemName = "endDate";
                    if("billDate".equals(vo.getItemName())){
                        if(vo.getValue1()!=null){
                            beginDateStr = vo.getValue1().toString();
                        }
                        if(vo.getValue2()!=null){
                            endDatestr = vo.getValue2().toString();
                        }
                        if(beginDateStr != null && endDatestr == null){
                            filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO(beginDateItemName, "between", beginDateStr, null));
                        } else if (beginDateStr == null && endDatestr != null) {
                            filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO(endDateItemName, "between", null, endDatestr));
                        } else if (beginDateStr != null && endDatestr != null) {
                            filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO(beginDateItemName, "between", beginDateStr, endDatestr));
                            filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO(endDateItemName, "between", beginDateStr, endDatestr));
                        }
                    }
                }
            }
        }
        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
