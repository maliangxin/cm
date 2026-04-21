package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 *
 */
@Slf4j
@Component("beforeQueryFundBillRule")
public class BeforeQueryFundBillRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {

        String queryCurrentLoggedInPersonData = AppContext.getEnvConfig("query_current_logged_in_person_data", "false");
        if ("false".equals(queryCurrentLoggedInPersonData)){
            return new RuleExecuteResult();
        }
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);

        if (IServicecodeConstant.FUNDCOLLECTION.equals(billDataDto.getParameter("serviceCode"))
                || IServicecodeConstant.FUNDPAYMENT.equals(billDataDto.getParameter("serviceCode"))){
            return new RuleExecuteResult();
        }
        FilterVO filterVO = null;
        if (billDataDto.getCondition() == null) {
            filterVO = new FilterVO();
        } else {
            // 若原先有过滤条件，则要在原来的基础上追加条件
            filterVO = billDataDto.getCondition();
        }
        filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "creatorId", ICmpConstant.QUERY_EQ, AppContext.getUserId());
        billDataDto.setCondition(filterVO);
        return new RuleExecuteResult();
    }
}
