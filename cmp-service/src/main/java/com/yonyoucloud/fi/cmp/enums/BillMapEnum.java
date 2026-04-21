package com.yonyoucloud.fi.cmp.enums;

import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.currencyapply.CurrencyApply;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.util.StringUtils;

public enum BillMapEnum {
    RECEIVEMARGIN(IBillNumConstant.CMP_RECEIVEMARGIN, IBillNumConstant.CMP_RECEIVEMARGIN, IServicecodeConstant.RECEIVEMARGIN, ReceiveMargin.ENTITY_NAME),
    PAYMARGIN(IBillNumConstant.CMP_PAYMARGIN, IBillNumConstant.CMP_PAYMARGIN, IServicecodeConstant.PAYMARGIN, PayMargin.ENTITY_NAME),
    TRANSFER_ACCOUNT(IBillNumConstant.TRANSFERACCOUNT, IBillNumConstant.TRANSFERACCOUNT, IServicecodeConstant.TRANSFERACCOUNT, TransferAccount.ENTITY_NAME),
    FOREIGNPAYMENT(IBillNumConstant.CMP_FOREIGNPAYMENT, IBillNumConstant.CMP_FOREIGNPAYMENT, IServicecodeConstant.FOREIGNPAYMENT, ForeignPayment.ENTITY_NAME),
    CURRENCYAPPLY(IBillNumConstant.CURRENCYAPPLY, IBillNumConstant.CURRENCYAPPLY, IServicecodeConstant.CURRENCYAPPLY, CurrencyApply.ENTITY_NAME),
    CURRENCYEXCHANGE(IBillNumConstant.CURRENCYEXCHANGE, IBillNumConstant.CURRENCYEXCHANGE, IServicecodeConstant.CURRENCYEXCHANGE, CurrencyExchange.ENTITY_NAME),
    FUNDPAYMENT(IBillNumConstant.FUND_PAYMENT, IBillNumConstant.FUND_PAYMENT, IServicecodeConstant.FUNDPAYMENT, FundPayment.ENTITY_NAME),
    FUNDCOLLECTION(IBillNumConstant.FUND_COLLECTION, IBillNumConstant.CURRENCYEXCHANGE, IServicecodeConstant.FUNDCOLLECTION, FundCollection.ENTITY_NAME);

    BillMapEnum(String billNum, String billCode, String serviceCode, String entityName) {
        this.billNum = billNum;
        this.billCode = billCode;
        this.serviceCode = serviceCode;
        this.entityName = entityName;
    }

    private String billNum;
    private String billCode;
    private String serviceCode;
    private String entityName;

    public String getBillNum() {
        return billNum;
    }

    public String getBillCode() {
        return billCode;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public String getEntityName() {
        return entityName;
    }

    public static BillMapEnum getEnumByBillNum(String billNum) {
        if (StringUtils.isEmpty(billNum)) {
            return null;
        }
        BillMapEnum[] values = BillMapEnum.values();
        for (BillMapEnum value : values) {
            if (value.billNum.equals(billNum)) {
                return value;
            }
        }
        return null;
    }
}
