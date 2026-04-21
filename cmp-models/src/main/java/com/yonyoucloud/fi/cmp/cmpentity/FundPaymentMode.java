package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 资金付款 付款模式
 */
public enum FundPaymentMode {
    VoluntaryPayment("主动付款", (short) 0),
    CounterpartyDeduction("对方扣款", (short) 1);

    private static HashMap<Short, FundPaymentMode> map = null;
    private String name;
    private short value;

    private FundPaymentMode(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, FundPaymentMode>();
        FundPaymentMode[] items = FundPaymentMode.values();
        for (FundPaymentMode item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static FundPaymentMode find(Number value) {
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
