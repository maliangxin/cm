package com.yonyoucloud.fi.cmp.fundpayment;

import lombok.Data;

import java.util.List;

@Data
public class FundCommonQueryDataVo {
    private List<String> accentityList;
    private String mainProject;
    private String startVouchDate;
    private String endVouchDate;
    private Short billtype;
    private Short settleStatus;
    private String expenseitem;
    private String subProject;
    private String entityUri;
}
