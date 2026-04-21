package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.impl;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.IBankDealDetailBusiOper;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
/**
 * @Author guoyangy
 * @Date 2024/7/24 11:21
 * @Description todo
 * @Version 1.0
 */
@Slf4j
public class YQLDealDetailConsumer extends DefaultDealDetailConsumer{
    @Resource
    private IBankDealDetailBusiOper bankDealDetailBusiOper;
    @Override
    public Map<String, List<BankReconciliation>> executeDedulication(List<BankReconciliation> bankReconciliations) {
        Map<String, List<BankReconciliation>> resultMap = bankDealDetailBusiOper.executeDedulication(bankReconciliations);
        log.error("【业务去重】步骤二:从流水业务库查询流水数据做业务去重,去重完成");
        return resultMap;
    }
}