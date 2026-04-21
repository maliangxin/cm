package com.yonyoucloud.fi.cmp.enums;

public enum SourceModelEnum {
    /**/ FINANCING_MGT((short) 0, "融资管理"),
    /**/ INVEST_MGT((short) 1, "投资管理"),
    /**/ CREDIT_MGT((short) 2, "授信管理"),
    /**/ SECURITY_MGT((short) 3, "担保管理"),
    /**/ GUARANTEE_MGT((short) 4, "保函管理"),
    /**/ CREDIT_LETTER_MGT((short) 5, "信用证管理"),
    /**/ COMMERCIAL_BILL((short) 6, "商业汇票");


    private Short value;
    private String memo;

    SourceModelEnum(Short value, String memo) {
        this.value = value;
        this.memo = memo;
    }

    public static SourceModelEnum getSourceModelByValue(Short value) {
        for (SourceModelEnum exportTypeEnum : SourceModelEnum.values()) {
            if (exportTypeEnum.value.equals(value)) {
                return exportTypeEnum;
            }
        }
        return null;
    }

    public Short getValue() {
        return Short.valueOf(value);
    }

    public String getMemo() {
        return memo;
    }
}
