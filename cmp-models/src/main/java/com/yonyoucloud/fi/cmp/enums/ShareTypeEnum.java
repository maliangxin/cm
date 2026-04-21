package com.yonyoucloud.fi.cmp.enums;

public enum ShareTypeEnum {
    /**/ NO_SHARE((short) 0, "不分摊"),
    /**/ TERM((short) 1, "期限"),
    /**/ CONTRACT((short) 2, "合同"),
    /**/ TERM_CONTRACT((short) 3, "合同+期限");


    private Short value;
    private String memo;

    ShareTypeEnum(Short value, String memo) {
        this.value = value;
        this.memo = memo;
    }

    public static ShareTypeEnum getShareTypeByValue(Short value) {
        for (ShareTypeEnum exportTypeEnum : ShareTypeEnum.values()) {
            if (exportTypeEnum.value.equals(value)) {
                return exportTypeEnum;
            }
        }
        return null;
    }

    public Short getValue() {
        return Short.valueOf(value);
    }

    public String getMemo() {
        return memo;
    }
}
