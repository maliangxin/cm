package com.yonyoucloud.fi.cmp.util;

/**
 * 客户和供应商匹配的请求信息
 * 
 * @author miaowb
 *
 */
public class MerchantRequst {

    private String accentity; //资金组织
    private String accName; //账户名称
    private String accNo; //账户
    private String currency; //币种
    private boolean accNameMust; //是否校验账户名称必输

    public MerchantRequst() {
    }

    public MerchantRequst(String accentity, String accName, String accNo) {
        this.accentity = accentity;
        this.accName = accName;
        this.accNo = accNo;
        this.currency = currency;
        this.accNameMust = true;
    }

    public MerchantRequst(String accentity, String accNo) {
        this.accentity = accentity;
        this.accName = null;
        this.accNo = accNo;
        this.accNameMust = false;
    }

    public String getAccentity() {
        return accentity;
    }

    public String getAccName() {
        return accName;
    }

    public String getAccNo() {
        return accNo;
    }

    public String getCurrency() {
        return currency;
    }

    public void setAccentity(String accentity) {
        this.accentity = accentity;
    }

    public void setAccName(String accName) {
        this.accName = accName;
    }

    public void setAccNo(String accNo) {
        this.accNo = accNo;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isAccNameMust() {
        return accNameMust;
    }

    public void setAccNameMust(boolean accNameMust) {
        this.accNameMust = accNameMust;
    }
}
