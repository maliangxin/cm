package com.yonyoucloud.fi.cmp.salarypay;

import com.yonyou.ucf.mdd.ext.voucher.base.VouchLine;
import com.yonyoucloud.fi.cmp.cmpentity.PaymentStatus;

/**
 * 薪资支付子表实体
 *
 * @author u
 * @version 1.0
 */
public class Salarypay_b extends VouchLine {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.salarypay.Salarypay_b";

    /**
     * 获取薪资支付id
     *
     * @return 薪资支付id.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置薪资支付id
     *
     * @param mainid 薪资支付id.ID
     */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}
	
	/**
     * 获取来源单据表体id
     *
     * @return 来源单据表体id
     */
	public String getSrcbillid_b() {
		return get("srcbillid_b");
	}

    /**
     * 设置来源单据表体id
     *
     * @param srcbillid_b 来源单据表体id
     */
	public void setSrcbillid_b(String srcbillid_b) {
		set("srcbillid_b", srcbillid_b);
	}

    /**
     * 获取员工编号
     *
     * @return 员工编号
     */
	public String getPersonnum() {
		return get("personnum");
	}

    /**
     * 设置员工编号
     *
     * @param personnum 员工编号
     */
	public void setPersonnum(String personnum) {
		set("personnum", personnum);
	}
	
	/**
     * 获取员工编号
     *
     * @return 员工编号
     */
	public String getShowpersonnum() {
		return get("showpersonnum");
	}

    /**
     * 设置员工编号
     *
     * @param showpersonnum 员工编号
     */
	public void setShowpersonnum(String showpersonnum) {
		set("showpersonnum", showpersonnum);
	}

    /**
     * 获取收方证件类型
     *
     * @return 收方证件类型
     */
	public String getIdentitytype() {
		return get("identitytype");
	}

    /**
     * 设置收方证件类型
     *
     * @param identitytype 收方证件类型
     */
	public void setIdentitytype(String identitytype) {
		set("identitytype", identitytype);
	}

    /**
     * 获取收方证件号码
     *
     * @return 收方证件号码
     */
	public String getIdentitynum() {
		return get("identitynum");
	}

    /**
     * 设置收方证件号码
     *
     * @param identitynum 收方证件号码
     */
	public void setIdentitynum(String identitynum) {
		set("identitynum", identitynum);
	}

    /**
     * 获取收方账号
     *
     * @return 收方账号
     */
	public String getCrtacc() {
		return get("crtacc");
	}

    /**
     * 设置收方账号
     *
     * @param crtacc 收方账号
     */
	public void setCrtacc(String crtacc) {
		set("crtacc", crtacc);
	}

    /**
     * 获取收方户名
     *
     * @return 收方户名
     */
	public String getCrtaccname() {
		return get("crtaccname");
	}

    /**
     * 设置收方户名
     *
     * @param crtaccname 收方户名
     */
	public void setCrtaccname(String crtaccname) {
		set("crtaccname", crtaccname);
	}

    /**
     * 获取收方开户行联行号
     *
     * @return 收方开户行联行号
     */
	public String getCrtcombine() {
		return get("crtcombine");
	}

    /**
     * 设置收方开户行联行号
     *
     * @param crtcombine 收方开户行联行号
     */
	public void setCrtcombine(String crtcombine) {
		set("crtcombine", crtcombine);
	}

    /**
     * 获取收方开户行名
     *
     * @return 收方开户行名
     */
	public String getCrtbank() {
		return get("crtbank");
	}

    /**
     * 设置收方开户行名
     *
     * @param crtbank 收方开户行名
     */
	public void setCrtbank(String crtbank) {
		set("crtbank", crtbank);
	}

    /**
     * 获取跨行结算标识
     *
     * @return 跨行结算标识
     */
	public String getPaymode() {
		return get("paymode");
	}

    /**
     * 设置跨行结算标识
     *
     * @param paymode 跨行结算标识
     */
	public void setPaymode(String paymode) {
		set("paymode", paymode);
	}

    /**
     * 获取收款金额
     *
     * @return 收款金额
     */
	public java.math.BigDecimal getAmount() {
		return get("amount");
	}

    /**
     * 设置收款金额
     *
     * @param amount 收款金额
     */
	public void setAmount(java.math.BigDecimal amount) {
		set("amount", amount);
	}

    /**
     * 获取支付信息
     *
     * @return 支付信息
     */
	public String getTrademessage() {
		return get("trademessage");
	}

    /**
     * 设置支付信息
     *
     * @param trademessage 支付信息
     */
	public void setTrademessage(String trademessage) {
		set("trademessage", trademessage);
	}

    /**
     * 获取网银支付状态
     *
     * @return 网银支付状态
     */
	public PaymentStatus getTradestatus() {
		Number v = get("tradestatus");
		return PaymentStatus.find(v);
	}

    /**
     * 设置网银支付状态
     *
     * @param tradestatus 网银支付状态
     */
	public void setTradestatus(PaymentStatus tradestatus) {
		if (tradestatus != null) {
			set("tradestatus", tradestatus.getValue());
		} else {
			set("tradestatus", null);
		}
	}

    /**
     * 获取工资明细
     *
     * @return 工资明细
     */
	public String getSalarydetail() {
		return get("salarydetail");
	}

    /**
     * 设置工资明细
     *
     * @param salarydetail 工资明细
     */
	public void setSalarydetail(String salarydetail) {
		set("salarydetail", salarydetail);
	}

    /**
     * 获取用途
     *
     * @return 用途
     */
	public String getPurpose() {
		return get("purpose");
	}

    /**
     * 设置用途
     *
     * @param purpose 用途
     */
	public void setPurpose(String purpose) {
		set("purpose", purpose);
	}

    /**
     * 获取附言
     *
     * @return 附言
     */
	public String getPostscript() {
		return get("postscript");
	}

    /**
     * 设置附言
     *
     * @param postscript 附言
     */
	public void setPostscript(String postscript) {
		set("postscript", postscript);
	}

    /**
     * 获取作废标志
     *
     * @return 作废标志
     */
	public Boolean getInvalidflag() {
	    return getBoolean("invalidflag");
	}

    /**
     * 设置作废标志
     *
     * @param invalidflag 作废标志
     */
	public void setInvalidflag(Boolean invalidflag) {
		set("invalidflag", invalidflag);
	}

    /**
     * 获取银行流水号
     *
     * @return 银行流水号
     */
	public String getBankseqno() {
		return get("bankseqno");
	}

    /**
     * 设置银行流水号
     *
     * @param bankseqno 银行流水号
     */
	public void setBankseqno(String bankseqno) {
		set("bankseqno", bankseqno);
	}

    /**
     * 获取交易流水号
     *
     * @return 交易流水号
     */
	public String getTranseqno() {
		return get("transeqno");
	}

    /**
     * 设置交易流水号
     *
     * @param transeqno 交易流水号
     */
	public void setTranseqno(String transeqno) {
		set("transeqno", transeqno);
	}

    /**
     * 获取行号
     *
     * @return 行号
     */
	public Integer getRowno() {
		return get("rowno");
	}

    /**
     * 设置行号
     *
     * @param rowno 行号
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
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public String getCharacterDefb() {
		return get("characterDefb");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param characterDefb 自定义项特征属性组.ID
	 */
	public void setCharacterDefb(String characterDefb) {
		set("characterDefb", characterDefb);
	}

	/**
	 * 获取是否占预算
	 *
	 * @return 是否占预算
	 */
	public Short getIsOccupyBudget() {
		return getShort("isOccupyBudget");
	}

	/**
	 * 设置是否占预算
	 *
	 * @param isOccupyBudget 是否占预算
	 */
	public void setIsOccupyBudget(Short isOccupyBudget) {
		set("isOccupyBudget", isOccupyBudget);
	}

	/**
	 * 获取支付成功日期
	 *
	 * @return 支付成功日期
	 */
	public java.util.Date getPaySuccessDate() {
		return get("paySuccessDate");
	}

	/**
	 * 设置支付成功日期
	 *
	 * @param paySuccessDate 支付成功日期
	 */
	public void setPaySuccessDate(java.util.Date paySuccessDate) {
		set("paySuccessDate", paySuccessDate);
	}

}

