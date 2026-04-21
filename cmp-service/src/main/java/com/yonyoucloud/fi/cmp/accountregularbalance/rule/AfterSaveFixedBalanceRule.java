package com.yonyoucloud.fi.cmp.accountregularbalance.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.accountfixedbalance.AccountFixedBalance;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("afterSaveFixedBalanceRule")
public class AfterSaveFixedBalanceRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<AccountFixedBalance> bills = getBills(billContext, paramMap);
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        // 导入
        boolean importFlag =  "import".equals(billDataDto.getRequestAction());

        if (bills != null && bills.size()>0) {
            //AccountFixedBalance balance = bills.get(0);
            if (importFlag) {
//                Date balanceData = balance.getBalancedate();
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
//                String enterpriseBankAccount = balance.getEnterpriseBankAccount();
//                String currency = balance.getCurrency();
//                String balanceDateStr = sdf.format(balanceData);
//                String lockStr = "fixedBalanceLock_" + balance.getAccentity() + enterpriseBankAccount + currency + balanceDateStr;
//                JedisLockUtils.unlockBillWithOutTrace(lockStr);
                JedisLockUtils.unlockRuleWithOutTrace(paramMap);
            }
        }
        return new RuleExecuteResult();
    }

}
