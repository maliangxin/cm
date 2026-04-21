package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <h1>资金收付款单保存并提交</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-05-10 15:19
 */
@Component("fundBillSaveAndSubmitRule")
public class FundBillSaveAndSubmitRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        RuleExecuteResult result = new RuleExecuteResult();
        if("saveandsubmit".equals(billContext.getAction())){
            // 保存并提交
            result = BillBiz.executeRule("submit", billContext, paramMap);
            result.setCancel(true);
        }
        return result;
    }
}
