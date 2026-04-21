package com.yonyoucloud.fi.cmp.intelligentdealdetail.repetition.service;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
/**
 * @author maliangn
 * @since 2024-06-22
 */
public interface IRepetitionService {
    /**
     * 格式化银行对账单的concat_info
     * @param bankReconciliation
     * @return
     */
    String formatConctaInfoBankReconciliation(BankReconciliation bankReconciliation);
}