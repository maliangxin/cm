package com.yonyoucloud.fi.cmp.weekday;

/**
 * @author shiqhs
 * @date 2022/05/01
 * @description 单链表节点
 * @param <T>
 */
public class Node<T> {

    /**
     * 数据域,存储数据元素
     */
    public T data;

    /**
     * 地址域,引用后继节点
     */
    public Node<T> next;

    public Node(T data, Node<T> next) {
        this.data = data;
        this.next = next;
    }

    public Node() {
        this(null, null);
    }

    @Override
    public String toString() {
        return this.data.toString();
    }
}
