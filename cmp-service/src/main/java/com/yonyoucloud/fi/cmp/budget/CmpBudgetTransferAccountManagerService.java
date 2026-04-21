package com.yonyoucloud.fi.cmp.budget;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;

import java.util.List;

public interface CmpBudgetTransferAccountManagerService {

    /**
     * 查询符合规则设置的数据
     *
     * @param budgetEventBill
     * @return
     * @throws Exception
     */
    CtmJSONArray queryBillByRule(CmpBudgetEventBill budgetEventBill) throws Exception;

    void updateBillList(List<TransferAccount> transferAccounts, Short isOccupyBudget) throws Exception;

    boolean budget(TransferAccount transferAccount) throws Exception;

    boolean releaseBudget(TransferAccount transferAccount) throws Exception;

    /**
     * 带判断的实占
     *
     * @param transferAccount
     * @return
     * @throws Exception
     */
    boolean implement(TransferAccount transferAccount) throws Exception;

    /**
     * 直接实占
     *
     * @param transferAccount
     * @return
     * @throws Exception
     */
    boolean implementOnly(TransferAccount transferAccount) throws Exception;

    /**
     * 释放实占
     *
     * @param transferAccount
     * @return
     * @throws Exception
     */
    boolean releaseImplement(TransferAccount transferAccount) throws Exception;

    void updateOccupyBudget(TransferAccount transferAccount, Short isOccupyBudget) throws Exception;


}
