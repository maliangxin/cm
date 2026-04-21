package com.yonyoucloud.fi.cmp.enums;

/**
 * 对账结果枚举值
 */
public enum ReconciliationResult {
    Confirm((short) 1, "相符"),
    NoConfirm((short) 2, "不相符");

    private short code;
    private String name;

    ReconciliationResult(short  code, String name) {
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
