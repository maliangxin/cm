package com.yonyoucloud.fi.cmp.event.enums;

/**
 * <h1>DataFlattenEnum</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-06-13 8:55
 */
public enum DataFlattenEnum {

    NO_CREATE("99"),
    POSTING("1");

    private String code;

    DataFlattenEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
