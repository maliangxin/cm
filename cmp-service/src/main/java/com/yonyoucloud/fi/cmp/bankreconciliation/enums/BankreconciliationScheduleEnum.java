package com.yonyoucloud.fi.cmp.bankreconciliation.enums;

public enum BankreconciliationScheduleEnum {

    BANSMAUTOASSOCITE("银行对账单自动关联任务", (short) 0),
    AUTOCREATEBILL("银行对账单自动生单任务", (short) 1),
    BANKRECAUTOPUBLIC("银行对账单自动发布", (short) 2),
    ACCOUNTHISTORYBALANCECHECK("银行账户余额弥补", (short) 3),
    ACCOUNTBALANCECHECK("银行账户余额检查", (short) 4),
    INTELLIGENCEIDENTIFICATIONTASK("银行对账单自动辨识（智能规则）", (short) 5),
    INTELLIGENCEGENERATEBILLTASK("银行对账单自动生单任务（智能规则）", (short) 6),
    INTELLIGENCEADVANCEACCOUNTTASK("银行对账单提前入账（智能规则）", (short) 7),
    BANKRECONCILIATIONAUTOMATICTASK("银企自动对账", (short) 8);

    BankreconciliationScheduleEnum(String name, Short value) {
        this.name = name;
        this.value = value;
    }

    private String name;
    private short value;

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

}
