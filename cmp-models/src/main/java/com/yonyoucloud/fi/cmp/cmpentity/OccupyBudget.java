package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 是否占预算
 */
public enum OccupyBudget {
    UnOccupy("未占用", (short) 0),
    PreSuccess("预占成功", (short) 1),
    ActualSuccess("实占成功", (short) 2);

    private static HashMap<Short, OccupyBudget> map = null;
    private String name;
    private short value;

    private OccupyBudget(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, OccupyBudget>();
        OccupyBudget[] items = OccupyBudget.values();
        for (OccupyBudget item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static OccupyBudget find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
    }

    public short getValue() {
        return value;
    }
}
