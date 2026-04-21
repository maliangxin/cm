package com.yonyoucloud.fi.cmp.payapplicationbill;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;

import java.util.HashMap;

/**
 * <h1>审批状态</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-10-15 9:33
 */
public enum ApprovalStatus {
    Free(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00062", "无") /* "无" */, (short) 1),
    Approving(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0005F", "待审批") /* "待审批" */, (short) 2),
    ApprovedPass(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00060", "已审批") /* "已审批" */, (short) 3);

    private static HashMap<Short, ApprovalStatus> map = null;
    private String name;
    private short value;

    ApprovalStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        ApprovalStatus[] items = ApprovalStatus.values();
        for (ApprovalStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static ApprovalStatus find(Number value) {
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

    public void setName(String name) {
        this.name = name;
    }

    public short getValue() {
        return value;
    }

    public void setValue(short value) {
        this.value = value;
    }


    public static String getName(short value) {
        if (map == null) {
            initMap();
        }

        if (1==value) {
            return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00062", "无");
        } else if (2==value) {
            return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0005F", "待审批");
        } else if (3==value) {
            return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00060", "已审批");
        } else {
            return map.get(value) == null ? null : ((ApprovalStatus)map.get(value)).getName();
        }
    }
}
