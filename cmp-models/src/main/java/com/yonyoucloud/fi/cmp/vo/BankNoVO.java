package com.yonyoucloud.fi.cmp.vo;

import lombok.Data;

@Data
public class BankNoVO {
    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    String account;

    public String getWarnPrimaryOrgId() {
        return warnPrimaryOrgId;
    }

    public void setWarnPrimaryOrgId(String warnPrimaryOrgId) {
        this.warnPrimaryOrgId = warnPrimaryOrgId;
    }

    String warnPrimaryOrgId;

    public String getAccentity() {
        return accentity;
    }

    public void setAccentity(String accentity) {
        this.accentity = accentity;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    String accentity;
    String currency;
    String branch;

    String warnPrimaryOrgName;

    String currencyName;
    //银行网点
    String bankNumber_name;
    //交易日期
    String tran_date;
    //交易金额
    String tran_amt;
    //流水号
    String bank_seq_no;

}
