package com.yonyoucloud.fi.cmp.enums;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;

/**
 * 期初升级标志(账户期初升级 内部枚举)
 */
public enum UpgradeSignEnum {
    //如果此字段为空 则是未升级数据
    SUPERFLUITY((short)0, InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D7404A804C00008","升级冗余") /* "升级冗余" */),
    CANTJUDGMENT((short)1,InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D74058205000006","升级存疑") /* "升级存疑" */),
    JUDGMENT((short)2,InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D7403BA04C00008","升级成功") /* "升级成功" */),
    ADDNEW((short)3,InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CB62C6E04380009","手工同步") /* "手工同步" */);



    private short value;
    private String name;

    private UpgradeSignEnum(short value, String name){
        this.value = value;
        this.name = name;
    }

    public short getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }}
