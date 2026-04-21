package com.yonyoucloud.fi.cmp.arap;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;

import java.util.HashMap;

public enum EventType {
    InitDate("UID:P_ARAP-FE_180721A604C80120", (short)0),
    SaleInvoice("UID:P_ARAP-FE_180721A604C80121", (short)1),
    OtherAREvent("UID:P_ARAP-FE_180721A604C80122", (short)2),
    PROJECT_OPEN_INVOICE("UID:P_ARAP-FE_180721A604C80123", (short)3),
    SUPPLIER_SETTLE("UID:P_ARAP-FE_180721A604C80124", (short)4),
    OrderDailyReport("UID:P_ARAP-FE_180721A604C80125", (short)5),
    InterSettlement("UID:P_ARAP-FE_180721A604C80126", (short)6),
    ReceiveBill("UID:P_ARAP-FE_180721A604C80127", (short)7),
    OtherAPEvent("UID:P_ARAP-FE_180721A604C80128", (short)8),
    ArRefund("UID:P_ARAP-FE_180721A604C80129", (short)9),
    PayMent("UID:P_ARAP-FE_180721A604C8012A", (short)10),
    ApRefund("UID:P_ARAP-FE_180721A604C8012B", (short)11),
    TransferAccount("UID:P_ARAP-FE_180721A604C8012C", (short)12),
    ExchangeBill("UID:P_ARAP-FE_180721A604C8012D", (short)13),
    CurrencyExchangeBill("UID:P_ARAP-FE_180721A604C8012E", (short)14),
    SalaryPayment("UID:P_ARAP-FE_180722BA04C80059", (short)15),
    CashMark("UID:P_ARAP-FE_180721A604C8012F", (short)16),
    ReceivableNoteInit("UID:P_ARAP-FE_180721A604C80130", (short)50),
    Register("UID:P_ARAP-FE_180721A604C80131", (short)51),
    ConsignBank("UID:P_ARAP-FE_180721A604C80132", (short)52),
    ExpireCash("UID:P_ARAP-FE_180721A604C80133", (short)53),
    SignNoteInit("UID:P_ARAP-FE_180721A604C80134", (short)54),
    SignNote("UID:P_ARAP-FE_180721A604C80135", (short)55),
    PayBillRegister("UID:P_ARAP-FE_180721A604C80136", (short)56),
    Discount("UID:P_ARAP-FE_180721A604C80137", (short)57),
    Endore("UID:P_ARAP-FE_180721A604C80138", (short)58),
    PayApplyBill("UID:P_ARAP-FE_180721A604C80139", (short)59),
    SALES_EXPENSE_AR("UID:P_ARAP-FE_180721A604C8013A", (short)70),
    SALES_EXPENSE_AP("UID:P_ARAP-FE_180721A604C8013A", (short)72),
    CHAIN_EXPENSE_AR("UID:P_ARAP-FE_180721A604C8013B", (short)71),
    CHAIN_EXPENSE_AP("UID:P_ARAP-FE_180721A604C8013B", (short)73),
    INTERNALTRADE_PRE_AR("UID:P_ARAP-FE_180721A604C8013C", (short)72),
    INTERNALTRADE_PRE_AP("UID:P_ARAP-FE_180721A604C8013C", (short)74),
    StwbSettleMentDetails("UID:P_ARAP-FE_180721A604C8013D", (short)60),
    Verify("UID:P_ARAP-FE_180721A604C8013E", (short)100);

    private String name;
    private short value;
    private static HashMap<Short, EventType> map = null;

    private EventType(String name, short value) {
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
            EventType[] items = values();
            EventType[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                EventType item = var1[var3];
                map.put(item.getValue(), item);
            }

        }
    }

    public static EventType find(Number value) {
        if (value == null) {
            return null;
        } else {
            if (map == null) {
                initMap();
            }

            return (EventType)map.get(value.shortValue());
        }
    }
}