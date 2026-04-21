package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 保证金台账--付款结算模式枚举*
 *
 * @author xuxbo
 * @date 2023/2/13 13:44
 */
public enum PaymentSettlemode {
    ActiveSettlement("主动结算", (short) 0),
    CounterpartyDeduction("被动扣款", (short) 1);

    private static HashMap<Short, PaymentSettlemode> map = null;
    private String name;
    private short value;

    private PaymentSettlemode(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, PaymentSettlemode>();
        PaymentSettlemode[] items = PaymentSettlemode.values();
        for (PaymentSettlemode item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static PaymentSettlemode find(Number value) {
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

