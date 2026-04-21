package com.yonyoucloud.fi.cmp.initprojectdata;


import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import org.imeta.orm.base.BizObject;

/**
 * 项目期初实体
 *
 * @author u
 * @version 1.0
 */
public class InitProjectData extends BizObject implements IAuditInfo, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.initprojectdata.InitProjectData";
	private static final long serialVersionUID = 4453585183584850834L;

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
	 * 获取项目名称
	 *
	 * @return 项目名称.ID
	 */
	public String getProject() {
		return get("project");
	}

	/**
	 * 设置项目名称
	 *
	 * @param project 项目名称.ID
	 */
	public void setProject(String project) {
		set("project", project);
	}

	/**
	 * 获取项目名称
	 *
	 * @return 项目名称.ID
	 */
	public String getProjectCode() {
		return get("projectCode");
	}

	/**
	 * 设置项目名称
	 *
	 * @param projectCode 项目名称.ID
	 */
	public void setProjectCode(String projectCode) {
		set("projectCode", projectCode);
	}

	/**
	 * 获取项目余额
	 *
	 * @return 项目余额
	 */
	public java.math.BigDecimal getProjectBalance() {
		return get("projectBalance");
	}

	/**
	 * 设置项目余额
	 *
	 * @param projectBalance 项目余额
	 */
	public void setProjectBalance(java.math.BigDecimal projectBalance) {
		set("projectBalance", projectBalance);
	}

	/**
	 * 获取项目期初方向
	 *
	 * @return 项目期初方向
	 */
	public Direction getDirection() {
		Number v = get("direction");
		return Direction.find(v);
	}

	/**
	 * 设置项目期初方向
	 *
	 * @param direction 项目期初方向
	 */
	public void setDirection(Direction direction) {
		if (direction != null) {
			set("direction", direction.getValue());
		} else {
			set("direction", null);
		}
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
	 * 获取期初借方金额
	 *
	 * @return 期初借方金额
	 */
	public java.math.BigDecimal getBorrowBalance() {
		return get("borrowBalance");
	}

	/**
	 * 设置期初借方金额
	 *
	 * @param borrowBalance 期初借方金额
	 */
	public void setBorrowBalance(java.math.BigDecimal borrowBalance) {
		set("borrowBalance", borrowBalance);
	}

	/**
	 * 获取期初贷方金额
	 *
	 * @return 期初贷方金额
	 */
	public java.math.BigDecimal getLoanBalance() {
		return get("loanBalance");
	}

	/**
	 * 设置期初贷方金额
	 *
	 * @param loanBalance 期初贷方金额
	 */
	public void setLoanBalance(java.math.BigDecimal loanBalance) {
		set("loanBalance", loanBalance);
	}

	/**
	 * 获取引用标记（0：未引用，1：已引用）
	 *
	 * @return 引用标记（0：未引用，1：已引用）
	 */
	public Integer getQuoteFlag() {
		return get("quoteFlag");
	}

	/**
	 * 设置引用标记（0：未引用，1：已引用）
	 *
	 * @param quoteFlag 引用标记（0：未引用，1：已引用）
	 */
	public void setQuoteFlag(Integer quoteFlag) {
		set("quoteFlag", quoteFlag);
	}

	/**
	 * 获取存储币种id
	 *
	 * @return 存储币种id
	 */
	public String getSavecurrency() {
		return get("savecurrency");
	}

	/**
	 * 设置存储币种id
	 *
	 * @param savecurrency 存储币种id
	 */
	public void setSavecurrency(String savecurrency) {
		set("savecurrency", savecurrency);
	}

	/**
	 * 获取存储友互通id
	 *
	 * @return 存储友互通id
	 */
	public String getSaveyht() {
		return get("saveyht");
	}

	/**
	 * 设置存储友互通id
	 *
	 * @param saveyht 存储友互通id
	 */
	public void setSaveyht(String saveyht) {
		set("saveyht", saveyht);
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

}
