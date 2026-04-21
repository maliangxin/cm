package com.yonyoucloud.fi.cmp.marginworkbench.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.utils.MddBaseUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.MarginFlag;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * 支付保证金参照过滤
 */
@Component("marginworkbenchReferRule")
public class MarginworkbenchReferRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {

        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        List<BizObject> bills = getBills(billContext, paramMap);
        BizObject bill = null;
        if (bills.size() > 0) {
            bill = bills.get(0);
        }

        if(!"cmp_paymarginworkbenchrefer".equals(billDataDto.getrefCode()) && !"cmp_recmarginworkbenchrefer".equals(billDataDto.getrefCode())){
            return new RuleExecuteResult();
        }
        FilterVO filterVO = new FilterVO();
        if (billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
        }
        String accentity = null;
        if (ObjectUtils.isNotEmpty(bill.get(IBussinessConstant.ACCENTITY))) {
            accentity = bill.get(IBussinessConstant.ACCENTITY).toString();
        }
        if ("cmp_paymarginworkbenchrefer".equals(billDataDto.getrefCode())) {
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "marginFlag", ICmpConstant.QUERY_EQ, MarginFlag.PayMargin.getValue()));
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "accentity", ICmpConstant.QUERY_EQ, accentity));
            if (ObjectUtils.isNotEmpty(bill.get(IBussinessConstant.CURRENCY))) {
                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "currency", ICmpConstant.QUERY_EQ, bill.get(IBussinessConstant.CURRENCY).toString()));
            }
//            else {
//                //取本币币种
//                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "currency", ICmpConstant.QUERY_EQ, bill.get(IBussinessConstant.NATCURRENCY).toString()));
//            }
        }else if ("cmp_recmarginworkbenchrefer".equals(billDataDto.getrefCode())) {
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "marginFlag", ICmpConstant.QUERY_EQ, MarginFlag.RecMargin.getValue()));
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "accentity", ICmpConstant.QUERY_EQ, accentity));
            if (ObjectUtils.isNotEmpty(bill.get(IBussinessConstant.CURRENCY))) {
                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "currency", ICmpConstant.QUERY_EQ, bill.get(IBussinessConstant.CURRENCY).toString()));
            }
//            else {
//                //取本币币种
//                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "currency", ICmpConstant.QUERY_EQ, bill.get(IBussinessConstant.NATCURRENCY).toString()));
//            }
        }
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
