package com.yonyoucloud.fi.cmp.bankreconciliation;

/**
 * @author qihaoc
 * @Description: 自动确认
 * @date 2024/1/27 22:23
 */
public interface BankAutoConfirmBillService {

    public void autoConfirm(BankReconciliation bankReconciliation) throws Exception;
}
