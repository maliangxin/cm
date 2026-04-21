package com.yonyoucloud.fi.cmp.withholding;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 协定存款利率变更历史记录子表实体
 *
 * @author u
 * @version 1.0
 */
public class AgreeIRSettingHistory extends BizObject implements IAuditInfo, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.withholding.AgreeIRSettingHistory";

    /**
     * 获取 计息方式
     *
     * @return  计息方式
     */
	public Short getAgreeinterestmethod() {
	    return getShort("agreeinterestmethod");
	}

    /**
     * 设置 计息方式
     *
     * @param agreeinterestmethod  计息方式
     */
	public void setAgreeinterestmethod(Short agreeinterestmethod) {
		set("agreeinterestmethod", agreeinterestmethod);
	}

    /**
     * 获取靠档方式
     *
     * @return 靠档方式
     */
	public Short getAgreerelymethod() {
	    return getShort("agreerelymethod");
	}

    /**
     * 设置靠档方式
     *
     * @param agreerelymethod 靠档方式
     */
	public void setAgreerelymethod(Short agreerelymethod) {
		set("agreerelymethod", agreerelymethod);
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
     * 获取结束日期
     *
     * @return 结束日期
     */
	public java.util.Date getEndDate() {
		return get("endDate");
	}

    /**
     * 设置结束日期
     *
     * @param endDate 结束日期
     */
	public void setEndDate(java.util.Date endDate) {
		set("endDate", endDate);
	}

    /**
     * 获取计息天数
     *
     * @return 计息天数
     */
	public String getInterestDays() {
	    return getString("interestDays");
	}

    /**
     * 设置计息天数
     *
     * @param interestDays 计息天数
     */
	public void setInterestDays(String interestDays) {
		set("interestDays", interestDays);
	}

    /**
     * 获取是否最新
     *
     * @return 是否最新
     */
	public Boolean getIsNew() {
	    return getBoolean("isNew");
	}

    /**
     * 设置是否最新
     *
     * @param isNew 是否最新
     */
	public void setIsNew(Boolean isNew) {
		set("isNew", isNew);
	}

    /**
     * 获取预提规则设置id
     *
     * @return 预提规则设置id.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置预提规则设置id
     *
     * @param mainid 预提规则设置id.ID
     */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
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
     * 获取变更类型
     *
     * @return 变更类型
     */
	public Short getOptionType() {
	    return getShort("optionType");
	}

    /**
     * 设置变更类型
     *
     * @param optionType 变更类型
     */
	public void setOptionType(Short optionType) {
		set("optionType", optionType);
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
     * 获取利率设置id
     *
     * @return 利率设置id
     */
	public Long getRateid() {
		return get("rateid");
	}

    /**
     * 设置利率设置id
     *
     * @param rateid 利率设置id
     */
	public void setRateid(Long rateid) {
		set("rateid", rateid);
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
     * 获取开始日期
     *
     * @return 开始日期
     */
	public java.util.Date getStartDate() {
		return get("startDate");
	}

    /**
     * 设置开始日期
     *
     * @param startDate 开始日期
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
     * 获取版本号
     *
     * @return 版本号
     */
	public Long getVersion() {
		return get("version");
	}

    /**
     * 设置版本号
     *
     * @param version 版本号
     */
	public void setVersion(Long version) {
		set("version", version);
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
     * 获取协定存款利率变更历史记录分档孙表集合
     *
     * @return 协定存款利率变更历史记录分档孙表集合
     */
	public java.util.List<AgreeIRSettingGradeHistory> agreeIRSettingGradeHistory() {
		return getBizObjects("agreeIRSettingGradeHistory", AgreeIRSettingGradeHistory.class);
	}

    /**
     * 设置协定存款利率变更历史记录分档孙表集合
     *
     * @param agreeIRSettingGradeHistory 协定存款利率变更历史记录分档孙表集合
     */
	public void setAgreeIRSettingGradeHistory(java.util.List<AgreeIRSettingGradeHistory> agreeIRSettingGradeHistory) {
		setBizObjects("agreeIRSettingGradeHistory", agreeIRSettingGradeHistory);
	}


}
