package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 匹配方向类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum DailySettlementControl {
    OutOfControl("不控制", (short) 0),
    AccruaAfterSettlement("先日结后预提", (short) 1);

    private static HashMap<Short, DailySettlementControl> map = null;
    private String name;
    private short value;

    private DailySettlementControl(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, DailySettlementControl>();
        DailySettlementControl[] items = DailySettlementControl.values();
        for (DailySettlementControl item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static DailySettlementControl find(Number value) {
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
