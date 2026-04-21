package com.yonyoucloud.fi.cmp.bankreconciliation.enums;

public enum BankreconciliationActionEnum {
    RECEIPTASSOCIATION("回单关联", (short) 0),
    DELETE("删除", (short) 1),
    CANCELDISPATCH("取消分配", (short) 2),
    NORELEASE("取消发布", (short) 3),
    RELEASEBODY("发布", (short) 4),
    NOPROCESS("无需处理", (short) 5),
    NORECEIPTASSOCIATION("取消回单关联", (short) 6),
    IDENTIFIMATCHRULE("自动辨识匹配", (short) 7),

    BATCHRETURNBILL("分配退回", (short) 8),
    DISPATCHBATCHBUSSINESS("分配业务人员", (short) 9),
    DISPATCHBATCHFINANCE("分配财务人员", (short)10),
    MANUALREFUND("手工退票", (short) 11),
    BATCHMODIFY("批改", (short) 12),
    BUSSASSOCIATION("业务关联", (short) 13);

    BankreconciliationActionEnum(String name, Short value) {
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
