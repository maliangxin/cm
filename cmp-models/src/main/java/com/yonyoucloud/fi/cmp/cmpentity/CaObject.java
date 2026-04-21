package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 收付款对象枚举
 *
 * @author u
 * @version 1.0
 */
public enum CaObject {
    Customer(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041800DE","客户") /* "客户" */, (short) 1),
    Supplier(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041800DF","供应商") /* "供应商" */, (short) 2),
    Employee(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041800E0","员工") /* "员工" */, (short) 3),
    Other(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041800E1","其他") /* "其他" */, (short) 4),
    CapBizObj(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041800DC","资金业务对象") /* "资金业务对象" */, (short) 5),
    InnerUnit(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041800F6","内部单位") /* "内部单位" */, (short) 6);

    private static HashMap<Short, CaObject> map = null;
    private String name;
    private short value;

    private CaObject(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, CaObject>();
        CaObject[] items = CaObject.values();
        for (CaObject item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static CaObject find(Number value) {
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
