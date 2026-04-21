package com.yonyoucloud.fi.cmp.bankvouchercheck.vo;

import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * @description: 银企对账工作台-账户对账信息展示VO
 * @author: wanxbo@yonyou.com
 * @date: 2025/2/13 17:16
 */
@Data
public class BankAccountReconciliationInfoVO {
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
     * 余额调节表id
     */
    private BalanceAdjustResult balanceAdjustResult;
    /**
     * 账簿id集合
     */
    private Set<String> accbookids;
    /**
     * 对账状态
     */
    private Short reconciliationStatus;
    /**
     * 余额调节表状态
     */
    private Short balanceStatus;
    /**
     * 未勾对账凭证数量
     */
    private Integer voucherUncheckTotalNum;
    /**
     * 未勾对凭证借方金额合计
     */
    private BigDecimal voucherUncheckDebitAmountSum;
    /**
     * 未勾对凭证贷方金额合计
     */
    private BigDecimal voucherUncheckCreditAmountSum;
    /**
     * 未勾对银行流水总笔数
     */
    private Integer bankUncheckTotalNum;
    /**
     * 未勾对银行流水借方金额合计
     */
    private BigDecimal bankUncheckDebitAmountSum;
    /**
     * 未勾对银行流水贷方金额合计
     */
    private BigDecimal bankUncheckCreditAmountSum;
    /**
     * 银行账户余额
     */
    private BigDecimal bankBalanceAmoount;
    /**
     * 企业日记账余额
     */
    private BigDecimal journalBalanceAmoount;
}
