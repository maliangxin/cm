package com.yonyoucloud.fi.cmp.salarypay.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 薪资支付工作台列表查询过滤规则
 * 规则表中先不注册，测试说先不修改添加该过滤，规则不注册
 * @author majfd
 *
 */
@Component
public class SalaryPayListQueryRule extends AbstractCommonRule{

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		// TODO Auto-generated method stub
		BillDataDto billDataDto = (BillDataDto)getParam(paramMap);
		
        FilterVO filterVO = new FilterVO();
        if(billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
        }
        if(!billDataDto.isSum()) {
        	//当表体+明细时，显示支付不明和支付失败的
        	Short[] status = new Short[] {3, 4}; 
//        	filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "Salarypay_b.tradestatus", "in", status);
        	SimpleFilterVO paymentStatus = new SimpleFilterVO("Salarypay_b.tradestatus", "in", status);
        	filterVO.appendCondition(ConditionOperator.and, paymentStatus);
        }

        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
	}

}
