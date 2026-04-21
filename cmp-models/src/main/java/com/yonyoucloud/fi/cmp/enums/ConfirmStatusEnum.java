package com.yonyoucloud.fi.cmp.enums;

/**
 * 确认状态枚举类
 */
public enum ConfirmStatusEnum {

    Confirming("0", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "待确认") /* "待确认" */),
    Confirmed("1", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "手工确认") /* "手工确认" */),
    RelationConfirmed("3", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "关联确认") /* "关联确认" */),
    All("2", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "全部") /* "全部" */);

    private String index;
    private String name;

    ConfirmStatusEnum(String index, String name) {
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
