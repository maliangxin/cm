package com.yonyoucloud.fi.cmp.autorefundcheckrule;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 退票辨识规则实体
 *
 * @author u
 * @version 1.0
 */
public class AutoRefundCheckRule extends BizObject implements IAuditInfo, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.autorefundcheckrule.AutoRefundCheckRule";

    /**
     * 获取资金组织标识
     *
     * @return 资金组织标识
     */
	public Short getAccentityflag() {
	    return getShort("accentityflag");
	}

    /**
     * 设置资金组织标识
     *
     * @param accentityflag 资金组织标识
     */
	public void setAccentityflag(Short accentityflag) {
		set("accentityflag", accentityflag);
	}

    /**
     * 获取币种标识
     *
     * @return 币种标识
     */
	public Short getCurrencyflag() {
	    return getShort("currencyflag");
	}

    /**
     * 设置币种标识
     *
     * @param currencyflag 币种标识
     */
	public void setCurrencyflag(Short currencyflag) {
		set("currencyflag", currencyflag);
	}

    /**
     * 获取金额标识
     *
     * @return 金额标识
     */
	public Short getAmountflag() {
	    return getShort("amountflag");
	}

    /**
     * 设置金额标识
     *
     * @param amountflag 金额标识
     */
	public void setAmountflag(Short amountflag) {
		set("amountflag", amountflag);
	}

    /**
     * 获取借贷方向标识
     *
     * @return 借贷方向标识
     */
	public Short getDirectionflag() {
	    return getShort("directionflag");
	}

    /**
     * 设置借贷方向标识
     *
     * @param directionflag 借贷方向标识
     */
	public void setDirectionflag(Short directionflag) {
		set("directionflag", directionflag);
	}

    /**
     * 获取本方账号标识
     *
     * @return 本方账号标识
     */
	public Short getAccountflag() {
	    return getShort("accountflag");
	}

    /**
     * 设置本方账号标识
     *
     * @param accountflag 本方账号标识
     */
	public void setAccountflag(Short accountflag) {
		set("accountflag", accountflag);
	}

    /**
     * 获取对方账号标识
     *
     * @return 对方账号标识
     */
	public Short getToaccountflag() {
	    return getShort("toaccountflag");
	}

    /**
     * 设置对方账号标识
     *
     * @param toaccountflag 对方账号标识
     */
	public void setToaccountflag(Short toaccountflag) {
		set("toaccountflag", toaccountflag);
	}

    /**
     * 获取对方户名标识
     *
     * @return 对方户名标识
     */
	public Short getToaccountnameflag() {
	    return getShort("toaccountnameflag");
	}

    /**
     * 设置对方户名标识
     *
     * @param toaccountnameflag 对方户名标识
     */
	public void setToaccountnameflag(Short toaccountnameflag) {
		set("toaccountnameflag", toaccountnameflag);
	}

    /**
     * 获取对方类型标识
     *
     * @return 对方类型标识
     */
	public Short getOppositetypeflag() {
	    return getShort("oppositetypeflag");
	}

    /**
     * 设置对方类型标识
     *
     * @param oppositetypeflag 对方类型标识
     */
	public void setOppositetypeflag(Short oppositetypeflag) {
		set("oppositetypeflag", oppositetypeflag);
	}

    /**
     * 获取摘要匹配方式
     *
     * @return 摘要匹配方式
     */
	public Short getRemarkmatch() {
	    return getShort("remarkmatch");
	}

    /**
     * 设置摘要匹配方式
     *
     * @param remarkmatch 摘要匹配方式
     */
	public void setRemarkmatch(Short remarkmatch) {
		set("remarkmatch", remarkmatch);
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

	/**
	 * 获取银行类别
	 *
	 * @return 银行类别
	 */
	public String getBanktype() {
		return get("banktype");
	}

	/**
	 * 设置银行类别
	 *
	 * @return 银行类别
	 */
	public void setBanktype(String banktype) {
		set("banktype", banktype);
	}

	/**
	 * 获取日期范围
	 *
	 * @return 日期范围
	 */
	public Integer getDaterange() {
		return get("daterange");
	}

	/**
	 * 获取日期范围
	 *
	 * @return 日期范围
	 */
	public void setDaterange(Integer daterange) {
		set("daterange", daterange);
	}

}
