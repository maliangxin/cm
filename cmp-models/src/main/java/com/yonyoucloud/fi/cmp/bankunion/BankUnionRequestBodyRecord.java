package com.yonyoucloud.fi.cmp.bankunion;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 接收银企联接口的请求实体类
 */
@Data
public class BankUnionRequestBodyRecord {

    @NotNull
    String acct_no;//银行账号                                   enterprisebankaccount
    @NotNull
    String acct_name;//账户名称
    @NotNull
    String curr_code;//币种    @NotNull                       currency
    String bank_seq_no;//银行交易流水号                          bankseqno
    @NotNull
    String tran_date;//交易日期                                   trandate
    String tran_time;//交易时间                                   trantime
    @NotNull
    String dc_flag;    //收支方向：1支出 2收入                    dc_flag
    @NotNull
    BigDecimal tran_amt;//交易金额                              tran_amt
    BigDecimal acct_bal;//余额                                 acctbal
    String to_acct_no;//对方账号                                to_acct_no
    String to_acct_name;//对方户名                              to_acct_name
    String to_acct_bank;//对方开户行                            to_acct_bank
    String to_acct_bank_name;//对方开户行名                      to_acct_bank_name
    String cash_flag;//钞汇标志                                 cashflag
    String oper;//操作员                                        oper
    String value_date;//起息日                                    value_date
    String use_name;//用途                                     use_name
    String remark;//摘要                                       remark
    String remark01;//附言                                     remark01
    String bank_check_code; //银行对账编号                       bankcheckno
    @NotNull
    String unique_no; //唯一识别码                               unique_no

}
