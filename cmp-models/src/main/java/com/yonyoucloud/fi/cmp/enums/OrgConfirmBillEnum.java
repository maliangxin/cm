package com.yonyoucloud.fi.cmp.enums;

/**
 * 确认状态枚举类
 */
public enum OrgConfirmBillEnum {

    CMP_BANKRECONCILIATION("1", "银行流水认领" /* "银行流水认领" */),
    CMP_BILLCLAIMCENTER("2", "到账认领" /* "到账认领" */);

    private String index;
    private String name;

    OrgConfirmBillEnum(String index, String name) {
        this.index = index;
        this.name = name;
    }

    public String getIndex() {
        return index;
    }

    public void setCode(String index) {
        this.index = index;
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name, name) /* name */;
    }

    public void setName(String name) {
        this.name = name;
    }
}
