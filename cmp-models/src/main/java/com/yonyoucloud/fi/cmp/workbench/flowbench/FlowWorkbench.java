package com.yonyoucloud.fi.cmp.workbench.flowbench;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 流水工作台实体
 *
 * @author guoxh
 * @version 1.0
 */
public class FlowWorkbench extends BizObject implements IAuditInfo, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.flowworkbench.FlowWorkbench";

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
     * 获取银行账号
     *
     * @return 银行账号
     */
	public String getAccountNo() {
		return get("accountNo");
	}

    /**
     * 设置银行账号
     *
     * @param accountNo 银行账号
     */
	public void setAccountNo(String accountNo) {
		set("accountNo", accountNo);
	}

	/**
	 * 获取账户币种
	 *
	 * @return 账户币种.ID
	 */
	public String getAccountCurrency() {
		return get("accountCurrency");
	}

	/**
	 * 设置账户币种
	 *
	 * @param accountCurrency 账户币种.ID
	 */
	public void setAccountCurrency(String accountCurrency) {
		set("accountCurrency", accountCurrency);
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
     * 获取折算币种
     *
     * @return 折算币种
     */
	public String getCurrency() {
		return get("currency");
	}

    /**
     * 设置折算币种
     *
     * @param currency 折算币种
     */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

    /**
     * 获取金额单位
     *
     * @return 金额单位
     */
	public Short getCurrencyUnit() {
	    return getShort("currencyUnit");
	}

    /**
     * 设置金额单位
     *
     * @param currencyUnit 金额单位
     */
	public void setCurrencyUnit(Short currencyUnit) {
		set("currencyUnit", currencyUnit);
	}

    /**
     * 获取结束时间
     *
     * @return 结束时间
     */
	public java.util.Date getEndDate() {
		return get("endDate");
	}

    /**
     * 设置结束时间
     *
     * @param endDate 结束时间
     */
	public void setEndDate(java.util.Date endDate) {
		set("endDate", endDate);
	}

    /**
     * 获取折算汇率
     *
     * @return 折算汇率
     */
	public String getExchangeRateType() {
		return get("exchangeRateType");
	}

    /**
     * 设置折算汇率
     *
     * @param exchangeRateType 折算汇率
     */
	public void setExchangeRateType(String exchangeRateType) {
		set("exchangeRateType", exchangeRateType);
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
     * 获取开始时间
     *
     * @return 开始时间
     */
	public java.util.Date getStartDate() {
		return get("startDate");
	}

    /**
     * 设置开始时间
     *
     * @param startDate 开始时间
     */
	public void setStartDate(java.util.Date startDate) {
		set("startDate", startDate);
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
     * 获取视图名称
     *
     * @return 视图名称
     */
	public String getViewName() {
		return get("viewName");
	}

    /**
     * 设置视图名称
     *
     * @param viewName 视图名称
     */
	public void setViewName(String viewName) {
		set("viewName", viewName);
	}

    /**
     * 获取预警天数
     *
     * @return 预警天数
     */
	public Integer getWarningDays() {
		return get("warningDays");
	}

    /**
     * 设置预警天数
     *
     * @param warningDays 预警天数
     */
	public void setWarningDays(Integer warningDays) {
		set("warningDays", warningDays);
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
	 * 获取排序
	 *
	 * @return 排序
	 */
	public Integer getIorder() {
		return get("iorder");
	}

	/**
	 * 设置排序
	 *
	 * @param iorder 排序
	 */
	public void setIorder(Integer iorder) {
		set("iorder", iorder);
	}

	/**
	 * 获取是否默认
	 *
	 * @return 是否默认
	 */
	public Short getIsDefault() {
		return getShort("isDefault");
	}

	/**
	 * 设置是否默认
	 *
	 * @param isDefault 是否默认
	 */
	public void setIsDefault(Short isDefault) {
		set("isDefault", isDefault);
	}

	/**
	 * 获取所属用户
	 *
	 * @return 所属用户
	 */
	public String getUserId() {
		return get("userId");
	}

	/**
	 * 设置所属用户
	 *
	 * @param userId 所属用户
	 */
	public void setUserId(String userId) {
		set("userId", userId);
	}
	/**
	 * 获取是否预置
	 *
	 * @return 是否预置
	 */
	public Short getIsPreset() {
		return getShort("isPreset");
	}

	/**
	 * 设置是否预置
	 *
	 * @param isPreset 是否预置
	 */
	public void setIsPreset(Short isPreset) {
		set("isPreset", isPreset);
	}


}
