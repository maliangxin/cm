package com.yonyoucloud.fi.cmp.enums;

/**
 * 流水完结状态
 */
public enum SerialdealendState {

    UNEND((short) 0,"未完结"),
    END((short) 1, "已完结")
    ;

    SerialdealendState(short value, String name) {
        this.value = value;
        this.name = name;
    }

    public short getValue() {
        return value;
    }

    public String getName() {
        return name;
    }


    private final short value;
    private final String name;



}
