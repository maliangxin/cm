package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 智能审核执行结果状态
 */
public enum ResultState {
    RigidNotPassed("刚性未通过", (short) 1),
    FlexibilityNotPassed("柔性未通过", (short) 2),
    AllPassed("全部通过", (short) 3);

    private static HashMap<Short, ResultState> map = null;
    private String name;
    private short value;

    private ResultState(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, ResultState>();
        ResultState[] items = ResultState.values();
        for (ResultState item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static ResultState find(Number value) {
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
