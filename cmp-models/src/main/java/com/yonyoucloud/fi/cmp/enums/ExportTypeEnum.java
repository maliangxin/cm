package com.yonyoucloud.fi.cmp.enums;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;


/*
 *@author lixuejun
 *@create 2020-08-26-10:45
 */
public enum ExportTypeEnum {
    SAP_EXPORT(0, MessageUtils.getMessage("P_YS_FI_CM_0001123718") /* "sap导出" */),
    EXCEL_EXPORT(1, MessageUtils.getMessage("P_YS_PF_PROCENTER_0000027272") /* "excel导出" */);



    private Integer code;
    private String value;

    ExportTypeEnum(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public static String getValueByKey(Integer code) {
        for (ExportTypeEnum exportTypeEnum : ExportTypeEnum.values()) {
            if (exportTypeEnum.code.equals(code)) {
                return exportTypeEnum.value;
            }
        }
        return "";
    }

    public static ExportTypeEnum getByCode(Integer code) {
        for (ExportTypeEnum exportTypeEnum : ExportTypeEnum.values()) {
            if (exportTypeEnum.code.equals(code)) {
                return exportTypeEnum;
            }
        }
        return null;
    }

    public Integer getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }
}
