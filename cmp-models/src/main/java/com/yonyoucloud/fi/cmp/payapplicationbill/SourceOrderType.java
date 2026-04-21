package com.yonyoucloud.fi.cmp.payapplicationbill;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * <h1>源头单据类型</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-10-15 9:29
 */
public enum SourceOrderType {
    PurchaseOrder(/*MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012585") */ "采购订单", (short) 0),
    OutsourcedOrder(/*MessageUtils.getMessage("P_YS_PF_METADATA_0001039706")*/ "委外订单", (short) 1),
    MattersOap(/*MessageUtils.getMessage("P_YS_PF_GZTSYS_0000013168") */ "应付事项", (short) 2),
    Free(/*MessageUtils.getMessage("P_YS_HR_HRJQ_0000031143") */ "无" , (short) 3),
    MattersOapInit(/*MessageUtils.getMessage("P_YS_PF_GZTSYS_0000013438") */ "应付事项期初", (short) 4);

    private static HashMap<Short, SourceOrderType> map = null;
    private String name;
    private short value;

    SourceOrderType(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        SourceOrderType[] items = SourceOrderType.values();
        for (SourceOrderType item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static SourceOrderType find(Number value) {
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
