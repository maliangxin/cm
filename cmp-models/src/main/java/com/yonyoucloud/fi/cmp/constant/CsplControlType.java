package com.yonyoucloud.fi.cmp.constant;

import java.util.HashMap;

public enum CsplControlType {

    NOCONTROL(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180443","不控制"), 0),
    CONTROLBYACC(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180444","按资金组织控制"), 1),
    CONTROLBYDEBT(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180445","按部门控制") , 2);

    private String key;
    private Integer value;

    private CsplControlType(String key, Integer value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Integer getValue() {
        return value;
    }

    private static HashMap<Integer, CsplControlType> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Integer, CsplControlType>();
        CsplControlType[] items = CsplControlType.values();
        for (CsplControlType item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static CsplControlType find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}

