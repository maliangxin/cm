package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.impl;
import com.google.common.collect.ImmutableMap;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.model.DealDetailConsumerDTO;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.impl.ManualDealDetailConsumer;
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
 * @Description todo
 * @Version 1.0
 */
@Service
@Slf4j
public class ManualDealDetailAccessImpl<T> extends DefaultDealDetailAccessImpl<T>{
    @Resource
    private ManualDealDetailConsumer manualDealDetailConsumer;
    @Autowired
    private BaseRefRpcService baseRefRpcService;
    @Override
    public BankDealDetailModel getBankDealDetailModelList(T o) {
        BankDealDetailModel bankDealDetailModel = null;
        if(o instanceof BankDealDetailModel){
            bankDealDetailModel = (BankDealDetailModel) o;
        }
        if(null==bankDealDetailModel || CollectionUtils.isEmpty(bankDealDetailModel.getStreamResponseRecordList())){
            log.info("【流水手工处理】流水集合为空，处理结束");
            return null;
        }
        log.info("【流水手工处理】本批次处理流水总量{}",bankDealDetailModel.getStreamResponseRecordList().size());
        return bankDealDetailModel;
    }
    @Override
    public String contentSign(String bankReconciliationId) {
        return bankReconciliationId==null?UUID.randomUUID().toString():bankReconciliationId;
    }
    /**
     *流水分类，根据流水是否在ods存在分成两类
     * 1.根据流水主键id批量查询ods,按照流水在ods是否存在分成两类
     * 2.获取第一步在ods中不存在的流水List<BankReconliciation>并转换成List<BankDealDetailODSModel>
     * 3.为每笔流水赋值ods主键，后续使用
     * */
    @Override
    public Map<String, List<BankDealDetailODSModel>> checkRepeatFromODS(BankDealDetailModel bankDealDetailModel){
        //step1:参数非空校验
        if(null==bankDealDetailModel || CollectionUtils.isEmpty(bankDealDetailModel.getStreamResponseRecordList())){
            log.error("ods去重逻辑实体为空，去重结束");
            return null;
        }
        List<BankReconciliation> bankReconciliationList = bankDealDetailModel.getStreamResponseRecordList();
        List<BankReconciliation> bankReconciliationNotexistsInOdsList = new ArrayList<>();
        List<BankDealDetailODSModel> bankDealDetailODSModelList = new ArrayList<>();
        //step2:筛出ods中不存在流水，不存在流水insert到ods
        this.checkRepeatFromODS(bankReconciliationList,bankReconciliationNotexistsInOdsList,bankDealDetailODSModelList);
        //step3:流水批量转BankDealDetailODSModel
        List<BankDealDetailODSModel> dealDetailODSModels = this.convertBankReconciliationToOdsModel(bankReconciliationNotexistsInOdsList,bankDealDetailModel.getRequestSeqNo());
        //step4: 为每笔流水附上ods主键id，后面执行辨识匹配规则回写ods用
        this.setOdsIdToBankReconciliation(dealDetailODSModels,bankReconciliationList);
        //step5:组装反参
        return this.collectResult(dealDetailODSModels,bankDealDetailODSModelList);
    }
    /**
     * 流水转ods
     * */
    protected  List<BankDealDetailODSModel> convertBankReconciliationToOdsModel(List<BankReconciliation> notExistsInOds,String requestSeqNo) {
        if(!CollectionUtils.isEmpty(notExistsInOds)){
            YmsOidGenerator ymsOidGenerator = AppContext.getBean(YmsOidGenerator.class);
            List<BankDealDetailODSModel> bankDealDetailODSModelList = new ArrayList<>();
            for(BankReconciliation bankReconciliation:notExistsInOds){
                BankDealDetailODSModel odsModel = DealDetailUtils.convertBankReconciliationToOdsModel(bankReconciliation,DealDetailEnumConst.AccessChannelEnum.MANUAL.getKey(),this.contentSign(bankReconciliation.getId()+""),requestSeqNo,ymsOidGenerator,baseRefRpcService);
                bankDealDetailODSModelList.add(odsModel);
            }
            return bankDealDetailODSModelList;
        }
        return null;
    }
    protected Map<String, List<BankDealDetailODSModel>> collectResult(List<BankDealDetailODSModel> dealDetailODSModels, List<BankDealDetailODSModel> bankDealDetailODSModelList) {
        if(CollectionUtils.isEmpty(dealDetailODSModels) &&!CollectionUtils.isEmpty(bankDealDetailODSModelList)){
            return ImmutableMap.of(BankDealDetailConst.REPEATSTREAM,bankDealDetailODSModelList);
        }
        if(!CollectionUtils.isEmpty(dealDetailODSModels) &&CollectionUtils.isEmpty(bankDealDetailODSModelList)){
            return ImmutableMap.of(BankDealDetailConst.NORMALSTREAM,dealDetailODSModels);
        }
        return ImmutableMap.of(BankDealDetailConst.NORMALSTREAM,dealDetailODSModels,BankDealDetailConst.REPEATSTREAM,bankDealDetailODSModelList);
    }

    protected void setOdsIdToBankReconciliation(List<BankDealDetailODSModel> dealDetailODSModels, List<BankReconciliation> bankReconciliationList) {
        for(BankReconciliation b : bankReconciliationList){
            if(b.get(DealDetailEnumConst.ODSID)!=null){
                continue;
            }
            Long bankReconciliationId = b.getId() instanceof String ?Long.parseLong(b.getId()):b.getId();
            for(BankDealDetailODSModel odsModel : dealDetailODSModels){
                String odsId = odsModel.getId();
                Long mainId = odsModel.getMainid();
                if(mainId.equals(bankReconciliationId)){
                    b.put(DealDetailEnumConst.ODSID,odsId);
                    break;
                }
            }
        }
    }
    protected void checkRepeatFromODS(List<BankReconciliation> bankReconciliationList, List<BankReconciliation> bankReconciliationNotexistsInOdsList, List<BankDealDetailODSModel> bankDealDetailODSModelList) {
        List<Long> bankReconciliationIds = new ArrayList<>();
        for(BankReconciliation bankReconciliation: bankReconciliationList){
            bankReconciliationIds.add(bankReconciliation.getId());
        }
        //根据查询到的银行流水id查询ODS表的数据
        bankDealDetailODSModelList.addAll(bankDealDetailAccessDao.batchQueryODSByMainid(bankReconciliationIds));
        int checkCount = 0;
        if (CollectionUtils.isEmpty(bankDealDetailODSModelList)) {
            bankReconciliationNotexistsInOdsList.addAll(bankReconciliationList);
            checkCount = bankReconciliationNotexistsInOdsList.size();
        } else {
            //把ODS表中的流水id取出来，比如银行流水10个查出来ODS小于10个比如是5个，那么不存在ODS的也就5个，若ODS表中有重复，则会造成不存在ODS表的大于5个
            List<Long> mainIdsInODS = bankDealDetailODSModelList.stream().map(BankDealDetailODSModel::getMainid).collect(Collectors.toList());
            List<BankDealDetailODSModel> finalBankDealDetailODSModelList = bankDealDetailODSModelList;
            bankReconciliationNotexistsInOdsList.addAll(bankReconciliationList.stream().filter(bankReconciliation -> {
                if (mainIdsInODS.contains(bankReconciliation.getId())) {
                    //流水已存在ods中,将ods主键id赋值给流水
                    bankReconciliation.put(DealDetailEnumConst.ODSID, DealDetailUtils.getOsdIdByReConciliationId(bankReconciliation.getId(), finalBankDealDetailODSModelList));
                    return false;
                }
                return true;
            }).collect(Collectors.toList()));
            checkCount = mainIdsInODS.size()+bankReconciliationNotexistsInOdsList.size();
        }
        if(checkCount != bankReconciliationList.size()){
            log.error("【手工流水处理】按照是否在ods存在分类后流水数量不一致，原始流水数{},分类后流水{}",bankReconciliationList.size(),checkCount);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540076F", "【手工流水处理】按照是否在ods存在分类后流水数量不一致,即物理去重的ODS表中存在多条数据对应一个银行流水表中的id") /* "【手工流水处理】按照是否在ods存在分类后流水数量不一致,即物理去重的ODS表中存在多条数据对应一个银行流水表中的id" */);
        }
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
            log.info("【流水手工处理】通知消费者消费失败，实体为空");
            return;
        }
        log.info("【流水手工处理】流水状态修改为开始辨识匹配");
        try{
            List<BankReconciliation> bankReconciliationList = bankDealDetailModel.getStreamResponseRecordList();
            long start = System.currentTimeMillis();
            if(DealDetailUtils.isOpenMultiInstanceAccess()){
                log.error("已开启多实例消费，不直接从流水接入组件调流水消费组件啦。");
               return;
            }
            DealDetailConsumerDTO eventBusModel = new DealDetailConsumerDTO(DealDetailUtils.getTraceId(), bankDealDetailModel.getRequestSeqNo(),bankReconciliationList);
            manualDealDetailConsumer.syncBankDealDetailConsumer(eventBusModel);
            log.info("【手工流水处理】调用流水消费端,共耗时{}ms,流水总量{},ids:{}",(System.currentTimeMillis()-start),bankReconciliationList.size(),bankReconciliationList.stream().map(BankReconciliation::getId).collect(Collectors.toList()).toString());
        }catch (Exception e){
            log.error("【手工流水处理】调用流水消费端异常",e);
        }
    }
}
