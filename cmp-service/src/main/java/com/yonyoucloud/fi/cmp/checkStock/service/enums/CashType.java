package com.yonyoucloud.fi.cmp.checkStock.service.enums;

public enum CashType {
    Manual((short) 1, "手工直接兑付"),
    Business((short) 2, "业务单据兑付");

    private Short index;
    private String name;

    CashType(Short index, String name) {
        this.index = index;
        this.name = name;
    }

    public short getIndex() {
        return index;
    }

    public void setCode(Short index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
