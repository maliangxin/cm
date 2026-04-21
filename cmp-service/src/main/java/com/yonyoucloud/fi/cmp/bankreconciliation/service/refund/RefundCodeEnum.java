package com.yonyoucloud.fi.cmp.bankreconciliation.service.refund;

public enum RefundCodeEnum {

    REFUND_SUCCESS("200","退票辨识成功，辨识结果为退票"),
    SUSPECTEDREFUND_SUCCESS("201","退票辨识成功，辨识结果为疑似退票"),
    REFUND_NONE("202","退票辨识结束，辨识结果为无退票");

    private String code;
    private String message;

    RefundCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
