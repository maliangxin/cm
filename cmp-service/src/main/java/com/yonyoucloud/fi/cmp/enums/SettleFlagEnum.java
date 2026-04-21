package com.yonyoucloud.fi.cmp.enums;

import com.yonyoucloud.fi.cmp.cmpentity.EventSource;

public enum SettleFlagEnum {
    NO("是否结算:否", (short) 0),
    YES("是否结算:是", (short) 1);

    SettleFlagEnum(String name, Short value) {
        this.name = name;
        this.value = value;
    }

    private String name;
    private short value;

    public String getName() {
        return name;
    }
    public short getValue() {
        return value;
    }

}
