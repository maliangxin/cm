package com.yonyoucloud.fi.cmp.event.func;

/**
 * <h1>创建一个名为BranchHandle的函数式接口，接口的参数为两个Runnable接口。
 *     这两个两个Runnable接口分别代表了为true或false时要进行的操作</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023/5/1 19:47
 */
@FunctionalInterface
public interface BranchHandleFunction {
    /**
     * 分支操作
     *
     * @param trueHandle 为true时要进行的操作
     * @param falseHandle 为false时要进行的操作
     *
     **/
    void trueOrFalseHandle(Runnable trueHandle, Runnable falseHandle);
}
