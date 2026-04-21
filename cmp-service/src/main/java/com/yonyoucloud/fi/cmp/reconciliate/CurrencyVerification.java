package com.yonyoucloud.fi.cmp.reconciliate;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class CurrencyVerification extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto bill = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = bill.getCondition();
        if(null == filterVO){
             filterVO = new FilterVO();
        }
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(bill.getrefCode())){
			if (bill.getData()!=null) {
                List<Map<String,Object>> list = (List<Map<String,Object>>) bill.getData();
                if(list!=null && list.size()>0){
                    String currency= (String) list.get(0).get(ICmpConstant.CURRENCY);
                    if(!StringUtils.isEmpty(currency)){
                        UiMetaDaoHelper.appendCondition(filterVO, ICmpConstant.CURRENCY_REf, ICmpConstant.QUERY_IN, currency);
                    }
                }
            }
		}
        bill.setCondition(filterVO);
        putParam(paramMap, bill);
        return new RuleExecuteResult();
    }
}
