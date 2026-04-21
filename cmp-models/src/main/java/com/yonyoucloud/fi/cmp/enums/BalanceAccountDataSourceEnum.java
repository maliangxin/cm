package com.yonyoucloud.fi.cmp.enums;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

/*
 *@author liuwtr
 *@create 2023-04-26-10:45
 */
public enum BalanceAccountDataSourceEnum {
    MANUAL_ENTRY((short)1, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00046", "手工录入") /* "手工录入" */ ),
    MANUAL_IMPORT((short)2, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00047", "手工导入") /* "手工导入" */ ),
    BANK_ENTERPRISE_DOWNLOAD((short)3, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00048", "银企联下载") /* "银企联下载" */ ),
    SUPPLEMENTARY_ADJUSTMENTS((short)4, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1B00617A04280002", "系统弥补计算") /* "补充调整" */ ),
    //UID:P_CM-UI_1B005A2C04400008
    INTERFACE_IMPORT((short)5,  com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1B005A2C04400008", "接口传入" )),
    CURRENTDAY_BAL((short)6, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1B005A7004400002", "当日余额生成" ));


    private short code;
    private String value;

    BalanceAccountDataSourceEnum(short code, String value) {
        this.code = code;
        this.value = value;
    }

    public static String getValueByKey(short code) {
        for (BalanceAccountDataSourceEnum exportTypeEnum : BalanceAccountDataSourceEnum.values()) {
            if (exportTypeEnum.code == code) {
                return exportTypeEnum.value;
            }
        }
        return "";
    }

    public static BalanceAccountDataSourceEnum getByCode(short code) {
        for (BalanceAccountDataSourceEnum exportTypeEnum : BalanceAccountDataSourceEnum.values()) {
            if (exportTypeEnum.code == code) {
                return exportTypeEnum;
            }
        }
        return null;
    }

    public short getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }
}
