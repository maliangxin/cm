package com.yonyoucloud.fi.cmp.arap;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;

import java.util.HashMap;

public enum AuditStatus {
    Complete("UID:P_ARAP-FE_180721A604C800D2", "已审批", (short)1),
    Incomplete("UID:P_ARAP-FE_180721A604C800D3", "未审批", (short)2);

    private String name;
    private short value;
    private String defaultName;
    private static HashMap<Short, AuditStatus> map = null;

    private AuditStatus(String name, String defaultName, short value) {
        this.name = name;
        this.defaultName = defaultName;
        this.value = value;
    }

    public String getName() {
        return InternationalUtils.getMessageWithDefault(this.name, this.defaultName);
    }

    public short getValue() {
        return this.value;
    }

    private static synchronized void initMap() {
        if (map == null) {
            map = new HashMap();
            AuditStatus[] items = values();
            AuditStatus[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                AuditStatus item = var1[var3];
                map.put(item.getValue(), item);
            }

        }
    }

    public static AuditStatus find(Number value) {
        if (value == null) {
            return null;
        } else {
            if (map == null) {
                initMap();
            }

            return (AuditStatus)map.get(value.shortValue());
        }
    }
}
