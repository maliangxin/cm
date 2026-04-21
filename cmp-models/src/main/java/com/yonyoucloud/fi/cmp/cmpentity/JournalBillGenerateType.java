package com.yonyoucloud.fi.cmp.cmpentity;

public enum JournalBillGenerateType {

    MANUAL_INPUT("手工新增", (short) 0),
    IMPORT("导入", (short) 1),
    API("API生单", (short) 2);

    private String name;
    private Short value;

    JournalBillGenerateType(String name, Short value) {
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
