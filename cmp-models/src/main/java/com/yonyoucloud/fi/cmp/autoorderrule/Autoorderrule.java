package com.yonyoucloud.fi.cmp.autoorderrule;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 自动关联设置实体实体
 *
 * @author u
 * @version 1.0
 */
public class Autoorderrule extends BizObject implements IAuditInfo, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.autoorderrule.Autoorderrule";

	/**
	 * 获取所属应用
	 *
	 * @return 所属应用
	 */
	public Short getApplication() {
		return getShort("application");
	}

	/**
	 * 获取是否启用
	 *
	 * @return 是否启用
	 */
	public Short getIsEnable() {
		return getShort("isEnable");
	}

	/**
	 * 设置是否启用
	 *
	 * @param isEnable 是否启用
	 */
	public void setIsEnable(Short isEnable) {
		set("isEnable", isEnable);
	}

	/**
	 * 设置所属应用
	 *
	 * @param application 所属应用
	 */
	public void setApplication(Short application) {
		set("application", application);
	}
	/**
	 * 获取子表id
	 *
	 * @return 子表id.ID
	 */
	public Long getDetailid() {
		return get("detailid");
	}

	/**
	 * 设置子表id
	 *
	 * @param detailid 子表id.ID
	 */
	public void setDetailid(Long detailid) {
		set("detailid", detailid);
	}
	/**
	 * 获取对账单方向
	 *
	 * @return 对账单方向
	 */
	public Short getDirection() {
		return getShort("direction");
	}

	/**
	 * 设置对账单方向
	 *
	 * @param direction 对账单方向
	 */
	public void setDirection(Short direction) {
		set("direction", direction);
	}

	/**
	 * 获取业务单据类型
	 *
	 * @return 业务单据类型
	 */
	public Short getBusDocumentType() {
		return getShort("busDocumentType");
	}

	/**
	 * 设置业务单据类型
	 *
	 * @param busDocumentType 业务单据类型
	 */
	public void setBusDocumentType(Short busDocumentType) {
		set("busDocumentType", busDocumentType);
	}

	/**
	 * 获取银行对账单-对方类型
	 *
	 * @return 银行对账单-对方类型
	 */
	public Short getOtherType() {
		return getShort("otherType");
	}

	/**
	 * 设置银行对账单-对方类型
	 *
	 * @param otherType 银行对账单-对方类型
	 */
	public void setOtherType(Short otherType) {
		set("otherType", otherType);
	}

	/**
	 * 获取敏感词规则
	 *
	 * @return 敏感词规则
	 */
	public Short getSensitiveWordsType() {
		return getShort("sensitiveWordsType");
	}

	/**
	 * 设置敏感词规则
	 *
	 * @param sensitiveWordsType 敏感词规则
	 */
	public void setSensitiveWordsType(Short sensitiveWordsType) {
		set("sensitiveWordsType", sensitiveWordsType);
	}

	/**
	 * 获取敏感词
	 *
	 * @return 敏感词
	 */
	public String getSensitiveWords() {
		return get("sensitiveWords");
	}

	/**
	 * 设置敏感词
	 *
	 * @param sensitiveWords 敏感词
	 */
	public void setSensitiveWords(String sensitiveWords) {
		set("sensitiveWords", sensitiveWords);
	}

	/**
	 * 获取执行时机
	 *
	 * @return 执行时机
	 */
	public Short getRuleExecuteTime() {
		return getShort("ruleExecuteTime");
	}

	/**
	 * 设置执行时机
	 *
	 * @param ruleExecuteTime 执行时机
	 */
	public void setRuleExecuteTime(Short ruleExecuteTime) {
		set("ruleExecuteTime", ruleExecuteTime);
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
	 * 获取自动生单设置集合
	 *
	 * @return 自动生单设置集合
	 */
	public java.util.List<AutoorderruleConfig> configs() {
		return getBizObjects("configs", AutoorderruleConfig.class);
	}

	/**
	 * 设置自动生单设置集合
	 *
	 * @param configs 自动生单设置集合
	 */
	public void setConfigs(java.util.List<AutoorderruleConfig> configs) {
		setBizObjects("configs", configs);
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

}
