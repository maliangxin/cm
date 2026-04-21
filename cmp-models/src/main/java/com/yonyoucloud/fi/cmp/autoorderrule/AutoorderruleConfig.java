package com.yonyoucloud.fi.cmp.autoorderrule;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 自动生单设置实体
 *
 * @author u
 * @version 1.0
 */
public class AutoorderruleConfig extends BizObject implements IAuditInfo, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.autoorderrule.AutoorderruleConfig";

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
	 * 获取对方类型
	 *
	 * @return 对方类型
	 */
	public Short getOppositetype() {
		return getShort("oppositetype");
	}

	/**
	 * 设置对方类型
	 *
	 * @param oppositetype 对方类型
	 */
	public void setOppositetype(Short oppositetype) {
		set("oppositetype", oppositetype);
	}


	/**
	 * 获取名称
	 *
	 * @return 名称
	 */
	public String getOppositeName() {
		return get("oppositeName");
	}

	/**
	 * 设置名称
	 *
	 * @param oppositeName 名称
	 */
	public void setOppositeName(String oppositeName) {
		set("oppositeName", oppositeName);
	}

	/**
	 * 获取编码
	 *
	 * @return 编码
	 */
	public String getOppositeCode() {
		return get("oppositeCode");
	}

	/**
	 * 设置编码
	 *
	 * @param oppositeCode 编码
	 */
	public void setOppositeCode(String oppositeCode) {
		set("oppositeCode", oppositeCode);
	}


	/**
	 * 获取供应商
	 *
	 * @return 供应商.ID
	 */
	public Long getSupplier() {
		return get("supplier");
	}

	/**
	 * 设置供应商
	 *
	 * @param supplier 供应商.ID
	 */
	public void setSupplier(Long supplier) {
		set("supplier", supplier);
	}

	/**
	 * 获取客户
	 *
	 * @return 客户.ID
	 */
	public Long getCustomer() {
		return get("customer");
	}

	/**
	 * 设置客户
	 *
	 * @param customer 客户.ID
	 */
	public void setCustomer(Long customer) {
		set("customer", customer);
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
