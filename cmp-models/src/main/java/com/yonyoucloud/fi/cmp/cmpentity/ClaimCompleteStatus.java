package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 我的认领-认领完结状态
 *
 * @author u
 * @version 1.0
 */
public enum ClaimCompleteStatus {
    Uncompleted("未完结", (short) 0),
    Completed("已完结", (short) 1);

    private static HashMap<Short, ClaimCompleteStatus> map = null;
    private String name;
    private short value;

    private ClaimCompleteStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, ClaimCompleteStatus>();
        ClaimCompleteStatus[] items = ClaimCompleteStatus.values();
        for (ClaimCompleteStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static ClaimCompleteStatus find(Number value) {
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
