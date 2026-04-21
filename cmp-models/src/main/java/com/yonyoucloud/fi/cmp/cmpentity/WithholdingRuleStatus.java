package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 匹配方向类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum WithholdingRuleStatus {
    Deactivate("停用", (short) 0),
    Enable("启用", (short) 1),
    Tobeset("待设置", (short) 2);

    private static HashMap<Short, WithholdingRuleStatus> map = null;
    private String name;
    private short value;

    private WithholdingRuleStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, WithholdingRuleStatus>();
        WithholdingRuleStatus[] items = WithholdingRuleStatus.values();
        for (WithholdingRuleStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static WithholdingRuleStatus find(Number value) {
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
