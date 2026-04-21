package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 分配财务人员状态 枚举
 * @author xujhn
 * @date 2022-10-20
 */
public enum DispatchFinanceStatus {
    Not(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933182") /* "未分配财务人员" */, (short)0),
    Auto(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933155") /* "自动分配财务人员" */, (short)1),
    Manual(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933149") /* "手工分配财务人员" */, (short)2);


    private String name;
    private short value;

    private DispatchFinanceStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, DispatchFinanceStatus> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, DispatchFinanceStatus>();
        DispatchFinanceStatus[] items = DispatchFinanceStatus.values();
        for (DispatchFinanceStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static DispatchFinanceStatus find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
