package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 银行对账单实体
 *
 * @author u
 * @version 1.0
 */
public class BalanceadjustBankreconciliation extends BizObject implements IAuditInfo, ITenant{
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.balanceadjustresult.BalanceadjustBankreconciliation";

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
     * 获取对账单日期
     *
     * @return 对账单日期
     */
	public java.util.Date getDzdate() {
		return get("dzdate");
	}

    /**
     * 设置对账单日期
     *
     * @param dzdate 对账单日期
     */
	public void setDzdate(java.util.Date dzdate) {
		set("dzdate", dzdate);
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
     * 获取借方金额
     *
     * @return 借方金额
     */
	public java.math.BigDecimal getDebitamount() {
		return get("debitamount");
	}

    /**
     * 设置借方金额
     *
     * @param debitamount 借方金额
     */
	public void setDebitamount(java.math.BigDecimal debitamount) {
		set("debitamount", debitamount);
	}

    /**
     * 获取贷方金额
     *
     * @return 贷方金额
     */
	public java.math.BigDecimal getCreditamount() {
		return get("creditamount");
	}

    /**
     * 设置贷方金额
     *
     * @param creditamount 贷方金额
     */
	public void setCreditamount(java.math.BigDecimal creditamount) {
		set("creditamount", creditamount);
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
	 * 获取第三方流水号
	 *
	 * @return 第三方流水号
	 */
	public String getThirdserialno() {
		return get("thirdserialno");
	}

	/**
	 * 设置第三方流水号
	 *
	 * @param thirdserialno 第三方流水号
	 */
	public void setThirdserialno(String thirdserialno) {
		set("thirdserialno", thirdserialno);
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
     * 获取是否已勾对
     *
     * @return 是否已勾对
     */
	public Boolean getCheckflag() {
	    return getBoolean("checkflag");
	}

    /**
     * 设置是否已勾对
     *
     * @param checkflag 是否已勾对
     */
	public void setCheckflag(Boolean checkflag) {
		set("checkflag", checkflag);
	}


	/**
	 * 获取其他模块是否已勾对
	 *
	 * @return 其他模块是否已勾对
	 */
	public Boolean getOther_checkflag() {
		return getBoolean("other_checkflag");
	}

	/**
	 * 设置其他模块是否已勾对
	 *
	 * @param other_checkflag 其他模块是否已勾对
	 */
	public void setOther_checkflag(Boolean other_checkflag) {
		set("other_checkflag", other_checkflag);
	}

    /**
     * 获取勾对人
     *
     * @return 勾对人.ID
     */
	public Long getCheckman() {
		return get("checkman");
	}

    /**
     * 设置勾对人
     *
     * @param checkman 勾对人.ID
     */
	public void setCheckman(Long checkman) {
		set("checkman", checkman);
	}

    /**
     * 获取勾对日期
     *
     * @return 勾对日期
     */
	public java.util.Date getCheckdate() {
		return get("checkdate");
	}

    /**
     * 设置勾对日期
     *
     * @param checkdate 勾对日期
     */
	public void setCheckdate(java.util.Date checkdate) {
		set("checkdate", checkdate);
	}

	/**
	 * 获取总账勾对日期
	 *
	 * @return 总账勾对日期
	 */
	public java.util.Date getOther_checkdate() {
		return get("other_checkdate");
	}

	/**
	 * 设置总账勾对日期
	 *
	 * @param checkdate 总账勾对日期
	 */
	public void setOther_checkdate(java.util.Date checkdate) {
		set("other_checkdate", checkdate);
	}

    /**
     * 获取勾对号
     *
     * @return 勾对号
     */
	public String getCheckno() {
		return get("checkno");
	}

    /**
     * 设置勾对号
     *
     * @param checkno 勾对号
     */
	public void setCheckno(String checkno) {
		set("checkno", checkno);
	}


	/**
	 * 获取总账勾对号
	 *
	 * @return 总帐勾对号
	 */
	public String getOther_checkno() {
		return get("other_checkno");
	}


	/**
	 * 设置总账勾对号
	 *
	 * @param other_checkno 总账勾对号
	 */
	public void setOther_checkno(String other_checkno) {
		set("other_checkno", other_checkno);
	}

	/**
	 * 获取是否已自动生单
	 *
	 * @return 是否已自动生单
	 */
	public Boolean getAutobill() {
		return getBoolean("autobill");
	}

	/**
	 * 设置是否期初
	 *
	 * @param autobill 是否已自动生单
	 */
	public void setAutobill(Boolean autobill) {
		set("autobill", autobill);
	}

	/**
	 * 获取余额调节表id
	 *
	 * @return 余额调节表.ID
	 */
	public Long getBalanceadjustresultid() {
		return get("balanceadjustresultid");
	}

	/**
	 * 设置余额调节表id
	 *
	 * @param balanceadjustresultid 余额调节表..ID
	 */
	public void setBalanceadjustresultid(Long balanceadjustresultid) {
		set("balanceadjustresultid", balanceadjustresultid);
	}

	/**
	 * 获取创建时间
	 *
	 * @return 创建时间
	 */
	public java.util.Date getCreateTime() {
		return get("createTime");
	}

	/**
	 * 设置创建时间
	 *
	 * @param createTime 创建时间
	 */
	public void setCreateTime(java.util.Date createTime) {
		set("createTime", createTime);
	}

	/**
	 * 获取创建日期
	 *
	 * @return 创建日期
	 */
	public java.util.Date getCreateDate() {
		return get("createDate");
	}

	/**
	 * 设置创建日期
	 *
	 * @param createDate 创建日期
	 */
	public void setCreateDate(java.util.Date createDate) {
		set("createDate", createDate);
	}

	/**
	 * 获取修改时间
	 *
	 * @return 修改时间
	 */
	public java.util.Date getModifyTime() {
		return get("modifyTime");
	}

	/**
	 * 设置修改时间
	 *
	 * @param modifyTime 修改时间
	 */
	public void setModifyTime(java.util.Date modifyTime) {
		set("modifyTime", modifyTime);
	}

	/**
	 * 获取修改日期
	 *
	 * @return 修改日期
	 */
	public java.util.Date getModifyDate() {
		return get("modifyDate");
	}

	/**
	 * 设置修改日期
	 *
	 * @param modifyDate 修改日期
	 */
	public void setModifyDate(java.util.Date modifyDate) {
		set("modifyDate", modifyDate);
	}

	/**
	 * 获取创建人名称
	 *
	 * @return 创建人名称
	 */
	public String getCreator() {
		return get("creator");
	}

	/**
	 * 设置创建人名称
	 *
	 * @param creator 创建人名称
	 */
	public void setCreator(String creator) {
		set("creator", creator);
	}

	/**
	 * 获取修改人名称
	 *
	 * @return 修改人名称
	 */
	public String getModifier() {
		return get("modifier");
	}

	/**
	 * 设置修改人名称
	 *
	 * @param modifier 修改人名称
	 */
	public void setModifier(String modifier) {
		set("modifier", modifier);
	}

	/**
	 * 获取创建人
	 *
	 * @return 创建人.ID
	 */
	public Long getCreatorId() {
		return get("creatorId");
	}

	/**
	 * 设置创建人
	 *
	 * @param creatorId 创建人.ID
	 */
	public void setCreatorId(Long creatorId) {
		set("creatorId", creatorId);
	}

	/**
	 * 获取修改人
	 *
	 * @return 修改人.ID
	 */
	public Long getModifierId() {
		return get("modifierId");
	}

	/**
	 * 设置修改人
	 *
	 * @param modifierId 修改人.ID
	 */
	public void setModifierId(Long modifierId) {
		set("modifierId", modifierId);
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
	 * 获取所属组织
	 *
	 * @return 所属组织.ID
	 */
	public String getOrgid() {
		return get("orgid");
	}

	/**
	 * 设置所属组织
	 *
	 * @param orgid 所属组织.ID
	 */
	public void setOrgid(String orgid) {
		set("orgid", orgid);
	}
}
