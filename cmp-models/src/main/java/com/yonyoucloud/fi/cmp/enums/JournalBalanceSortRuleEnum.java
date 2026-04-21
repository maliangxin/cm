package com.yonyoucloud.fi.cmp.enums;


/**
 * 日记账余额排序规则枚举
 */
public enum JournalBalanceSortRuleEnum {
    RECFIRST_THENPAY((short)1, "先收后支"),
    DZ_TIME((short)0,"结算成功系统时间"),;


    private short value;
    private String name;

    JournalBalanceSortRuleEnum(short value, String name){
        this.value = value;
        this.name = name;
    }

    public short getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setValue(short value) {
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }
}
