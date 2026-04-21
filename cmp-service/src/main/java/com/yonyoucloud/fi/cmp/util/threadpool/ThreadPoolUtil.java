package com.yonyoucloud.fi.cmp.util.threadpool;

import com.yonyoucloud.fi.cmp.common.CtmException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @Author guoyangy
 * @Date 2023/11/27 10:41
 * @Description 线程池工具类
 * @Version 1.0
 */
@Slf4j
public class ThreadPoolUtil {

    public static String errorListKey = "errorList";
    public static String sucessListKey = "sucessList";
    public static ThreadFactory createThreadFactory(String threadNamePrefix, boolean daemon) {
        if (threadNamePrefix != null) {
            return new ThreadFactoryImpl(threadNamePrefix, daemon);
        }
        return Executors.defaultThreadFactory();
    }

    /**
     * @param lists 待执行业务数据
     * @param batchcount 每批执行数量
     * @desc 业务拆批执行
     *
     * */
    public static <T> List<Object> executeByBatch(ExecutorService executorService , List<T> lists, int batchcount, String taskName, MyTask myTask) throws Exception{
        return executeByBatch(executorService,lists,batchcount,taskName,myTask,null);
    }


    public static <T> List<Object> executeByBatch(ExecutorService executorService , List<T> lists, int batchcount, String taskName, MyTask myTask,boolean isShutdownPool) throws Exception{
        return executeByBatch(executorService,lists,batchcount,taskName,myTask,null,isShutdownPool);
    }


    public static <T> ThreadResult executeByBatchCollectResultsNoSemaphore(ExecutorService executorService , List<T> lists,  int batchcount, String taskName,boolean isShutdownPool, MyTask myTask) throws Exception{
        return executeByBatchCollectResults(executorService,lists,  batchcount,taskName,null,isShutdownPool,myTask);
    }

    /**
     * @param lists 待执行业务数据
     * @param batchcount 每批执行数量
     * @desc 业务拆批执行
     *
     * */
    public static <T> List<Object> executeByBatchNotShutDown(ExecutorService executorService , List<T> lists, int batchcount, String taskName, MyTask myTask) throws Exception{
        return executeByBatchNotShutDown(executorService,lists,batchcount,taskName,myTask,null);
    }

    /**
     * @param lists 待执行业务数据
     * @param batchcount 每批执行数量
     * @desc 业务拆批执行
     *
     * */
    public static <T> List<Object> executeByBatchNotShutDown(ExecutorService executorService , List<T> lists, int batchcount, String taskName, MyTask myTask,Semaphore semaphore) throws Exception{
        int listSize = lists.size();
        int totalTask = (listSize % batchcount == 0 ? listSize / batchcount : (listSize / batchcount)+1);
        int task =0;
        List<Callable<Object>> callables = new ArrayList<>(totalTask);
        List<Object> resultList = new ArrayList<>();
        long startThread = System.currentTimeMillis();
        try{
            for (int i = 1; i <= listSize; i++) {
                //batchcount作为一个任务提交到线程池执行
                if(i % batchcount == 0){
                    int finalI = i;
                    task++;
                    long taskPushTime = System.currentTimeMillis();
                    int finalTask = task;
                    log.error("----{}任务拆批执行，共{}批,当前第{}批，提交线程池",taskName,totalTask,finalTask);
                    callables.add(()->{
                        long start = System.currentTimeMillis();
                        try{
                            //通过信号量控制线程任务执行
                            if(semaphore!=null){
                                semaphore.acquire();
                            }
                            Object result = myTask.doTask(finalI-batchcount,finalI);
                            log.error("----{}任务，共{}批，第{}批，线程池等待时间{}ms,执行时间{}ms,共耗时{}ms", taskName,totalTask,finalTask,(start-taskPushTime),(System.currentTimeMillis()-start),(System.currentTimeMillis()-taskPushTime));
                            return result;
                        }catch (Exception e){
                            log.error(taskName+"线程子任务执行异常",e);
                            throw e;
                        }finally {
                            if(semaphore!=null){
                                semaphore.release();
                            }
                        }
                    });
                }
            }
            //最后一批 任务提交到线程池
            if( listSize % batchcount !=0 ){
                task++;
                long taskPushTime = System.currentTimeMillis();
                int finalTask = task;
                log.error("----{}任务业务拆批执行，共{}批,当前最后一批，即第{}批，提交线程池",taskName,totalTask,finalTask);
                callables.add(()->{
                    long start = System.currentTimeMillis();
                    try{
                        //通过信号量控制线程任务执行
                        if(semaphore!=null){
                            semaphore.acquire();
                        }
                        Object result = myTask.doTask((listSize / batchcount) * batchcount,listSize);
                        log.error("----{}任务,第{}批，线程池等待时间{}ms,执行时间{}ms,共耗时{}ms",taskName, finalTask,(start-taskPushTime),(System.currentTimeMillis()-start),(System.currentTimeMillis()-taskPushTime));
                        return result;
                    }catch (Exception e){
                        log.error(taskName+"线程子任务执行异常",e);
                        throw e;
                    }finally {
                        if(semaphore!=null){
                            semaphore.release();
                        }
                    }

                });
            }

            List<Future<Object>> list = executorService.invokeAll(callables);
            if(!CollectionUtils.isEmpty(list)){
                for(Future<Object> futrue:list){
                    resultList.add(futrue.get());
                }
            }
        }catch (Exception e ){
            log.error(e.getMessage(),e);
            throw e;
        }
        log.error("----所有任务均执行完成共耗时{}s",(System.currentTimeMillis()-startThread)/1000);
        return resultList;
    }

    public static <T> List<Object> executeBySingleNotShutDown(ExecutorService executorService, String taskName, MyTask myTask,Semaphore semaphore) throws Exception{
        List<Callable<Object>> callables = new ArrayList<>(1); // 准备存放任务的可调用列表
        List<Object> resultList = new ArrayList<>(); // 准备存放任务执行结果的列表
        try{
            // 添加任务到可调用列表中
            callables.add(() ->{
                try{
                    // 通过信号量控制线程任务执行，如果信号量非空，则获取许可
                    if(semaphore!=null){
                        semaphore.acquire();
                    }
                    Object result = myTask.doTask(0,1); // 执行任务，并获取结果
                    return result;
                }catch (Exception e){
                    log.error(taskName+"线程子任务执行异常",e); // 记录任务执行异常
                    throw e;
                }finally {
                    // 无论任务执行成功或失败，最后都释放信号量
                    if(semaphore!=null){
                        semaphore.release();
                    }
                }
            });
            // 执行所有可调用任务，并收集结果
            List<Future<Object>> list = executorService.invokeAll(callables);
            if(!CollectionUtils.isEmpty(list)){
                for(Future<Object> futrue:list){
                    resultList.add(futrue.get()); // 获取并添加任务执行结果
                }
            }
        }catch(Exception e){
            log.error(e.getMessage(),e); // 记录捕获到的异常
            throw e;
        }
        return resultList; // 返回任务执行结果列表
    }

    /**
     * 使用单个线程执行任务的函数。
     * @param executorService 执行任务的线程池。
     * @param taskName 任务名称，用于日志记录。
     * @param myTask 实现了MyTask接口的任务对象，用于执行具体的任务。
     * @param semaphore 信号量，用于控制任务的并发执行数量。
     * @return 返回任务执行的结果列表。
     * @throws Exception 如果任务执行过程中发生异常，则抛出。
     */
    public static <T> List<Object> executeBySingle(ExecutorService executorService, String taskName, MyTask myTask,Semaphore semaphore) throws Exception{
        List<Callable<Object>> callables = new ArrayList<>(1); // 准备存放任务的可调用列表
        List<Object> resultList = new ArrayList<>(); // 准备存放任务执行结果的列表

        try{
            // 添加任务到可调用列表中
            callables.add(() ->{
                try{
                    // 通过信号量控制线程任务执行，如果信号量非空，则获取许可
                    if(semaphore!=null){
                        semaphore.acquire();
                    }
                    Object result = myTask.doTask(0,1); // 执行任务，并获取结果
                    return result;
                }catch (Exception e){
                    log.error(taskName+"线程子任务执行异常",e); // 记录任务执行异常
                    throw e;
                }finally {
                    // 无论任务执行成功或失败，最后都释放信号量
                    if(semaphore!=null){
                        semaphore.release();
                    }
                }
            });

            // 执行所有可调用任务，并收集结果
            List<Future<Object>> list = executorService.invokeAll(callables);
            if(!CollectionUtils.isEmpty(list)){
                for(Future<Object> futrue:list){
                    resultList.add(futrue.get()); // 获取并添加任务执行结果
                }
            }

        }catch(Exception e){
            log.error(e.getMessage(),e); // 记录捕获到的异常
            throw e;

        }finally {
            // 关闭线程池
            if(executorService != null){
                executorService.shutdown();
            }
        }
        return resultList; // 返回任务执行结果列表
    }

    /**
     * @param lists 待执行业务数据
     * @param batchcount 每批执行数量
     * @desc 业务拆批执行
     *
     * */
    //public static <T> List<Object> executeByBatch(ExecutorService executorService , List<T> lists, Map<String, List> resultMap, int batchcount, String taskName, MyTask myTask, Semaphore semaphore) throws Exception{
    public static <T> List<Object> executeByBatch(ExecutorService executorService , List<T> lists, int batchcount, String taskName, MyTask myTask,Semaphore semaphore) throws Exception{
        int listSize = lists.size();
        int totalTask = (listSize % batchcount == 0 ? listSize / batchcount : (listSize / batchcount)+1);
        int task =0;
        List<Callable<Object>> callables = new ArrayList<>(totalTask);
        List<Object> resultList = new ArrayList<>();
        long startThread = System.currentTimeMillis();
        try{
            for (int i = 1; i <= listSize; i++) {
                //batchcount作为一个任务提交到线程池执行
                if(i % batchcount == 0){
                    int finalI = i;
                    task++;
                    long taskPushTime = System.currentTimeMillis();
                    int finalTask = task;
                    log.error("----{}任务拆批执行，共{}批,当前第{}批，提交线程池",taskName,totalTask,finalTask);
                    callables.add(()->{
                        long start = System.currentTimeMillis();
                        try{
                            //通过信号量控制线程任务执行
                            if(semaphore!=null){
                                semaphore.acquire();
                            }
                            Object result = myTask.doTask(finalI-batchcount,finalI);
                            log.error("----{}任务，共{}批，第{}批，线程池等待时间{}ms,执行时间{}ms,共耗时{}ms", taskName,totalTask,finalTask,(start-taskPushTime),(System.currentTimeMillis()-start),(System.currentTimeMillis()-taskPushTime));
                            return result;
                        }catch (Exception e){
                            log.error(taskName+"线程子任务执行异常",e);
                            throw e;
                        }finally {
                            if(semaphore!=null){
                                semaphore.release();
                            }
                        }
                    });
                }
            }
            //最后一批 任务提交到线程池
            if( listSize % batchcount !=0 ){
                task++;
                long taskPushTime = System.currentTimeMillis();
                int finalTask = task;
                log.error("----{}任务业务拆批执行，共{}批,当前最后一批，即第{}批，提交线程池",taskName,totalTask,finalTask);
                callables.add(()->{
                    long start = System.currentTimeMillis();
                    try{
                        //通过信号量控制线程任务执行
                        if(semaphore!=null){
                            semaphore.acquire();
                        }
                        Object result = myTask.doTask((listSize / batchcount) * batchcount,listSize);
                        log.error("----{}任务,第{}批，线程池等待时间{}ms,执行时间{}ms,共耗时{}ms",taskName, finalTask,(start-taskPushTime),(System.currentTimeMillis()-start),(System.currentTimeMillis()-taskPushTime));
                        return result;
                    }catch (Exception e){
                        log.error(taskName+"线程子任务执行异常",e);
                        throw e;
                    }finally {
                        if(semaphore!=null){
                            semaphore.release();
                        }
                    }

                });
            }

            //错误收集后，一起抛出，否则只能抛出第一个异常
            ArrayList<Object> errorList = new ArrayList<>();
            List<Future<Object>> list = executorService.invokeAll(callables);
            if(!CollectionUtils.isEmpty(list)){
                for(Future<Object> futrue:list){
                    try {
                        resultList.add(futrue.get());
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        errorList.add(e.getMessage());
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(errorList)) {
                throw new CtmException(errorList.toString());
            }
        }catch (Exception e ){
            log.error(e.getMessage(),e);
            throw e;
        }finally{
            if(executorService != null){
                executorService.shutdown();
            }
        }
        log.error("----所有任务均执行完成共耗时{}s",(System.currentTimeMillis()-startThread)/1000);
        return resultList;
    }

    /**
     * @param lists 待执行业务数据
     * @param batchcount 每批执行数量
     * @desc 业务拆批执行
     *
     * */
    public static <T> List<Object> executeByBatch(ExecutorService executorService , List<T> lists, Map<String, List> resultMap, int batchcount, String taskName, MyTask myTask, Semaphore semaphore) throws Exception{
        int listSize = lists.size();
        int totalTask = (listSize % batchcount == 0 ? listSize / batchcount : (listSize / batchcount)+1);
        int task =0;
        List<Callable<Object>> callables = new ArrayList<>(totalTask);
        List<Object> resultList = new ArrayList<>();
        long startThread = System.currentTimeMillis();
        try{
            for (int i = 1; i <= listSize; i++) {
                //batchcount作为一个任务提交到线程池执行
                if(i % batchcount == 0){
                    int finalI = i;
                    task++;
                    long taskPushTime = System.currentTimeMillis();
                    int finalTask = task;
                    log.error("----{}任务拆批执行，共{}批,当前第{}批，提交线程池",taskName,totalTask,finalTask);
                    callables.add(()->{
                        long start = System.currentTimeMillis();
                        try{
                            //通过信号量控制线程任务执行
                            if(semaphore!=null){
                                semaphore.acquire();
                            }
                            Object result = myTask.doTask(finalI-batchcount,finalI);
                            log.error("----{}任务，共{}批，第{}批，线程池等待时间{}ms,执行时间{}ms,共耗时{}ms", taskName,totalTask,finalTask,(start-taskPushTime),(System.currentTimeMillis()-start),(System.currentTimeMillis()-taskPushTime));
                            return result;
                        }catch (Exception e){
                            log.error(taskName+"线程子任务执行异常",e);
                            throw e;
                        }finally {
                            if(semaphore!=null){
                                semaphore.release();
                            }
                        }
                    });
                }
            }
            //最后一批 任务提交到线程池
            if( listSize % batchcount !=0 ){
                task++;
                long taskPushTime = System.currentTimeMillis();
                int finalTask = task;
                log.error("----{}任务业务拆批执行，共{}批,当前最后一批，即第{}批，提交线程池",taskName,totalTask,finalTask);
                callables.add(()->{
                    long start = System.currentTimeMillis();
                    try{
                        //通过信号量控制线程任务执行
                        if(semaphore!=null){
                            semaphore.acquire();
                        }
                        Object result = myTask.doTask((listSize / batchcount) * batchcount,listSize);
                        log.error("----{}任务,第{}批，线程池等待时间{}ms,执行时间{}ms,共耗时{}ms",taskName, finalTask,(start-taskPushTime),(System.currentTimeMillis()-start),(System.currentTimeMillis()-taskPushTime));
                        return result;
                    }catch (Exception e){
                        log.error(taskName+"线程子任务执行异常",e);
                        throw e;
                    }finally {
                        if(semaphore!=null){
                            semaphore.release();
                        }
                    }

                });
            }

            List<Future<Object>> list = executorService.invokeAll(callables);
            //if(!CollectionUtils.isEmpty(list)){
            //    for(Future<Object> futrue:list){
            //        resultList.add(futrue.get());
            //    }
            //}



            //收集线程执行情况并返回
            List sucessReturnList = resultMap.get(sucessListKey);
            List errorReturnList = resultMap.get(errorListKey);
            List<Throwable> exceptions = new ArrayList<>();
            for (Future<Object> future : list) {
                try {
                    Map<String, List> threadresultMap = (Map<String, List>) future.get();
                    errorReturnList.addAll(threadresultMap.get(errorListKey));
                    sucessReturnList.addAll(threadresultMap.get(sucessListKey));
                } catch (ExecutionException e) {
                    exceptions.add(e.getCause());
                    errorReturnList.add(e.getCause().getMessage());
                }
            }
            if (!exceptions.isEmpty()) {
                log.error("Some tasks failed with exceptions: " + exceptions);
            }
        }catch (Exception e ){
            log.error(e.getMessage(),e);
            throw e;
        }finally{
            if(executorService != null){
                executorService.shutdown();
            }
        }
        log.error("----所有任务均执行完成共耗时{}s",(System.currentTimeMillis()-startThread)/1000);
        return resultList;
    }


    /**
     * @param lists 待执行业务数据
     * @param batchcount 每批执行数量
     * @desc 业务拆批执行
     *
     * */
    public static <T> List<Object> executeByBatch(ExecutorService executorService , List<T> lists, int batchcount, String taskName, MyTask myTask,Semaphore semaphore,boolean isShutdownPool) throws Exception{
        int listSize = lists.size();
        int totalTask = (listSize % batchcount == 0 ? listSize / batchcount : (listSize / batchcount)+1);
        int task =0;
        List<Callable<Object>> callables = new ArrayList<>(totalTask);
        List<Object> resultList = new ArrayList<>();
        long startThread = System.currentTimeMillis();
        try{
            for (int i = 1; i <= listSize; i++) {
                //batchcount作为一个任务提交到线程池执行
                if(i % batchcount == 0){
                    int finalI = i;
                    task++;
                    long taskPushTime = System.currentTimeMillis();
                    int finalTask = task;
                    log.error("----{}任务拆批执行，共{}批,当前第{}批，提交线程池",taskName,totalTask,finalTask);
                    callables.add(()->{
                        long start = System.currentTimeMillis();
                        try{
                            //通过信号量控制线程任务执行
                            if(semaphore!=null){
                                semaphore.acquire();
                            }
                            Object result = myTask.doTask(finalI-batchcount,finalI);
                            log.error("----{}任务，共{}批，第{}批，线程池等待时间{}ms,执行时间{}ms,共耗时{}ms", taskName,totalTask,finalTask,(start-taskPushTime),(System.currentTimeMillis()-start),(System.currentTimeMillis()-taskPushTime));
                            return result;
                        }catch (Exception e){
                            log.error(taskName+"线程子任务执行异常",e);
                            throw e;
                        }finally {
                            if(semaphore!=null){
                                semaphore.release();
                            }
                        }
                    });
                }
            }
            //最后一批 任务提交到线程池
            if( listSize % batchcount !=0 ){
                task++;
                long taskPushTime = System.currentTimeMillis();
                int finalTask = task;
                log.error("----{}任务业务拆批执行，共{}批,当前最后一批，即第{}批，提交线程池",taskName,totalTask,finalTask);
                callables.add(()->{
                    long start = System.currentTimeMillis();
                    try{
                        //通过信号量控制线程任务执行
                        if(semaphore!=null){
                            semaphore.acquire();
                        }
                        Object result = myTask.doTask((listSize / batchcount) * batchcount,listSize);
                        log.error("----{}任务,共{}批，第{}批，线程池等待时间{}ms,执行时间{}ms,共耗时{}ms",taskName, finalTask,(start-taskPushTime),(System.currentTimeMillis()-start),(System.currentTimeMillis()-taskPushTime));
                        return result;
                    }catch (Exception e){
                        log.error(taskName+"线程子任务执行异常",e);
                        throw e;
                    }finally {
                        if(semaphore!=null){
                            semaphore.release();
                        }
                    }

                });
            }
            //List<Future<Object>> list = executorService.invokeAll(callables);
            //if(!CollectionUtils.isEmpty(list)){
            //    for(Future<Object> futrue:list){
            //        resultList.add(futrue.get());
            //    }
            //}
            //错误收集后，一起抛出，否则只能抛出第一个异常
            ArrayList<Object> errorList = new ArrayList<>();
            List<Future<Object>> list = executorService.invokeAll(callables);
            if(!CollectionUtils.isEmpty(list)){
                for(Future<Object> futrue:list){
                    try {
                        resultList.add(futrue.get());
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        errorList.add(e.getMessage());
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(errorList)) {
                throw new CtmException(errorList.toString());
            }
        }catch (Exception e ){
            log.error(e.getMessage(),e);
            throw e;
        }finally{
            if(executorService != null && isShutdownPool){
                executorService.shutdown();
            }
        }
        log.error("----{},所有任务均执行完成共耗时{}s",taskName,(System.currentTimeMillis()-startThread)/1000);
        return resultList;
    }

    /**
     * @param lists 待执行业务数据
     * @param batchcount 每批执行数量
     * @desc 业务拆批执行
     *
     * */
    public static <T> ThreadResult executeByBatchCollectResults(ExecutorService executorService , List<T> lists,  int batchcount, String taskName, Semaphore semaphore, boolean isShutdownPool, MyTask myTask) throws Exception{
        int listSize = lists.size();
        int totalTask = (listSize % batchcount == 0 ? listSize / batchcount : (listSize / batchcount)+1);
        int task =0;
        List<Callable<Object>> callables = new ArrayList<>(totalTask);
        ThreadResult threadResult = new ThreadResult();
        long startThread = System.currentTimeMillis();
        Map<String, List> ResultMap = new HashMap<>();
        try{
            for (int i = 1; i <= listSize; i++) {
                //batchcount作为一个任务提交到线程池执行
                if(i % batchcount == 0){
                    int finalI = i;
                    task++;
                    long taskPushTime = System.currentTimeMillis();
                    int finalTask = task;
                    log.error("----{}任务拆批执行，共{}批,当前第{}批，提交线程池",taskName,totalTask,finalTask);
                    callables.add(()->{
                        long start = System.currentTimeMillis();
                        try{
                            //通过信号量控制线程任务执行
                            if(semaphore!=null){
                                semaphore.acquire();
                            }
                            Object result = myTask.doTask(finalI-batchcount,finalI);
                            log.error("----{}任务，共{}批，第{}批，线程池等待时间{}ms,执行时间{}ms,共耗时{}ms", taskName,totalTask,finalTask,(start-taskPushTime),(System.currentTimeMillis()-start),(System.currentTimeMillis()-taskPushTime));
                            return result;
                        }catch (Exception e){
                            log.error(taskName+"线程子任务执行异常",e);
                            throw e;
                        }finally {
                            if(semaphore!=null){
                                semaphore.release();
                            }
                        }
                    });
                }
            }
            //最后一批 任务提交到线程池
            if( listSize % batchcount !=0 ){
                task++;
                long taskPushTime = System.currentTimeMillis();
                int finalTask = task;
                log.error("----{}任务业务拆批执行，共{}批,当前最后一批，即第{}批，提交线程池",taskName,totalTask,finalTask);
                callables.add(()->{
                    long start = System.currentTimeMillis();
                    try{
                        //通过信号量控制线程任务执行
                        if(semaphore!=null){
                            semaphore.acquire();
                        }
                        Object result = myTask.doTask((listSize / batchcount) * batchcount,listSize);
                        log.error("----{}任务,共{}批，第{}批，线程池等待时间{}ms,执行时间{}ms,共耗时{}ms",taskName, finalTask,(start-taskPushTime),(System.currentTimeMillis()-start),(System.currentTimeMillis()-taskPushTime));
                        return result;
                    }catch (Exception e){
                        log.error(taskName+"线程子任务执行异常",e);
                        throw e;
                    }finally {
                        if(semaphore!=null){
                            semaphore.release();
                        }
                    }

                });
            }

            List<Future<Object>> list = executorService.invokeAll(callables);
            //if(!CollectionUtils.isEmpty(list)){
            //    for(Future<Object> futrue:list){
            //        resultList.add(futrue.get());
            //    }
            //}
            //收集线程执行情况并返回
            List<Throwable> exceptions = new ArrayList<>();
            for (Future<Object> future : list) {
                try {
                    //线程返回的数据结构需要是ThreadResult
                    ThreadResult subThreadResult = (ThreadResult) future.get();
                    threadResult.getErrorReturnList().addAll(subThreadResult.getErrorReturnList());
                    threadResult.getSucessReturnList().addAll(subThreadResult.getSucessReturnList());
                } catch (ExecutionException e) {
                    exceptions.add(e.getCause());
                    threadResult.getErrorReturnList().add(e.getCause().getMessage());
                }
            }
            if (!exceptions.isEmpty()) {
                log.error("Some tasks failed with exceptions: " + exceptions);
            }
        }catch (Exception e ){
            log.error(e.getMessage(),e);
            throw e;
        }finally{
            if(executorService != null && isShutdownPool){
                executorService.shutdown();
            }
        }
        log.error("----{},所有任务均执行完成共耗时{}s",taskName,(System.currentTimeMillis()-startThread)/1000);
        return threadResult;
    }

}
