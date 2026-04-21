package com.yonyoucloud.fi.cmp.initdata.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author lidchn@yonyou.com
 */
@Component
public class InitDataBatchOutputRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size()>0) {
            BizObject bizObject = bills.get(0);
            BigDecimal bankInitoriBalance = bizObject.get("bankinitoribalance");
            BigDecimal coinitloriBalance = bizObject.get("coinitloribalance");
            BigDecimal coinitLocalBalance = bizObject.get("coinitloribalance");
            bizObject.put("bankinitoribalance", bankInitoriBalance.abs());
            bizObject.put("coinitloribalance",coinitloriBalance.abs());
            bizObject.put("coinitlocalbalance",coinitLocalBalance.abs());
        }
        return new RuleExecuteResult();
    }

}
