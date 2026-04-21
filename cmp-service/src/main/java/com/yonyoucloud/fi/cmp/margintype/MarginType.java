package com.yonyoucloud.fi.cmp.margintype;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalFlow;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IPrintCount;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import org.imeta.orm.base.BizObject;

/**
 * 保证金类型实体
 *
 * @author u
 * @version 1.0
 */
public class MarginType extends BizObject implements IAuditInfo, ITenant, IYTenant, ICurrency, IApprovalFlow, IPrintCount {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.margintype.MarginType";

    /**
     * 获取类型名称
     *
     * @return 类型名称
     */
	public String getTypename() {
		return get("typename");
	}

    /**
     * 设置类型名称
     *
     * @param typename 类型名称
     */
	public void setTypename(String typename) {
		set("typename", typename);
	}

    /**
     * 获取说明
     *
     * @return 说明
     */
	public String getDescription() {
		return get("description");
	}

    /**
     * 设置说明
     *
     * @param description 说明
     */
	public void setDescription(String description) {
		set("description", description);
	}

    /**
     * 获取启用状态
     *
     * @return 启用状态
     */
	public Boolean getIsEnabledType() {
	    return getBoolean("isEnabledType");
	}

    /**
     * 设置启用状态
     *
     * @param isEnabledType 启用状态
     */
	public void setIsEnabledType(Boolean isEnabledType) {
		set("isEnabledType", isEnabledType);
	}

    /**
     * 获取保证金类型特征
     *
     * @return 保证金类型特征.ID
     */
	public String getCharacterDef() {
		return get("characterDef");
	}

    /**
     * 设置保证金类型特征
     *
     * @param characterDef 保证金类型特征.ID
     */
	public void setCharacterDef(String characterDef) {
		set("characterDef", characterDef);
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
	 * 获取租户id
	 *
	 * @return 租户id
	 */
	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	/**
	 * 设置租户id
	 *
	 * @param ytenant 租户id
	 */
	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}

    /**
     * 获取审批人名称
     *
     * @return 审批人名称
     */
	public String getAuditor() {
		return get("auditor");
	}

    /**
     * 设置审批人名称
     *
     * @param auditor 审批人名称
     */
	public void setAuditor(String auditor) {
		set("auditor", auditor);
	}

    /**
     * 获取审批人
     *
     * @return 审批人.ID
     */
	public Long getAuditorId() {
		return get("auditorId");
	}

    /**
     * 设置审批人
     *
     * @param auditorId 审批人.ID
     */
	public void setAuditorId(Long auditorId) {
		set("auditorId", auditorId);
	}

    /**
     * 获取审批时间
     *
     * @return 审批时间
     */
	public java.util.Date getAuditTime() {
		return get("auditTime");
	}

    /**
     * 设置审批时间
     *
     * @param auditTime 审批时间
     */
	public void setAuditTime(java.util.Date auditTime) {
		set("auditTime", auditTime);
	}

    /**
     * 获取审批日期
     *
     * @return 审批日期
     */
	public java.util.Date getAuditDate() {
		return get("auditDate");
	}

    /**
     * 设置审批日期
     *
     * @param auditDate 审批日期
     */
	public void setAuditDate(java.util.Date auditDate) {
		set("auditDate", auditDate);
	}

    /**
     * 获取本币
     *
     * @return 本币.ID
     */
	public String getNatCurrency() {
		return get("natCurrency");
	}

    /**
     * 设置本币
     *
     * @param natCurrency 本币.ID
     */
	public void setNatCurrency(String natCurrency) {
		set("natCurrency", natCurrency);
	}

    /**
     * 获取原币
     *
     * @return 原币.ID
     */
	public String getCurrency() {
		return get("currency");
	}

    /**
     * 设置原币
     *
     * @param currency 原币.ID
     */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

    /**
     * 获取汇率
     *
     * @return 汇率
     */
	public java.math.BigDecimal getExchRate() {
		return get("exchRate");
	}

    /**
     * 设置汇率
     *
     * @param exchRate 汇率
     */
	public void setExchRate(java.math.BigDecimal exchRate) {
		set("exchRate", exchRate);
	}

    /**
     * 获取是否审批流控制
     *
     * @return 是否审批流控制
     */
	public Boolean getIsWfControlled() {
	    return getBoolean("isWfControlled");
	}

    /**
     * 设置是否审批流控制
     *
     * @param isWfControlled 是否审批流控制
     */
	public void setIsWfControlled(Boolean isWfControlled) {
		set("isWfControlled", isWfControlled);
	}

    /**
     * 获取审批状态
     *
     * @return 审批状态
     */
	public Short getVerifystate() {
	    return getShort("verifystate");
	}

    /**
     * 设置审批状态
     *
     * @param verifystate 审批状态
     */
	public void setVerifystate(Short verifystate) {
		set("verifystate", verifystate);
	}

    /**
     * 获取退回次数
     *
     * @return 退回次数
     */
	public Short getReturncount() {
	    return getShort("returncount");
	}

    /**
     * 设置退回次数
     *
     * @param returncount 退回次数
     */
	public void setReturncount(Short returncount) {
		set("returncount", returncount);
	}

    /**
     * 获取打印次数
     *
     * @return 打印次数
     */
	public Integer getPrintCount() {
		return get("printCount");
	}

    /**
     * 设置打印次数
     *
     * @param printCount 打印次数
     */
	public void setPrintCount(Integer printCount) {
		set("printCount", printCount);
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
	 * 获取是否工程类保证金
	 *
	 * @return 是否工程类保证金
	 */
	public Short getIsEngineeringMargin() {
		return getShort("isEngineeringMargin");
	}

	/**
	 * 设置是否工程类保证金
	 *
	 * @param isEngineeringMargin 是否工程类保证金
	 */
	public void setIsEngineeringMargin(Short isEngineeringMargin) {
		set("isEngineeringMargin", isEngineeringMargin);
	}


}
