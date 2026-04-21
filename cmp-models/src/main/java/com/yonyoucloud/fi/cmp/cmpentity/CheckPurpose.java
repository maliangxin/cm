package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 支票用途
 *
 * @author pd
 * @version 1.0
 */
public enum CheckPurpose {
    VirtualToBank(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B8001E","提现") /* "提现" */, (short) 0),
    BankToVirtual(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B8001F","转账") /* "转账" */, (short) 1);


    private static HashMap<Short, CheckPurpose> map = null;
    private String name;
    private short value;

    private CheckPurpose(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, CheckPurpose>();
        CheckPurpose[] items = CheckPurpose.values();
        for (CheckPurpose item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static CheckPurpose find(Number value) {
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
