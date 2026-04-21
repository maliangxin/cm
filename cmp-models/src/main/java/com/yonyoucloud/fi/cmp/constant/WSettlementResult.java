package com.yonyoucloud.fi.cmp.constant;

import java.util.HashMap;

public enum WSettlementResult {


    AllSuccess(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418032A","全部成功") /* "全部成功" */, (short)0),
    PartSuccess(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418032B","部分成功") /* "部分成功" */, (short)1),
    AllFaital(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418032C","全部失败") /* "全部失败" */, (short)2),
    ISNULL(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180328","空") /* "空" */, (short)3);

    private String name;
    private short value;

    WSettlementResult(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
    }

    public void setName(String name) {
        this.name = name;
    }

    public short getValue() {
        return value;
    }

    public void setValue(short value) {
        this.value = value;
    }

    private static HashMap<Short, WSettlementResult> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, WSettlementResult>();
        WSettlementResult[] items = WSettlementResult.values();
        for (WSettlementResult item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static WSettlementResult find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
