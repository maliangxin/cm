package com.yonyoucloud.fi.cmp.checkinventory;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IAutoCode;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.*;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 支票盘点主表实体
 *
 * @author u
 * @version 1.0
 */
public class CheckInventory extends BizObject implements IAutoCode, IAuditInfo, IApprovalInfo, ICurrency, IApprovalFlow, IPrintCount, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.checkinventory.CheckInventory";

	/**
	 * 获取盘点编号
	 *
	 * @return 盘点编号
	 */
	public String getCode() {
		return get("code");
	}

	/**
	 * 设置盘点编号
	 *
	 * @param code 盘点编号
	 */
	public void setCode(String code) {
		set("code", code);
	}


	/**
	 * 获取资金组织
	 *
	 * @return 资金组织.ID
	 */
	public String getAccentity() {
		return get("accentity");
	}

	/**
	 * 设置资金组织
	 *
	 * @param accentity 资金组织.ID
	 */
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}

	/**
	 * 获取盘点日期
	 *
	 * @return 盘点日期
	 */
	public java.util.Date getInventorydate() {
		return get("inventorydate");
	}

	/**
	 * 设置盘点日期
	 *
	 * @param inventorydate 盘点日期
	 */
	public void setInventorydate(java.util.Date inventorydate) {
		set("inventorydate", inventorydate);
	}

	/**
	 * 获取盘点范围
	 *
	 * @return 盘点范围
	 */
	public String getInventoryrange() {
		return get("inventoryrange");
	}

	/**
	 * 设置盘点范围
	 *
	 * @param inventoryrange 盘点范围
	 */
	public void setInventoryrange(String inventoryrange) {
		set("inventoryrange", inventoryrange);
	}

	/**
	 * 获取盘点结果
	 *
	 * @return 盘点结果
	 */
	public String getCheckresult() {
		return get("checkresult");
	}

	/**
	 * 设置盘点结果
	 *
	 * @param checkresult 盘点结果
	 */
	public void setCheckresult(String checkresult) {
		set("checkresult", checkresult);
	}

	/**
	 * 获取盘点说明
	 *
	 * @return 盘点说明
	 */
	public String getRemark() {
		return get("remark");
	}

	/**
	 * 设置盘点说明
	 *
	 * @param remark 盘点说明
	 */
	public void setRemark(String remark) {
		set("remark", remark);
	}

	/**
	 * 获取盘点人
	 *
	 * @return 盘点人.ID
	 */
	public String getCheckman() {
		return get("checkman");
	}

	/**
	 * 设置盘点人
	 *
	 * @param checkman 盘点人.ID
	 */
	public void setCheckman(String checkman) {
		set("checkman", checkman);
	}

	/**
	 * 获取监盘人
	 *
	 * @return 监盘人.ID
	 */
	public Long getMonitorman() {
		return get("monitorman");
	}

	/**
	 * 设置监盘人
	 *
	 * @param monitorman 监盘人.ID
	 */
	public void setMonitorman(Long monitorman) {
		set("monitorman", monitorman);
	}

	/**
	 * 获取审批状态
	 *
	 * @return 审批状态
	 */
	public Short getAuditstatus() {
		return getShort("auditstatus");
	}

	/**
	 * 设置审批状态
	 *
	 * @param auditstatus 审批状态
	 */
	public void setAuditstatus(Short auditstatus) {
		set("auditstatus", auditstatus);
	}

	/**
	 * 获取交易类型
	 *
	 * @return 交易类型
	 */
	public Short getTradetype() {
		return getShort("tradetype");
	}

	/**
	 * 设置交易类型
	 *
	 * @param tradetype 交易类型
	 */
	public void setTradetype(Short tradetype) {
		set("tradetype", tradetype);
	}

	/**
	 * 获取单据类型
	 *
	 * @return 单据类型
	 */
	public String getBillType() {
		return get("billType");
	}

	/**
	 * 设置单据类型
	 *
	 * @param billType 单据类型
	 */
	public void setBillType(String billType) {
		set("billType", billType);
	}

	/**
	 * 获取支票盘点主表特征
	 *
	 * @return 支票盘点主表特征.ID
	 */
	public String getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置支票盘点主表特征
	 *
	 * @param characterDef 支票盘点主表特征.ID
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
	 * 获取盘点支票簿集合
	 *
	 * @return 盘点支票簿集合
	 */
	public java.util.List<CheckInventory_b> CheckInventory_b() {
		return getBizObjects("CheckInventory_b", CheckInventory_b.class);
	}

	/**
	 * 设置盘点支票簿集合
	 *
	 * @param CheckInventory_b 盘点支票簿集合
	 */
	public void setCheckInventory_b(java.util.List<CheckInventory_b> CheckInventory_b) {
		setBizObjects("CheckInventory_b", CheckInventory_b);
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
