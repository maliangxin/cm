package com.yonyoucloud.fi.cmp.enums;

/**
 * 业务模式 枚举值
 * 普通结算、统收统支、资金中心代理
 */
public enum BusinessModel {
    General_Settlement((short) 1, "普通结算"),
    Unify_InOut((short) 2, "统收统支"),
    FundCenter_Agent((short) 3, "结算中心代理");

    private short code;
    private String name;

    BusinessModel(short  code, String name) {
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
