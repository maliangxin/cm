package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 保证金台账--收付类型枚举*
 * @author xuxbo
 * @date 2023/2/13 13:44
 */
public enum PaymentType {
    FundCollection("收款",(short)0),
    FundPayment("付款",(short)1);

    private String name;
    private short value;

    private PaymentType(String name, short value) {
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

