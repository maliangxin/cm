package com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationActionEnum;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationScheduleEnum;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Service;

import java.util.List;

// ... existing code ...

/**
 * 银行对账数据校验和过滤策略上下文类
 * 通过委托模式将实际的业务逻辑交给 CheckAndFilterStrategy 策略接口处理
 *
 * @deprecated 该类已废弃，不建议在新代码中使用，请查找替代的新实现
 */
@Service
   /**
    * @deprecated
    */
  @Deprecated
public class BankreconciliationContext {
    private CheckAndFilterStrategy checkAndFilterStrategy;

    public BankreconciliationContext(CheckAndFilterStrategy checkAndFilterStrategy) {
        this.checkAndFilterStrategy = checkAndFilterStrategy;
    }

    public void checkDataLegalList(List<BizObject> bills, BankreconciliationActionEnum actionEnum) {
        checkAndFilterStrategy.checkDataLegalList(bills, actionEnum);
    }

    public String checkDataLegal(BankReconciliation bill, BankreconciliationActionEnum actionEnum) {
        return checkAndFilterStrategy.checkDataLegal(bill, actionEnum);
    }

    public void checkAndFilterData(List<BankReconciliation> bills, BankreconciliationScheduleEnum scheduleTaskCodeEnum) {
        checkAndFilterStrategy.checkAndFilterData(bills, scheduleTaskCodeEnum);
    }
}

