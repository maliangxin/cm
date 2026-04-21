package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * @Author ningff
 * @Date 2024/5/21 16:55
 * @DESCRIPTION
 */
public enum GenerationType {
    AddManually ("手工新增",(short)1),
    ManualInput ("导入",(short)2);

    private String name;
    private short value;

    private GenerationType(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, GenerateType> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, GenerateType>();
        GenerateType[] items = GenerateType.values();
        for (GenerateType item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static GenerateType find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
