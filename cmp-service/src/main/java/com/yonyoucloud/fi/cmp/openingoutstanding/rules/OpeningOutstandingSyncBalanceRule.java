package com.yonyoucloud.fi.cmp.openingoutstanding.rules;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.openingoutstanding.OpeningOutstandingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @description: 期初未达项，期初余额同步规则类 action=syncOpeningBalance
 * @author: wanxbo@yonyou.com
 * @date: 2025/8/25 13:43
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class OpeningOutstandingSyncBalanceRule extends AbstractCommonRule {

    @Autowired
    private OpeningOutstandingService openingOutstandingService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size()>0) {
            for (BizObject bizobject : bills){
                openingOutstandingService.syncOpeningBalance(bizobject);
            }
        }
        return new RuleExecuteResult();
    }
}
