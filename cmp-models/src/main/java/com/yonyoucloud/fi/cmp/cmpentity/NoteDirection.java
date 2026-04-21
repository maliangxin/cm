package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

public enum NoteDirection {

    CollectionNote(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_CTM_CM-BE_0001506031") /* "应收票据" */, (short)1),
    PaymentNote(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_CTM_CM-BE_0001506030") /* "应付票据" */, (short)2),
    PaymentCheckNo(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","开出票证") /* "开出票证" */, (short) 3),
    CollectionCheckNo(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","收到票证") /* "收到票证" */, (short) 4);
    private String name;
    private short value;

    private NoteDirection(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, NoteDirection> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, NoteDirection>();
        NoteDirection[] items = NoteDirection.values();
        for (NoteDirection item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static NoteDirection find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}