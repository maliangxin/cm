package com.yonyoucloud.fi.cmp.autocorrsettings;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 自动关联设置实体实体
 *
 * @author u
 * @version 1.0
 */
public class Autocorrsetting extends BizObject implements IAuditInfo, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.autocorrsettings.Autocorrsetting";

	/**
	 * 获取执行顺序号
	 *
	 * @return 执行顺序号
	 */
	public Integer getExorder() {
		return get("exorder");
	}

	/**
	 * 设置执行顺序号
	 *
	 * @param exorder 执行顺序号
	 */
	public void setExorder(Integer exorder) {
		set("exorder", exorder);
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
	 * 获取银行对账单
	 *
	 * @return 银行对账单
	 */
	public Short getBankRecCode() {
		return getShort("bankRecCode");
	}

	/**
	 * 设置银行对账单
	 *
	 * @param bankRecCode 银行对账单
	 */
	public void setBankRecCode(Short bankRecCode) {
		set("bankRecCode", bankRecCode);
	}

	/**
	 * 获取本方银行账号
	 *
	 * @return 本方银行账号
	 */
	public Short getOurBankNum() {
		return getShort("ourBankNum");
	}

	/**
	 * 设置本方银行账号
	 *
	 * @param ourBankNum 本方银行账号
	 */
	public void setOurBankNum(Short ourBankNum) {
		set("ourBankNum", ourBankNum);
	}

	/**
	 * 获取对方银行账号
	 *
	 * @return 对方银行账号
	 */
	public Short getOthBankNum() {
		return getShort("othBankNum");
	}

	/**
	 * 设置对方银行账号
	 *
	 * @param othBankNum 对方银行账号
	 */
	public void setOthBankNum(Short othBankNum) {
		set("othBankNum", othBankNum);
	}

	/**
	 * 获取对方户名
	 *
	 * @return 对方户名
	 */
	public Short getOthBankNumName() {
		return getShort("othBankNumName");
	}

	/**
	 * 设置对方户名
	 *
	 * @param othBankNumName 对方户名
	 */
	public void setOthBankNumName(Short othBankNumName) {
		set("othBankNumName", othBankNumName);
	}


	/**
	 * 获取金额
	 *
	 * @return 金额
	 */
	public Short getMoney() {
		return getShort("money");
	}

	/**
	 * 设置金额
	 *
	 * @param money 金额
	 */
	public void setMoney(Short money) {
		set("money", money);
	}

	/**
	 * 获取摘要
	 *
	 * @return 摘要
	 */
	public Short getBillabstract() {
		return getShort("billabstract");
	}

	/**
	 * 设置摘要
	 *
	 * @param billabstract 摘要
	 */
	public void setBillabstract(Short billabstract) {
		set("billabstract", billabstract);
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
	 * 获取日期浮动天数
	 *
	 * @return 日期浮动天数
	 */
	public java.math.BigDecimal getFloatDays() {
		return get("floatDays");
	}

	/**
	 * 设置日期浮动天数
	 *
	 * @param floatDays 日期浮动天数
	 */
	public void setFloatDays(java.math.BigDecimal floatDays) {
		set("floatDays", floatDays);
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

}
