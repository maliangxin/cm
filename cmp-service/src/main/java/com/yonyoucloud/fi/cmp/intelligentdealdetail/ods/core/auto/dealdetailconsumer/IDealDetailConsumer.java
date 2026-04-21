package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.model.DealDetailConsumerDTO;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.ODSLifeCycle;
import java.util.List;
import java.util.Map;

public interface IDealDetailConsumer extends ODSLifeCycle {
    /**
     * 基于eventBusModel获取流水实体集合
     * @param eventBusModel
     * @return List<BankReconciliation>
     * */
    List<BankReconciliation> getBankReconciliations(DealDetailConsumerDTO eventBusModel);
    /**
     * 流水业务出重
     * @param bankReconciliations
     * @return Map <p>
     *  KEY: com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst.Deduplication_KEYEnum
     * */
    Map<String, List<BankReconciliation>> executeDedulication(List<BankReconciliation> bankReconciliations);
    /**
     * BankDealDetail入库
     * */
    default void batchConvertAndSaveBankDealDetail(List<BankReconciliation> bankReconciliationList){}
    /**
     * 缓存中读取BankDealDetailODSModel
     * */
    default List<BankDealDetailODSModel> getBankDealDetailODSModelListFromCacheOrDB(DealDetailConsumerDTO eventBusModel){return null;}
}