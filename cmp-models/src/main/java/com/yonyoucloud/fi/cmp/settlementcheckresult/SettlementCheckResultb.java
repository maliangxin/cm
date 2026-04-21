package com.yonyoucloud.fi.cmp.settlementcheckresult;


import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;

import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;


import org.imeta.orm.base.BizObject;

/**
 * 日结检查结果子表实体
 *
 * @author u
 * @version 1.0
 */
public class SettlementCheckResultb extends BizObject implements IYTenant, IAuditInfo, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.settlementcheckresult.SettlementCheckResultb";

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
	 * 获取单据日期
	 *
	 * @return 单据日期
	 */
	public String getDate() {
		return get("date");
	}

	/**
	 * 设置单据日期
	 *
	 * @param date 单据日期
	 */
	public void setDate(String date) {
		set("date", date);
	}

	/**
	 * 获取单据状态
	 *
	 * @return 单据状态
	 */
	public String getErrorMessage() {
		return get("errorMessage");
	}

	/**
	 * 设置单据状态
	 *
	 * @param errorMessage 单据状态
	 */
	public void setErrorMessage(String errorMessage) {
		set("errorMessage", errorMessage);
	}

	/**
	 * 获取日结检查结果主键
	 *
	 * @return 日结检查结果主键.ID
	 */
	public Long getMainid() {
		return get("mainid");
	}

	/**
	 * 设置日结检查结果主键
	 *
	 * @param mainid 日结检查结果主键.ID
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
	 * 获取单据编号
	 *
	 * @return 单据编号
	 */
	public String getOrderno() {
		return get("orderno");
	}

	/**
	 * 设置单据编号
	 *
	 * @param orderno 单据编号
	 */
	public void setOrderno(String orderno) {
		set("orderno", orderno);
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
	 * 获取单据类型
	 *
	 * @return 单据类型
	 */
	public String getType() {
		return get("type");
	}

	/**
	 * 设置单据类型
	 *
	 * @param type 单据类型
	 */
	public void setType(String type) {
		set("type", type);
	}

	/**
	 * 获取租户id
	 *
	 * @return 租户id.ID
	 */
	public String getYtenant() {
		return get("ytenant");
	}

	/**
	 * 设置租户id
	 *
	 * @param ytenant 租户id.ID
	 */
	public void setYtenant(String ytenant) {
		set("ytenant", ytenant);
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
