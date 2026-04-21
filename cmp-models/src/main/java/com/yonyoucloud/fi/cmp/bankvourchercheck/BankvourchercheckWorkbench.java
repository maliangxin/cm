package com.yonyoucloud.fi.cmp.bankvourchercheck;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

import java.util.Date;

/**
 * 银企对账工作台展示数据实体
 *
 * @author u
 * @version 1.0
 */
public class BankvourchercheckWorkbench extends BizObject implements IYTenant, ITenant, IAuditInfo {
	// 实体全称
	public static final String ENTITY_NAME = "..BankvourchercheckWorkbench";

    /**
     * 获取对账财务账簿
     *
     * @return 对账财务账簿.ID
     */
	public String getAccbook() {
		return get("accbook");
	}

    /**
     * 设置对账财务账簿
     *
     * @param accbook 对账财务账簿.ID
     */
	public void setAccbook(String accbook) {
		set("accbook", accbook);
	}

    /**
     * 获取对账组织
     *
     * @return 对账组织.ID
     */
	public String getAccentity() {
		return get("accentity");
	}

    /**
     * 设置对账组织
     *
     * @param accentity 对账组织.ID
     */
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}

    /**
     * 获取余额调节表状态
     *
     * @return 余额调节表状态
     */
	public Short getBalance_status() {
	    return getShort("balance_status");
	}

    /**
     * 设置余额调节表状态
     *
     * @param balance_status 余额调节表状态
     */
	public void setBalance_status(Short balance_status) {
		set("balance_status", balance_status);
	}

    /**
     * 获取已勾对银行流水贷方金额合计
     *
     * @return 已勾对银行流水贷方金额合计
     */
	public java.math.BigDecimal getBank_checked_creditamountsum() {
		return get("bank_checked_creditamountsum");
	}

    /**
     * 设置已勾对银行流水贷方金额合计
     *
     * @param bank_checked_creditamountsum 已勾对银行流水贷方金额合计
     */
	public void setBank_checked_creditamountsum(java.math.BigDecimal bank_checked_creditamountsum) {
		set("bank_checked_creditamountsum", bank_checked_creditamountsum);
	}

    /**
     * 获取已勾对银行流水借方金额合计
     *
     * @return 已勾对银行流水借方金额合计
     */
	public java.math.BigDecimal getBank_checked_debitamountsum() {
		return get("bank_checked_debitamountsum");
	}

    /**
     * 设置已勾对银行流水借方金额合计
     *
     * @param bank_checked_debitamountsum 已勾对银行流水借方金额合计
     */
	public void setBank_checked_debitamountsum(java.math.BigDecimal bank_checked_debitamountsum) {
		set("bank_checked_debitamountsum", bank_checked_debitamountsum);
	}

    /**
     * 获取已勾对银行流水总笔数
     *
     * @return 已勾对银行流水总笔数
     */
	public Integer getBank_checked_totalnum() {
		return get("bank_checked_totalnum");
	}

    /**
     * 设置已勾对银行流水总笔数
     *
     * @param bank_checked_totalnum 已勾对银行流水总笔数
     */
	public void setBank_checked_totalnum(Integer bank_checked_totalnum) {
		set("bank_checked_totalnum", bank_checked_totalnum);
	}

    /**
     * 获取未勾对银行流水贷方金额合计
     *
     * @return 未勾对银行流水贷方金额合计
     */
	public java.math.BigDecimal getBank_uncheck_creditamountsum() {
		return get("bank_uncheck_creditamountsum");
	}

    /**
     * 设置未勾对银行流水贷方金额合计
     *
     * @param bank_uncheck_creditamountsum 未勾对银行流水贷方金额合计
     */
	public void setBank_uncheck_creditamountsum(java.math.BigDecimal bank_uncheck_creditamountsum) {
		set("bank_uncheck_creditamountsum", bank_uncheck_creditamountsum);
	}

    /**
     * 获取未勾对银行流水借方金额合计
     *
     * @return 未勾对银行流水借方金额合计
     */
	public java.math.BigDecimal getBank_uncheck_debitamountsum() {
		return get("bank_uncheck_debitamountsum");
	}

    /**
     * 设置未勾对银行流水借方金额合计
     *
     * @param bank_uncheck_debitamountsum 未勾对银行流水借方金额合计
     */
	public void setBank_uncheck_debitamountsum(java.math.BigDecimal bank_uncheck_debitamountsum) {
		set("bank_uncheck_debitamountsum", bank_uncheck_debitamountsum);
	}

    /**
     * 获取未勾对银行流水总笔数
     *
     * @return 未勾对银行流水总笔数
     */
	public Integer getBank_uncheck_totalnum() {
		return get("bank_uncheck_totalnum");
	}

    /**
     * 设置未勾对银行流水总笔数
     *
     * @param bank_uncheck_totalnum 未勾对银行流水总笔数
     */
	public void setBank_uncheck_totalnum(Integer bank_uncheck_totalnum) {
		set("bank_uncheck_totalnum", bank_uncheck_totalnum);
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
     * 获取业务日期
     *
     * @return 业务日期
     */
	public java.util.Date getBusiness_date() {
		return get("business_date");
	}

    /**
     * 设置业务日期
     *
     * @param business_date 业务日期
     */
	public void setBusiness_date(java.util.Date business_date) {
		set("business_date", business_date);
	}

    /**
     * 获取对账截止日期
     *
     * @return 对账截止日期
     */
	public java.util.Date getCheck_end_date() {
		return get("check_end_date");
	}

    /**
     * 设置对账截止日期
     *
     * @param check_end_date 对账截止日期
     */
	public void setCheck_end_date(java.util.Date check_end_date) {
		set("check_end_date", check_end_date);
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

	@Override
	public Date getCreateDate() {
		return get("createDate");
	}

	@Override
	public void setCreateDate(Date createDate) {
		set("createDate", createDate);
	}

	/**
     * 获取创建人
     *
     * @return 创建人.ID
     */
	public String getCreator() {
		return get("creator");
	}

    /**
     * 设置创建人
     *
     * @param creator 创建人.ID
     */
	public void setCreator(String creator) {
		set("creator", creator);
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
     * 获取最近操作对账时间
     *
     * @return 最近操作对账时间
     */
	public java.util.Date getLast_execute_time() {
		return get("last_execute_time");
	}

    /**
     * 设置最近操作对账时间
     *
     * @param last_execute_time 最近操作对账时间
     */
	public void setLast_execute_time(java.util.Date last_execute_time) {
		set("last_execute_time", last_execute_time);
	}

    /**
     * 获取修改人
     *
     * @return 修改人.ID
     */
	public String getModifier() {
		return get("modifier");
	}

    /**
     * 设置修改人
     *
     * @param modifier 修改人.ID
     */
	public void setModifier(String modifier) {
		set("modifier", modifier);
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

	@Override
	public Date getModifyDate() {
		return get("modifyDate");
	}

	@Override
	public void setModifyDate(Date modifyDate) {
		set("modifyDate", modifyDate);
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
     * 获取对账方案
     *
     * @return 对账方案.ID
     */
	public Long getReconciliation_scheme() {
		return get("reconciliation_scheme");
	}

    /**
     * 设置对账方案
     *
     * @param reconciliation_scheme 对账方案.ID
     */
	public void setReconciliation_scheme(Long reconciliation_scheme) {
		set("reconciliation_scheme", reconciliation_scheme);
	}

    /**
     * 获取对账状态
     *
     * @return 对账状态
     */
	public Short getReconciliation_status() {
	    return getShort("reconciliation_status");
	}

    /**
     * 设置对账状态
     *
     * @param reconciliation_status 对账状态
     */
	public void setReconciliation_status(Short reconciliation_status) {
		set("reconciliation_status", reconciliation_status);
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
     * 获取已勾对凭证贷方金额合计
     *
     * @return 已勾对凭证贷方金额合计
     */
	public java.math.BigDecimal getVourcher_checked_creditamountsum() {
		return get("vourcher_checked_creditamountsum");
	}

    /**
     * 设置已勾对凭证贷方金额合计
     *
     * @param vourcher_checked_creditamountsum 已勾对凭证贷方金额合计
     */
	public void setVourcher_checked_creditamountsum(java.math.BigDecimal vourcher_checked_creditamountsum) {
		set("vourcher_checked_creditamountsum", vourcher_checked_creditamountsum);
	}

    /**
     * 获取已勾对凭证借方金额合计
     *
     * @return 已勾对凭证借方金额合计
     */
	public java.math.BigDecimal getVourcher_checked_debitamountsum() {
		return get("vourcher_checked_debitamountsum");
	}

    /**
     * 设置已勾对凭证借方金额合计
     *
     * @param vourcher_checked_debitamountsum 已勾对凭证借方金额合计
     */
	public void setVourcher_checked_debitamountsum(java.math.BigDecimal vourcher_checked_debitamountsum) {
		set("vourcher_checked_debitamountsum", vourcher_checked_debitamountsum);
	}

    /**
     * 获取已勾对凭证总笔数
     *
     * @return 已勾对凭证总笔数
     */
	public Integer getVourcher_checked_totalnum() {
		return get("vourcher_checked_totalnum");
	}

    /**
     * 设置已勾对凭证总笔数
     *
     * @param vourcher_checked_totalnum 已勾对凭证总笔数
     */
	public void setVourcher_checked_totalnum(Integer vourcher_checked_totalnum) {
		set("vourcher_checked_totalnum", vourcher_checked_totalnum);
	}

    /**
     * 获取未勾对凭证贷方金额合计
     *
     * @return 未勾对凭证贷方金额合计
     */
	public java.math.BigDecimal getVourcher_uncheck_creditamountsum() {
		return get("vourcher_uncheck_creditamountsum");
	}

    /**
     * 设置未勾对凭证贷方金额合计
     *
     * @param vourcher_uncheck_creditamountsum 未勾对凭证贷方金额合计
     */
	public void setVourcher_uncheck_creditamountsum(java.math.BigDecimal vourcher_uncheck_creditamountsum) {
		set("vourcher_uncheck_creditamountsum", vourcher_uncheck_creditamountsum);
	}

    /**
     * 获取未勾对凭证借方金额合计
     *
     * @return 未勾对凭证借方金额合计
     */
	public java.math.BigDecimal getVourcher_uncheck_debitamountsum() {
		return get("vourcher_uncheck_debitamountsum");
	}

    /**
     * 设置未勾对凭证借方金额合计
     *
     * @param vourcher_uncheck_debitamountsum 未勾对凭证借方金额合计
     */
	public void setVourcher_uncheck_debitamountsum(java.math.BigDecimal vourcher_uncheck_debitamountsum) {
		set("vourcher_uncheck_debitamountsum", vourcher_uncheck_debitamountsum);
	}

    /**
     * 获取未勾对凭证总笔数
     *
     * @return 未勾对凭证总笔数
     */
	public Integer getVourcher_uncheck_totalnum() {
		return get("vourcher_uncheck_totalnum");
	}

    /**
     * 设置未勾对凭证总笔数
     *
     * @param vourcher_uncheck_totalnum 未勾对凭证总笔数
     */
	public void setVourcher_uncheck_totalnum(Integer vourcher_uncheck_totalnum) {
		set("vourcher_uncheck_totalnum", vourcher_uncheck_totalnum);
	}

	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}
}
