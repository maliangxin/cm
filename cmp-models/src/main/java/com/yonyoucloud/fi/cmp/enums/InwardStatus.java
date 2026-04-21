package com.yonyoucloud.fi.cmp.enums;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

/**
 * 收款性质
 * 1：货到收汇 2：预收款 3：退款 4：其他
 */
public enum InwardStatus {

    TO_BE_CONFIRMED((short)1, MessageUtils.getMessage("P_YS_SD_UDHBI_0000032336") /* "待确认" */),
    PROCESSING((short)2, MessageUtils.getMessage("inProcess") /* "处理中" */),
    SUCCESS((short)3, MessageUtils.getMessage("P_YS_CTM_CM-BE_1668920464760635398") /* "汇入成功" */),
    FAIL((short)4, MessageUtils.getMessage("P_YS_CTM_CM-BE_1668920464760635395") /* "汇入失败" */);

    private short index;
    private String name;

    InwardStatus(short index, String name) {
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
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
