package com.yonyoucloud.fi.cmp.internaltransferprotocol.enums;

import lombok.Getter;

import java.util.HashMap;

/**
 * <h1>分摊方式</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-09-08 14:09
 */
public enum ApportionmentMethod {

    PRORATED("UID:P_CM-BE_1A033EB805B8001B", (short) 0),
    FIXED_AMOUNT("UID:P_CM-BE_1A033EB805B8001C", (short) 1);

    private static HashMap<Short, ApportionmentMethod> map = null;
    private final String name;
    @Getter
    private final short value;

    ApportionmentMethod(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private static synchronized void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        ApportionmentMethod[] items = ApportionmentMethod.values();
        for (ApportionmentMethod item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static ApportionmentMethod find(Number value) {
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

}
