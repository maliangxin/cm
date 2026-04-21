package com.yonyoucloud.fi.cmp.enums;


/**
 * 是和否的枚举
 */
public enum YesOrNoEnum {
    YES((short)1, "是"),
    NO((short)0,"否"),;


    private short value;
    private String name;

    YesOrNoEnum(short value, String name){
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
