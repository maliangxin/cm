package com.yonyoucloud.fi.cmp.budget;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;


import java.util.List;
import java.util.Map;

public interface CmpBudgetBatchTransferAccountManagerService {

    /**
     * 查询符合规则设置的数据
     *
     * @param budgetEventBill
     * @return
     * @throws Exception
     */
    CtmJSONArray queryBillByRule(CmpBudgetEventBill budgetEventBill) throws Exception;

    /**
     * 更新列表预算状态
     * @param batchTransferAccountBs
     * @param isOccupyBudget
     * @throws Exception
     */
    void updateBillList(List<BatchTransferAccount_b> batchTransferAccountBs, Short isOccupyBudget) throws Exception;


    /**
     * 获取预算数据
     * @param billNum
     */
    Map[] getBudgetBills(String billNum, Long id) throws Exception;

    String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception;

    /**
     * 提交占用预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget submitOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception;

    /**
     * 撤回释放预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget unsubmitOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception;

    /**
     * 结算止付释放执行
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget stopSettleOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception;

    /**
     * 审核占用预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget auditOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception;

    /**
     * 审批撤回释放预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget unauditOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception;

    /**
     * 结算成功占用预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget settleSuccessOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception;

    /**
     * 取消结算释放预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget cancelSettleOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception;

    /**
     * 保存占用预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget saveOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception;

    /**
     * 删除释放预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget deleteOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception;
}
