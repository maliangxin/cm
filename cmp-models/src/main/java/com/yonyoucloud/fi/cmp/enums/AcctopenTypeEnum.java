package com.yonyoucloud.fi.cmp.enums;

/**
 * 开户类型(企业银行账户档案 内部枚举)
 */
public enum AcctopenTypeEnum {
    BankAccount (0, "银行开户" /* "银行开户" */),
    SettlementCenter(1,"结算中心开户" /* "结算中心开户" */),
    FinancialCompany(2,"财务公司" /* "财务公司" */),
    OtherFinancial(3,"其他金融机构" /* "其他金融机构" */),
    DigitalWallet(5,"数币钱包" /* 数币钱包 */);

    private int value;
    private String name;

    private AcctopenTypeEnum(int value, String name){
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }}
