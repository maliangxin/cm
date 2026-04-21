package com.yonyoucloud.fi.cmp.payapplicationbill;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * <h1>付款方式</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-12-14 15:21
 */
public enum PaymentMode {
    Cash(MessageUtils.getMessage("ficloud.000616") /* "现金" */, (short) 1),
    BankTransfer(MessageUtils.getMessage("P_YS_FI_YYFI-UI_0001185458") /* "银行汇款" */, (short) 2),
    Stiff(MessageUtils.getMessage("P_YS_PF_METADATA_0001041065") /* "票据" */, (short) 3);

    private static HashMap<Short, PaymentMode> map = null;
    private String name;
    private short value;

    PaymentMode(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        PaymentMode[] items = PaymentMode.values();
        for (PaymentMode item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static PaymentMode find(Number value) {
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
