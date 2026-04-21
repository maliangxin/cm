package com.yonyoucloud.fi.cmp.enums;

public enum DfTypeEnum {
    /**/ CUSTOMER((short) 1, "客户"),
    /**/ SUPPLIER((short) 2, "供应商"),
    /**/ FUNBUSOBJ((short) 3, "资金伙伴");


    private Short value;
    private String memo;

    DfTypeEnum(Short value, String memo) {
        this.value = value;
        this.memo = memo;
    }

    public static DfTypeEnum getDfTypeByValue(Short value) {
        for (DfTypeEnum exportTypeEnum : DfTypeEnum.values()) {
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
