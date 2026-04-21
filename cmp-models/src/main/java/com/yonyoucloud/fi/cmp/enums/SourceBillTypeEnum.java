package com.yonyoucloud.fi.cmp.enums;

public enum SourceBillTypeEnum {
    /**/ FINANCING_REGISTER("0", "tlm.financingregister.FinancingRegister", "yonbip_fi_ctmfm", "protocolNumber", "", "", "融资合同"),
    /**/ INTEGRATION_REGISTRATION("1", "tlm.financein.FinanceIn", "yonbip_fi_ctmfm", "debtcode", "", "", "融入登记"),
    /**/ INVESTMENT_REGISTRATION("2", "tlm.financingregister.FinancingRegister", "yonbip_fi_ctmfm", "protocolNumber", "", "", "投资登记"),
    /**/ CREDIT_CONTRACT("3", "cam.agreement.Agreement", "yonbip-fi-ctmcam", "agreementNo", "", "", "授信合同"),
    /**/ GUARANTEE_CONTRACT("4", "grm.guaranteecontract.GuaranteeContract", "yonbip-fi-ctmgrm", "contractCode", "", "", "担保合同"),
    /**/ OPEN_LETTER_REGISTRATION("5", "lgm.guaranteeregister.GuaranteeRegister", "yonbip-fi-ctmlgm", "guaranteeNumber", "", "", "开函登记"),
    /**/ ISSUE_REGISTER("6", "lcm.issueregister.Issueregister", "yonbip-fi-ctmlcm", "letterCreditCode", "", "", "开证办理"),
    /**/ ARRIVE_BILL("7", "lcm.arrivebill.ArriveBill", "yonbip-fi-ctmlcm", "billNum", "", "", "到单登记"),
    /**/ RECEIPT_CERTIFICATE("8", "lcm.receiptcertificate.ReceiptCertificate", "yonbip-fi-ctmlcm", "certificateNo", "", "", "收证办理"),
    /**/ PRESENTATION("9", "lcm.presentation.Presentation", "yonbip-fi-ctmlcm", "presentationNo", "", "", "交单办理"),
    /**/ BILL_ISSUANCE("10", "drft.billno.Billno", "yonbip-fi-ctmdrft", "noteno", "", "", "票据签发"),
    /**/ PURCHASE_REGISTER("11", "tlm.purchaseregister.PurchaseRegister", "yonbip_fi_ctmfm", "purchaseCode", "", "", "申购登记"),
    /**/ DISCOUNT_PROCESSING("12", "drft.discount.Discount", "yonbip-fi-ctmdrft", "noteno", "", "", "贴现办理"),
    /**/ OPEN_LETTER_APPLY("13", "lgm.guaranteeapply.GuaranteeApply", "yonbip-fi-ctmlgm", "code", "", "", "开函申请");


    private String value;
    private String fullname;
    private String domain;
    private String field;
    private String startDateField;
    private String endDateField;
    private String memo;

    SourceBillTypeEnum(String value, String fullname, String domain, String field, String startDateField, String endDateField, String memo) {
        this.value = value;
        this.fullname = fullname;
        this.domain = domain;
        this.field = field;
        this.startDateField = startDateField;
        this.endDateField = endDateField;
        this.memo = memo;
    }

    public static SourceBillTypeEnum getSouceBillTypeByValue(String value) {
        for (SourceBillTypeEnum exportTypeEnum : SourceBillTypeEnum.values()) {
            if (exportTypeEnum.value.equals(value)) {
                return exportTypeEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getFullName() {
        return fullname;
    }

    public String getMemo() {
        return memo;
    }

    public String getDomain() {
        return domain;
    }

    public String getField() {
        return field;
    }

    public String getStartDateField() {
        return startDateField;
    }

    public String getEndDateField() {
        return endDateField;
    }
}
