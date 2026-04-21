package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 回单关联状态
 *
 * @author u
 * @version 1.0
 */
public enum ReceiptassociationStatus {
    AutomaticAssociated(MessageUtils.getMessage("UID:P_CM-UI_18108E6604B81E7F") /* "自动关联" */, (short) 0),
    ManualAssociated(MessageUtils.getMessage("UID:P_CM-UI_18108E6604B824E0") /* "手工关联" */, (short) 1),
    NoAssociated(MessageUtils.getMessage("P_YS_SD_SDOC-UI_0001178268") /* "未关联" */, (short) 4);

    private static HashMap<Short, ReceiptassociationStatus> map = null;
    private String name;
    private short value;

    private ReceiptassociationStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, ReceiptassociationStatus>();
        ReceiptassociationStatus[] items = ReceiptassociationStatus.values();
        for (ReceiptassociationStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static ReceiptassociationStatus find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }
}
