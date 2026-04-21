package com.yonyoucloud.fi.cmp.intelligentdealdetail.converts;


import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;

public class BaseConvertModel {
    /**
     * 主键
     */
    @DictColumn(detailField = "id",reconciliationField = "id",odsField = "id")
    public String id;

    /**
     * 授权使用组织
     */
    @DictColumn(detailField = "authoruseaccentity",reconciliationField = "accentity")
    public String accentity;
    /**
     * 企业银行账户
     */
    @DictColumn(detailField = "enterpriseBankAccount",reconciliationField = "bankaccount" ,odsField = "bankaccount")
    public String enterpriseBankAccount;
    /**
     * 登账日期
     */
    @DictColumn(reconciliationField = "dzdate", odsField = "tran_date")
    public java.util.Date dzdate;
    /**
     * 交易日期
     */
    @DictColumn(detailField = "tranDate",reconciliationField = "tran_date", odsField = "tran_date")
    public java.util.Date tranDate;
    /**
     * 交易时间
     */
    @DictColumn(detailField = "tranTime",reconciliationField = "tran_time", odsField = "tran_time")
    public java.util.Date tranTime;
    /**
     * 获取借/贷
     */
    @DictColumn(detailField = "dc_flag",reconciliationField = "dc_flag", odsField = "dc_flag")
    public Direction dc_flag;
    /**
     * 银行交易流水号
     */
    @DictColumn(detailField = "bankseqno",reconciliationField = "bank_seq_no", odsField = "bank_seq_no")
    public String bankseqno;
    /**
     * 对号方账
     */
    @DictColumn(detailField = "to_acct_no",reconciliationField = "to_acct_no", odsField = "to_acct_no")
    public String to_acct_no;
    /**
     * 对方户名
     */
    @DictColumn(detailField = "to_acct_name",reconciliationField = "to_acct_name", odsField = "to_acct_name")
    public String to_acct_name;
    /**
     * 对方开户行
     */
    @DictColumn(detailField = "to_acct_bank",reconciliationField = "to_acct_bank", odsField = "to_acct_bank")
    public String to_acct_bank;
    /**
     * 对方开户行名
     */
    @DictColumn(detailField = "to_acct_bank_name",reconciliationField = "to_acct_bank_name", odsField = "to_acct_bank_name")
    public String to_acct_bank_name;
    /**
     * 币种
     */
    @DictColumn(detailField = "currency",reconciliationField = "currency", odsField = "currency")
    public String currency;
    /**
     * 钞汇标志
     */
    @DictColumn(detailField = "cashflag",reconciliationField = "cashflag", odsField = "cash_flag")
    public String cashflag;
    /**
     * 币种编码
     */
    @DictColumn(detailField = "currency_code",reconciliationField = "currency_code", odsField = "curr_code")
    public String curr_code;

    /**
     * 余额
     */
    @DictColumn(detailField = "acctbal",reconciliationField = "acct_bal", odsField = "acct_bal")
    public java.math.BigDecimal acctbal;
    /**
     * 交易金额
     */
    @DictColumn(detailField = "tran_amt",reconciliationField = "tran_amt", odsField = "tran_amt")
    public java.math.BigDecimal tran_amt;
    /**
     * 操作员
     */
    @DictColumn(detailField = "oper",reconciliationField = "oper", odsField = "oper")
    public String oper;
    /**
     * 起息日
     */
    @DictColumn(detailField = "value_date",reconciliationField = "value_date", odsField = "value_date")
    public java.util.Date value_date;
    /**
     * 用途
     */
    @DictColumn(detailField = "use_name",reconciliationField = "use_name", odsField = "use_name")
    public String use_name;
    /**
     * 摘要
     */
    @DictColumn(detailField = "remark",reconciliationField = "remark", odsField = "remark")
    public String remark;
    /**
     * 银行账户银行类别
     */
    @DictColumn(detailField = "banktype",reconciliationField = "banktype")
    public String banktype;
    /**
     * 创建时间
     */
    @DictColumn(detailField = "createTime",reconciliationField = "createTime" , odsField = "create_time")
    public java.util.Date createTime;
    /**
     * 创建日期
     */
    @DictColumn(detailField = "createDate",reconciliationField = "createDate")
    public java.util.Date createDate;
    /**
     * 修改时间
     */
    @DictColumn(detailField = "modifyTime",reconciliationField = "modifyTime")
    public java.util.Date modifyTime;
    /**
     * 修改日期
     */
    @DictColumn(detailField = "modifyDate",reconciliationField = "modifyDate")
    public java.util.Date modifyDate;
    /**
     * 创建人名称
     */
    @DictColumn(detailField = "creator",reconciliationField = "creator")
    public String creator;
    /**
     * 修改人名称
     */
    @DictColumn(detailField = "modifier",reconciliationField = "modifier")
    public String modifier;
    /**
     * 创建人
     */
    @DictColumn(detailField = "creatorId",reconciliationField = "creatorId")
    public Long creatorId;
    /**
     * 修改人
     */
    @DictColumn(detailField = "modifierId",reconciliationField = "modifierId")
    public Long modifierId;
    /**
     * 时间戳
     */
    @DictColumn(detailField = "pubts",reconciliationField = "pubts")
    public java.util.Date pubts;
    /**
     * 内部账户交易明细标志
     */
    @DictColumn(detailField = "isaccountflag",reconciliationField = "isaccountflag")
    public Boolean isaccountflag;
    /**
     * 代理手续费
     */
    @DictColumn(detailField = "corr_fee_amt",reconciliationField = "corr_fee_amt",odsField = "corr_fee_amt")
    public java.math.BigDecimal corr_fee_amt;
    /**
     * 代理手续费币种
     */
    @DictColumn(detailField = "corr_fee_amt_cur",reconciliationField = "corr_fee_amt_cur",odsField = "corr_fee_amt_cur")
    public String corr_fee_amt_cur;
    /**
     * 手续费金额
     */
    @DictColumn(detailField = "fee_amt",reconciliationField = "fee_amt",odsField = "fee_amt")
    public java.math.BigDecimal fee_amt;
    /**
     * 手续费币种
     */
    @DictColumn(detailField = "fee_amt_cur",reconciliationField = "fee_amt_cur",odsField = "fee_amt_cur")
    public String fee_amt_cur;
    /**
     * 汇款用途
     */
    @DictColumn(detailField = "pay_use_desc",reconciliationField = "pay_use_desc",odsField = "pay_use_desc")
    public String pay_use_desc;
    /**
     * 汇率
     */
    @DictColumn(detailField = "rate",reconciliationField = "rate",odsField = "rate")
    public java.math.BigDecimal rate;
    /**
     * 新增附言
     */
    @DictColumn(detailField = "remark01",reconciliationField = "remark01",odsField = "remark01")
    public String remark01;

    /**
     * 唯一标识码
     */
    @DictColumn(detailField = "unique_no",reconciliationField = "unique_no",odsField = "unique_no")
    public String unique_no;

    /**
     * 字段唯一标识码
     */
    @DictColumn(detailField = "concat_info",reconciliationField = "concat_info")
    public String concat_info;

    /**
     * 利息
     */
    @DictColumn(detailField = "interest",reconciliationField = "interest")
    public java.math.BigDecimal interest;
    /**
     * 银行账户id
     */
    @DictColumn(detailField = "bankaccount",reconciliationField = "bankaccount" , odsField = "bankaccount")
    public String bankaccount;
    /**
     * 所属组织id
     */
    @DictColumn(detailField = "accentity",reconciliationField = "orgid" , odsField = "orgid")
    public String orgid;
    /**
     * 代理银行账户
     */
    @DictColumn(detailField = "agentBankAccount",reconciliationField = "agentBankAccount")
    public String agentBankAccount;

    /**
     * 代理银行账户
     */
    @DictColumn(detailField = "bankdetailno",reconciliationField = "bankcheckno",odsField = "bank_check_code")
    public String bankcheckno;

    /**
     * 回单关联码
     */
    @DictColumn(detailField = "detailReceiptRelationCode",reconciliationField = "detailReceiptRelationCode",odsField = "detail_check_id")
    public String detailReceiptRelationCode;
}
