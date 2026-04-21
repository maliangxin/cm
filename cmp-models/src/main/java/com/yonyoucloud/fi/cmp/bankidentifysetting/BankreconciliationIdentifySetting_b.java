package com.yonyoucloud.fi.cmp.bankidentifysetting;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.LogicDelete;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 银行流水辨识匹配规则子表实体
 *
 * @author u
 * @version 1.0
 */
public class BankreconciliationIdentifySetting_b extends BizObject implements IYTenant, ITenant, LogicDelete {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankidentifysetting.BankreconciliationIdentifySetting_b";

    /**
     * 获取适用对象字段
     *
     * @return 适用对象字段
     */
	public String getApplyfield() {
		return get("applyfield");
	}

    /**
     * 设置适用对象字段
     *
     * @param applyfield 适用对象字段
     */
	public void setApplyfield(String applyfield) {
		set("applyfield", applyfield);
	}

    /**
     * 获取适用对象字段操作
     *
     * @return 适用对象字段操作
     */
	public String getApplyfieldoption() {
		return get("applyfieldoption");
	}

    /**
     * 设置适用对象字段操作
     *
     * @param applyfieldoption 适用对象字段操作
     */
	public void setApplyfieldoption(String applyfieldoption) {
		set("applyfieldoption", applyfieldoption);
	}

    /**
     * 获取适用对象字段类型
     *
     * @return 适用对象字段类型
     */
	public String getApplyfieldtype() {
		return get("applyfieldtype");
	}

    /**
     * 设置适用对象字段类型
     *
     * @param applyfieldtype 适用对象字段类型
     */
	public void setApplyfieldtype(String applyfieldtype) {
		set("applyfieldtype", applyfieldtype);
	}

	/**
	 * 获取常量
	 *
	 * @return 常量
	 */
	public String getConstant() {
		return get("constant");
	}

	/**
	 * 设置常量
	 *
	 * @param constant 常量
	 */
	public void setConstant(String constant) {
		set("constant", constant);
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
     * 获取浮动天数
     *
     * @return 浮动天数
     */
	public Integer getFloatdays() {
		return get("floatdays");
	}

    /**
     * 设置浮动天数
     *
     * @param floatdays 浮动天数
     */
	public void setFloatdays(Integer floatdays) {
		set("floatdays", floatdays);
	}

	/**
	 * 获取相关性规则ID
	 *
	 * @return 相关性规则ID
	 */
	public String getIdentifyruleid() {
		return get("identifyruleid");
	}

	/**
	 * 设置相关性规则ID
	 *
	 * @param identifyruleid 相关性规则ID
	 */
	public void setIdentifyruleid(String identifyruleid) {
		set("identifyruleid", identifyruleid);
	}

    /**
     * 获取相关性规则编码
     *
     * @return 相关性规则编码
     */
	public String getIdentifyrulecode() {
		return get("identifyrulecode");
	}

    /**
     * 设置相关性规则编码
     *
     * @param identifyrulecode 相关性规则编码
     */
	public void setIdentifyrulecode(String identifyrulecode) {
		set("identifyrulecode", identifyrulecode);
	}

    /**
     * 获取相关性规则名称
     *
     * @return 相关性规则名称
     */
	public String getIdentifyrulename() {
		return get("identifyrulename");
	}

    /**
     * 设置相关性规则名称
     *
     * @param identifyrulename 相关性规则名称
     */
	public void setIdentifyrulename(String identifyrulename) {
		set("identifyrulename", identifyrulename);
	}

	/**
	 * 获取行号
	 *
	 * @return 行号
	 */
	public java.math.BigDecimal getLineno() {
		return get("lineno");
	}

	/**
	 * 设置行号
	 *
	 * @param lineno 行号
	 */
	public void setLineno(Double lineno) {
		set("lineno", lineno);
	}

	/**
     * 设置行号
     *
     * @param lineno 行号
     */
	public void setLineno(java.math.BigDecimal lineno) {
		set("lineno", lineno);
	}

    /**
     * 获取主表id
     *
     * @return 主表id.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置主表id
     *
     * @param mainid 主表id.ID
     */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}

    /**
     * 获取匹配对象字段
     *
     * @return 匹配对象字段
     */
	public String getMatchfield() {
		return get("matchfield");
	}

    /**
     * 设置匹配对象字段
     *
     * @param matchfield 匹配对象字段
     */
	public void setMatchfield(String matchfield) {
		set("matchfield", matchfield);
	}

    /**
     * 获取匹配对象字段说明
     *
     * @return 匹配对象字段说明
     */
	public String getMatchfielddes() {
		return get("matchfielddes");
	}

    /**
     * 设置匹配对象字段说明
     *
     * @param matchfielddes 匹配对象字段说明
     */
	public void setMatchfielddes(String matchfielddes) {
		set("matchfielddes", matchfielddes);
	}

    /**
     * 获取匹配对象字段操作
     *
     * @return 匹配对象字段操作
     */
	public String getMatchfieldoption() {
		return get("matchfieldoption");
	}

    /**
     * 设置匹配对象字段操作
     *
     * @param matchfieldoption 匹配对象字段操作
     */
	public void setMatchfieldoption(String matchfieldoption) {
		set("matchfieldoption", matchfieldoption);
	}

    /**
     * 获取匹配对象字段类型
     *
     * @return 匹配对象字段类型
     */
	public String getMatchfieldtype() {
		return get("matchfieldtype");
	}

    /**
     * 设置匹配对象字段类型
     *
     * @param matchfieldtype 匹配对象字段类型
     */
	public void setMatchfieldtype(String matchfieldtype) {
		set("matchfieldtype", matchfieldtype);
	}

    /**
     * 获取匹配方式
     *
     * @return 匹配方式
     */
	public Short getMatchoption() {
	    return getShort("matchoption");
	}

    /**
     * 设置匹配方式
     *
     * @param matchoption 匹配方式
     */
	public void setMatchoption(Short matchoption) {
		set("matchoption", matchoption);
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
	 * 获取适用对象字段名称
	 *
	 * @return 适用对象字段名称
	 */
	public String getApplyfieldname() {
		return get("applyfieldname");
	}

	/**
	 * 设置适用对象字段名称
	 *
	 * @param applyfieldname 适用对象字段名称
	 */
	public void setApplyfieldname(String applyfieldname) {
		set("applyfieldname", applyfieldname);
	}

	/**
	 * 获取匹配对象字段名称
	 *
	 * @return 匹配对象字段名称
	 */
	public String getMatchfieldname() {
		return get("matchfieldname");
	}

	/**
	 * 设置匹配对象字段名称
	 *
	 * @param matchfieldname 匹配对象字段名称
	 */
	public void setMatchfieldname(String matchfieldname) {
		set("matchfieldname", matchfieldname);
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
