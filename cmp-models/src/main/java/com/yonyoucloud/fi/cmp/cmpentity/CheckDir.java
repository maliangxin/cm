package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 支票方向
 *
 * @author pd
 * @version 1.0
 */
public enum CheckDir {
    Collect(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_18108E6604B81B53","收票") /* "收票" */, (short) 1),
    Pay(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_18108E6604B81B55","付票") /* "付票" */, (short) 2);

    private static HashMap<Short, CheckDir> map = null;
    private String name;
    private short value;

    private CheckDir(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, CheckDir>();
        CheckDir[] items = CheckDir.values();
        for (CheckDir item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static CheckDir find(Number value) {
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
