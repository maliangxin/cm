package com.yonyoucloud.fi.cmp.payapplicationbill;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;
/**
 * <h1>付款申请单关闭状态</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021-07-27 9:37
 */
public enum CloseStatus {
    Normal(MessageUtils.getMessage("P_YS_OA_XTLCZX_0000030776") /* "正常" */, (short)0),
    Closed(MessageUtils.getMessage("P_YS_FED_EXAMP_0000020183") /* "关闭" */, (short)1);

    private String name;
    private short value;

    private CloseStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, CloseStatus> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, CloseStatus>();
        CloseStatus[] items = CloseStatus.values();
        for (CloseStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static CloseStatus find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
