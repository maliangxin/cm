package com.yonyoucloud.fi.cmp.bankreconciliationrepeat.dao;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;

import java.util.List;

public interface BankReconciliationRepeatDAO {

    Integer selectBankReconciliationRepeatCount(String ytenantId);

    List<BankReconciliation> selectBankReconciliationRepeatData(String ytenantId);

    void updateBankReconciliationRepeat(List<BankReconciliation> ids, String ytenantId);

    /**
     * 查询当前租户疑似重复的数据,然后将这部分数据相同疑重要素的正常数据查询出来
     * @param repeatFactors
     * @param ytenantId
     * @return
     */
    List<BankReconciliation> selectRepeatDataWithNormal(String repeatFactors,String ytenantId);
}
