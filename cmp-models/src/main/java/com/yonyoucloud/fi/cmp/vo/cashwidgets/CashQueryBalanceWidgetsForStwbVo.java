package com.yonyoucloud.fi.cmp.vo.cashwidgets;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CashQueryBalanceWidgetsForStwbVo {

    private BigDecimal deposit;//今日存款
    private BigDecimal depositQoq;//今日存款昨日环比

    private BigDecimal avaDeposit;//今日可用存款
    private BigDecimal avaDepositQoq;//今日可用存款昨日环比

    private BigDecimal bankDeposit;//商业银行存款
    private BigDecimal avaBankDeposit;//商业银行可用存款

    private BigDecimal companyDeposit;//财务公司存款
    private BigDecimal avaCompanyDeposit;//财务公司可用存款

    private BigDecimal settleCenterDeposit;//结算中心存款
    private BigDecimal avaSettleCenterDeposit;//结算中心可用存款
}
