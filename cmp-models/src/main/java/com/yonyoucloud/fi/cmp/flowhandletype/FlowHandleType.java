package com.yonyoucloud.fi.cmp.flowhandletype;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.LogicDelete;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.ext.base.itf.ITree;
import org.imeta.orm.base.BizObject;

import java.util.Date;

/**
 * 流水处理类型实体
 *
 * @author u
 * @version 1.0
 */
public class FlowHandleType extends BizObject implements IYTenant, ITenant, ITree, LogicDelete, IAuditInfo {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.flowhandletype.FlowHandleType";

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

	@Override
	public Date getCreateDate() {
		return get("createDate");
	}

	@Override
	public void setCreateDate(Date createDate) {
		set("createDate", createDate);
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
     * 获取逻辑删除标记
     *
     * @return 逻辑删除标记
     */
	public Short getDr() {
	    return getShort("dr");
	}

    /**
     * 设置逻辑删除标记
     *
     * @param dr 逻辑删除标记
     */
	public void setDr(Short dr) {
		set("dr", dr);
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

    /**
     * 获取状态
     *
     * @return 状态
     */
	public Short getEnablestatus() {
	    return getShort("enablestatus");
	}

    /**
     * 设置状态
     *
     * @param enablestatus 状态
     */
	public void setEnablestatus(Short enablestatus) {
		set("enablestatus", enablestatus);
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
	public void setParent(Object parent) {
		set("parent", parent);
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
     * 获取名称
     *
     * @return 名称
     */
	public String getName() {
		return get("name");
	}

    /**
     * 设置名称
     *
     * @param name 名称
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
     * 获取类型
     *
     * @return 类型
     */
	public Short getType() {
	    return getShort("type");
	}

    /**
     * 设置类型
     *
     * @param type 类型
     */
	public void setType(Short type) {
		set("type", type);
	}

    /**
     * 获取租户id
     *
     * @return 租户id.ID
     */
	@Override
	public String getYTenant() {
		return get("ytenant");
	}

    /**
     * 设置租户id
     *
     * @param ytenant 租户id.ID
     */
	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}




}
