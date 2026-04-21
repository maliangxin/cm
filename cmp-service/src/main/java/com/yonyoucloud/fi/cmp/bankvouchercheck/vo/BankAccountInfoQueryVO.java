package com.yonyoucloud.fi.cmp.bankvouchercheck.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @description: 银企对账工作台展示数据查询请求参数
 * @author: wanxbo@yonyou.com
 * @date: 2025/2/13 17:14
 */
@Data
public class BankAccountInfoQueryVO {
    /**
     * 对账组织集合
     */
    private List<String> accentityList = new ArrayList<>();

    /**
     * 银行账户集合
     */
    private List<String> bankAccountList = new ArrayList<>();

    /**
     * 币种集合
     */
    private List<String> currencyList = new ArrayList<>();

    /**
     * 银行类别集合
     */
    private List<String> banktypeList = new ArrayList<>();

    /**
     * 对账方案
     */
    private List<String> reconciliationSchemeList = new ArrayList<>();

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
     * 对账状态
     */
    private Short reconciliationStatus;

    /**
     * 对账数据源 1凭证；2银行日记账
     */
    private Short reconciliationDataSource;
}
