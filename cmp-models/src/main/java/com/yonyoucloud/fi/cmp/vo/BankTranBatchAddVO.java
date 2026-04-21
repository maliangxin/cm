package com.yonyoucloud.fi.cmp.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.io.Serializable;


/**
 * 银行回单
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTranBatchAddVO implements Serializable {

    private static final long serialVersionUID = 1L;


    /**资金组织ID 非必填，同时传入以ID为准*/
    private String accentity;
    /**资金组织编码 非必填，同时传入以ID为准 1*/
    private String accentity_code;

    /**币种ID 必填其一，同时传入以ID为准*/
    private String currency;
    /**币种编码 必填其一，同时传入以ID为准 1*/
    private String currency_code;

    /**本方银行账户ID 必填其一，同时传入以ID为准*/
    private String bankaccount;
    /**本方银行账户-账号 必填其一，同时传入以ID为准 1*/
    private String bankaccount_account;

    /**回单编号 存储传入值*/
    @NotEmpty(message = "回单编号不能为空")//@notranslate
    private String receiptno;
    /**银行交易流水号*/
    @NotEmpty(message = "银行交易流水号不能为空")//@notranslate
    private String bank_seq_no;

    /**交易日期 yyyy-MM-dd*/
    @NotEmpty(message = "交易日期不能为空")//@notranslate
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "交易日期格式不正确，应为 yyyy-MM-dd")//@notranslate
    private String tran_date;
    /**交易时间 yyyy-MM-dd HH:mm:ss*/
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$", message = "交易时间格式不正确，应为 yyyy-MM-dd HH:mm:ss")//@notranslate
    private String tranTime;

    /**收支标识*/
    @NotEmpty(message = "收支标识不能为空 1:借 2:贷")//@notranslate
    private String dc_flag;
    /**交易金额*/
    @NotEmpty(message = "交易金额不能为空")//@notranslate
    private String tran_amt;
    /**银行对账码*/
    private String bankcheckcode;
    /**对方账号*/
    private String to_acct_no;
    /**对方账户名*/
    private String to_acct_name;
    /**对方开户行*/
    private String to_acct_bank;
    /**对方开户行名*/
    private String to_acct_bank_name;
    /**用途*/
    private String use_name;
    /**摘要*/
    private String remark;
    /**附言*/
    private String postscript;
    /**扩展信息*/
    private String extendContent;
    /**电子回单文件名称*/
    private String filename;

    /**银行类别Id*/
    private String bankTypeId;
    /**流水回单关联码*/
    private String detailReceiptRelationCode;

}
