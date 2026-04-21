package com.yonyoucloud.fi.cmp.enums;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import lombok.Getter;

/**
 * 流水自动辨识匹配 适用对象枚举
 */
@Getter
public enum ApplyObjectEnum {
    BANKDETAIL("1","银行流水", MessageUtils.getMessage("UID:P_CM-UI_1C41A99E04700001")),
    CLAIM("2","认领单",MessageUtils.getMessage("UID:P_CM-UI_18108E6604B8228E"));

    private final String code;
    private final String name;
    private final String resid;
    ApplyObjectEnum(String code, String name,String resid) {
        this.code = code;
        this.name = name;
        this.resid = resid;
    }
}
