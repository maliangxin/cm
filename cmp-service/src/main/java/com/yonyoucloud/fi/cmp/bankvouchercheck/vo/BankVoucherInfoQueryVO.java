package com.yonyoucloud.fi.cmp.bankvouchercheck.vo;

import lombok.Data;

import java.util.Set;

/**
 * @description: 银行流水凭证信息查询参数
 * @author: wanxbo@yonyou.com
 * @date: 2025/2/14 15:29
 */
@Data
public class BankVoucherInfoQueryVO {
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
     * 对账截止日期
     */
    private String checkEndDate;

    /**
     * 交易开始日期； 结束日期为对账截止日期
     */
    private String tranStartDate;

    /**
     * 业务开始日期; 结束日期为对账截止日期
     */
    private String businessStartDate;

    /**
     * 账簿id集合
     */
    private Set<String> accbookids;
    /**
     * 对账数据源 1凭证；2银行日记账
     */
    private Short reconciliationDataSource;
    /**
     * 是否分页查询
     */
    private boolean pageQuery = false;
}
