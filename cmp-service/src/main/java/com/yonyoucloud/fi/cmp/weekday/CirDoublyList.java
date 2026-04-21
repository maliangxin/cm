package com.yonyoucloud.fi.cmp.weekday;

/**
 * @author shiqhs
 * @date 2022/04/26
 * @description 循环双向链表
 */
public class CirDoublyList<T> {

    public DoubleNode<T> head;

    public CirDoublyList() {
        this.head = new DoubleNode<>();
        this.head.prev = this.head;
        this.head.next = this.head;
    }

    public boolean isEmpty() {
        return this.head.next == this.head;
    }

    public DoubleNode<T> insert(int i, T x) {
        if (x == null) {
            return null;
        }
        DoubleNode<T> front = this.head;
        for (int j = 0; front.next != this.head && j < i ; j++) {
            front = front.next;
        }
        DoubleNode<T> q = new DoubleNode<>(x, front, front.next);
        front.next.prev = q;
        front.next = q;
        return q;
    }

    public DoubleNode<T> insert(T x) {
        if (x == null) {
            return null;
        }
        DoubleNode<T> q = new DoubleNode<>(x, head.prev, head);
        head.prev.next = q;
        head.prev = q;
        return q;
    }
    public T remove(int i) {
        DoubleNode<T> front = this.head;
        for (int j = 0; front.next != null && j < i; j++) {
            front = front.next;
        }
        if (i >= 0 && front.next != null) {
            T x = front.next.data;
            front.next = front.next.next;
            return x;
        }
        return null;
    }
}
