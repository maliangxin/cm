package com.yonyoucloud.fi.cmp.bankreconciliation.rule.pushbill;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * zxl
 * 删除银行账单-更新账户历史余额
 */
@Slf4j
@Component
public class BankreconciliationAfterPushBillRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<YmsLock> lockList = (List<YmsLock>) paramMap.get("lockList");
        if (lockList != null && lockList.size() > 0) {
            // 释放锁
            for(YmsLock ymsLock:lockList){
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
        return new RuleExecuteResult();
    }
}