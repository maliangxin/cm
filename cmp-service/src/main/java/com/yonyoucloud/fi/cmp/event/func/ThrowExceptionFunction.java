package com.yonyoucloud.fi.cmp.event.func;

/**
 * <h1>定义一个抛出异常的形式的函数式接口, 这个接口只有参数没有返回值是个消费型接口</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023/6/4 21:09
 */
@FunctionalInterface
public interface ThrowExceptionFunction {
    /**
     * 抛出异常信息
     *
     * @param message 异常信息
     **/
    void throwMessage(String message);
}
