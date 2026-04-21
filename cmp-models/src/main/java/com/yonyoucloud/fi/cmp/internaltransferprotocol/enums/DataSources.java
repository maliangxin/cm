package com.yonyoucloud.fi.cmp.internaltransferprotocol.enums;

import lombok.Getter;

import java.util.HashMap;

/**
 * <h1>内转协议-数据来源</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-09-19 9:54
 */
public enum DataSources {
    CONSTRUCTION_CLOUD("UID:P_CM-BE_1A033EB805B80018", (short) 0),
    THIRD_PARTY("UID:P_CM-BE_1A033EB805B80016", (short) 1),
    MANUALLY_ADD("UID:P_CM-BE_1A033EB805B80017", (short) 2);

    private static HashMap<Short, DataSources> map = null;
    private final String name;
    @Getter
    private final short value;


    DataSources(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private static synchronized void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        DataSources[] items = DataSources.values();
        for (DataSources item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static DataSources find(Number value) {
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
