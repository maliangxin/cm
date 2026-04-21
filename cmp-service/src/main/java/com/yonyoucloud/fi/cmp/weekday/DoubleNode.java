package com.yonyoucloud.fi.cmp.weekday;

/**
 * @author shiqhs
 * @date 2022/04/26
 * @description 双链表节点
 */
public class DoubleNode<T> {

    public T data;
    public DoubleNode<T> prev, next;
    public DoubleNode(T data, DoubleNode<T> prev, DoubleNode<T> next) {
        this.data = data;
        this.prev = prev;
        this.next = next;
    }
    public DoubleNode() {}
}
