package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.impl;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.ODSBasicInfoObject;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.IDealDetailAccess;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.impl.DefaultDealDetailConsumer;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
/**
 * @Author guoyangy
 * @Date 2024/7/23 16:15
 * @Description 流水接入默认实现类
 * @Version 1.0
 */
@Slf4j
@Service
public abstract class DefaultDealDetailAccessImpl<T> extends ODSBasicInfoObject implements IDealDetailAccess<T> {
    @Resource
    protected IBankDealDetailAccessDao bankDealDetailAccessDao;
    @Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = RuntimeException.class)
    public void dealDetailAccess(T param) {
        /**
         * step1:生成流水日志模型
         * */
        BankDealDetailModel bankDealDetailModel = this.getBankDealDetailModelList(param);
        if(null == bankDealDetailModel){
            log.error("【流水接入】流水解析完成,解析结果为空,traceId={},流程结束", DealDetailUtils.getTraceId());
            return;
        }
        /**
         * step2:执行ods去重逻辑
         * */
        Map<String, List<BankDealDetailODSModel>> mapByRepeatStatus = this.checkRepeatFromODS(bankDealDetailModel);
        /**
         * step3:流水入库
         * */
        this.processDealDetailTOODS(bankDealDetailModel,mapByRepeatStatus);
        /**
         * step4:发送通知 - 通知消费端 DefaultDealDetailConsumer#asyncBankDealDetailConsumer
         * */
        if(mapByRepeatStatus != null){
            if(TransactionSynchronizationManager.isActualTransactionActive()){
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyConsumer(bankDealDetailModel,mapByRepeatStatus);
                    }
                });
            }else{
                this.notifyConsumer(bankDealDetailModel,mapByRepeatStatus);
            }
        }
    }
    @Override
    public void processDealDetailTOODS(BankDealDetailModel streamModel,Map<String, List<BankDealDetailODSModel>> resultMap) {
        try{
            long start = System.currentTimeMillis();
            //step1:流水解析失败，没有获取流水反参信息
            if(null == resultMap || resultMap.size()==0){
                log.error("【流水接入入库】4.未获取流水内容,请求参数、响应参数准备入库，流水处理流程结束");
                bankDealDetailAccessDao.dealDetailInsert(streamModel.getDetailOperLogModel(),null,null);
                log.error("【流水接入入库】5.流水入库日志库成功,耗时{}ms",(System.currentTimeMillis()-start));
                return;
            }
            //step2:ods验重后发现没有重复流水，全部入ods库
            List<BankDealDetailODSModel> bankDealDetailODSFailModelList = resultMap.get(BankDealDetailConst.REPEATSTREAM);
            if(CollectionUtils.isEmpty(bankDealDetailODSFailModelList)){
                log.error("【流水接入入库】4.流水验重后不存在异常流水准备全部入ODS库");
                this.bankDealDetailAccessDao.dealDetailInsert(streamModel.getDetailOperLogModel(),resultMap.get(BankDealDetailConst.NORMALSTREAM),null);
                log.error("【流水接入入库】6.流水入ods库成功,耗时{}ms",(System.currentTimeMillis()-start));
                return;
            }
            //step3:ods验重后发现有重复流水，部分入ods库、部分入ods_fail库
            this.processDealDetailTOODS(streamModel,resultMap,bankDealDetailODSFailModelList);
        }catch (Exception e){
            log.error("【流水接入】4.流水入库异常",e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400394", "流水入库异常") /* "流水入库异常" */+e.getMessage());
        }
    }
    /**
     * 银企联流水接入实现该方法
     * */
    protected void processDealDetailTOODS(BankDealDetailModel streamModel, Map<String, List<BankDealDetailODSModel>> resultMap, List<BankDealDetailODSModel> bankDealDetailODSFailModelList) {
        List<BankDealDetailODSModel> updateODS = resultMap.get(BankDealDetailConst.REPEATSTREAM);
        List<BankDealDetailODSModel> insertODS = resultMap.get(BankDealDetailConst.NORMALSTREAM);
        this.bankDealDetailAccessDao.dealDetailInsertAndUpdate(insertODS,updateODS, DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_NO.getProcessstatus(),DealDetailUtils.getTraceId(),streamModel.getRequestSeqNo());
        log.error("【流水接入入库】新增及更新ods成功");
        return;
    }
}
