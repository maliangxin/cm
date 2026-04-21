package com.yonyoucloud.fi.cmp.enums;

/**
 * 资金切分方式 枚举值
 * 内部账户划转\银行账户划转
 */
public enum FundSplitMethod {
    InnerAccount_Trans((short) 1, "内部账户划转"),
    BankAccount_Trans((short) 2, "银行账户划转");

    private short code;
    private String name;

    FundSplitMethod(short  code, String name) {
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
