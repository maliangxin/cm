package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

public enum EnableStatus {

    Saved(MessageUtils.getMessage("P_YS_PF_GZTTMP_0000073615") /* "已保存" */, (short)0),
    Enabled(MessageUtils.getMessage("P_YS_FED_FW_0000021723") /* "已启用" */, (short)1),
    Disabled(MessageUtils.getMessage("P_YS_FED_FW_0000021703") /* "已停用" */, (short)2);

    private String name;
    private short value;



    private EnableStatus(String name, short value){
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, EnableStatus> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, EnableStatus>();
        EnableStatus[] items = EnableStatus.values();
        for (EnableStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static EnableStatus find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
