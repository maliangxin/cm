package com.yonyoucloud.fi.cmp.enums;

import java.util.HashMap;

/**
 * 流水自动辨识匹配 适用对象新枚举
 */
public enum ApplyObjectNewEnum {
    BANKDETAIL("银行流水", (short)1),
    CLAIM("认领单", (short)2),
    TICKET("收票登记", (short)3);

    private String name;
    private short value;

    private ApplyObjectNewEnum(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, ApplyObjectNewEnum> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, ApplyObjectNewEnum>();
        ApplyObjectNewEnum[] items = ApplyObjectNewEnum.values();
        for (ApplyObjectNewEnum item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static ApplyObjectNewEnum find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}