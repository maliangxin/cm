
package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 相同数据匹配方式类型
 *
 * @author wanxbo@yonyou.com
 * @version 1.0
 */
public enum SamedataMatchMethodType {
    NoMatching("不匹配", (short)0),
    NearestDateMatching("最近日期匹配", (short)1),
    FarthestDateMatching("最远日期匹配", (short)2),
    RandomMatching("随机匹配", (short)3);

    private String name;
    private short value;

    private SamedataMatchMethodType(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, SamedataMatchMethodType> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, SamedataMatchMethodType>();
        SamedataMatchMethodType[] items = SamedataMatchMethodType.values();
        for (SamedataMatchMethodType item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static SamedataMatchMethodType find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}