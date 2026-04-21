package com.yonyoucloud.fi.cmp.enums;



import java.util.HashMap;

/**
 * 被追索方/追索方
 *
 * @author u
 * @version 1.0
 */
public enum Dftype {
    /**
     * "客户"
     */
    Merchant("UID:P_DRFT-UI_1C74234A04680446", (short) 1),
    /**
     * "供应商"
     */
    Supplier("UID:P_DRFT-UI_1C74234A04680448", (short) 2),
    /**
     * "资金伙伴"
     */
    Funbusobj("UID:P_DRFT-UI_1C74234A0468044A", (short) 3);

    private String name;
    private short value;

    private Dftype(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, Dftype> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, Dftype>();
        Dftype[] items = Dftype.values();
        for (Dftype item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static Dftype find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
