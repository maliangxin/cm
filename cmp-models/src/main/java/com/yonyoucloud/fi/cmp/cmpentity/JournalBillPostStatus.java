package com.yonyoucloud.fi.cmp.cmpentity;

public enum JournalBillPostStatus {

    EMPTY("未过账", (short) 0),
    POSTING("过账中", (short) 1),
    SUCCESS("过账成功", (short) 2),
    FAIL("过账失败", (short) 3);

    private String name;
    private Short value;

    JournalBillPostStatus(String name, Short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Short getValue() {
        return value;
    }

    public void setValue(Short value) {
        this.value = value;
    }

}
