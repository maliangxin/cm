package com.yonyoucloud.fi.cmp.bankreconciliation.rule.pushbill;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
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
public class BankreconciliationBeforePushBillRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        List<YmsLock> lockList = new ArrayList<>();
        for (BizObject bill : bills) {
            BankReconciliation originBankReconciliation = new BankReconciliation();
            originBankReconciliation.init(bill);
            //加锁
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(originBankReconciliation.getId().toString());
            if (ymsLock == null) {
                throw new CtmException(new com.yonyou.yonbip.ctm.error.CtmErrorCode(""), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "银行交易流水号:" + originBankReconciliation.getBank_seq_no() + "获取锁失败，请稍后重试!"));
            } else {
                lockList.add(ymsLock);
            }
        }
        paramMap.put("lockList", lockList);
        return new RuleExecuteResult();
    }
}