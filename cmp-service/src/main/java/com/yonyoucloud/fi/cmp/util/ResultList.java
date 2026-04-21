package com.yonyoucloud.fi.cmp.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName ResultList * @Description 后端分页工具类
 * @Author yuanyhui
 * @Date 15:08 2022/11/1
 * @Version 1.0
 **/
public class ResultList<T> {

    private static final long serialVersionUID = 1L;
    private final List<T> list = new ArrayList();
    private Integer total;

    public ResultList() {
    }

    public ResultList(int total, List<T> list) {
        this.list.addAll(list);
        this.total = total;
    }

    public List<T> getList() {
        return this.list;
    }

    public void add(T entity) {
        this.list.add(entity);
    }

    public void setList(List<T> list) {
        this.list.addAll(list);
    }

    public Integer getTotal() {
        return this.total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "ResultList(list=" + this.getList() + ", total=" + this.getTotal() + ")";
    }
}
