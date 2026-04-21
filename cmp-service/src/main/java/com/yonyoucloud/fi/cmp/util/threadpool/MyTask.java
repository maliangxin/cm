package com.yonyoucloud.fi.cmp.util.threadpool;

/**
 * @Author guoyangy
 * @Date 2023/11/27 14:19
 * @Description todo
 * @Version 1.0
 */
public interface MyTask {
    Object doTask(int fromIndex,int toIndex) throws Exception;
}
