package com.yonyoucloud.fi.cmp.bankidentify.enums;

import lombok.Getter;

@Getter
public enum StwbStatementDetailStatusEnum {
    STATUS_1("1","待结算","UID:P_STWB-UI_180B8F48053826AD"),
    STATUS_2("2","结算中","UID:P_STWB-UI_180B8F4805382646"),
    STATUS_4("3","结算成功","UID:P_STWB-UI_180B8F4805382648"),
    STATUS_5("4","结算失败","UID:P_STWB-UI_180B8F480538264A"),
    STATUS_6("6","已止付","UID:P_STWB-UI_180B8F480538264C"),
    STATUS_7("7","部分成功","UID:P_STWB-UI_1A08DBE804700102"),
    STATUS_8("8","已退回","UID:P_STWB-UI_1C5A093805580047");

    private String code;
    private String name;
    private String resId;

    StwbStatementDetailStatusEnum(String code, String name, String resId) {
        this.code = code;
        this.name = name;
        this.resId = resId;
    }

    public static StwbStatementDetailStatusEnum getByCode(String code) {
        for (StwbStatementDetailStatusEnum e : StwbStatementDetailStatusEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
