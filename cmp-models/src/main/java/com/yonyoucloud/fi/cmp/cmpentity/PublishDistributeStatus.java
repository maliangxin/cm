package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 分配业务人员状态 枚举
 * @author xujhn
 * @date 2022-10-8
 */
public enum PublishDistributeStatus {
    Not(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933122") /* "未分配业务人员" */, (short)0),
    Auto(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933215") /* "自动分配业务人员" */, (short)1),
    Manual(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933206") /* "手工分配业务人员" */, (short)2);


    private String name;
    private short value;

    private PublishDistributeStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, PublishDistributeStatus> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, PublishDistributeStatus>();
        PublishDistributeStatus[] items = PublishDistributeStatus.values();
        for (PublishDistributeStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static PublishDistributeStatus find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
