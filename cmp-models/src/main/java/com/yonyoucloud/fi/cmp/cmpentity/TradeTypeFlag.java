package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 保证金台账--交易类型标识枚举*
 * @author xuxbo
 * @date 2023/2/13 13:44
 */
public enum TradeTypeFlag {
    MarginDeposit("保证金存入",(short)0),
    MarginWithdrawal("保证金支取",(short)1);

    private String name;
    private short value;

    private TradeTypeFlag(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, TradeTypeFlag> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, TradeTypeFlag>();
        TradeTypeFlag[] items = TradeTypeFlag.values();
        for (TradeTypeFlag item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static TradeTypeFlag find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}

