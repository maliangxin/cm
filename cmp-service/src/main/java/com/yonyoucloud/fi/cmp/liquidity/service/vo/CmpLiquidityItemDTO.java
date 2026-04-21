package com.yonyoucloud.fi.cmp.liquidity.service.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <h1>CmpLiquidityItemDTO</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-05-23 12:29
 */
@Data
public class CmpLiquidityItemDTO {

    public Short netMovementCategory;
    public String liquidityProject;
    public String instrument;
    public String transType;
    public String planProgram;
    public Short sourceBusinessSystem;
    public String launchBusinessSystem;
    public String accentity;
    public String accentityName;
    public String orgId;
    public String deptId;
    public String projectId;
    public Date businessDate;
    public Short businessType;
    public String launchBusiness;
    public Short businessDetailType;
    public String billNum;
    public String billId;
    public String serviceCode;
    public String billDetailNum;
    public String paymentType;
    public String settlementMethod;
    public String ourAccId;
    public String ourAcc;
    public String ourCashAccId;
    public String ourCashAcc;
    public String noteId;
    public String noteCode;
    public String expenseItem;
    public String contractNum;
    public String contractName;
    public Short oppType;
    public String oppDocId;
    public String oppName;
    public String oppAcc;
    public String oppAccName;
    public String oppAccType;
    public String oppAccOpenName;
    public Boolean isOpeningBalance;
    public Integer receiptType;
    public String originCurrency;
    public BigDecimal originAmount;
    public String exchangeRateType;
    public String remark;
    public Date planSettlementDate;
    public Short planCycle;
    public String businessContrastCode;
    public String extra;
    public String businessLink;
    private Boolean isIntegrateSettlementPlatform;
    private Short businessStatus;
    private Short settleStatus;
    private Short payStatus;
    private Short dataSource;

}
