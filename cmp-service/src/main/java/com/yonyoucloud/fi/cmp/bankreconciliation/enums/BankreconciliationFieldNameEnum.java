package com.yonyoucloud.fi.cmp.bankreconciliation.enums;

/**
 * 银行对账单字段名称枚举
 */
public enum BankreconciliationFieldNameEnum {
    TRAN_AMT("UID:P_CM-BE_17FE8C540418037B","金额"),
    ACCT_BAL("UID:P_CM-BE_1EC255C604280007","余额"),
    DC_FLAG("UID:P_CM-BE_1EC2562405C80005","收支方向"),
    TRAN_DATE("UID:P_CM-BE_1EC2567805C80008","交易日期"),
    BANK_SEQ_NO("UID:P_CM-BE_1EC256C205C80002","交易流水号"),
    TO_ACCT_NO("UID:P_CM-BE_1EC2572A04280007","对方账号"),
    TO_ACCT_NAME("UID:P_CM-BE_1EC2578004280007","对方户名"),
    TO_ACCT_BANK("UID:P_CM-BE_1EC257DE05C80000","对方开户行"),
    TO_ACCT_BANK_NAME("UID:P_CM-BE_1EC2582204280005","对方开户行名"),
    USE_NAME("UID:P_CM-BE_1EC2589205C80002","用途"),
    REMARK("UID:P_CM-BE_1EC258E805C80003","摘要"),
    ;


    private String name;
    private String defaultName;

    BankreconciliationFieldNameEnum(String name, String defaultName) {
        this.name = name;
        this.defaultName = defaultName;
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name, defaultName) /* name */;
    }

    public void setName(String name) {
        this.name = name;
    }
}
