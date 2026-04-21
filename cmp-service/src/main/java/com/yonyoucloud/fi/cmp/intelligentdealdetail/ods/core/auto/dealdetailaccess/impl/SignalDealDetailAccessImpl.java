package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.impl;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.impl.SignalDealDetailConsumer;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.model.DealDetailConsumerDTO;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author guoyangy
 * @Date 2024/7/23 16:19
 * @Description 单笔流水接入
 * @Version 1.0
 */
@Service
@Slf4j
public class SignalDealDetailAccessImpl<T> extends ManualDealDetailAccessImpl<T>{
    @Resource
    private SignalDealDetailConsumer signalDealDetailConsumer;
    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Override
    public void processDealDetailTOODS(BankDealDetailModel streamModel, Map<String, List<BankDealDetailODSModel>> resultMap) {
        this.processDealDetailTOODS(streamModel,resultMap,null);
    }

    /**
     * 通知消费组件流水消费
     * 1.因为是手工流水处理，流水表中已存在流水，先将流水状态改为开始辨识匹配
     * 2.手工处理的这批流水可能存在多个traceId,按照traceId_requestSeqNo格式统计List<traceId>
     * 3.调用消费组件DefaultDealDetailConsumer#bankDealDetailConsumer(List,EventBusModel)方法进行流水消费
     * */
    @Override
    public void notifyConsumer(BankDealDetailModel bankDealDetailModel,Map<String, List<BankDealDetailODSModel>> mapByRepeatStatus){
        //step1:非空判断
        if(null == bankDealDetailModel){
            log.info("【流水单笔导入】通知消费者消费失败，实体为空");
            return;
        }
        log.info("【流水单笔导入】流水状态修改为开始辨识匹配");
        try{
            List<BankReconciliation> bankReconciliationList = bankDealDetailModel.getStreamResponseRecordList();
            long start = System.currentTimeMillis();
            DealDetailConsumerDTO eventBusModel = new DealDetailConsumerDTO(DealDetailUtils.getTraceId(), bankDealDetailModel.getRequestSeqNo(),bankReconciliationList);
            signalDealDetailConsumer.bankDealDetailConsumer(eventBusModel);
            log.info("【流水单笔导入】调用流水消费端,共耗时{}ms,流水总量{},ids:{}",(System.currentTimeMillis()-start),bankReconciliationList.size(),bankReconciliationList.stream().map(BankReconciliation::getId).collect(Collectors.toList()).toString());
        }catch (Exception e){
            log.error("【流水单笔导入】调用流水消费端异常",e);
           // DealDetailUtils.recordException(DealDetailUtils.getTraceId(),bankDealDetailModel.getRequestSeqNo(),e);
            throw new BankDealDetailException(-1,e.getMessage(),e);
        }
    }

    @Override
    protected List<BankDealDetailODSModel> convertBankReconciliationToOdsModel(List<BankReconciliation> notExistsInOds, String requestSeqNo) {
        if(!CollectionUtils.isEmpty(notExistsInOds)){
            YmsOidGenerator ymsOidGenerator = AppContext.getBean(YmsOidGenerator.class);
            List<BankDealDetailODSModel> bankDealDetailODSModelList = new ArrayList<>();
            for(BankReconciliation bankReconciliation:notExistsInOds){
                BankDealDetailODSModel odsModel = DealDetailUtils.convertBankReconciliationToOdsModel(bankReconciliation,DealDetailEnumConst.AccessChannelEnum.SIGNAL.getKey(),this.contentSign(bankReconciliation.getId()+""),requestSeqNo,ymsOidGenerator,baseRefRpcService);
                bankDealDetailODSModelList.add(odsModel);
            }
            return bankDealDetailODSModelList;
        }
        return null;
    }
}