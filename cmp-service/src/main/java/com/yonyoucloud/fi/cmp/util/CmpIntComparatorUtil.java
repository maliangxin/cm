package com.yonyoucloud.fi.cmp.util;

import java.util.Comparator;

/**
 * @Description: 集合排序工具类
 * @Author: gengrong
 * @createTime: 2022/10/5
 * @version: 1.0
 */
public class CmpIntComparatorUtil implements Comparator<Integer> {

    @Override
    public int compare(Integer o1, Integer o2) {
        return o1.compareTo(o2);
    }
}
