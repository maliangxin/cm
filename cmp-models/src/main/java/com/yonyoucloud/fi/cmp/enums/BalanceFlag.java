package com.yonyoucloud.fi.cmp.enums;

/**
 * 业务系统类型枚举值
 */
public enum BalanceFlag {
    AutoPull("1", "自动拉取"),
    Manually("2", "手工新增");

    private String code;
    private String name;

    BalanceFlag(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
