package com.yonyoucloud.fi.cmp.payapplicationbill;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * <h1>来源系统</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-10-15 9:24
 */
public enum SourceSystem {
    Handle(MessageUtils.getMessage("P_YS_SD_SDMBF_0000141542") /* "应付" */, (short) 1),
    Receivable(MessageUtils.getMessage("P_YS_SCM_STOCK_0000034423") /* "应收" */, (short) 2),
    Purchase(MessageUtils.getMessage("P_YS_PF_PROCENTER_0000023090") /* "采购" */, (short) 3),
    Cash(MessageUtils.getMessage("ficloud.000616") /* "现金" */, (short) 4),
    ThirdParty(MessageUtils.getMessage("P_YS_OA_XTLCZX_0000030199") /* "第三方" */, (short) 5),
    SupplyChain(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000134858") /* "供应链" */,(short) 6),
    Manufacturing(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012837") /* "生产制造" */,(short) 7);

    private static HashMap<Short, SourceSystem> map = null;
    private String name;
    private short value;

    SourceSystem(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        SourceSystem[] items = SourceSystem.values();
        for (SourceSystem item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static SourceSystem find(Number value) {
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
