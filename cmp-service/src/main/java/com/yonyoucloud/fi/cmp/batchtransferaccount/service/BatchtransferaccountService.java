package com.yonyoucloud.fi.cmp.batchtransferaccount.service;


import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.vo.EventAsyncResultVO;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDetail;
import com.yonyoucloud.ctm.stwb.unifiedsettle.event.SettleDetailFinishEvent;
import com.yonyoucloud.ctm.stwb.unifiedsettle.vo.UnifiedSettleDetail;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;



/**
 * @author xuxbo
 * @date 2025/6/9 19:16
 */

public interface BatchtransferaccountService {


    /**
     * 更新结算信息
     * @param businessEvent 业务事件
     * @param unifiedSettleDetail 结算申请明细
     * @param settleDetailFinishEvent 事件实体
     * @throws Exception
     */
    EventAsyncResultVO updateSettledInfoOfBatchtransferaccount(BusinessEvent businessEvent, UnifiedSettleDetail unifiedSettleDetail, SettleDetailFinishEvent settleDetailFinishEvent) throws Exception;

    void generateVoucher(BatchTransferAccount billb, String entityName) throws Exception;

    void generateVoucher(String businessBillId) throws Exception;

    void pushPaymentSettleFirst(BatchTransferAccount batchTransferAccount,Boolean isCashBusiness) throws Exception;

    void pushPaymentSettleSecond(BatchTransferAccount batchTransferAccount,String bizbilldetailid, Boolean isCashBusiness) throws Exception;

    /**
     * 撤回
     * @param batchTransferAccountId 主表id
     * @throws Exception
     */
    void revokeBatchTransferAccount(String batchTransferAccountId) throws Exception;


    void updateSettledInfo(DataSettledDetail dataSettledDetail) throws Exception;

    CtmJSONObject checkAddTransType(CtmJSONObject params) throws Exception;


}
