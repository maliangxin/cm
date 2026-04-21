package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 认领状态枚举*
 *
 * @author xuxbo
 * @date 2023/2/13 13:44
 */
public enum RecheckStatus {
    NotReviewed("待复核", (short) 0),
    Reviewed("认领成功", (short) 1),
    Saved("已保存", (short)2),
    Submited("审批中", (short)3),
    Terminated("流程终止", (short)4),
    Rejected("已驳回", (short)5);

    private static HashMap<Short, RecheckStatus> map = null;
    private String name;
    private short value;

    private RecheckStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, RecheckStatus>();
        RecheckStatus[] items = RecheckStatus.values();
        for (RecheckStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static RecheckStatus find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
    }

    public short getValue() {
        return value;
    }
}

