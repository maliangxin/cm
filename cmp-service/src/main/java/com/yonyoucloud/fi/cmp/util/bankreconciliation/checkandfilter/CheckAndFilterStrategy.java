package com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter;

import com.yonyou.ucf.mdd.plugin.base.MddPlugin;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationActionEnum;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationScheduleEnum;
import org.imeta.orm.base.BizObject;

import java.util.List;

/*
  银行流水处理相关操作策略类
 */
public interface CheckAndFilterStrategy extends MddPlugin {
    void checkDataLegalList(List<BizObject> bills, BankreconciliationActionEnum actionEnum);

    String checkDataLegal(BankReconciliation bill, BankreconciliationActionEnum actionEnum);

    void checkAndFilterData(List<BankReconciliation> bills, BankreconciliationScheduleEnum scheduleTaskCodeEnum);
}
