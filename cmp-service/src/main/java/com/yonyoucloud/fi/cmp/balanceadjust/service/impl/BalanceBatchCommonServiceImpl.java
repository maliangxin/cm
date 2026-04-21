package com.yonyoucloud.fi.cmp.balanceadjust.service.impl;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockCallable;
import com.yonyou.yonbip.ctm.util.lock.LockRunnable;
import com.yonyoucloud.fi.cmp.util.threadpool.MyTask;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import cn.hutool.core.thread.BlockPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;


@Service
@Slf4j
public class BalanceBatchCommonServiceImpl implements BalanceBatchCommonService{

    private static long HOLD_LOCK_BATCH_THREAD_TIMEOUT=60*60*2L;
    private static int BATCH_COUNT=10;
//    分隔符
    private static String HOLD_LOCK_BATCH_THREAD_SEPARATOR="|";
    static ExecutorService executorService;
    static {
        HOLD_LOCK_BATCH_THREAD_TIMEOUT = Long.parseLong(getValidBatchConfigFromEnv("hold.lock.batch.thread.timeout",String.valueOf(60*60*2L)));
        BATCH_COUNT = Integer.parseInt(getValidBatchConfigFromEnv("cmp.balanceBatch.batchCount", "10"));
    }

    static {
        // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
        String threadParam = AppContext.getEnvConfig("cmp.balanceBatch.thread.param","8,32,1000,cmp-balance-batch-");
        String[] threadParamArray = threadParam.split(",");
        int corePoolSize = Integer.parseInt(threadParamArray[0]);
        int maxPoolSize = Integer.parseInt(threadParamArray[1]);
        int queueSize = Integer.parseInt(threadParamArray[2]);
        String threadNamePrefix = threadParamArray[3];
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setDaemon(false)
                .setRejectHandler(new BlockPolicy())
                .builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);

    }


    @Override
    public BiFunction<String, String, Object> executeInOneServiceLock(LockRunnable lockRunnable) {
        return (bankAccount,currency)->{
            if(StringUtils.isBlank(bankAccount) || StringUtils.isBlank(currency)){
                throw new CtmException("bankAccount or currency is null");
            }
            // 加锁的账号信息，使用 StringBuilder 避免潜在的冲突
            String lockKey = new StringBuilder().append(bankAccount).append(HOLD_LOCK_BATCH_THREAD_SEPARATOR).append(currency).toString();
            try {
                CtmLockTool.executeInOneServiceLock(lockKey,Long.valueOf(HOLD_LOCK_BATCH_THREAD_TIMEOUT), TimeUnit.SECONDS,lockRunnable);
                return null;
            } catch (Exception e) {
                throw new CtmException(e.getMessage());
            }
        };
    }

    @Override
    public <V> BiFunction<String, String, V> executeInOneServiceLock(LockCallable callBack) {
        return (bankAccount,currency)->{
            if(StringUtils.isBlank(bankAccount) || StringUtils.isBlank(currency)){
                throw new CtmException("bankAccount or currency is null");
            }
            // 加锁的账号信息，使用 StringBuilder 避免潜在的冲突
            String lockKey = new StringBuilder().append(bankAccount).append(HOLD_LOCK_BATCH_THREAD_SEPARATOR).append(currency).toString();
            try {
                return CtmLockTool.executeInOneServiceLock(lockKey,Long.valueOf(HOLD_LOCK_BATCH_THREAD_TIMEOUT), TimeUnit.SECONDS,callBack);
            } catch (Exception e) {
                throw new CtmException(e.getMessage());
            }
        };
    }


    @Override
    public  Function<String, String> executeInOneServiceLockByLockKey(LockRunnable lockRunnable) {
        return (lockKey)->{
            try {
                CtmLockTool.executeInOneServiceLock(lockKey,Long.valueOf(HOLD_LOCK_BATCH_THREAD_TIMEOUT), TimeUnit.SECONDS,lockRunnable);
                return null;
            } catch (Exception e) {
                throw new CtmException(e.getMessage());
            }
        };
    }


    @Override
    public <V> Function<String, V> executeInOneServiceLockByLockKey(LockCallable callBack) {
        return (lockKey)->{
            try {
                return CtmLockTool.executeInOneServiceLock(lockKey,Long.valueOf(HOLD_LOCK_BATCH_THREAD_TIMEOUT), TimeUnit.SECONDS,callBack);
            } catch (Exception e) {
                throw new CtmException(e.getMessage());
            }
        };
    }

    @Override
    public <T> List<Object> executeByBatchNotShutDown( List<T> params, String taskName, MyTask myTask) {
        try {
            return ThreadPoolUtil.executeByBatchNotShutDown(executorService, params, BATCH_COUNT, taskName, myTask);
        } catch (Exception e) {
            throw new CtmException(e.getMessage());
        }
    }

    /**
     * 获取有效的批量处理数量
     * 该方法首先从环境变量中获取指定的批量处理数量配置，如果未找到或值无效，则使用默认值
     * 无效的情况包括非数字字符串或数字小于等于0
     *
     * @param batchStr     批量处理数量的环境变量名称
     * @param defaultValue 如果环境变量中未找到指定配置，使用的默认值
     * @return 有效的批量处理数量如果输入无效，返回默认值10
     */
    private static String getValidBatchConfigFromEnv(String batchStr, String defaultValue) {
        // 从环境变量中获取批量处理数量的配置，如果不存在，则使用默认值
        try {
            // 如果转换成功且值有效，返回该值
            return AppContext.getEnvConfig(batchStr.toLowerCase(), defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
