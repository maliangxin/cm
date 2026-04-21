package com.yonyoucloud.fi.cmp.enums;

import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.util.StringUtils;

public enum CharacterDefEnum {
    RECEIVEMARGIN(IBillNumConstant.CMP_RECEIVEMARGIN, "characterDef"),

    PAYMARGIN(IBillNumConstant.CMP_PAYMARGIN, "characterDef"),

    TRANSFER_ACCOUNT(IBillNumConstant.TRANSFERACCOUNT, "characterDef"),

    FOREIGNPAYMENT(IBillNumConstant.CMP_FOREIGNPAYMENT, "characterDef"),

    CURRENCYAPPLY(IBillNumConstant.CURRENCYAPPLY, "characterDef"),

    CURRENCYEXCHANGE(IBillNumConstant.CURRENCYEXCHANGE, "characterDef");

    CharacterDefEnum(String billCode, String characterDefName) {
        this.billCode = billCode;
        this.characterDefName = characterDefName;
    }

    private String billCode;
    private String characterDefName;


    public String getBillCode() {
        return billCode;
    }

    public String getCharacterDefName() {
        return characterDefName;
    }

    public static CharacterDefEnum getEnumByBillCode(String billCode) {
        if (StringUtils.isEmpty(billCode)) {
            return null;
        }
        CharacterDefEnum[] values = CharacterDefEnum.values();
        for (CharacterDefEnum value : values) {
            if (value.billCode.equals(billCode)) {
                return value;
            }
        }
        return null;
    }
}
