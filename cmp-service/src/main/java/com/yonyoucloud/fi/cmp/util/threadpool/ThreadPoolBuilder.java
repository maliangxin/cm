package com.yonyoucloud.fi.cmp.util.threadpool;

import com.yonyou.iuap.context.YmsContextWrappers;
import cn.hutool.core.thread.BlockPolicy;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.util.Constant.ThreadConstant;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;

/**
 * @Author guoyangy
 * @Date 2023/11/27 10:43
 * @Description 自定义线程池建造工具类的核心实现
 * @Version 1.0
 */
public class ThreadPoolBuilder {
    private static final RejectedExecutionHandler defaultRejectHandler = new BlockPolicy();

    /**
     * cpu核数
     */
    private static int CORE_POOLSIZE = Runtime.getRuntime().availableProcessors();


    /**
     * 创建IO密集型线程池
     *
     * @return ThreadPoolExecutor
     */
    public static IOThreadPoolBuilder ioThreadPoolBuilder() {
        return new IOThreadPoolBuilder();
    }

    /**
     * 创建计算密集型线程池
     *
     * @return
     */
    public static IOThreadPoolBuilder cpuPoolBuilder() {
        return new IOThreadPoolBuilder();
    }

    /**
     * 创建IO密集型线程池
     *
     * @return ThreadPoolExecutor
     */
    public static IOThreadPoolBuilder defaultThreadPoolBuilder() {
        return new IOThreadPoolBuilder();
    }


    public static class IOThreadPoolBuilder {

        private ThreadFactory threadFactory;

        private RejectedExecutionHandler rejectHandler;


        private int queueSize = -1;

        private int maximumPoolSize = CORE_POOLSIZE;

        //private int keepAliveTime = 120;

        private boolean daemon = false;

        private String threadNamePrefix;



        public int getCorePooSize(int ioTime, int cpuTime) {
            return CORE_POOLSIZE * (1 + (ioTime / cpuTime));
        }

        public IOThreadPoolBuilder setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
            return this;
        }

        public IOThreadPoolBuilder setDaemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }



        public IOThreadPoolBuilder setRejectHandler(RejectedExecutionHandler rejectHandler) {
            this.rejectHandler = rejectHandler;
            return this;
        }

        public IOThreadPoolBuilder setQueueSize(int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        public IOThreadPoolBuilder setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

//        public IOThreadPoolBuilder setKeepAliveTime(int keepAliveTime) {
//            this.keepAliveTime = keepAliveTime;
//            return this;
//        }


        public ExecutorService builder(int ioTime, int cpuTime) {
            if (rejectHandler == null) {
                rejectHandler = defaultRejectHandler;
            }
            threadFactory = ThreadPoolUtil.createThreadFactory(this.threadNamePrefix, this.daemon);
            ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
            threadPoolTaskExecutor.setCorePoolSize(getCorePooSize(ioTime, cpuTime));
            threadPoolTaskExecutor.setMaxPoolSize(getCorePooSize(ioTime, cpuTime));
            threadPoolTaskExecutor.setQueueCapacity(queueSize);
            threadPoolTaskExecutor.setKeepAliveSeconds(120);
            threadPoolTaskExecutor.setThreadNamePrefix(threadNamePrefix);
            threadPoolTaskExecutor.setPrestartAllCoreThreads(true);
            threadPoolTaskExecutor.setRejectedExecutionHandler(rejectHandler);
            threadPoolTaskExecutor.initialize();
            ExecutorService taskExecutor = YmsContextWrappers.wrap(threadPoolTaskExecutor.getThreadPoolExecutor());
            return taskExecutor;
        }

        public ExecutorService builder(Integer corePoolSize,Integer maxPoolSize,Integer queueSize,String threadNamePrefix) {
            if (rejectHandler == null) {
                rejectHandler = defaultRejectHandler;
            }
            if(corePoolSize == null){
                corePoolSize = CORE_POOLSIZE;
            }
            if(maxPoolSize == null){
                maxPoolSize = CORE_POOLSIZE;
            }
            if(queueSize == null){
                queueSize = 3000;
            }
            if(threadNamePrefix == null || "".equals(threadNamePrefix)){
                threadNamePrefix = "default-threadNamePrefix";
            }
            threadFactory = ThreadPoolUtil.createThreadFactory(this.threadNamePrefix, this.daemon);
            ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
            threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
            threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
            threadPoolTaskExecutor.setQueueCapacity(queueSize);
            threadPoolTaskExecutor.setKeepAliveSeconds(120);
            threadPoolTaskExecutor.setThreadNamePrefix(threadNamePrefix);
            threadPoolTaskExecutor.setPrestartAllCoreThreads(true);
            threadPoolTaskExecutor.setRejectedExecutionHandler(rejectHandler);
            threadPoolTaskExecutor.initialize();
            ExecutorService taskExecutor = YmsContextWrappers.wrap(threadPoolTaskExecutor.getThreadPoolExecutor());
            return taskExecutor;
        }


        public ExecutorService builder(Integer corePoolSize,Integer maxPoolSize,Integer queueSize,int ioTime, int cpuTime,String threadNamePrefix) {
            if (rejectHandler == null) {
                rejectHandler = defaultRejectHandler;
            }
            if(corePoolSize == null){
                corePoolSize = CORE_POOLSIZE;
            }
            if(maxPoolSize == null){
                maxPoolSize = CORE_POOLSIZE;
            }
            if(queueSize == null){
                queueSize = 3000;
            }
            if(threadNamePrefix == null || "".equals(threadNamePrefix)){
                threadNamePrefix = "default-threadNamePrefix";
            }
            //调度任务传入为0时 通过ioTime 和 cputime计算
            if(corePoolSize == 0){
                corePoolSize = getCorePooSize(ioTime,cpuTime);
            }
            //核心线程 比较 最大线程数
            if(corePoolSize>maxPoolSize){
                corePoolSize=maxPoolSize;
            }
            threadFactory = ThreadPoolUtil.createThreadFactory(this.threadNamePrefix, this.daemon);
            ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
            threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
            threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
            threadPoolTaskExecutor.setQueueCapacity(queueSize);
            threadPoolTaskExecutor.setKeepAliveSeconds(120);
            threadPoolTaskExecutor.setThreadNamePrefix(threadNamePrefix);
            threadPoolTaskExecutor.setPrestartAllCoreThreads(true);
            threadPoolTaskExecutor.setRejectedExecutionHandler(rejectHandler);
            threadPoolTaskExecutor.initialize();
            ExecutorService taskExecutor = YmsContextWrappers.wrap(threadPoolTaskExecutor.getThreadPoolExecutor());
            return taskExecutor;
        }
    }

    public static ExecutorService buildThreadPoolByYmsParam(String threadPrefix) {
        return buildThreadPoolByYmsParam(threadPrefix, threadPrefix + ThreadConstant.THREAD_PARAM_POSTFIX, ThreadConstant.THREAD_YMS_PARAM_DEFAULT_VALUE);
    }
    public static ExecutorService buildThreadPoolByYmsParam(String threadPrefix, String  threadYmsParamDefaultValue) {
        return buildThreadPoolByYmsParam(threadPrefix, threadPrefix + ThreadConstant.THREAD_PARAM_POSTFIX, threadYmsParamDefaultValue);
    }


    public static ExecutorService buildThreadPoolByYmsParam(String threadNamePrefix, String threadYmsParamName, String  threadYmsParamDefaultValue) {
        // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
        ExecutorService executorService = null;
        String threadParam = AppContext.getEnvConfig(threadYmsParamName, threadYmsParamDefaultValue);
        String[] threadParamArray = threadParam.split(",");
        int corePoolSize = Integer.parseInt(threadParamArray[0]);
        int maxPoolSize = Integer.parseInt(threadParamArray[1]);
        int queueSize = Integer.parseInt(threadParamArray[2]);
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setDaemon(false)
                .setRejectHandler(new BlockPolicy())
                .builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);
        return executorService;
    }

}
