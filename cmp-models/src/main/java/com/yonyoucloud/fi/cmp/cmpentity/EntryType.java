package com.yonyoucloud.fi.cmp.cmpentity;



import java.util.HashMap;

public enum EntryType {
//    AA("UID:P_CM-BE_1932A8FE0408000B",(short)99),//入账类型
//    BB("UID:P_CM-BE_1932A8FE0408000C",(short)88),//虚拟入账类型
//    CC("UID:P_CM-BE_1933295E05B0000A",(short)111),//对账单入账类型
    Normal_Entry("UID:P_CM-BE_1930EDC805B00008", (short) 1),//正常入账
    Hang_Entry("UID:P_CM-BE_1930EDC805B00009", (short) 2),//挂账
    CrushHang_Entry("UID:P_CM-BE_1930EDC805B0000A", (short) 3);//冲挂账
    private static HashMap<Short, EntryType> map = null;
    private String name;
    private short value;

    private EntryType(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, EntryType>();
        EntryType[] items = EntryType.values();
        for (EntryType item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static EntryType find(Number value) {
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
