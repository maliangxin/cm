package com.yonyoucloud.fi.cmp.cashhttp;

import lombok.Data;

import java.util.Date;

/**
 * 用于现金管理 和银企联交互的相关信息
 * 使用此vo实体
 */
@Data
public class CashHttpBankEnterpriseLinkVo {

    private String accentity;
    private String enterpriseBankAccount;
    private String acct_no;
    private String acct_name;
    private String bank;
    private String curr_code;
    private String customNo;
    private String signature;
    private String operator;
    private String beg_date;
    private String end_date;
    private String channel;
}
