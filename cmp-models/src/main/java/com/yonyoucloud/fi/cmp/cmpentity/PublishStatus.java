package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 发布状态 枚举*
 * @author xuxbo
 * @date 2024/6/28 9:36
 */
public enum PublishStatus {

    Effective( "已生效" , (short)1),
    Voided( "已作废" ,(short)2);
    private String name;
    private short value;


    private PublishStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, PaymentType> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, PaymentType>();
        PaymentType[] items = PaymentType.values();
        for (PaymentType item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static PaymentType find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
