package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 支票簿盘点结果
 *
 * @author pd
 * @version 1.0
 */
public enum CheckResultStatus {
    NoInventory(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1830F41204E00007","待盘点") /* "待盘点" */, (short) 1),
    InventoryLoss(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_18108E6604B81B95","盘亏") /* "盘亏" */, (short) 2),
    ToConform(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_18108E6604B81B9F","相符") /* "相符" */, (short) 3),
    InventoryProfit(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_18108E6604B81B9B","盘盈") /* "盘盈" */, (short) 4);

    private static HashMap<Short, CheckResultStatus> map = null;
    private String name;
    private short value;

    private CheckResultStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, CheckResultStatus>();
        CheckResultStatus[] items = CheckResultStatus.values();
        for (CheckResultStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static CheckResultStatus find(Number value) {
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
