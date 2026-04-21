package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 我的认领-认领完结方式
 * 银行流水处理-流水认领处理方式
 * aa_enum：cmp_ClaimCompleteType
 *
 * @author u
 * @version 1.0
 */
public enum ClaimCompleteType {
    RecePayAssociated("收付单据关联", (short) 1),
    RecePayGen("收付单据生单", (short) 2),
    BusinessAssociated("业务凭据关联即完结", (short) 3),
    THIRD("异构系统处理", (short) 4),
    NoProcess("无需处理", (short) 5),
    ;

    private static HashMap<Short, ClaimCompleteType> map = null;
    private String name;
    private short value;

    private ClaimCompleteType(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, ClaimCompleteType>();
        ClaimCompleteType[] items = ClaimCompleteType.values();
        for (ClaimCompleteType item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static ClaimCompleteType find(Number value) {
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
