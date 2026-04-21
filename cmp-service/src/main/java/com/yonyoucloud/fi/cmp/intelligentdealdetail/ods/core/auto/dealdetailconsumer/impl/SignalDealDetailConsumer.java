package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.impl;

import com.google.common.collect.ImmutableMap;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.IBankDealDetailBusiOper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.model.DealDetailConsumerDTO;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.SignalBankDealDetailMatchChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Arrays;
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
public class SignalDealDetailConsumer extends DefaultDealDetailConsumer{
    @Resource
    private IBankDealDetailBusiOper bankDealDetailBusiOper;
    @Resource
    private IBankDealDetailAccessDao bankDealDetailAccessDao;
    @Override
    public List<BankReconciliation> getBankReconciliations(DealDetailConsumerDTO eventBusModel) {
        return eventBusModel.getBankReconciliationList();
    }
    /**
     * 业务去重
     * */
    @Override
    public Map<String, List<BankReconciliation>> executeDedulication(List<BankReconciliation> bankReconciliations) {
        BankReconciliation bankReconciliation = bankReconciliations.get(0);
        if(bankReconciliation.getEntityStatus()!=null && EntityStatus.Update.name().equals(bankReconciliation.getEntityStatus().name())){
            //更新
            return ImmutableMap.of(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_UPDATE.getKey(), Arrays.asList(bankReconciliation));
        }
        //新增
        Map<String, List<BankReconciliation>> resultMap = bankDealDetailBusiOper.executeDedulication(bankReconciliations);
        if(CollectionUtils.isEmpty(resultMap)){
            log.error("【业务去重】步骤二:业务去重后待处理流水为空，流程结束");
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540078B", "业务去重后待处理流水为空，流程结束") /* "业务去重后待处理流水为空，流程结束" */);
        }
        List<BankReconciliation> addBankReconciliationList = resultMap.get(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_ADD.getKey());
        if(CollectionUtils.isEmpty(addBankReconciliationList)){
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540078C", "重复数据，新增/导入失败！") /* "重复数据，新增/导入失败！" */);
        }
        log.error("【业务去重】步骤二:从流水业务库查询流水数据做业务去重,去重完成");
        return resultMap;
    }

    @Override
    public void callBankDetailManageAccess(List<BankDealDetailWrapper> bankDealDetailProcessList, List<BankReconciliation> updateBankReconciliationList,String traceid,String requestSeqNo) {
        long start = System.currentTimeMillis();
        bankDealDetailManageAccess.bankDealDetailManageAccess(bankDealDetailProcessList,updateBankReconciliationList, SignalBankDealDetailMatchChainImpl.get(),traceid,requestSeqNo);
        log.error("【消费端调流水管理器】流水管理器处理完成耗时{}ms",System.currentTimeMillis()-start);
    }
    @Override
    public List<BankDealDetailODSModel> getBankDealDetailODSModelListFromCacheOrDB(DealDetailConsumerDTO eventBusModel) {
        return null;
    }

    @Override
    public void batchConvertAndSaveBankDealDetail(List<BankReconciliation> bankReconciliationList) {
        if(!CollectionUtils.isEmpty(bankReconciliationList)){
            bankDealDetailAccessDao.batchConvertAndSaveBankDealDetail(bankReconciliationList);
            log.error("【业务去重】步骤四:BankReconciliation转BankDealDetail后流水已提前入库");
        }
    }
}
