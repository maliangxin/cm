package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2024/6/18 16:14
 */

public enum MatchMethodEnum {
    SimpleMapping("简单映射", (short)1),
    FuzzyMatching("模糊匹配", (short)2),
    ;

    private String name;
    private short value;

    private MatchMethodEnum(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, MatchMethodEnum> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, MatchMethodEnum>();
        MatchMethodEnum[] items = MatchMethodEnum.values();
        for (MatchMethodEnum item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static MatchMethodEnum find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
