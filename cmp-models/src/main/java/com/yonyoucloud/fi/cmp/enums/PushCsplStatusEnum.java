package com.yonyoucloud.fi.cmp.enums;

/**
 * <h1>占用资金计划状态枚举</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-09-05 10:49
 */

public enum PushCsplStatusEnum {
    NO_OCCUPIED(0, "未占用"),
    ALREADY_OCCUPIED(1, "已占用"),
    PRE_OCCUPIED(2, "预占用");

    private Integer value;
    private String name;

    PushCsplStatusEnum(Integer  code, String name) {
        this.value = code;
        this.name = name;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
