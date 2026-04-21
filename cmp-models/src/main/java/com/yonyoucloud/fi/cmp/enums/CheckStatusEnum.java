package com.yonyoucloud.fi.cmp.enums;

/**
 * @Author zhucongcong
 * 对账状态枚举
 * @Date 2024/12/6
 */
public enum CheckStatusEnum {
    UnCheck((short) 0, "未对账"),
    Checked((short) 1, "已对账"),
    Checking((short) 2, "对账中"),
    CheckFail((short) 3, "对账失败"),
    CheckSuccess((short) 4, "无需对账");

    private Short value;
    private String name;

    private CheckStatusEnum(Short value, String name) {
        this.value = value;
        this.name = name;
    }

    public Short getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static String getNameByValue(Short value) {
        for (CheckStatusEnum checkStatusEnum : CheckStatusEnum.values()) {
            if (checkStatusEnum.value.equals(value)) {
                return checkStatusEnum.name;
            }
        }
        return null;
    }
}
