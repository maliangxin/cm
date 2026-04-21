package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.classify;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.BankIdentifyTypeEnum;

import java.util.List;
import java.util.Map;

public interface IBankDealDetailClassifyService {

    /**
     * 根据单据状态分类银行对账单
     *
     * @param bankReconciliationList
     * @return
     */
    Map<String, List<BankReconciliation>> classifyList(List<BankReconciliation> bankReconciliationList , BankIdentifyTypeEnum bankIdentifyType);

}
