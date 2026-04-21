package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model;

import com.yonyou.iuap.yms.annotation.*;
import com.yonyou.iuap.yms.param.BaseEntity;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.sqls.ODSSql;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * @Author guoyangy
 * @Date 2024/6/21 17:22
 * @Description todo
 * @Version 1.0
 */
@Data
@YMSEntity
@YMSTable(tableName = ODSSql.CMP_BANKDEALDETAIL_ODS_FAIL)
public class BankDealDetailODSFailModel extends BaseEntity {

    @YMSId
    @YMSColumn(name = "id")
    @YMSGeneratedValue(domain = "ctm-cmp")
    private String id;
    @YMSColumn(name = "traceid")
    private String traceId;
    @YMSColumn(name = "contentsignature")
    private String contentsignature;
    @YMSColumn(name = "acct_no")
    private String acctNo;
    @YMSColumn(name = "acct_name")
    private String acctName;
    @YMSColumn(name = "create_time")
    private Date create_time;// '创建时间',
    @YMSColumn(name = "tran_date")
    private String tran_date;
    @YMSColumn(name = "tenant_id")
    private Long tenant_id;// '租户',
    @YMSColumn(name = "ytenant_id")
    private String ytenant_id;// '租户id',
    @YMSColumn(name = "tran_time")
    private String tran_time;
    @YMSColumn(name = "dc_flag")
    private String dc_flag;
    @YMSColumn(name = "bank_seq_no")
    private String bank_seq_no;
    @YMSColumn(name = "to_acct_no")
    private String to_acct_no;
    @YMSColumn(name = "to_acct_name")
    private String to_acct_name;
    @YMSColumn(name = "to_acct_bank")
    private String to_acct_bank;
    @YMSColumn(name = "to_acct_bank_name")
    private String to_acct_bank_name;
    @YMSColumn(name = "curr_code")
    private String curr_code;
    @YMSColumn(name = "cash_flag")
    private String cash_flag;
    @YMSColumn(name = "acct_bal")
    private BigDecimal acct_bal;
    @YMSColumn(name = "tran_amt")
    private BigDecimal tran_amt;
    @YMSColumn(name = "oper")
    private String oper;
    @YMSColumn(name = "value_date")
    private String value_date;
    @YMSColumn(name = "use_name")
    private String use_name;
    @YMSColumn(name = "remark")
    private String remark;
    @YMSColumn(name = "bank_reconciliation_code")
    private String bank_reconciliation_code;
    @YMSColumn(name = "unique_no")
    private String unique_no;
    @YMSColumn(name = "detail_check_id")
    private String detail_check_id;
    @YMSColumn(name = "bank_check_code")
    private String bank_check_code;
    @YMSColumn(name = "rate")
    private String rate;
    @YMSColumn(name = "fee_amt")
    private String fee_amt;
    @YMSColumn(name = "fee_amt_cur")
    private String fee_amt_cur;
    @YMSColumn(name = "remark01")
    private String remark01;
    @YMSColumn(name = "pay_use_desc")
    private String pay_use_desc;
    @YMSColumn(name = "corr_fee_amt")
    private String corr_fee_amt;
    @YMSColumn(name = "corr_fee_amt_cur")
    private String corr_fee_amt_cur;
    @YMSColumn(name = "sub_name")
    private String sub_name;
    @YMSColumn(name = "sub_code")
    private String sub_code;
    @YMSColumn(name = "proj_name")
    private String proj_name;
    @YMSColumn(name = "budget_source")
    private String budget_source;
    @YMSColumn(name = "voucher_type")
    private String voucher_type;
    @YMSColumn(name = "voucher_no")
    private String voucher_no;
    @YMSColumn(name = "mdcard_no")
    private String mdcard_no;
    @YMSColumn(name = "mdcard_name")
    private String mdcard_name;
    @YMSColumn(name = "payment_manage_type")
    private String payment_manage_type;
    @YMSColumn(name = "eco_class")
    private String eco_class;
    @YMSColumn(name = "budget_relevance_no")
    private String budget_relevance_no;
    @YMSColumn(name = "add_amt")
    private String add_amt;
    @YMSColumn(name = "balance")
    private String balance;
    @YMSColumn(name = "is_refund")
    private String is_refund;
    @YMSColumn(name = "refund_original_transaction")
    private String refund_original_transaction;
    @YMSColumn(name = "bankaccountid")
    private String bankaccountid;
    @YMSColumn(name = "currencyid")
    private String currencyid;
    @YMSColumn(name = "orgid")
    private String orgid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankDealDetailODSFailModel that = (BankDealDetailODSFailModel) o;
        return Objects.equals(contentsignature, that.contentsignature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentsignature);
    }
}
