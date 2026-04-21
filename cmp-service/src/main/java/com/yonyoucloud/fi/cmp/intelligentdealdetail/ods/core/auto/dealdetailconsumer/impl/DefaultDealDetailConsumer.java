package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.impl;

import com.google.common.cache.Cache;
import com.google.common.eventbus.Subscribe;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.converts.BankdealDetailOdsConvertManager;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.ODSBasicInfoObject;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.impl.YQLDealDetailAccessImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.IDealDetailConsumer;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.model.DealDetailConsumerDTO;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.BankDealDetailManageFacade;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.BankDealDetailMatchChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailExceptionCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.EntityStatus;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @Author guoyangy
 * @Date 2024/7/24 11:19
 * @Description 流水消费核心组件类
 * @Version 1.0
 */
@Slf4j
public abstract class DefaultDealDetailConsumer extends ODSBasicInfoObject implements IDealDetailConsumer {
    @Resource
    protected BankDealDetailManageFacade bankDealDetailManageAccess;
    @Resource
    private IBankDealDetailAccessDao bankDealDetailAccessDao;
    @Resource
    protected BankdealDetailOdsConvertManager bankdealDetailOdsConvertManager;
    @Resource
    private CurrencyQueryService currencyQueryService;

    /**
     * 手工流水消费
     */
    public void syncBankDealDetailConsumer(DealDetailConsumerDTO eventBusModel) {
        String traceId = eventBusModel.getTraceId();
        String requestSeqNo = eventBusModel.getRequestSeqNo();
        log.error("【流水消费-手工处理】1.traceId={},requestSeqNo={},队列剩余={},提交线程池消费", traceId, requestSeqNo, odsConsumerArrayBlockingQueue.getOdsConsumerBlockingQueue().remainingCapacity());
        Future future = odsConsumerExecutorService.submit(() -> {
            try {
                bankDealDetailConsumer(eventBusModel);
            } catch (Exception e) {
                log.error("消费端消费流水异常", e);
                throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400766", "消费端消费流水异常") /* "消费端消费流水异常" */ + e.getMessage(), e);
            }
        });

        try {
            future.get();
        } catch (Exception e) {
            log.error("手工流水处理失败", e);
            DealDetailUtils.recordException(traceId, requestSeqNo, e);
            throw new BankDealDetailException(e.getMessage(), e);
        }
    }

    /**
     * 基于事件流水消费
     */
    @Subscribe
    public void asyncBankDealDetailConsumer(DealDetailConsumerDTO eventBusModel) {
        String traceId = eventBusModel.getTraceId();
        String requestSeqNo = eventBusModel.getRequestSeqNo();
        long s0 = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String batNo = UUID.randomUUID().toString().replaceAll("-", "");
        log.error("【流水消费】1.已接受guava事件开始,执行时间={},UUID={},traceId={},requestSeqNo={},队列剩余={},提交线程池消费",now.format(formatter),batNo, traceId, requestSeqNo, odsConsumerArrayBlockingQueue.getOdsConsumerBlockingQueue().remainingCapacity());
        odsConsumerExecutorService.submit(() -> {
            try {
                eventBusModel.setSaveDirect(DealDetailEnumConst.SAVE_DIRECT);
                this.bankDealDetailConsumer(eventBusModel);
                log.error("【流水消费】已接受guava事件结束=======================执行完成,UUID={},traceId={},requestSeqNo={},匹配银行回单共耗时{}s", batNo,traceId, requestSeqNo , (System.currentTimeMillis() - s0) / 1000.0);
            } catch (Exception e) {
                //删除ODS表的数据
                bankDealDetailAccessDao.deleteOdsByTraceIdAndRequestSeqNo(traceId,requestSeqNo);
                log.error("消费端消费流水异常", e);
                DealDetailUtils.recordException(traceId, requestSeqNo, e);
                throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400766", "消费端消费流水异常") /* "消费端消费流水异常" */, e);
            }
        });
    }

    /**
     * 消费端消费流水
     *
     * @param eventBusModel
     * @throws Exception
     */
    public void bankDealDetailConsumer(DealDetailConsumerDTO eventBusModel) throws Exception {
        String traceId = eventBusModel.getTraceId();
        String requestSeqNo = eventBusModel.getRequestSeqNo();
        log.error("【流水消费】2.线程池开始消费,traceId={},requestSeqNo={}", traceId, requestSeqNo);
        String lockey = traceId + "_" + requestSeqNo;
        CtmLockTool.executeInOneServiceLock(lockey, 600L, TimeUnit.SECONDS, (int status) -> {
            //step1:以traceId+requestSeqNo作为分布式锁key,加锁失败
            if (status == LockStatus.GETLOCK_FAIL) {
                log.error("【流水消费】3.消费者加批量锁失败,lockey={}", "__YmsLock_yonbip-fi-ctmcmp:" + InvocationInfoProxy.getTenantid() + ":" + "lock" + ":" + lockey);
                throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400768", "流水正在处理中，请稍后再试") /* "流水正在处理中，请稍后再试" */);
            }
            log.error("【流水消费】3.消费者加批量锁成功,lockey={}", "__YmsLock_yonbip-fi-ctmcmp:" + InvocationInfoProxy.getTenantid() + ":" + "lock" + ":" + lockey);
            //step2:流水消费具体处理逻辑
            this.doBankDealDetailConsumer(eventBusModel);
            // AppContext.getBean(StringUtils.uncapitalize(this.getClass().getSimpleName()), DefaultDealDetailConsumer.class).doBankDealDetailConsumer(eventBusModel);
        });
    }

    /**
     * 流水消费核心逻辑
     */
        private void doBankDealDetailConsumer(DealDetailConsumerDTO eventBusModel) {
        /**
         * setp1:从缓存获取ods流水
         * */
        List<BankReconciliation> bankReconciliationList = this.getBankReconciliations(eventBusModel);
        if (CollectionUtils.isEmpty(bankReconciliationList)) {
            log.error("【流水消费】读取流水为空，已结束");
            return;
        }
        /**
         * step2: ods流水状态改为处理中，防止被调度任务捞起重复处理
         * */
        bankDealDetailAccessDao.batchUpdateODSprocessstatus(bankReconciliationList, DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_START.getProcessstatus());
        log.error("【业务去重】步骤一:将流水状态更新为处理中(ods-> processstatus=2),即将执行业务去重，业务去重前流水数量{}", bankReconciliationList.size());



        /**
         * step3:流水业务去重
         * */
        Map<String, List<BankReconciliation>> resultMap = this.executeDedulication(bankReconciliationList);
        if (CollectionUtils.isEmpty(resultMap)) {
            log.error("【业务去重】步骤二:业务去重后待处理流水为空，流程结束");
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400763", "业务去重后待处理流水为空，流程结束") /* "业务去重后待处理流水为空，流程结束" */);
        }
        resultMap.keySet().forEach(key -> {
            List<BankReconciliation> bankReconciliations = resultMap.get(key);
            /**
             * 判断疑似重复
             */
            batchUpdateIsRepeat(bankReconciliations);
        });


        /**
         * step4:去重后修改ods状态
         * */
        List<BankReconciliation> repeatBankReconciliationList = resultMap.get(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_REPEAT.getKey());
        List<BankReconciliation> addBankReconciliationList = resultMap.get(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_ADD.getKey());
        List<BankReconciliation> updateBankReconciliationList = resultMap.get(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_UPDATE.getKey());
        List<BankReconciliation> rollbackBankReconciliationList = resultMap.get(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_ROLLBACK.getKey());
        // todo 需要新增和修改的数据已经明确，如果后续流程有异常，则需要在catch里将这部分数据处理好出库
        if (!CollectionUtils.isEmpty(repeatBankReconciliationList) && repeatBankReconciliationList.size() == bankReconciliationList.size()) {
            log.error("【业务去重】步骤三:从流水业务库查询流水数据做业务去重,去重完成,该批次流水被全部防重，流水处理结束");
            bankDealDetailAccessDao.batchUpdateODSprocessstatus(repeatBankReconciliationList, DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_NOTHING.getProcessstatus());
            return;
        }
        //更新业务流水表id+ods状态
        bankDealDetailAccessDao.batchUpdateODSprocessstatus(resultMap);
//        bankDealDetailAccessDao.batchUpdateODSprocessstatus(addBankReconciliationList,DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_REPEAT_FINISH_WAIT_MATCH.getProcessstatus());
//        bankDealDetailAccessDao.batchUpdateODSprocessstatus(updateBankReconciliationList,DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_REPEAT_FINISH_WAIT_MATCH.getProcessstatus());
//        bankDealDetailAccessDao.batchUpdateODSprocessstatus(repeatBankReconciliationList,DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_NOTHING.getProcessstatus());
//        bankDealDetailAccessDao.batchUpdateODSprocessstatus(rollbackBankReconciliationList,DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_REPEAT_ERROR.getProcessstatus());

        if (!CollectionUtils.isEmpty(repeatBankReconciliationList)) {
            DealDetailUtils.recordExecuteDedulicationProcessing(eventBusModel.getTraceId(), eventBusModel.getRequestSeqNo(), repeatBankReconciliationList, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400764", "业务去重无需后续处理") /* "业务去重无需后续处理" */);
        }
        if (!CollectionUtils.isEmpty(rollbackBankReconciliationList)) {
            DealDetailUtils.recordExecuteDedulicationProcessing(eventBusModel.getTraceId(), eventBusModel.getRequestSeqNo(), rollbackBankReconciliationList, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400765", "业务去重异常") /* "业务去重异常" */);
        }

        log.error("【业务去重】步骤三:更新ods流水状态已完成，枚举值 1:流水在业务表已存在,2:流水在业务表不存在,3:流水在业务表已存在且更新流水,4:执行流水去重异常");

        /**
         * step5:流水直接入cmp_bankdealdetail库，后续不做处理
         * */
//        if(!CollectionUtils.isEmpty(addBankReconciliationList)){
//            this.batchConvertAndSaveBankDealDetail(addBankReconciliationList);
//            log.error("【业务去重】步骤四:BankReconciliation转BankDealDetail后流水已提前入库");
//        }
        /**
         * step6:封装流水包装类,基于此构建流水处理上下文
         * */
        List<BankDealDetailWrapper> bankDealDetailProcessList = this.getBankDealDetailWrappers(resultMap);
        log.error("【业务去重】步骤五:流水包装类已封装完成，业务去重已完成");
        /**
         * step7:调辨识匹配规则入口
         * */
        this.callBankDetailManageAccess(bankDealDetailProcessList, updateBankReconciliationList, eventBusModel.getTraceId(), eventBusModel.getRequestSeqNo());

        /**
         *todo step8: ods表在这里进行更新
         */

    }

    void batchUpdateIsRepeat(List<BankReconciliation> bankReconciliationList){
        try {
            bankReconciliationList.stream().forEach(e -> {
                //e.setEntityStatus(EntityStatus.Insert);
                //e.setCreateTime(new Date());
                //e.setDataOrigin(DateOrigin.DownFromYQL);
                CommonSaveUtils.setOppositeobjectidToBankReconciliationField(e, e.getOppositetype(), e.getOppositeobjectid() != null ? Long.parseLong(e.getOppositeobjectid()) : null);
                try {
                    if (!e.containsKey("isrepeat") || e.getIsRepeat() == null) {
                        //疑重判断
                        e.setIsRepeat((short) BankDealDetailConst.REPEAT_INIT);
                        Map<String, Object> map = new HashMap<>();
                        map.put("startDate", DateUtils.convertToStr(e.getTran_date(),"yyyy-MM-dd"));
                        CtmCmpCheckRepeatDataService checkRepeatDataService = AppContext.getBean(CtmCmpCheckRepeatDataService.class);
                        checkRepeatDataService.deal4FactorsBankDealDetail(Collections.singletonList(e), map);
                    }
                }catch (Exception ex){
                    log.error("疑重处理失败！",ex);
                    e.setIsRepeat((short) BankDealDetailConst.REPEAT_INIT);
                }
            });
        }catch (Exception e){
            log.error("银行对账单意思重复操作异常",e);
        }
    }

    public void callBankDetailManageAccess(List<BankDealDetailWrapper> bankDealDetailProcessList, List<BankReconciliation> updateBankReconciliationList, String traceid, String requestSeqNo) {
        long start = System.currentTimeMillis();
        bankDealDetailManageAccess.bankDealDetailManageAccess(bankDealDetailProcessList, updateBankReconciliationList, BankDealDetailMatchChainImpl.get().code(null), traceid, requestSeqNo);
        log.error("【消费端调流水管理器】流水管理器处理完成耗时{}ms", System.currentTimeMillis() - start);
    }

    @Override
    public abstract Map<String, List<BankReconciliation>> executeDedulication(List<BankReconciliation> bankReconciliations);

    private List<BankDealDetailWrapper> getBankDealDetailWrappers(Map<String, List<BankReconciliation>> resultMap) {
        List<BankReconciliation> processBankReconciliationList = new ArrayList<>();
        List<BankReconciliation> addBankReconciliationList = resultMap.get(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_ADD.getKey());
        List<BankReconciliation> updateBankReconciliationList = resultMap.get(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_UPDATE.getKey());
        if (!CollectionUtils.isEmpty(addBankReconciliationList)) {
            processBankReconciliationList.addAll(addBankReconciliationList);
        }
        if (!CollectionUtils.isEmpty(updateBankReconciliationList)) {
            processBankReconciliationList.addAll(updateBankReconciliationList);
        }
        //流水转包装类
        List<BankDealDetailWrapper> bankDealDetailProcessList = DealDetailUtils.convertBankReconciliationToWrapper(processBankReconciliationList);
        return bankDealDetailProcessList;
    }

    /**
     * 根据traceid+requestno从ods取待处理流水信息，适用于银企联和补偿任务
     */
    @Override
    public List<BankReconciliation> getBankReconciliations(DealDetailConsumerDTO eventBusModel) {
        /**
         * 1.获取BankDealDetailODSModel集合，先读缓存在读DB
         * */
        List<BankDealDetailODSModel> bankDealDetailODSModelList = this.getBankDealDetailODSModelListFromCacheOrDB(eventBusModel);
        if (CollectionUtils.isEmpty(bankDealDetailODSModelList)) {
            log.error("【流水消费】4.消费端消费，缓存不存在数据,从ods库读取流水数据量空，流水处理结束");
            return null;
        }
        int originCount = bankDealDetailODSModelList.size();
        /**
         * 2.ods表中可能存在重复的mainid,如果有重复mainid，只处理一个mainid,其余的ods状态改为无需处理
         * */
        //采集需要处理的ods
        List<BankDealDetailODSModel> needHandleODSModel = new ArrayList<>();
        //冗余ods状态改为无需处理
        List<BankDealDetailODSModel> noNeedHandleODSModel = new ArrayList<>();
        if (eventBusModel.getType() != null && DealDetailConsumerDTO.TYPE_YQL.equals(eventBusModel.getType())) {
            needHandleODSModel.addAll(bankDealDetailODSModelList);
        } else {
            DealDetailUtils.removeRepeatODSBymainid(bankDealDetailODSModelList, needHandleODSModel, noNeedHandleODSModel);
        }
        log.error("【流水消费】4.消费端消费，缓存不存在数据,从ods库读取流水数据量{},去重后数据量:{}", originCount, needHandleODSModel.size());
        /**
         * 3.批量将ODS实体转为流水实体
         * */
        List<BankReconciliation> bankReconciliationList = this.batchConvertODSToBankReconciliation(needHandleODSModel);
        //银企联拉取的数据设置
        if ( StringUtils.isNotEmpty(eventBusModel.getSaveDirect()) && DealDetailEnumConst.SAVE_DIRECT.equals(eventBusModel.getSaveDirect()) && !CollectionUtils.isEmpty(bankReconciliationList)){
            bankReconciliationList.stream().forEach(p->{
                p.put(DealDetailEnumConst.SAVE_DIRECT,DealDetailEnumConst.SAVE_DIRECT);
            });
            //移除标识
            eventBusModel.setSaveDirect(null);
        }
        return bankReconciliationList;
    }

    @Override
    public List<BankDealDetailODSModel> getBankDealDetailODSModelListFromCacheOrDB(DealDetailConsumerDTO eventBusModel) {
        //step1:先读缓存
        Cache<DealDetailConsumerDTO, List<BankDealDetailODSModel>> odsCache = YQLDealDetailAccessImpl.getOdsCache();
        if (null != odsCache) {
            List<BankDealDetailODSModel> bankDealDetailResponseRecords = odsCache.getIfPresent(eventBusModel);
            if (!CollectionUtils.isEmpty(bankDealDetailResponseRecords)) {
                //step2:缓存显示回收
                odsCache.invalidate(eventBusModel);
                return bankDealDetailResponseRecords;
            }
        }
        //step3:缓存不存在，从ods表读
        String traceId = eventBusModel.getTraceId();
        String requestSeqNo = eventBusModel.getRequestSeqNo();
        if (StringUtils.isEmpty(traceId) || StringUtils.isEmpty(requestSeqNo)) {
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400767", "流水消费traceid或requestseqno不能为空") /* "流水消费traceid或requestseqno不能为空" */);
        }
        IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
        return bankDealDetailAccessDao.batchQueryODSByTranceidAndRequestseqnoAndodsstatus(traceId, requestSeqNo, DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_NO.getProcessstatus());
    }

    protected List<BankReconciliation> batchConvertODSToBankReconciliation(List<BankDealDetailODSModel> bankDealDetailResponseRecords) {
        //step1:非空校验
        if (CollectionUtils.isEmpty(bankDealDetailResponseRecords)) {
            return null;
        }
        List<BankReconciliation> bankReconciliations = bankdealDetailOdsConvertManager.convertOdsTOBankReconciliation(bankDealDetailResponseRecords);
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            bankReconciliation.setCurrency(translateCurrencyCode(bankReconciliation));
            bankReconciliation.setDc_flag(translateDcFlag(bankReconciliation));
        }
        log.error("【流水消费】5.已将ods实体转成银行对账单实体");
        return bankReconciliations;
    }

    /**
     * 币种编码翻译币种id
     *
     * @param bankReconciliation
     */
    public String translateCurrencyCode(BankReconciliation bankReconciliation) {
        String currency_code = "";
        try {
            currency_code = currencyQueryService.getCurrencyByCode(bankReconciliation.get(ICmpConstant.CURRENCY_CODE));
        } catch (Exception e) {
            log.error("翻译币种异常:{}", e);
        }
        return currency_code;
    }

    /**
     * 转换dcflag  Short dc_flag = 1;
     *
     * @param bankReconciliation
     */
    public static Direction translateDcFlag(BankReconciliation bankReconciliation) {
        Short dc_flag = 1;
        if ("d".equals(bankReconciliation.get("dc_flag")) || dc_flag.equals(bankReconciliation.get("dc_flag"))) {
            return Direction.Debit;
        }
        return Direction.Credit;
    }
}
