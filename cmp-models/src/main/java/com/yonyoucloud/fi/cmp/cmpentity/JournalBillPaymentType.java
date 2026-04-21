package com.yonyoucloud.fi.cmp.cmpentity;

public enum JournalBillPaymentType {
    DEBIT("收入", (short) 0),
    CREDIT("支出", (short) 1);

    private String name;
    private short value;

    JournalBillPaymentType(String name, short value) {
        this.name = name;
        this.value = value;
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
