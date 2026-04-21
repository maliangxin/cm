package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * @description: 流水支持方式
 * @date: 2024/6/18 16:14
 */

public enum ReconciliationSupportWayEnum {
    GENERATION_OR_ASSOCIATION("生单或关联", (short)0),
    ONLY_ASSOCIATION("仅关联", (short)1),
    ;

    private String name;
    private short value;

    ReconciliationSupportWayEnum(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, ReconciliationSupportWayEnum> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, ReconciliationSupportWayEnum>();
        ReconciliationSupportWayEnum[] items = ReconciliationSupportWayEnum.values();
        for (ReconciliationSupportWayEnum item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static ReconciliationSupportWayEnum find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}