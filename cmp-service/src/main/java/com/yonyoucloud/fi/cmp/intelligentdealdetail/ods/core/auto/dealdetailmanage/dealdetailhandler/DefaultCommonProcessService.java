package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;

import java.util.List;

public interface DefaultCommonProcessService {

    public void intelligentFlowIdentification(List<BankReconciliation> BankReconciliations,List<String> codes);
}
