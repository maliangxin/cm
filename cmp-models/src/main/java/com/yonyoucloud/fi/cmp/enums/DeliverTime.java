package com.yonyoucloud.fi.cmp.enums;

/**
 * 交割类型，枚举值
 */
public enum DeliverTime {
    T0((short) 0, "T+0"),
    T1((short) 1, "T+1"),
    T2((short) 2, "T+2");

    private short code;
    private String name;

    DeliverTime(short  code, String name) {
        this.code = code;
        this.name = name;
    }

    public short getCode() {
        return code;
    }

    public void setCode(short code) {
        this.code = code;
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
    }

    public void setName(String name) {
        this.name = name;
    }
}
