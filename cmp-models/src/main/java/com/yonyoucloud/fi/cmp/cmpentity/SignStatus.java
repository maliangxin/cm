package com.yonyoucloud.fi.cmp.cmpentity;



import java.util.HashMap;

public enum SignStatus {

    SignSuccess("UID:P_CM-BE_18E935520550000D", (short) 1),
    SignFail("UID:P_CM-BE_18E935520550000E", (short) 2),
    UnSign("UID:P_CM-BE_18E935520550000F", (short) 3),
    UnSupported("UID:P_CM-BE_18E9355205500010", (short) 0);
    private static HashMap<Short, SignStatus> map = null;
    private String name;
    private short value;

    private SignStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, SignStatus>();
        SignStatus[] items = SignStatus.values();
        for (SignStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static SignStatus find(Number value) {
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
