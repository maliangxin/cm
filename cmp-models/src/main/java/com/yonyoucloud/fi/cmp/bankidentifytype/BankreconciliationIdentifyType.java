package com.yonyoucloud.fi.cmp.bankidentifytype;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.LogicDelete;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.ext.base.itf.ITree;
import org.imeta.orm.base.BizObject;

/**
 * 银行流水辨识匹配规则类型实体
 *
 * @author u
 * @version 1.0
 */
public class BankreconciliationIdentifyType extends BizObject implements IYTenant, ITenant, ITree, LogicDelete, IAuditInfo {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankidentifytype.BankreconciliationIdentifyType";

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
     * 获取启用状态
     *
     * @return 启用状态
     */
	public Short getEnablestatus() {
	    return getShort("enablestatus");
	}

    /**
     * 设置启用状态
     *
     * @param enablestatus 启用状态
     */
	public void setEnablestatus(Short enablestatus) {
		set("enablestatus", enablestatus);
	}

    /**
     * 获取执行顺序
     *
     * @return 执行顺序
     */
	public Integer getExcuteorder() {
		return get("excuteorder");
	}

    /**
     * 设置执行顺序
     *
     * @param excuteorder 执行顺序
     */
	public void setExcuteorder(Integer excuteorder) {
		set("excuteorder", excuteorder);
	}

    /**
     * 获取扩展信息
     *
     * @return 扩展信息
     */
	public String getExtend() {
		return get("extend");
	}

    /**
     * 设置扩展信息
     *
     * @param extend 扩展信息
     */
	public void setExtend(String extend) {
		set("extend", extend);
	}

    /**
     * 获取辨识匹配类型
     *
     * @return 辨识匹配类型
     */
	public Short getIdentifytype() {
	    return getShort("identifytype");
	}

    /**
     * 设置辨识匹配类型
     *
     * @param identifytype 辨识匹配类型
     */
	public void setIdentifytype(Short identifytype) {
		set("identifytype", identifytype);
	}

    /**
     * 获取是否末级
     *
     * @return 是否末级
     */
	public Boolean getIsEnd() {
	    return getBoolean("isEnd");
	}

    /**
     * 设置是否末级
     *
     * @param isEnd 是否末级
     */
	public void setIsEnd(Boolean isEnd) {
		set("isEnd", isEnd);
	}

	@Override
	public void setParent(Object o) {

	}

	/**
     * 获取层级
     *
     * @return 层级
     */
	public Integer getLevel() {
		return get("level");
	}

    /**
     * 设置层级
     *
     * @param level 层级
     */
	public void setLevel(Integer level) {
		set("level", level);
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
     * 获取规则类型名称
     *
     * @return 规则类型名称
     */
	public String getName() {
		return get("name");
	}

    /**
     * 设置规则类型名称
     *
     * @param name 规则类型名称
     */
	public void setName(String name) {
		set("name", name);
	}

    /**
     * 获取上级分类
     *
     * @return 上级分类
     */
	public Long getParent() {
		return get("parent");
	}

    /**
     * 设置上级分类
     *
     * @param parent 上级分类
     */
	public void setParent(Long parent) {
		set("parent", parent);
	}

    /**
     * 获取路径
     *
     * @return 路径
     */
	public String getPath() {
		return get("path");
	}

    /**
     * 设置路径
     *
     * @param path 路径
     */
	public void setPath(String path) {
		set("path", path);
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
     * 获取右卡页面类型
     *
     * @return 右卡页面类型
     */
	public Short getRightcardtype() {
	    return getShort("rightcardtype");
	}

    /**
     * 设置右卡页面类型
     *
     * @param rightcardtype 右卡页面类型
     */
	public void setRightcardtype(Short rightcardtype) {
		set("rightcardtype", rightcardtype);
	}

    /**
     * 获取排序号
     *
     * @return 排序号
     */
	public Integer getSort() {
		return get("sort");
	}

    /**
     * 设置排序号
     *
     * @param sort 排序号
     */
	public void setSort(Integer sort) {
		set("sort", sort);
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
     * 获取是否终止规则
     *
     * @return 是否终止规则
     */
	public Boolean getStoptag() {
	    return getBoolean("stoptag");
	}

    /**
     * 设置是否终止规则
     *
     * @param stoptag 是否终止规则
     */
	public void setStoptag(Boolean stoptag) {
		set("stoptag", stoptag);
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
	 * 获取停用时间
	 *
	 * @return 停用时间
	 */
	public java.util.Date getStopdate() {
		return get("stopdate");
	}

	/**
	 * 设置停用时间
	 *
	 * @param stopdate 停用时间
	 */
	public void setStopdate(java.util.Date stopdate) {
		set("stopdate", stopdate);
	}

	/**
	 * 获取启用时间
	 *
	 * @return 启用时间
	 */
	public java.util.Date getEnabledate() {
		return get("enabledate");
	}

	/**
	 * 设置启用时间
	 *
	 * @param enabledate 启用时间
	 */
	public void setEnabledate(java.util.Date enabledate) {
		set("enabledate", enabledate);
	}

	@Override
	public Short getDr() {
		return getShort("dr");
	}

	@Override
	public void setDr(Short aShort) {
		set("dr",aShort);
	}
}
