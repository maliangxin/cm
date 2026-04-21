package com.yonyoucloud.fi.cmp.balanceadjust.service.impl;

import com.yonyou.yonbip.ctm.util.lock.LockCallable;
import com.yonyou.yonbip.ctm.util.lock.LockRunnable;
import com.yonyoucloud.fi.cmp.util.threadpool.MyTask;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;


public interface BalanceBatchCommonService {

    /**
     * 锁住一个服务，执行一个方法，无返回值
     * @param lockRunnable 执行的方法
     * @return
     */
    BiFunction<String, String, Object> executeInOneServiceLock(LockRunnable lockRunnable);

    /**
     * 锁住一个服务，执行一个方法,有返回值
     * @param callBack
     * @return
     * @param <V>
     */
    <V> BiFunction<String, String, V> executeInOneServiceLock(LockCallable callBack);

    /**
     * 锁住一个服务，执行一个方法，根据lockKey,无返回值
     * @param lockRunnable
     * @return
     */
    Function<String, String> executeInOneServiceLockByLockKey(LockRunnable lockRunnable);

    /**
     * 锁住一个服务，执行一个方法，根据lockKey,有返回值
     * @param callBack
     * @return
     * @param <V>
     */
    <V> Function<String, V> executeInOneServiceLockByLockKey(LockCallable callBack);


    <T> List<Object> executeByBatchNotShutDown(List<T> lists, String taskName, MyTask myTask);
}
