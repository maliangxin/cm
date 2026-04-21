package com.yonyoucloud.fi.cmp.intelligentdealdetail.model;

import lombok.Data;

@Data
public class DirectAccountRequestInfo {

    String currencyCode;
    String currency;
    String bankaccountId;
    String accEntityId;
    String banktype;
    String acct_name;
    String acct_no;
    String startDate;
    String endDate;
    String customNo;
    String lineNumber;
    String channel;
    private String tranCode;
    Object queryExtend;
    int begNum;
    String operator;
    String signature;
}
