package com.yonyoucloud.fi.cmp.weekday;

/**
 * @author shiqhs
 * @date 2022/05/01
 * @description 链式栈类,实现栈接口,T表示数据元素的数据类型
 * @param <T>
 */
public final class LinkedStack<T> implements Stack<T> {

    private SinglyList<T> list;

    public LinkedStack() {
        this.list = new SinglyList<>();
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public void push(T x) {
        this.list.insert(0, x);
    }

    @Override
    public T peek() {
        return this.list.get(0);
    }

    @Override
    public T pop() {
        return this.list.remove(0);
    }
}
