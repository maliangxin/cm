package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 对方类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum MarginOppositeType {
    Customer("客户", (short) 1),
    Supplier("供应商", (short) 2),
    Other("其他", (short) 3),
    OwnOrg("内部单位", (short) 4),
    CapBizObj("资金业务对象", (short) 5);

    private static HashMap<Short, MarginOppositeType> map = null;
    private String name;
    private short value;

    private MarginOppositeType(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, MarginOppositeType>();
        MarginOppositeType[] items = MarginOppositeType.values();
        for (MarginOppositeType item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static MarginOppositeType find(Number value) {
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
