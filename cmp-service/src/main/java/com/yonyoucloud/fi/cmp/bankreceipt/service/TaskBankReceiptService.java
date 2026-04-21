package com.yonyoucloud.fi.cmp.bankreceipt.service;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadResult;

import java.util.List;
import java.util.Map;

public interface TaskBankReceiptService {

    public ThreadResult bankReceiptAsyncProcess(CtmJSONObject paramNew, Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup) throws Exception;
    //public void bankReceiptAsyncProcess(CtmJSONObject paramNew) throws Exception;

    /**
     * 流水匹配关联上的回单，仅供智能流水使用，不会实际更新库中的数据，只修改入参和出参
     *
     * @param bankReconciliationList
     * @throws Exception
     */
    Map<BankReconciliation, BankElectronicReceipt> matchBankReconciliationAndBankreceipt(List<BankReconciliation> bankReconciliationList) throws Exception;

    /**
     * 批量更新银行对账单和回单关联关系
     * @param updateBankElectronicReceipts
     * @param sendRelateEventMap
     * @param bankReconciliationIds
     * @throws Exception
     */
    void updateBankreceiptAssociationstatus(List<BankElectronicReceipt> updateBankElectronicReceipts, Map<Long, BankElectronicReceipt> sendRelateEventMap, Map<Long, BankElectronicReceipt> reconciliationIdAndBankElectronicReceiptMap, List<String> bankReconciliationIds,List<BankReconciliation> bankReconciliationList) throws Exception;

    /**
     * 匹配关联上的银行对账单
     * @param callableList
     * @throws Exception
     */
    void matchBankreceiptAndBankReconciliation(List<Map<String, Object>> callableList) throws Exception;
    /**
     * *银行交易回单文件下载调度任务
     * @param params
     * @return
     * @throws Exception
     */
    ThreadResult bankTradeDetailElectronTask(CtmJSONObject params ) throws Exception;

    Map<String, List<EnterpriseBankAcctVO>> bankReceipt(CtmJSONObject param) throws Exception;
}
