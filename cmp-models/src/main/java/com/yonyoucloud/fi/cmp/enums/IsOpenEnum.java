package com.yonyoucloud.fi.cmp.enums;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;


/*
 *@author lixuejun
 *@create 2020-08-26-10:45
 */
public enum IsOpenEnum {

    CLOSE(false, MessageUtils.getMessage("P_YS_FED_EXAMP_0000020183") /* "关闭" */),

    OPEN(true, MessageUtils.getMessage("P_YS_PF_GZTLOG_0000025973") /* "开启" */);




    private boolean code;
    private String value;

    IsOpenEnum(boolean code, String value) {
        this.code = code;
        this.value = value;
    }


    public boolean getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }
}
