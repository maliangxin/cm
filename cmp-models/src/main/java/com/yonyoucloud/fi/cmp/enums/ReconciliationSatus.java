package com.yonyoucloud.fi.cmp.enums;

/**
 * 对账状态枚举值
 */
public enum ReconciliationSatus {
    No((short) 0, "未对账"),
    Done((short) 1, "已对账"),
    Doing((short) 2, "对账中"),
    Fail((short) 3, "对账失败"),
    None((short) 4, "无需对账"),
    NoMesg((short) 5, "无对账信息");

    private short code;
    private String name;

    ReconciliationSatus(short  code, String name) {
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
