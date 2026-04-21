package com.yonyoucloud.fi.cmp.bankreconciliation;

import org.imeta.orm.base.BizObject;

/**
 * 认领单明细子表子表实体
 */
public class BillClaimItem extends BizObject {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankreconciliation.BillClaimItem";

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
     * 获取银行对账单
     *
     * @return 银行对账单.ID
     */
	public Long getBankbill() {
		return get("bankbill");
	}

    /**
     * 设置银行对账单
     *
     * @param bankbill 银行对账单.ID
     */
	public void setBankbill(Long bankbill) {
		set("bankbill", bankbill);
	}

    /**
     * 获取认领金额
     *
     * @return 认领金额
     */
	public java.math.BigDecimal getClaimamount() {
		return get("claimamount");
	}

    /**
     * 设置认领金额
     *
     * @param claimamount 认领金额
     */
	public void setClaimamount(java.math.BigDecimal claimamount) {
		set("claimamount", claimamount);
	}

    /**
     * 获取认领合计金额
     *
     * @return 认领合计金额
     */
	public java.math.BigDecimal getTotalamount() {
		return get("totalamount");
	}

    /**
     * 设置认领合计金额
     *
     * @param totalamount 认领合计金额
     */
	public void setTotalamount(java.math.BigDecimal totalamount) {
		set("totalamount", totalamount);
	}

    /**
     * 获取核算会计 主体
     *
     * @return 核算会计 主体.ID
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
	 * 获取交易金额
	 *
	 * @return 交易金额
	 */
	public java.math.BigDecimal getTran_amt() {
		return get("tran_amt");
	}

	/**
	 * 设置交易金额
	 *
	 * @param tran_amt 交易金额
	 */
	public void setTran_amt(java.math.BigDecimal tran_amt) {
		set("tran_amt", tran_amt);
	}

    /**
     * 获取待认领金额
     *
     * @return 待认领金额
     */
	public java.math.BigDecimal getAmounttobeclaimed() {
		return get("amounttobeclaimed");
	}

    /**
     * 设置待认领金额
     *
     * @param amounttobeclaimed 待认领金额
     */
	public void setAmounttobeclaimed(java.math.BigDecimal amounttobeclaimed) {
		set("amounttobeclaimed", amounttobeclaimed);
	}

    /**
     * 获取借贷方向
     *
     * @return 借贷方向
     */
	public Short getDirection() {
	    return getShort("direction");
	}

    /**
     * 设置借贷方向
     *
     * @param direction 借贷方向
     */
	public void setDirection(Short direction) {
		set("direction", direction);
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
     * 获取Y租户Id
     *
     * @return Y租户Id
     */
	public String getYtenantId() {
		return get("ytenantId");
	}

    /**
     * 设置Y租户Id
     *
     * @param ytenantId Y租户Id
     */
	public void setYtenantId(String ytenantId) {
		set("ytenantId", ytenantId);
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
	 * 获取对方单位
	 *
	 * @return 对方单位
	 */
	public String getOppositeobjectid() {
		return get("oppositeobjectid");
	}

	/**
	 * 设置对方单位
	 *
	 * @param oppositeobjectid 对方单位
	 */
	public void setOppositeobjectid(String oppositeobjectid) {
		set("oppositeobjectid", oppositeobjectid);
	}

	/**
	 * 获取对方类型
	 *
	 * @return 对方类型
	 */
	public Short getOppositetype() {
		return getShort("oppositetype");
	}

	/**
	 * 设置对方类型
	 *
	 * @param oppositetype 对方类型
	 */
	public void setOppositetype(Short oppositetype) {
		set("oppositetype", oppositetype);
	}

	/**
	 * 获取对方单位名称
	 *
	 * @return 对方单位名称
	 */
	public String getOppositeobjectname() {
		return get("oppositeobjectname");
	}

	/**
	 * 设置对方单位名称
	 *
	 * @param oppositeobjectname 对方单位名称
	 */
	public void setOppositeobjectname(String oppositeobjectname) {
		set("oppositeobjectname", oppositeobjectname);
	}


	/**
	 * 获取实际认领单位
	 *
	 * @return 实际认领单位
	 */
	public String getActualclaimaccentiry() {
		return get("actualclaimaccentiry");
	}

	/**
	 * 设置实际认领单位
	 *
	 * @param actualclaimaccentiry 实际认领单位
	 */
	public void setActualclaimaccentiry(String actualclaimaccentiry) {
		set("actualclaimaccentiry", actualclaimaccentiry);
	}

	/**
	 * 获取入账类型
	 *
	 * @return 入账类型
	 */
	public Short getEntrytype() {
		return getShort("entrytype");
	}

	/**
	 * 设置入账类型
	 *
	 * @param entrytype 入账类型
	 */
	public void setEntrytype(Short entrytype) {
		set("entrytype", entrytype);
	}

	/**
	 * 获取款项类型
	 *
	 * @return 款项类型
	 */
	public Short getQuicktype() {
		return get("quicktype");
	}

	/**
	 * 设置款项类型
	 *
	 * @param quicktype 款项类型
	 */
	public void setQuicktype(String quicktype) {
		set("quicktype", quicktype);
	}

}
