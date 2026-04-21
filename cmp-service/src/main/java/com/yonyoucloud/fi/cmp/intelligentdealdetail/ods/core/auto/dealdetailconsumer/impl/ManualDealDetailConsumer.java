package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.impl;

import com.google.common.collect.ImmutableMap;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.model.DealDetailConsumerDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
/**
 * @Author guoyangy
 * @Date 2024/7/24 11:21
 * @Description todo
 * @Version 1.0
 */
@Service
@Slf4j
public class ManualDealDetailConsumer extends DefaultDealDetailConsumer{
    @Override
    public List<BankReconciliation> getBankReconciliations(DealDetailConsumerDTO eventBusModel) {
        return eventBusModel.getBankReconciliationList();
    }
    @Override
    public Map<String, List<BankReconciliation>> executeDedulication(List<BankReconciliation> bankReconciliations) {
        if(CollectionUtils.isEmpty(bankReconciliations)){
            return null;
        }
        return ImmutableMap.of(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_UPDATE.getKey(),bankReconciliations);
    }

    @Override
    public List<BankDealDetailODSModel> getBankDealDetailODSModelListFromCacheOrDB(DealDetailConsumerDTO eventBusModel) {
        return null;
    }
}