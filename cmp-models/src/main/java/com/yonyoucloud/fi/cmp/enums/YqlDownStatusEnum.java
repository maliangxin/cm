package com.yonyoucloud.fi.cmp.enums;

/**
 * 银企联下载回单文件时，返回数据明细中，数据的下载状态
 *
 * @author zhuguangqian
 * @since 2024/12/20
 */
public enum YqlDownStatusEnum {
    /**
     * 0-下载失败
     */
    DOWN_FAIL("0", "下载失败"),
    /**
     * 1-下载成功
     */
    DOWN_SUCCESS("1", "下载成功"),
    /**
     * 2-下载中
     */
    DOWNLOADING("2", "下载中");

    private final String value;

    private final String desc;

    YqlDownStatusEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }
}
