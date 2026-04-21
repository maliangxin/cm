package com.yonyoucloud.fi.cmp.bankidentify.enums;

import lombok.Getter;

@Getter
public enum StwbStatementStatusEnum {
    STATUS_1("1","已保存","UID:P_STWB-UI_180B8F48053825A6"),
    STATUS_2("2","待审核","UID:P_STWB-UI_180B8F48053825A8"),
    STATUS_4("4","结算中","UID:P_STWB-UI_180B8F48053825AA"),
    STATUS_5("5","已办结","UID:P_STWB-UI_180B8F48053825AC"),
    STATUS_6("6","已终止","UID:P_STWB-UI_180B8F4805382611"),
    STATUS_7("7","暂存","UID:P_STWB-UI_180B8F48053823EA"),
    STATUS_8("8","已退回","UID:P_STWB-UI_1C5A093805580045");

    private String code;
    private String name;
    private String resId;

    StwbStatementStatusEnum(String code, String name, String resId) {
        this.code = code;
        this.name = name;
        this.resId = resId;
    }

    public static StwbStatementStatusEnum getByCode(String code) {
        for (StwbStatementStatusEnum e : StwbStatementStatusEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
