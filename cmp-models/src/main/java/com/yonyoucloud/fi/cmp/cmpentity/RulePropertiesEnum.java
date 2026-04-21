package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * @description: 规则属性枚举
 * @author: wanxbo@yonyou.com
 * @date: 2024/6/18 11:27
 */

public enum RulePropertiesEnum {
    Match("匹配类", (short)1),
    Identify("辨识类", (short)2),
    ;

    private String name;
    private short value;

    private RulePropertiesEnum(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, RulePropertiesEnum> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, RulePropertiesEnum>();
        RulePropertiesEnum[] items = RulePropertiesEnum.values();
        for (RulePropertiesEnum item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static RulePropertiesEnum find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
