package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 账户收支明细-单据状态（剔除状态）
 *
 * @author jpk
 * @date 2023/10/23 13:44
 */
public enum CullingStatus {
    Excluding("剔除中", (short) 0),
    ExclusionCompleted("已剔除", (short) 1);

    private static HashMap<Short, CullingStatus> map = null;
    private String name;
    private short value;

    private CullingStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, CullingStatus>();
        CullingStatus[] items = CullingStatus.values();
        for (CullingStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static CullingStatus find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name, name) /* name */;
    }

    public short getValue() {
        return value;
    }
}

