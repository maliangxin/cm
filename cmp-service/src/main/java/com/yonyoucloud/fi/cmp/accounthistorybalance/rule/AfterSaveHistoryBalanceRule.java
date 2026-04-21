package com.yonyoucloud.fi.cmp.accounthistorybalance.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountHistoryBalanceServiceImpl;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 账户历史余额保存后规则
 */
@Slf4j
@Component("afterSaveHistoryBalanceRule")
public class AfterSaveHistoryBalanceRule extends AbstractCommonRule {
    @Autowired
    private AccountHistoryBalanceServiceImpl accountHistoryBalanceServiceImpl;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        // 释放redis锁
//        List<BizObject> bills = getBills(billContext, paramMap);
//        for (BizObject bizObject : bills) {
//            String accentity = bizObject.get(IBussinessConstant.ACCENTITY);
//            String enterpriseBankAccount = bizObject.get("enterpriseBankAccount");
//            String currency = bizObject.get("currency");
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//            Date balancedate =  bizObject.get("balancedate");
//            String balancedateStr = sdf.format(balancedate);
//            String lockStr = "historyBalanceLock_" + accentity + enterpriseBankAccount + currency + balancedateStr;
//            JedisLockUtils.unlockBillWithOutTrace(lockStr);
//            //释放锁
//            if(bizObject.get("id") != null){
//                String lockkey = ICmpConstant.MY_BILL_CLAIM_LIST + bizObject.get("id");
//                JedisLockUtils.unlockBillWithOutTrace(lockkey);
//            }
//        }
        JedisLockUtils.unlockRuleWithOutTrace(paramMap);
        return new RuleExecuteResult();
    }

}
