package com.yonyoucloud.fi.cmp.enums;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

/**
 * <h1>回写第三方平台处理类型枚举</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023/3/14 16:19
 */
public enum ThirdPartyBackWriteDealTypeEnum {

    WORKING(1, MessageUtils.getMessage("P_YS_OA_XTLCZX_0000030692") /* "进行中" */),
    SUCCESS(2, MessageUtils.getMessage("success") /* "成功" */),
    FAIL(3, MessageUtils.getMessage("P_YS_FED_FW_0000020935") /* "失败" */);

    private int value;
    private String name;

    ThirdPartyBackWriteDealTypeEnum(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setCode(int value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
