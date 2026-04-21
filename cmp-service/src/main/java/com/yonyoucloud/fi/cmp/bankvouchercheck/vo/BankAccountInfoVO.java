package com.yonyoucloud.fi.cmp.bankvouchercheck.vo;

import lombok.Data;

/**
 * @description:银企对账工作台，展示数据中账户币种等信息
 * @author: wanxbo@yonyou.com
 * @date: 2025/2/13 17:33
 */
@Data
public class BankAccountInfoVO {
    /**
     * 银行账户
     */
    private String bankaccount;
    private String bankaccount_name;
    private String bankaccount_account;
    /**
     * 币种
     */
    private String currency;
    private String currency_name;
    private String currency_moneyDigit;
    /**
     * 对账组织
     */
    private String accentity;
    private String accentity_name;
    private String accentity_code;
    /**
     * 对账方案
     */
    private String reconciliationScheme;
    private String reconciliationScheme_name;
    /**
     * 银行类别
     */
    private String banktype;
    private String banktype_name;
    /**
     * 财务账簿
     */
    private String accbook_b;
    private String accbook_b_name;
    /**
     * 对账数据源 1凭证；2银行日记账
     */
    private Short reconciliationDataSource;
    /**
     * 对账方案启用日期，格式为yyyy-MM-dd
     */
    private String enableDate;
}
