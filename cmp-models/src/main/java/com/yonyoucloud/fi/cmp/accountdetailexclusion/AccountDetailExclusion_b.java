package com.yonyoucloud.fi.cmp.accountdetailexclusion;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 账户收支明细剔除子表实体
 *
 * @author u
 * @version 1.0
 */
public class AccountDetailExclusion_b extends BizObject implements ITenant, IYTenant {
    // 实体全称
    public static final String ENTITY_NAME = "cmp.accountdetailexclusion.AccountDetailExclusion_b";

    /**
     * 获取主表id
     *
     * @return 主表id.ID
     */
    public Long getMainid() {
        return get("mainid");
    }

    /**
     * 设置主表id
     *
     * @param mainid 主表id.ID
     */
    public void setMainid(Long mainid) {
        set("mainid", mainid);
    }

    /**
     * 获取银行对账单ID
     *
     * @return 银行对账单ID
     */
    public String getBankReconciliationId() {
        return get("bankReconciliationId");
    }

    /**
     * 设置银行对账单ID
     *
     * @param bankReconciliationId 银行对账单ID
     */
    public void setBankReconciliationId(String bankReconciliationId) {
        set("bankReconciliationId", bankReconciliationId);
    }

    /**
     * 获取账户收支明细剔除单子表特征
     *
     * @return 账户收支明细剔除单子表特征.ID
     */
    public String getCharacterDefb() {
        return get("characterDefb");
    }

    /**
     * 设置账户收支明细剔除单子表特征
     *
     * @param characterDefb 账户收支明细剔除单子表特征.ID
     */
    public void setCharacterDefb(String characterDefb) {
        set("characterDefb", characterDefb);
    }

    /**
     * 获取资金组织
     *
     * @return 资金组织.ID
     */
    public String getAccentity() {
        return get("accentity");
    }

    /**
     * 设置资金组织
     *
     * @param accentity 资金组织.ID
     */
    public void setAccentity(String accentity) {
        set("accentity", accentity);
    }

    /**
     * 获取交易日期
     *
     * @return 交易日期
     */
    public java.util.Date getTran_date() {
        return get("tran_date");
    }

    /**
     * 设置交易日期
     *
     * @param tran_date 交易日期
     */
    public void setTran_date(java.util.Date tran_date) {
        set("tran_date", tran_date);
    }

    /**
     * 获取银行交易流水号
     *
     * @return 银行交易流水号
     */
    public String getBank_seq_no() {
        return get("bank_seq_no");
    }

    /**
     * 设置银行交易流水号
     *
     * @param bank_seq_no 银行交易流水号
     */
    public void setBank_seq_no(String bank_seq_no) {
        set("bank_seq_no", bank_seq_no);
    }

    /**
     * 获取银行类别
     *
     * @return 银行类别.ID
     */
    public String getBanktype() {
        return get("banktype");
    }

    /**
     * 设置银行类别
     *
     * @param banktype 银行类别.ID
     */
    public void setBanktype(String banktype) {
        set("banktype", banktype);
    }

    /**
     * 获取银行账户
     *
     * @return 银行账户.ID
     */
    public String getBankaccount() {
        return get("bankaccount");
    }

    /**
     * 设置银行账户
     *
     * @param bankaccount 银行账户.ID
     */
    public void setBankaccount(String bankaccount) {
        set("bankaccount", bankaccount);
    }

    /**
     * 获取收付方向
     *
     * @return 收付方向
     */
    public Short getDc_flag() {
        return getShort("dc_flag");
    }

    /**
     * 设置收付方向
     *
     * @param dc_flag 收付方向
     */
    public void setDc_flag(Short dc_flag) {
        set("dc_flag", dc_flag);
    }

    /**
     * 获取币种
     *
     * @return 币种.ID
     */
    public String getCurrency() {
        return get("currency");
    }

    /**
     * 设置币种
     *
     * @param currency 币种.ID
     */
    public void setCurrency(String currency) {
        set("currency", currency);
    }

    /**
     * 获取金额
     *
     * @return 金额
     */
    public java.math.BigDecimal getTran_amt() {
        return get("tran_amt");
    }

    /**
     * 设置金额
     *
     * @param tran_amt 金额
     */
    public void setTran_amt(java.math.BigDecimal tran_amt) {
        set("tran_amt", tran_amt);
    }

    /**
     * 获取剔除金额
     *
     * @return 剔除金额
     */
    public java.math.BigDecimal getEliminate_amt() {
        return get("eliminate_amt");
    }

    /**
     * 设置剔除金额
     *
     * @param eliminate_amt 剔除金额
     */
    public void setEliminate_amt(java.math.BigDecimal eliminate_amt) {
        set("eliminate_amt", eliminate_amt);
    }

    /**
     * 获取剔除后余额
     *
     * @return 剔除后余额
     */
    public java.math.BigDecimal getAfter_eliminate_amt() {
        return get("after_eliminate_amt");
    }

    /**
     * 设置剔除后余额
     *
     * @param after_eliminate_amt 剔除后余额
     */
    public void setAfter_eliminate_amt(java.math.BigDecimal after_eliminate_amt) {
        set("after_eliminate_amt", after_eliminate_amt);
    }

    /**
     * 获取剔除原因类型
     *
     * @return 剔除原因类型
     */
    public String getEliminateReasonType() {
        return get("eliminateReasonType");
    }

    /**
     * 设置剔除原因类型
     *
     * @param eliminateReasonType 剔除原因类型
     */
    public void setEliminateReasonType(String eliminateReasonType) {
        set("eliminateReasonType", eliminateReasonType);
    }

    /**
     * 获取剔除原因
     *
     * @return 剔除原因
     */
    public String getRemovereasons() {
        return get("removereasons");
    }

    /**
     * 设置剔除原因
     *
     * @param removereasons 剔除原因
     */
    public void setRemovereasons(String removereasons) {
        set("removereasons", removereasons);
    }

    /**
     * 获取余额
     *
     * @return 余额
     */
    public java.math.BigDecimal getAcct_bal() {
        return get("acct_bal");
    }

    /**
     * 设置余额
     *
     * @param acct_bal 余额
     */
    public void setAcct_bal(java.math.BigDecimal acct_bal) {
        set("acct_bal", acct_bal);
    }

    /**
     * 获取对方账号id
     *
     * @return 对方账号id
     */
    public String getTo_acct() {
        return get("to_acct");
    }

    /**
     * 设置对方账号id
     *
     * @param to_acct 对方账号id
     */
    public void setTo_acct(String to_acct) {
        set("to_acct", to_acct);
    }

    /**
     * 获取对方账号
     *
     * @return 对方账号
     */
    public String getTo_acct_no() {
        return get("to_acct_no");
    }

    /**
     * 设置对方账号
     *
     * @param to_acct_no 对方账号
     */
    public void setTo_acct_no(String to_acct_no) {
        set("to_acct_no", to_acct_no);
    }

    /**
     * 获取对方户名
     *
     * @return 对方户名
     */
    public String getTo_acct_name() {
        return get("to_acct_name");
    }

    /**
     * 设置对方户名
     *
     * @param to_acct_name 对方户名
     */
    public void setTo_acct_name(String to_acct_name) {
        set("to_acct_name", to_acct_name);
    }

    /**
     * 获取对方开户行
     *
     * @return 对方开户行
     */
    public String getTo_acct_bank() {
        return get("to_acct_bank");
    }

    /**
     * 设置对方开户行
     *
     * @param to_acct_bank 对方开户行
     */
    public void setTo_acct_bank(String to_acct_bank) {
        set("to_acct_bank", to_acct_bank);
    }

    /**
     * 获取对方开户行名
     *
     * @return 对方开户行名
     */
    public String getTo_acct_bank_name() {
        return get("to_acct_bank_name");
    }

    /**
     * 设置对方开户行名
     *
     * @param to_acct_bank_name 对方开户行名
     */
    public void setTo_acct_bank_name(String to_acct_bank_name) {
        set("to_acct_bank_name", to_acct_bank_name);
    }

    /**
     * 获取用途
     *
     * @return 用途
     */
    public String getUse_name() {
        return get("use_name");
    }

    /**
     * 设置用途
     *
     * @param use_name 用途
     */
    public void setUse_name(String use_name) {
        set("use_name", use_name);
    }

    /**
     * 获取摘要
     *
     * @return 摘要
     */
    public String getRemark() {
        return get("remark");
    }

    /**
     * 设置摘要
     *
     * @param remark 摘要
     */
    public void setRemark(String remark) {
        set("remark", remark);
    }

    /**
     * 获取交易时间
     *
     * @return 交易时间
     */
    public java.util.Date getTran_time() {
        return get("tran_time");
    }

    /**
     * 设置交易时间
     *
     * @param tran_time 交易时间
     */
    public void setTran_time(java.util.Date tran_time) {
        set("tran_time", tran_time);
    }

    /**
     * 获取银行对账编号
     *
     * @return 银行对账编号
     */
    public String getBankcheckno() {
        return get("bankcheckno");
    }

    /**
     * 设置银行对账编号
     *
     * @param bankcheckno 银行对账编号
     */
    public void setBankcheckno(String bankcheckno) {
        set("bankcheckno", bankcheckno);
    }

    /**
     * 获取租户
     *
     * @return 租户.ID
     */
    public Long getTenant() {
        return get("tenant");
    }

    /**
     * 设置租户
     *
     * @param tenant 租户.ID
     */
    public void setTenant(Long tenant) {
        set("tenant", tenant);
    }

    /**
     * 获取租户id
     *
     * @return 租户id.ID
     */
    public String getYtenant() {
        return get("ytenant");
    }

    /**
     * 设置租户id
     *
     * @param ytenant 租户id.ID
     */
    public void setYtenant(String ytenant) {
        set("ytenant", ytenant);
    }


    /**
     * 获取序号
     *
     * @return 序号
     */
    public Integer getRowno() {
        return get("rowno");
    }

    /**
     * 设置序号
     *
     * @param rowno 序号
     */
    public void setRowno(Integer rowno) {
        set("rowno", rowno);
    }

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
    public java.util.Date getPubts() {
        return get("pubts");
    }

    /**
     * 设置时间戳
     *
     * @param pubts 时间戳
     */
    public void setPubts(java.util.Date pubts) {
        set("pubts", pubts);
    }


    /**
     * 获取行号
     *
     * @return 行号
     */
    public java.math.BigDecimal getLineno() {
        return get("lineno");
    }

    /**
     * 设置行号
     *
     * @param lineno 行号
     */
    public void setLineno(java.math.BigDecimal lineno) {
        set("lineno", lineno);
    }

    /**
     * 获取租户id
     *
     * @return 租户id
     */
    @Override
    public String getYTenant() {
        return get("ytenant");
    }

    /**
     * 设置租户id
     *
     * @param ytenant 租户id
     */
    @Override
    public void setYTenant(String ytenant) {
        set("ytenant", ytenant);
    }
}
