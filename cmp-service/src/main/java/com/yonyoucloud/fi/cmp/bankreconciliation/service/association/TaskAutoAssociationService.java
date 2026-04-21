//package com.yonyoucloud.fi.cmp.bankreconciliation.service.association;
//
//
//import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
//import com.yonyoucloud.fi.cmp.autocorrsetting.CorrOperationService;
//import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
//import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
//import com.yonyoucloud.fi.cmp.util.TaskUtils;
//import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.collections4.CollectionUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.*;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Future;
//import java.util.concurrent.atomic.AtomicInteger;
//
//
///**
// * 自动关联操作 - 定时任务
// * @author msc
// */
//
//@Service
//@Slf4j
//@Transactional(rollbackFor = RuntimeException.class)
//public class TaskAutoAssociationService {
//
//    @Autowired
//    CorrOperationService corrOperationService;//写入关联关系
//
//    @Autowired
//    ReWriteBusCorrDataService reWriteBusCorrDataService;
//
//    @Autowired
//    private CtmThreadPoolExecutor executorServicePool;
//
//    /**
//     * 自动关联 - 定时任务
//     * @param corrDataEntities
//     * @param paramMap
//     * @return
//     */
//    public Map<String,Object> autoCorrBill(List<CorrDataEntity> corrDataEntities,Map<String,Object> paramMap) throws InterruptedException {
//        //如没有需要关联处理的单据直接返回
//        if (corrDataEntities == null || corrDataEntities.size() < 1) {
//            TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001B4", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
//            Map<String, Object> retMap = new HashMap<>(16);
//            retMap.put("asynchronized", "true");
//            return retMap;
//        }
//        //去除重复关联到同一个业务单据上的对账单数据
//        HashMap<String,List<CorrDataEntity>> corrDataEntitiesMap = new HashMap<String,List<CorrDataEntity>>();
//        for (CorrDataEntity corrDataEntity : corrDataEntities) {
//            String key = corrDataEntity.getBillType() + corrDataEntity.getBusid();
//            List<CorrDataEntity> groupData  = null;
//            if(corrDataEntitiesMap.get(key) == null ){
//                groupData = new ArrayList<CorrDataEntity>();
//            } else {
//                groupData = corrDataEntitiesMap.get(key);
//            }
//            groupData.add(corrDataEntity);
//            corrDataEntitiesMap.put(key, groupData);
//        }
//        List<CorrDataEntity> newCorrDataEntities = new ArrayList<>();
//        Iterator<String> iterator = corrDataEntitiesMap.keySet().iterator();
//        while (iterator.hasNext()){
//            String key = iterator.next();
//            List<CorrDataEntity> tmpList = corrDataEntitiesMap.get(key);
//            //不论是否是多个对账单关联到同一个业务单据，只保留一个，其他的不处理
//            if(tmpList.size() >=1){
//                newCorrDataEntities.add(tmpList.get(0));
//            }
//        }
//        //异步任务开启线程池，线程池任务中关联(并确认)业务单据，线程池中任务事务独立，只回滚失败的
//        executorServicePool.getThreadPoolExecutor().submit(() -> {
//            List<Callable<Object>> callables = new ArrayList<>();
//            AtomicInteger taskCount = new AtomicInteger(0);
//            for (CorrDataEntity corrDataEntity : newCorrDataEntities) {
//                callables.add(() -> {
//                    try {
//                        corrOperationService.runCorrTask(corrDataEntity);
//                        if (newCorrDataEntities.size() == taskCount.addAndGet(1)) {
//                            TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001B4", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
//                        }
//                    } catch (Exception e) {
//                        log.error("自动关联出错: " + corrDataEntity.toString(), e);
//                        if (newCorrDataEntities.size() == taskCount.addAndGet(1)) {
//                            TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001B4", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
//                        }
//                    }
//                    return 1;
//                });
//            }
//            ExecutorService executorService = null;
//            try {
//                executorService = buildThreadPoolForAutoCorr();
//                List<Future<Object>> list = executorService.invokeAll(callables);
//                if (!CollectionUtils.isEmpty(list)) {
//                    for (Future<Object> futrue : list) {
//                        futrue.get();
//                    }
//                }
//            } catch (InterruptedException | ExecutionException e) {
//                log.error("自动关联任务异常：", e);
//                TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_FAILURE, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001B3", "执行失败") /* "执行失败" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
//            } finally {
//                if (executorService != null) {
//                    executorService.shutdown();
//                }
//            }
//        });
//        Map<String, Object> retMap = new HashMap<>(16);
//        retMap.put("asynchronized", true);
//        return retMap;
//    }
//
//    private ExecutorService buildThreadPoolForAutoCorr(){
//        ExecutorService executorService = ThreadPoolBuilder.defaultThreadPoolBuilder()
//                .builder(null,null,null,null);
//        return executorService;
//    }
//
//}
