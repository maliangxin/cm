package com.yonyoucloud.fi.cmp.enums;


public enum BankReconciliationActions {

    AutoAssociation("自动关联"),
    ManualAssociation("手动关联"),
    AutoBillGeneration("自动生单"),
    ManualBillGeneration("手动生单"),
    AutoReconciliation("自动对账"),
    ManualReconciliation("手动对账"),
    ReceiptAssociation("回单关联"),
    ChequeRecognition("退票辨识"),
    CounterpartyTypeRecognition("对方类型辨识"),
    AutoFrozen("自动冻结"),
    AutoPublic("自动发布"),
    ManualPublic("手工发布");

    BankReconciliationActions(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }

}
