package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 关联结息单枚举
 *
 * @author u
 * @version 1.0
 */
public enum Relatedinterest {
    relatedUnAssociated("未关联", (short) 0),
    relatedAssociated("已关联", (short) 1);

    private static HashMap<Short, Relatedinterest> map = null;
    private String name;
    private short value;

    private Relatedinterest(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, Relatedinterest>();
        Relatedinterest[] items = Relatedinterest.values();
        for (Relatedinterest item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static Relatedinterest find(Number value) {
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
