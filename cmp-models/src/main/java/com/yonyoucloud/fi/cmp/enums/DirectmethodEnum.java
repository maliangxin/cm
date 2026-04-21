package com.yonyoucloud.fi.cmp.enums;

/**
 * @author: liaojbo
 * @Date: 2024年12月02日 22:05
 * @Description:
 */
public enum DirectmethodEnum {
    YINQI_LINK(1,"银企直联"),
    RPA_LINK(2,"RPA直联"),
    SWIFT_LINK(3,"SWIFT直联");

    private int value;
    private String name;

    private DirectmethodEnum(int value, String name){
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
