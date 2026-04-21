package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * @description: 收付方向
 * @author: wanxbo@yonyou.com
 * @date: 2024/6/18 11:12
 */

public enum DcFlagEnum {
    Debit("支出", (short)1),
    Credit("收入", (short)2),
    All("所有", (short)3),
    ;

    private String name;
    private short value;

    private DcFlagEnum(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, DcFlagEnum> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, DcFlagEnum>();
        DcFlagEnum[] items = DcFlagEnum.values();
        for (DcFlagEnum item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static DcFlagEnum find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
