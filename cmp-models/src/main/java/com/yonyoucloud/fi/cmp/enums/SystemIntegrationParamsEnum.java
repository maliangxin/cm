package com.yonyoucloud.fi.cmp.enums;

/**
 * <h1>系统间集成参数枚举</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-08-14 11:47
 */
public enum SystemIntegrationParamsEnum {
    FundCollection("16", "cmp_fundcollection"),
    FundPayment("15", "cmp_fundpayment"),
    SALARY_PAYMENT("37", "cmp_salarypay"),
    PAY_MARGIN("160", "cmp_paymargin"),
    RECEIVE_MARGIN("161", "cmp_receivemargin"),
    TRANSFER_ACCOUNT("49", "cm_transfer_account"),
    FOREIGN_PAYMENT("165", "cmp_foreignpayment"),
    BATCH_TRANSFER_ACCOUNT("171", "cmp_batchtransferaccount"),
    CURRENCY_EXCHANGE("167", "cmp_currencyexchange");

    SystemIntegrationParamsEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }


    private final String value;
    private final String name;


}
