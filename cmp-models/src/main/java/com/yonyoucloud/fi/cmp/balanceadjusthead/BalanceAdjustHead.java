package com.yonyoucloud.fi.cmp.balanceadjusthead;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 余额调节表表头实体
 *
 * @author u
 * @version 1.0
 */
public class BalanceAdjustHead extends BizObject implements IAuditInfo, ITenant {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.balanceadjusthead.BalanceAdjustHead";

    /**
     * 获取企业日记账截止日期
     *
     * @return 企业日记账截止日期
     */
	public java.util.Date getJournaldate() {
		return get("journaldate");
	}

    /**
     * 设置企业日记账截止日期
     *
     * @param journaldate 企业日记账截止日期
     */
	public void setJournaldate(java.util.Date journaldate) {
		set("journaldate", journaldate);
	}

    /**
     * 获取企业日记账余额
     *
     * @return 企业日记账余额
     */
	public java.math.BigDecimal getJournalye() {
		return get("journalye");
	}

    /**
     * 设置企业日记账余额
     *
     * @param journalye 企业日记账余额
     */
	public void setJournalye(java.math.BigDecimal journalye) {
		set("journalye", journalye);
	}

    /**
     * 获取银行已收企业未收
     *
     * @return 银行已收企业未收
     */
	public java.math.BigDecimal getJournalyhys() {
		return get("journalyhys");
	}

    /**
     * 设置银行已收企业未收
     *
     * @param journalyhys 银行已收企业未收
     */
	public void setJournalyhys(java.math.BigDecimal journalyhys) {
		set("journalyhys", journalyhys);
	}

    /**
     * 获取银行已付企业未付
     *
     * @return 银行已付企业未付
     */
	public java.math.BigDecimal getJournalyhyf() {
		return get("journalyhyf");
	}

    /**
     * 设置银行已付企业未付
     *
     * @param journalyhyf 银行已付企业未付
     */
	public void setJournalyhyf(java.math.BigDecimal journalyhyf) {
		set("journalyhyf", journalyhyf);
	}

    /**
     * 获取调整后余额
     *
     * @return 调整后余额
     */
	public java.math.BigDecimal getJournaltzye() {
		return get("journaltzye");
	}

    /**
     * 设置调整后余额
     *
     * @param journaltzye 调整后余额
     */
	public void setJournaltzye(java.math.BigDecimal journaltzye) {
		set("journaltzye", journaltzye);
	}

	/**
	 * 获取企业日记账余额详情
	 *
	 * @return 企业日记账余额详情
	 */
	public String getJournalyedetailinfo() {
		return get("journalyedetailinfo");
	}

	/**
	 * 设置企业日记账余额详情
	 *
	 * @param journalyedetailinfo 企业日记账余额详情
	 */
	public void setJournalyedetailinfo(String journalyedetailinfo) {
		set("journalyedetailinfo", journalyedetailinfo);
	}

    /**
     * 获取银行对账单截止日期
     *
     * @return 银行对账单截止日期
     */
	public java.util.Date getBankdate() {
		return get("bankdate");
	}

    /**
     * 设置银行对账单截止日期
     *
     * @param bankdate 银行对账单截止日期
     */
	public void setBankdate(java.util.Date bankdate) {
		set("bankdate", bankdate);
	}

    /**
     * 获取银行对账单余额
     *
     * @return 银行对账单余额
     */
	public java.math.BigDecimal getBankye() {
		return get("bankye");
	}

    /**
     * 设置银行对账单余额
     *
     * @param bankye 银行对账单余额
     */
	public void setBankye(java.math.BigDecimal bankye) {
		set("bankye", bankye);
	}

    /**
     * 获取企业已收银行未收
     *
     * @return 企业已收银行未收
     */
	public java.math.BigDecimal getBankqyys() {
		return get("bankqyys");
	}

    /**
     * 设置企业已收银行未收
     *
     * @param bankqyys 企业已收银行未收
     */
	public void setBankqyys(java.math.BigDecimal bankqyys) {
		set("bankqyys", bankqyys);
	}

    /**
     * 获取企业已付银行未付
     *
     * @return 企业已付银行未付
     */
	public java.math.BigDecimal getBankqyyf() {
		return get("bankqyyf");
	}

    /**
     * 设置企业已付银行未付
     *
     * @param bankqyyf 企业已付银行未付
     */
	public void setBankqyyf(java.math.BigDecimal bankqyyf) {
		set("bankqyyf", bankqyyf);
	}

    /**
     * 获取调整后余额
     *
     * @return 调整后余额
     */
	public java.math.BigDecimal getBanktzye() {
		return get("banktzye");
	}

    /**
     * 设置调整后余额
     *
     * @param banktzye 调整后余额
     */
	public void setBanktzye(java.math.BigDecimal banktzye) {
		set("banktzye", banktzye);
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
     * 获取创建人
     *
     * @return 创建人
     */
	public String getCreator() {
		return get("creator");
	}

    /**
     * 设置创建人
     *
     * @param creator 创建人
     */
	public void setCreator(String creator) {
		set("creator", creator);
	}

    /**
     * 获取修改人
     *
     * @return 修改人
     */
	public String getModifier() {
		return get("modifier");
	}

    /**
     * 设置修改人
     *
     * @param modifier 修改人
     */
	public void setModifier(String modifier) {
		set("modifier", modifier);
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
	 * 获取审批状态
	 *
	 * @return 审批状态
	 */
	public Short getAuditstatus() {
		return getShort("auditstatus");
	}

	/**
	 * 设置审批状态
	 *
	 * @param auditstatus 审批状态
	 */
	public void setAuditstatus(Short auditstatus) {
		set("auditstatus", auditstatus);
	}

	/**
	 * 获取是否存在未勾对
	 *
	 * @return 是否存在未勾对
	 */
	public Boolean getUncheckflag() {
		return getBoolean("uncheckflag");
	}

	/**
	 * 设置是否存在未勾对
	 *
	 * @param uncheckflag 是否存在未勾对
	 */
	public void setUncheckflag(Boolean uncheckflag) {
		set("uncheckflag", uncheckflag);
	}

}
