package com.yonyoucloud.fi.cmp.arap;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;

import java.util.HashMap;

public enum EventSource {
    Sale("UID:P_ARAP-FE_180721A604C80044", (short)1),
    OrderCenter("UID:P_ARAP-FE_180721A604C80045", (short)2),
    Online("UID:P_ARAP-FE_180721A604C80046", (short)3),
    Retail("UID:P_ARAP-FE_180721A604C80047", (short)4),
    Stock("UID:P_ARAP-FE_180721A604C80048", (short)5),
    Manual("UID:P_ARAP-FE_180721A604C80049", (short)6),
    Purchase("UID:P_ARAP-FE_180721A604C8004A", (short)7),
    Cmpchase("UID:P_ARAP-FE_180721A604C8004B", (short)8),
    ManualImport("UID:P_ARAP-FE_180721A604C8004C", (short)9),
    SystemOut("UID:P_ARAP-FE_180721A604C8004D", (short)10),
    GL("UID:P_ARAP-FE_180721A604C8004E", (short)11),
    INTERNALTRADE("UID:P_ARAP-FE_180721A604C8004F", (short)12),
    HRSalaryChase("UID:P_ARAP-FE_180721A604C80050", (short)13),
    PMP_CLOUD("UID:P_ARAP-FE_180721A604C80051", (short)14),
    THIRD_PARTY("UID:P_ARAP-FE_180721A604C80052", (short)20),
    Drftchase("UID:P_ARAP-FE_180721A604C80053", (short)50),
    SALES_EXPENSE_AR("UID:P_ARAP-FE_180721A604C80054", (short)70),
    SALES_EXPENSE_AP("UID:P_ARAP-FE_180721A604C80054", (short)72),
    CHAIN_EXPENSE_AR("UID:P_ARAP-FE_180721A604C80055", (short)71),
    CHAIN_EXPENSE_AP("UID:P_ARAP-FE_180721A604C80055", (short)73),
    PURINRECORD_AP("UID:P_ARAP-FE_180721A604C80048", (short)74),
    SALES_REBATE("UID:P_ARAP-FE_180721A604C80056", (short)75),
    OUTSOURCING_STORAGE("UID:P_ARAP-FE_180721A604C80057", (short)76),
    PURCHASE_BILL("UID:P_ARAP-FE_180721A604C8004A", (short)77),
    INTERNALTRADE_PRE_AR("UID:P_ARAP-FE_180721A604C80058", (short)78),
    INTERNALTRADE_PRE_AP("UID:P_ARAP-FE_180721A604C80059", (short)79),
    THIRD_SYS_TO_ARAP("UID:P_ARAP-FE_180721A604C8005A", (short)80),
    FIXED_100("UID:P_ARAP-FE_180721A604C8005B", (short)100),
    StwbSettlement("UID:P_ARAP-FE_180721A604C8005C", (short)15),
    CONSUMEORDER("UID:P_ARAP-FE_180721A604C8005D", (short)105);

    private String name;
    private short value;
    private static HashMap<Short, EventSource> map = null;

    private EventSource(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return InternationalUtils.getMessage(this.name);
    }

    public short getValue() {
        return this.value;
    }

    private static synchronized void initMap() {
        if (map == null) {
            map = new HashMap();
            EventSource[] items = values();
            EventSource[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                EventSource item = var1[var3];
                map.put(item.getValue(), item);
            }

        }
    }

    public static EventSource find(Number value) {
        if (value == null) {
            return null;
        } else {
            if (map == null) {
                initMap();
            }

            return (EventSource)map.get(value.shortValue());
        }
    }
}