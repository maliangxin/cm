package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 智能审核执行结果状态
 */
public enum BusinessPart {
    save("保存", (short) 0),
    submit("提交", (short) 1);

    private static HashMap<Short, BusinessPart> map = null;
    private String name;
    private short value;

    private BusinessPart(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, BusinessPart>();
        BusinessPart[] items = BusinessPart.values();
        for (BusinessPart item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static BusinessPart find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name, name) /* name */;
    }

    public short getValue() {
        return value;
    }
}
