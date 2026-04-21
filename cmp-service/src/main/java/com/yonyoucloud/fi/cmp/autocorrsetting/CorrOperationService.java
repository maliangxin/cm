package com.yonyoucloud.fi.cmp.autocorrsetting;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;

import java.util.List;
import java.util.Map;

/**
 * 关联
 */
public interface CorrOperationService {

    /**
     * 关联操作
     * @param corrData
     */
    BankReconciliationbusrelation_b corrOpration(CorrDataEntity corrData,int ordernum) throws Exception;

    void runCorrTask(CorrDataEntity corrDataEntity,int ordernum) throws Exception;
    /**
     * 确认关联Service
     *
     * @param corrIds @return
     * @throws Exception
     * @return
     */
    public Map<String,Object> confirmCorrOpration(List corrIds, List dcFlags);

    public Map<String,Object> confirmCorrOprationUseException(List corrIds, List dcFlags) throws Exception;

    /**
     * 拒绝关联Servie
     *
     * @param corrIds@return
     * @throws Exception
     */
    public Map<String,Object> refuseCorrOpration(List corrIds);
    /**
     * 回写银行流水认领关联数据
     * 同名账户划转: 银行转账单,缴存现金单（收款流水）,提取现金单（付款流水）,第三方转账
     * @param corrData
     * @throws Exception
     */
    void reWriteTransferAccCorrelationOperation(CorrDataEntity corrData) throws Exception ;

    int confirmUseException(Long corrId, Long dcFlag) throws Exception;

    /**
     * 根据结算明细单条进行确认
     * @param corrId
     * @param dcFlag
     * @param isBatch
     * @return
     * @throws Exception
     */
    int confirmUseExeception(Long corrId, Long dcFlag,boolean isBatch) throws Exception;

    /**
     * 批量确认异步进度条
     * @param paramMap
     */
    CtmJSONObject progess(CtmJSONObject paramMap);
    void asyncConfirmCorrOpration(List corrIds, List dcFlags,String uid);
    void asyncRefuseCorrOpration(List corrIds,String uid);
}
