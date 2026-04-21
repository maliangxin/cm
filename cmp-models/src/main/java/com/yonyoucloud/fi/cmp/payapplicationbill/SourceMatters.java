package com.yonyoucloud.fi.cmp.payapplicationbill;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * <h1>来源事项</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-10-15 9:27
 */
public enum SourceMatters {

    MattersDue(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000013168") /* "应付事项" */, (short) 1),
    ReceiveMatters(MessageUtils.getMessage("P_YS_FI_YYFI-UI_0001145930") /* "收款事项" */, (short) 2),
    RubricReceivable(MessageUtils.getMessage("P_YS_SD_UDHLIB_0000133796") /* "红字应收" */, (short) 3),
    PurchaseOrderMaterial(MessageUtils.getMessage("P_YS_CTM_CM-BE_1483922848803717131") /* "采购订单-物料" */, (short) 4),
    ManualInput(MessageUtils.getMessage("P_YS_SCM_PU_0000028404") /* "手工录入" */, (short) 5),
    PurchaseContract(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012789") /* "采购合同" */, (short) 6),
    PurchaseOrderPlan(MessageUtils.getMessage("P_YS_CTM_CM-BE_1483922848803717128") /* "采购订单-计划" */, (short) 7),
    OutsourcingOrderAll(MessageUtils.getMessage("P_YS_CTM_CM-BE_1502701708539068427") /* "委外订单-整单" */ , (short) 8),
    OutsourcingOrderProductLine(MessageUtils.getMessage("P_YS_CTM_CM-BE_1502701708539068430") /* "委外订单-物料" */, (short) 9),
    MattersDueInit(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000013438") /* "应付事项期初" */, (short) 10);

    private static HashMap<Short, SourceMatters> map = null;
    private String name;
    private short value;

    SourceMatters(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        SourceMatters[] items = SourceMatters.values();
        for (SourceMatters item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static SourceMatters find(Number value) {
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
