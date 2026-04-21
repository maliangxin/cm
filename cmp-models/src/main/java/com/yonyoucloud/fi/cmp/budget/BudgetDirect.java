package com.yonyoucloud.fi.cmp.budget;

/**
 * 预算方向
 * 0：增加预算 1：扣除预算
 */
public enum BudgetDirect {

    ADD((short) 0, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418018B","增加") /* "待确认" */),
    REDUCE((short) 1, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418018C","扣除") /* "处理中" */);

    private short index;
    private String name;

    BudgetDirect(short index, String name) {
        this.index = index;
        this.name = name;
    }

    public short getIndex() {
        return index;
    }

    public void setCode(short index) {
        this.index = index;
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
    }

    public void setName(String name) {
        this.name = name;
    }
}
