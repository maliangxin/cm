package com.yonyoucloud.fi.cmp.accountrealtimebalance;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 银行账户实时余额实体
 *
 * @author u
 * @version 1.0
 */
public class AccountRealtimeBalance extends BizObject implements IAuditInfo, ITenant {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.accountrealtimebalance.AccountRealtimeBalance";

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
     * 获取账户余额
     *
     * @return 账户余额
     */
	public java.math.BigDecimal getAcctbal() {
		return get("acctbal");
	}

    /**
     * 设置账户余额
     *
     * @param acctbal 账户余额
     */
	public void setAcctbal(java.math.BigDecimal acctbal) {
		set("acctbal", acctbal);
	}

    /**
     * 获取账户可用余额
     *
     * @return 账户可用余额
     */
	public java.math.BigDecimal getAvlbal() {
		return get("avlbal");
	}

    /**
     * 设置账户可用余额
     *
     * @param avlbal 账户可用余额
     */
	public void setAvlbal(java.math.BigDecimal avlbal) {
		set("avlbal", avlbal);
	}

    /**
     * 获取银行账户银行类别
     *
     * @return 银行账户银行类别.ID
     */
	public String getBanktype() {
		return get("banktype");
	}

    /**
     * 设置银行账户银行类别
     *
     * @param banktype 银行账户银行类别.ID
     */
	public void setBanktype(String banktype) {
		set("banktype", banktype);
	}

    /**
     * 获取钞汇标志
     *
     * @return 钞汇标志
     */
	public String getCashflag() {
		return get("cashflag");
	}

    /**
     * 设置钞汇标志
     *
     * @param cashflag 钞汇标志
     */
	public void setCashflag(String cashflag) {
		set("cashflag", cashflag);
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
     * 获取企业银行账户
     *
     * @return 企业银行账户.ID
     */
	public String getEnterpriseBankAccount() {
		return get("enterpriseBankAccount");
	}

    /**
     * 设置企业银行账户
     *
     * @param enterpriseBankAccount 企业银行账户.ID
     */
	public void setEnterpriseBankAccount(String enterpriseBankAccount) {
		set("enterpriseBankAccount", enterpriseBankAccount);
	}

    /**
     * 获取冻结金额
     *
     * @return 冻结金额
     */
	public java.math.BigDecimal getFrzbal() {
		return get("frzbal");
	}

    /**
     * 设置冻结金额
     *
     * @param frzbal 冻结金额
     */
	public void setFrzbal(java.math.BigDecimal frzbal) {
		set("frzbal", frzbal);
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
     * 获取本币币种
     *
     * @return 本币币种.ID
     */
	public String getNatCurrency() {
		return get("natCurrency");
	}

    /**
     * 设置本币币种
     *
     * @param natCurrency 本币币种.ID
     */
	public void setNatCurrency(String natCurrency) {
		set("natCurrency", natCurrency);
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
     * 获取昨日余额
     *
     * @return 昨日余额
     */
	public java.math.BigDecimal getYesterbal() {
		return get("yesterbal");
	}

    /**
     * 设置昨日余额
     *
     * @param yesterbal 昨日余额
     */
	public void setYesterbal(java.math.BigDecimal yesterbal) {
		set("yesterbal", yesterbal);
	}

    /**
     * 获取余额日期
     *
     * @return 余额日期
     */
	public java.util.Date getBalancedate() {
		return get("balancedate");
	}

    /**
     * 设置余额日期
     *
     * @param balancedate 余额日期
     */
	public void setBalancedate(java.util.Date balancedate) {
		set("balancedate", balancedate);
	}

    /**
     * 获取同步标志
     *
     * @return 同步标志
     */
	public String getFlag() {
		return get("flag");
	}

    /**
     * 设置同步标志
     *
     * @param flag 同步标志
     */
	public void setFlag(String flag) {
		set("flag", flag);
	}


	/**
	 * 获取项目名称
	 *
	 * @return 项目名称
	 */
	public String getProj_name() {
		return get("proj_name");
	}

	/**
	 * 设置项目名称
	 *
	 * @param proj_name 项目名称
	 */
	public void setProj_name(String proj_name) {
		set("proj_name", proj_name);
	}

	/**
	 * 获取科目名称
	 *
	 * @return 科目名称
	 */
	public String getSub_name() {
		return get("sub_name");
	}

	/**
	 * 设置科目名称
	 *
	 * @param sub_name 科目名称
	 */
	public void setSub_name(String sub_name) {
		set("sub_name", sub_name);
	}

	/**
	 * 获取预算来源
	 *
	 * @return 预算来源
	 */
	public String getBudget_source() {
		return get("budget_source");
	}

	/**
	 * 设置预算来源
	 *
	 * @param budget_source 预算来源
	 */
	public void setBudget_source(String budget_source) {
		set("budget_source", budget_source);
	}

	/**
	 * 获取账户余额计算
	 *
	 * @return 账户余额计算
	 */
	public java.math.BigDecimal getAcctbalcount() {
		return get("acctbalcount");
	}

	/**
	 * 设置账户余额计算
	 *
	 * @param acctbalcount 账户余额计算
	 */
	public void setAcctbalcount(java.math.BigDecimal acctbalcount) {
		set("acctbalcount", acctbalcount);
	}

	/**
	 * 获取余额对比结果
	 *
	 * @return 余额对比结果
	 */
	public Short getBalancecontrast() {
		return getShort("balancecontrast");
	}

	/**
	 * 设置余额对比结果
	 *
	 * @param balancecontrast 余额对比结果
	 */
	public void setBalancecontrast(Short balancecontrast) {
		set("balancecontrast", balancecontrast);
	}

	/**
	 * 获取存款余额
	 *
	 * @return 存款余额
	 */
	public java.math.BigDecimal getDepositbalance() {
		return get("depositbalance");
	}

	/**
	 * 设置存款余额
	 *
	 * @param depositbalance 存款余额
	 */
	public void setDepositbalance(java.math.BigDecimal depositbalance) {
		set("depositbalance", depositbalance);
	}

	/**
	 * 获取透支余额
	 *
	 * @return 透支余额
	 */
	public java.math.BigDecimal getOverdraftbalance() {
		return get("overdraftbalance");
	}

	/**
	 * 设置透支余额
	 *
	 * @param overdraftbalance 透支余额
	 */
	public void setOverdraftbalance(java.math.BigDecimal overdraftbalance) {
		set("overdraftbalance", overdraftbalance);
	}

	/**
	 * 获取已确认
	 *
	 * @return 已确认
	 */
	public Boolean getIsconfirm() {
		return getBoolean("isconfirm");
	}

	/**
	 * 设置已确认
	 *
	 * @param isconfirm 已确认
	 */
	public void setIsconfirm(Boolean isconfirm) {
		set("isconfirm", isconfirm);
	}

	/**
	 * 获取数据来源
	 *
	 * @return 数据来源
	 */
	public Short getDatasource() {
		return getShort("datasource");
	}

	/**
	 * 设置数据来源
	 *
	 * @param datasource 数据来源
	 */
	public void setDatasource(Short datasource) {
		set("datasource", datasource);
	}

	/**
	 * 获取余额检查说明
	 *
	 * @return 余额检查说明
	 */
	public String getBalancecheckinstruction() {
		return get("balancecheckinstruction");
	}

	/**
	 * 设置余额检查说明
	 *
	 * @param balancecheckinstruction 余额检查说明
	 */
	public void setBalancecheckinstruction(String balancecheckinstruction) {
		set("balancecheckinstruction", balancecheckinstruction);
	}

	/**
	 * 获取余额确认人
	 *
	 * @return 余额确认人.ID
	 */
	public Long getBalanceconfirmerid() {
		return get("balanceconfirmerid");
	}

	/**
	 * 设置余额确认人
	 *
	 * @param balanceconfirmerid 余额确认人.ID
	 */
	public void setBalanceconfirmerid(Long balanceconfirmerid) {
		set("balanceconfirmerid", balanceconfirmerid);
	}

	/**
	 * 获取余额确认时间
	 *
	 * @return 余额确认时间
	 */
	public java.util.Date getBalanceconfirmtime() {
		return get("balanceconfirmtime");
	}

	/**
	 * 设置余额确认时间
	 *
	 * @param balanceconfirmtime 余额确认时间
	 */
	public void setBalanceconfirmtime(java.util.Date balanceconfirmtime) {
		set("balanceconfirmtime", balanceconfirmtime);
	}

	/**
	 * 获取首次查询标识
	 *
	 * @return 首次查询标识
	 */
	public String getFirst_flag() {
		return get("first_flag");
	}

	/**
	 * 设置首次查询标识
	 *
	 * @param first_flag 首次查询标识
	 */
	public void setFirst_flag(String first_flag) {
		set("first_flag", first_flag);
	}

	/**
	 * 获取定期余额
	 *
	 * @return 定期余额
	 */
	public java.math.BigDecimal getRegular_amt() {
		return get("regular_amt");
	}

	/**
	 * 设置定期余额
	 *
	 * @param regular_amt 定期余额
	 */
	public void setRegular_amt(java.math.BigDecimal regular_amt) {
		set("regular_amt", regular_amt);
	}


	/**
	 * 获取合计余额
	 *
	 * @return 合计余额
	 */
	public java.math.BigDecimal getTotal_amt() {
		return get("total_amt");
	}

	/**
	 * 设置合计余额
	 *
	 * @param total_amt 合计余额
	 */
	public void setTotal_amt(java.math.BigDecimal total_amt) {
		set("total_amt", total_amt);
	}

	/**
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public String getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param characterDef 自定义项特征属性组.ID
	 */
	public void setCharacterDef(String characterDef) {
		set("characterDef", characterDef);
	}
}
