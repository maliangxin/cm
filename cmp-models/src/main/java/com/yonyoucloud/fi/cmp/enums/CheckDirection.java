package com.yonyoucloud.fi.cmp.enums;

/**
 * 业务系统类型枚举值
 */
public enum CheckDirection {
    Pay("2", "付款"),
    Receive("1", "收款");

    private String index;
    private String name;

    CheckDirection(String index, String name) {
        this.index = index;
        this.name = name;
    }

    public String getIndex() {
        return index;
    }

    public void setCode(String index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
