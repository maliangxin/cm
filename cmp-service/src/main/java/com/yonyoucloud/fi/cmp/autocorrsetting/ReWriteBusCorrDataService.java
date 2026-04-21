package com.yonyoucloud.fi.cmp.autocorrsetting;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;

import java.util.List;

public interface ReWriteBusCorrDataService {

    /**
     * 回写银行对账单数据
     * @param corrData
     */
    void reWriteBankReconciliationData(CorrDataEntity corrData, boolean autoTask) throws Exception;


    /**
     * 回写付款单数据
     */
    void reWritePayMentData(CorrDataEntity corrData) throws Exception;

    /**
     * 回写收款单数据
     */
    void reWriteFundCollectionData(CorrDataEntity corrData) throws Exception;

    /**
     * 回写转账单数据
     */
    void reWriteTransferAccountData(CorrDataEntity corrData) throws Exception;

    /**
     * 回写银行认领单数据
     * @param id
     * @param associationStatus
     * @param smartcheckno 智能对账勾兑码
     */
    void reWriteBillClaimData(Long id,Short associationStatus,String smartcheckno,Short claimcompletetype) throws Exception;

    /**
     * 资金收付款单关联关系删除
     * @param json
     * @return
     * @throws Exception
     */
    void resDelData(CtmJSONObject json) throws Exception;

    /**
     * 银行对账单业务关联外币兑换单回写
     * @param corrEntity
     * @throws Exception
     */
    void reWriteCurrencyexchangeData(CorrDataEntity corrEntity) throws Exception;

    /**
     * 回写付款单数据
     */
    void reWritePayBillData(CorrDataEntity corrData) throws Exception;

    /**
     * 回写收款单数据
     */
    void reWriteReceiveBillData(CorrDataEntity corrData) throws Exception;
    /**
     * 回写银行对账单关联关系
     * 同名账户划转: 银行转账单,缴存现金单（收款流水）,提取现金单（付款流水）,第三方转账
     * @param corrDataList
     */
    void reWriteBankReconciliationRelationData(List<CorrDataEntity> corrDataList) throws Exception;

    /**
     * 回写银行对账单数据
     * 同名账户划转: 银行转账单,缴存现金单（收款流水）,提取现金单（付款流水）,第三方转账
     * @param corrData
     */
    void reWriteBankReconData(CorrDataEntity corrData) throws Exception;

    /**
     * 回写转账单数据
     * 同名账户划转: 银行转账单,缴存现金单（收款流水）,提取现金单（付款流水）,第三方转账
     * @param corrData
     */
    void reWriteTransferAccountDataInfo(CorrDataEntity corrData) throws Exception;

    BankReconciliation reWriteBankReconciliationStatus(CorrDataEntity corrData) throws Exception;
}
