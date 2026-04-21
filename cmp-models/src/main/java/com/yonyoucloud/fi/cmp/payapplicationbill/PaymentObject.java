package com.yonyoucloud.fi.cmp.payapplicationbill;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 收款对象枚举
 *
 * @author u
 * @version 1.0
 */
public enum PaymentObject {
    Customer(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012583") /* "客户" */, (short)1),
    Supplier(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012772") /* "供应商" */, (short)2),
    Employee(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012657") /* "员工" */, (short)3),
    Other(MessageUtils.getMessage(MessageUtils.getMessage("P_YS_PF_PROCENTER_0000023062") /* "其他" */), (short)4);

    private String name;
    private short value;

    private PaymentObject(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, PaymentObject> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, PaymentObject>();
        PaymentObject[] items = PaymentObject.values();
        for (PaymentObject item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static PaymentObject find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
