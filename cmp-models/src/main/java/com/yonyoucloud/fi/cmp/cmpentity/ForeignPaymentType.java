package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 外汇付款--外汇支付方式枚举*
 * @author xuxbo
 */
public enum ForeignPaymentType {
    SpotExchangePayment("现汇支付",(short)1),
    ForeignExchangePurchasePayment("购汇支付",(short)2),
    OtherPayments("其他支付",(short)3);

    private String name;
    private short value;

    private ForeignPaymentType(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name, name) /* name */;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, ForeignPaymentType> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, ForeignPaymentType>();
        ForeignPaymentType[] items = ForeignPaymentType.values();
        for (ForeignPaymentType item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static ForeignPaymentType find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}

