package com.yonyoucloud.fi.cmp.weekday;

/**
 * @author shiqhs
 * @date 2022/04/30
 * @description 顺序栈类,实现栈接口,T表示数据元素的数据类型
 * @param <T>
 */
public final class SeqStack<T> implements Stack<T> {

    private SeqList<T> list;

    public SeqStack(int length) {
        this.list = new SeqList<>(length);
    }

    public SeqStack() {
        this(64);
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public void push(T x) {
        this.list.insert(x);
    }

    @Override
    public T peek() {
        return this.list.get(list.size() - 1);
    }

    @Override
    public T pop() {
        return this.list.remove(list.size() - 1);
    }
}
