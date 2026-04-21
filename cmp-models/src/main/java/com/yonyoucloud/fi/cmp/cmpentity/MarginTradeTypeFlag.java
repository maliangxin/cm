package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 支付和收到保证金台账--交易类型标识枚举*
 *
 * @author xuxbo
 * @date 2023/8/19 11:44
 */
public enum MarginTradeTypeFlag {
    MarginPayment("支付保证金", (short) 0),
    MarginWithdraw("取回保证金", (short) 1),
    MarginReceive("收到保证金", (short) 2),
    MarginReturn("退还保证金", (short) 3);

    private static HashMap<Short, MarginTradeTypeFlag> map = null;
    private String name;
    private short value;

    private MarginTradeTypeFlag(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, MarginTradeTypeFlag>();
        MarginTradeTypeFlag[] items = MarginTradeTypeFlag.values();
        for (MarginTradeTypeFlag item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static MarginTradeTypeFlag find(Number value) {
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

