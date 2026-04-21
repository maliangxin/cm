package com.yonyoucloud.fi.cmp.weekday;

/**
 * @author shiqhs
 * @date 2022/04/30
 * @description 栈接口,描述栈抽象数据类型,T 表示数据元素的数据类型
 */
public interface Stack<T> {

    /**
     * 判断栈是否为空
     * @return 是否为空
     */
    boolean isEmpty();

    /**
     * 元素 x 入栈
     * @param x T
     */
    void push(T x);

    /**
     * 返回栈顶元素,未出栈
     * @return 栈顶元素
     */
    T peek();

    /**
     * 出栈,返回栈顶元素
     * @return 栈顶元素
     */
    T pop();
}
