package com.yonyoucloud.fi.cmp.weekday;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author shiqhs
 * @date 2022/04/30
 * @description 顺序表类
 * @param <T>
 */
public class SeqList<T> {

    /**
     * 顺序表元素个数(长度)
     */
    protected int n;

    /**
     * 对象数组存储顺序表的数据元素
     */
    protected Object[] element;

    /**
     * element数组容量的最小值
     */
    private static final int MIN_CAPACITY = 16;

    public SeqList(int length) {
        if (length < MIN_CAPACITY) {
            length = MIN_CAPACITY;
        }
        this.element = new Object[length];
        this.n = 0;
    }

    public SeqList() {
        this(MIN_CAPACITY);
    }

    /**
     * 顺序表的浅拷贝
     * @param list 原顺序表
     */
//    public SeqList(SeqList<T> list) {
//        this.n = list.n;
//        this.element = list.element;
//    }

    /**
     * 顺序表的深拷贝
     * @param list 原顺序表
     */
    public SeqList(SeqList<? extends T> list) {
        this.element = new Object[list.element.length];
        for (int i = 0; i < list.n; i++) {
            this.element[i] = list.element[i];
        }
        this.n = list.n;
    }

    /**
     * 创建2倍values数组容量的空表,若 values==null,则抛出异常
     * @param values 元素数组
     */
    public SeqList(T[] values) {
        this(values.length * 2);
        for (T value : values) {
            if (value != null) {
                this.element[this.n++] = value;
            }
        }
    }

    /**
     * 判断是否空，若为空，返回true
     * @return 是否空
     */
    public boolean isEmpty() {
        return this.n == 0;
    }

    /**
     * 返回元素个数
     * @return 元素个数
     */
    public int size() {
        return this.n;
    }

    /**
     * 若 0≤i<n,则返回第 i 个元素,否则返回 null
     * @param i 第 i 个元素
     * @return 元素
     */
    public T get(int i) {
        if (i >= 0 && i < this.n) {
            return (T) this.element[i];
        }
        return null;
    }

    /**
     * 若 0≤i<n 且 x≠null,则设置第 i 个元素为x,否则抛出序号越界异常或空对象异常
     * @param i 第 i 个位置
     * @param x 要设置的元素
     */
    public void set(int i, T x) {
        if (x == null) {
            throw new NullPointerException("x == null");
        }
        if (i >= 0 && i < this.n) {
            this.element[i] = x;
        } else {
            throw new IndexOutOfBoundsException(i+"");
        }
    }

    /**
     * 插入元素
     * @param i 插入位置序号
     * @param x 元素
     * @return 插入位置序号
     */
    public int insert(int i, T x) {
        if (x == null) {
            return -1;
        }
        // 插入位置 i 容错, 插入在最前(头插入)
        if (i < 0) {
            i = 0;
        }
        // 插入在最后（尾插入）
        if (i > this.n) {
            i = this.n;
        }
        // 数组变量引用赋值,source也引用element数组
        Object[] source = this.element;
        // 若数组满,则扩充顺序表的数组容量
        if (this.n == element.length) {
            // 2倍扩容
            this.element = new Object[source.length * 2];
            // 复制当前数组前 n - 1 个元素
            // 复制数组元素,传递对象引用
            System.arraycopy(source, 0, this.element, 0, i);
        }
        // 从 i 开始至表尾的元素向后移动,次序从后向前
        if (this.n - i >= 0) {
            System.arraycopy(source, i, this.element, i + 1, this.n - i);
        }
        this.element[i] = x;
        this.n++;
        return i;
    }

    /**
     * 顺序表尾插入 x 元素
     * @param x 元素
     * @return 插入位置序号
     */
    public int insert(T x) {
        return this.insert(this.n, x);
    }

    /**
     * 删除第 i 个元素, 0≤i<n, 返回被删除元素,若 i 越界,则返回 null
     * @param i 第 i 个元素序号
     * @return 第 i 个元素
     */
    public T remove(int i) {
        if (i >= 0 && i < this.n) {
            T x = (T) this.element[i];
            for (int j = i; j < this.n - 1;j++) {
                this.element[j] = this.element[j +1];
            }
            this.element[this.n - 1] = null;
            this.n--;
            return x;
        }
        return null;
    }

    /**
     * 查找
     * @param key 元素
     * @return 元素序号
     */
    public int search(T key) {
        for (int i = 0; i < this.n; i++) {
            if (key.equals(this.element[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 删除元素
     * @param key 元素
     * @return 元素
     */
    public T remove(T key) {
        int i = this.search(key);
        return this.remove(i);
    }

    /**
     * 删除所有元素
     */
    public void clear() {
        this.n = 0;
    }

    @Override
    public boolean equals(Object obj) {
        // this 和 obj 引用同一个顺序表实例,则相等
        if (this == obj) {
            return true;
        }
        if (obj instanceof SeqList<?>) {
            SeqList<T> list = (SeqList<T>) obj;
            if (this.n == list.n) {
                for (int i = 0; i < this.n; i++) {
                    if (!(this.element[i].equals(list.element[i]))) {
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(n);
        result = 31 * result + Arrays.hashCode(element);
        return result;
    }

    @Override
    public String toString() {
        // 返回类名
        StringBuilder str = new StringBuilder(this.getClass().getName() + "(");
        if (this.n > 0) {
            str.append(this.element[0].toString());
        }
        for (int i = 1; i < this.n; i++) {
            str.append(", ").append(this.element[i].toString());
        }
        return str + ")";
    }
}
