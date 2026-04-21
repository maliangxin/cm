package com.yonyoucloud.fi.cmp.fcdsusesetting;


import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 流水认领数据源使用设置实体
 * table cmp_fc_datasource_use_setting
 * @author guoxh
 * @version 1.0
 */
public class FcDsUseSetting extends BizObject implements IAuditInfo, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.fcdsusesetting.FcDsUseSetting";

    /**
     * 获取流水处理环节
     *
     * @return 流水处理环节
     */
	public Short getAction() {
	    return getShort("action");
	}

    /**
     * 设置流水处理环节
     *
     * @param action 流水处理环节
     */
	public void setAction(Short action) {
		set("action", action);
	}

    /**
     * 获取适用对象
     *
     * @return 适用对象
     */
	public String getBusinessScenario() {
		return get("businessScenario");
	}

    /**
     * 设置适用对象
     *
     * @param businessScenario 适用对象
     */
	public void setBusinessScenario(String businessScenario) {
		set("businessScenario", businessScenario);
	}

    /**
     * 获取流程编码
     *
     * @return 流程编码
     */
	public String getCode() {
		return get("code");
	}

    /**
     * 设置流程编码
     *
     * @param code 流程编码
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
     * 获取适用收付方向
     *
     * @return 适用收付方向
     */
	public Short getDcFlag() {
	    return getShort("dcFlag");
	}

    /**
     * 设置适用收付方向
     *
     * @param dcFlag 适用收付方向
     */
	public void setDcFlag(Short dcFlag) {
		set("dcFlag", dcFlag);
	}

    /**
     * 获取停用时间
     *
     * @return 停用时间
     */
	public java.util.Date getDisablets() {
		return get("disablets");
	}

    /**
     * 设置停用时间
     *
     * @param disablets 停用时间
     */
	public void setDisablets(java.util.Date disablets) {
		set("disablets", disablets);
	}

    /**
     * 获取启用
     *
     * @return 启用
     */
	public String getEnable() {
		return get("enable");
	}

    /**
     * 设置启用
     *
     * @param enable 启用
     */
	public void setEnable(String enable) {
		set("enable", enable);
	}

    /**
     * 获取启用人
     *
     * @return 启用人
     */
	public String getEnableUser() {
		return get("enableUser");
	}

    /**
     * 设置启用人
     *
     * @param enableUser 启用人
     */
	public void setEnableUser(String enableUser) {
		set("enableUser", enableUser);
	}

    /**
     * 获取启用人id
     *
     * @return 启用人id
     */
	public String getEnableUserId() {
		return get("enableUserId");
	}

    /**
     * 设置启用人id
     *
     * @param enableUserId 启用人id
     */
	public void setEnableUserId(String enableUserId) {
		set("enableUserId", enableUserId);
	}

    /**
     * 获取启用时间
     *
     * @return 启用时间
     */
	public java.util.Date getEnablets() {
		return get("enablets");
	}

    /**
     * 设置启用时间
     *
     * @param enablets 启用时间
     */
	public void setEnablets(java.util.Date enablets) {
		set("enablets", enablets);
	}

    /**
     * 获取是否预制
     *
     * @return 是否预制
     */
	public Short getIsPreset() {
	    return getShort("isPreset");
	}

    /**
     * 设置是否预制
     *
     * @param isPreset 是否预制
     */
	public void setIsPreset(Short isPreset) {
		set("isPreset", isPreset);
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
     * 获取流程名称
     *
     * @return 流程名称
     */
	public String getName() {
		return get("name");
	}

    /**
     * 设置流程名称
     *
     * @param name 流程名称
     */
	public void setName(String name) {
		set("name", name);
	}

    /**
     * 获取适用组织 
     *
     * @return 适用组织 .ID
     */
	public String getAccentity() {
		return get("accentity");
	}

    /**
     * 设置适用组织 
     *
     * @param accentity 适用组织 .ID
     */
	public void setAccentity(String accentity) {
		set("accentity", accentity);
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
     * 获取备注
     *
     * @return 备注
     */
	public String getRemark() {
		return get("remark");
	}

    /**
     * 设置备注
     *
     * @param remark 备注
     */
	public void setRemark(String remark) {
		set("remark", remark);
	}

    /**
     * 获取来源系统编码
     *
     * @return 来源系统编码
     */
	public String getSourceSystemCode() {
		return get("sourceSystemCode");
	}

    /**
     * 设置来源系统编码
     *
     * @param sourceSystemCode 来源系统编码
     */
	public void setSourceSystemCode(String sourceSystemCode) {
		set("sourceSystemCode", sourceSystemCode);
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
     * 获取流水认领数据源使用设置子表集合
     *
     * @return 流水认领数据源使用设置子表集合
     */
	public java.util.List<FcDsUseSetting_b> fcDsUseSettingList() {
		return getBizObjects("fcDsUseSettingList", FcDsUseSetting_b.class);
	}

    /**
     * 设置流水认领数据源使用设置子表集合
     *
     * @param fcDsUseSettingList 流水认领数据源使用设置子表集合
     */
	public void setFcDsUseSettingList(java.util.List<FcDsUseSetting_b> fcDsUseSettingList) {
		setBizObjects("fcDsUseSettingList", fcDsUseSettingList);
	}

}
