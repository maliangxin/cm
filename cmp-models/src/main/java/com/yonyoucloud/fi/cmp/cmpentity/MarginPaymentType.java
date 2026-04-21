package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 保证金台账--保证金支付类型枚举*
 * @author xuxbo
 * @date 2023/2/13 13:44
 */
public enum MarginPaymentType {
    SameName("同名账户划转",(short)0),
    ThirdParty("第三方账户支付",(short)1),
    SpecialAccount("专户资金冻结",(short)2);

    private String name;
    private short value;

    private MarginPaymentType(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, MarginPaymentType> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, MarginPaymentType>();
        MarginPaymentType[] items = MarginPaymentType.values();
        for (MarginPaymentType item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static MarginPaymentType find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}

