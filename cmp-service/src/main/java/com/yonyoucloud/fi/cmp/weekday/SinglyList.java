package com.yonyoucloud.fi.cmp.weekday;

/**
 * @author shiqhs
 * @date 2022/05/01
 * @description 单链表
 * @param <T>
 */
public class SinglyList<T> {

    /**
     * 头指针,指向单链表的头节点
     */
    public Node<T> head;

    /**
     * 构造空单链表
     */
    public SinglyList() {
        this.head = new Node<>();
    }

    public SinglyList(T[] values) {
        this();
        // rear尾指针指向单链表最后一个节点,使尾插入效率是O(1)
        Node<T> rear = this.head;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                rear.next = new Node<>(values[i], null);
                rear = rear.next;
            }
        }
    }

    public boolean isEmpty() {
        return this.head.next == null;
    }

    /**
     * 返回第 i 个元素
     * @param i 元素序号
     * @return 第 i 个元素
     */
    public T get(int i) {
        Node<T> p = this.head.next;
        for (int j = 0; p != null && j < i; j++) {
            p = p.next;
        }
        return (i >= 0 && p != null) ? p.data :null;
    }

    /**
     * 设置第 i 个元素为 x
     * @param i 元素序号
     * @param x 元素值
     */
    public void set(int i, T x) {
        Node<T> p = this.head.next;
        for (int j = 0; p != null && j < i; j++) {
            p = p.next;
        }
        p.data = x;
    }

    /**
     * 长度
     * @return 链表长度
     */
    public int size() {
        int n = 0;
        for (Node<T> p = this.head.next; p != null; p = p.next) {
            n++;
        }
        return n;
    }

    /**
     * 插入,插入 x 为第 i 个元素,x!=null 返回插入节点。对 i 容错,若 i<0,则头插入i>0,则尾插入
     * @param i 元素序号
     * @param x 元素值
     * @return 第 i 个节点
     */
    public Node<T> insert(int i, T x) {
        if (x == null) {
            return null;
        }
        Node<T> front = this.head;
        for (int j = 0; front.next != null && j < i; j++) {
            front = front.next;
        }
        front.next = new Node<>(x, front.next);
        return front.next;
    }

    /**
     * 尾插入
     * @param x 元素值
     * @return 元素节点
     */
    public Node<T> insert(T x) {
        return insert(Integer.MAX_VALUE, x);
    }

    /**
     * 删除
     * @param i 元素序号
     * @return 元素节点
     */
    public T remove(int i) {
        Node<T> front = this.head;
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

    /**
     * 删除
     * @param key 元素值
     * @return 元素值
     */
    public T remove(T key) {
        Node<T> front = this.head;
        for (int i = 0; front.next != null; i++) {
            if (front.next.data == key) {
                front.next = front.next.next;
                return key;
            }
            front = front.next;
        }
        return null;
    }

    /**
     * 查找
     * @param key 元素值
     * @return 节点
     */
    public Node<T> search(T key) {
        Node<T> front = this.head;
        for (int i = 0; front.next != null; i++) {
            if (front.next.data == key) {
                return front.next;
            }
            front = front.next;
        }
        return null;
    }

    /**
     * 删除所有元素
     */
    public void clear() {
        this.head.next = null;
    }

    @Override
    public String toString() {
        String str = this.getClass().getName() + "(";
        for (Node<T> p = this.head.next; p != null; p = p.next) {
            str += p.data.toString() + (p.next != null ? "," : "");
        }
        return str +")";
    }
}
