package com.yonyoucloud.fi.cmp.constant;

import com.yonyou.ucf.mdd.ext.core.AppContext;

/**
 * @author: liaojbo
 * @Date: 2025年03月18日 17:14
 * @Description:
 */
public enum CmpYmsConfigEnum {

    bankReceiptDownloadMaxsize("cmp.bankReceipt.download.maxsize", "2500");

    private String key;
    private String ymsVaule;
    private String defaultValue;
    private String finalValue;

    CmpYmsConfigEnum(String key, String defaultValue) {
        this.key = key;
        this.ymsVaule = AppContext.getEnvConfig(key);
        this.defaultValue = defaultValue;
        this.finalValue = AppContext.getEnvConfig(key, defaultValue);
    }

    public String getKey() {
        return key;
    }
    public String getDefaultValue() {
        return defaultValue;
    }
    public String getYmsVaule() {
        return ymsVaule;
    }
    public String getFinalValue() {
        return finalValue;
    }
}
