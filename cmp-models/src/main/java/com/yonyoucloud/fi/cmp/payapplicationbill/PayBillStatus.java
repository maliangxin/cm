package com.yonyoucloud.fi.cmp.payapplicationbill;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * <h1>单据状态</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-10-15 9:29
 */
public enum PayBillStatus {
    Open(MessageUtils.getMessage("P_YS_SCM_PU_0000028170") /* "开立" */, (short) 1),
    Auditing(MessageUtils.getMessage("P_YS_SD_UDHBN_0000032662") /* "待审核" */, (short) 2),
    PendingApproval(MessageUtils.getMessage("P_YS_SCM_PU_0000028093") /* "已审核" */, (short) 3),
    PendingPayment(MessageUtils.getMessage("P_YS_SD_UDHBI_0000032151") /* "待付款" */, (short) 4),
    PartialPayment(MessageUtils.getMessage("P_YS_SD_UDHBI_0000032190") /* "部分付款" */, (short) 5),
    PaymentCompleted(MessageUtils.getMessage("P_YS_SCM_PU_0000027706") /* "付款完成" */, (short) 6);

    private static HashMap<Short, PayBillStatus> map = null;
    private String name;
    private short value;

    PayBillStatus(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        PayBillStatus[] items = PayBillStatus.values();
        for (PayBillStatus item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static PayBillStatus find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }

    public String getName() {
        return name;
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
}
