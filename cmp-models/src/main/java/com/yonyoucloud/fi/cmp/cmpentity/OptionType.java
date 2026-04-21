package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 *变更类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum OptionType {
    Create("新建", (short) 0),
    Update("变更", (short) 1);

    private static HashMap<Short, OptionType> map = null;
    private String name;
    private short value;

    private OptionType(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, OptionType>();
        OptionType[] items = OptionType.values();
        for (OptionType item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static OptionType find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }
}
