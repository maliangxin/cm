package com.yonyoucloud.fi.cmp.accountfixedbalance;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 银行账户定期余额维护实体
 *
 * @author u
 * @version 1.0
 */
public class AccountFixedBalance extends BizObject implements IAuditInfo, ITenant, IYTenant {

	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.accountfixedbalance.AccountFixedBalance";

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
     * 获取确认人
     *
     * @return 确认人.ID
     */
	public Long getBalanceconfirmerid() {
		return get("balanceconfirmerid");
	}

    /**
     * 设置确认人
     *
     * @param balanceconfirmerid 确认人.ID
     */
	public void setBalanceconfirmerid(Long balanceconfirmerid) {
		set("balanceconfirmerid", balanceconfirmerid);
	}

    /**
     * 获取确认时间
     *
     * @return 确认时间
     */
	public java.util.Date getBalanceconfirmtime() {
		return get("balanceconfirmtime");
	}

    /**
     * 设置确认时间
     *
     * @param balanceconfirmtime 确认时间
     */
	public void setBalanceconfirmtime(java.util.Date balanceconfirmtime) {
		set("balanceconfirmtime", balanceconfirmtime);
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
     * 获取银行网点
     *
     * @return 银行网点.ID
     */
	public String getBankname() {
		return get("bankname");
	}

    /**
     * 设置银行网点
     *
     * @param bankname 银行网点.ID
     */
	public void setBankname(String bankname) {
		set("bankname", bankname);
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
     * 获取编码
     *
     * @return 编码
     */
	public String getCode() {
		return get("code");
	}

    /**
     * 设置编码
     *
     * @param code 编码
     */
	public void setCode(String code) {
		set("code", code);
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
     * 获取备注
     *
     * @return 备注
     */
	public String getDescription() {
		return get("description");
	}

    /**
     * 设置备注
     *
     * @param description 备注
     */
	public void setDescription(String description) {
		set("description", description);
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
     * 获取余额确认状态
     *
     * @return 余额确认状态
     */
	public Boolean getIsconfirm() {
	    return getBoolean("isconfirm");
	}

    /**
     * 设置余额确认状态
     *
     * @param isconfirm 余额确认状态
     */
	public void setIsconfirm(Boolean isconfirm) {
		set("isconfirm", isconfirm);
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
     * 获取单据状态
     *
     * @return 单据状态
     */
	public Short getStatus() {
	    return getShort("status");
	}

    /**
     * 设置单据状态
     *
     * @param status 单据状态
     */
	public void setStatus(Short status) {
		set("status", status);
	}

    /**
     * 获取租户ID
     *
     * @return 租户ID
     */
	public String getTenant() {
		return get("tenant");
	}

    /**
     * 设置租户ID
     *
     * @param tenant 租户ID
     */
	public void setTenant(String tenant) {
		set("tenant", tenant);
	}

    /**
     * 获取模板id
     *
     * @return 模板id
     */
	public Long getTplid() {
		return get("tplid");
	}

    /**
     * 设置模板id
     *
     * @param tplid 模板id
     */
	public void setTplid(Long tplid) {
		set("tplid", tplid);
	}

    /**
     * 获取单据日期
     *
     * @return 单据日期
     */
	public java.util.Date getVouchdate() {
		return get("vouchdate");
	}

    /**
     * 设置单据日期
     *
     * @param vouchdate 单据日期
     */
	public void setVouchdate(java.util.Date vouchdate) {
		set("vouchdate", vouchdate);
	}

    /**
     * 获取租户id
     *
     * @return 租户id.ID
     */
	public String getYTenant() {
		return get("ytenant");
	}

    /**
     * 设置租户id
     *
     * @param ytenant 租户id.ID
     */
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}

	/**
	 * 获取银行账户定期余额特征
	 *
	 * @return 银行账户定期余额特征.ID
	 */
	public String getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置银行账户定期余额特征
	 *
	 * @param characterDef 银行账户定期余额特征.ID
	 */
	public void setCharacterDef(String characterDef) {
		set("characterDef", characterDef);
	}

}
