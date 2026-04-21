package com.yonyoucloud.fi.cmp.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 *
 */
@Data
@Builder
public class BalanceAdjustQueryVO {

    Boolean initFlag;

    String accentity;

    String bankaccount;

    Date dzdate;

    Date enableDate;

    String currency;

    Long bankreconciliationscheme;

    BigDecimal debitoriSumJour;//借方原币金额

    BigDecimal creditoriSumJour;//贷方原币金额

    BigDecimal journalye;//企业日记账余额

    BigDecimal bankye;//银行对账单余额

}
