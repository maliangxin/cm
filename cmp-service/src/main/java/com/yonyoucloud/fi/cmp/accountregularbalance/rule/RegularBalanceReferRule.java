package com.yonyoucloud.fi.cmp.accountregularbalance.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 定期余额，参照前规则 - 10
 */
@Component("regularBalanceReferRule")
public class RegularBalanceReferRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
//        BillDataDto billDataDto = (BillDataDto) getParam(map);
//        BizObject bizObject = null;
//        List<BizObject> bills = getBills(billContext, map);
//        if (bills.size() > 0) {
//            bizObject = bills.get(0);
//        }
//        if ("bd_enterprisebankacctref".equals(billDataDto.getrefCode()) && bizObject != null) {
//            //会计主体
//            String accentity = bills.get(0).get("accentity");
//            if (accentity != null) {
//                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", ICmpConstant.QUERY_EQ, accentity));
//            }
//            //币种
//            String currency = bills.get(0).get("currency");
//            if (currency != null) {
//                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_EQ, currency));
//            }
//        }
        return new RuleExecuteResult();
    }
}
