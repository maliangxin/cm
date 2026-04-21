package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;


/**
 * 对账单业务单据关联实体
 *
 * @author u
 * @version 1.0
 */
public class BankReconciliationbusrelation_b extends BizObject implements IAuditInfo, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankreconciliation.BankReconciliationbusrelation_b";

	/**
	 * 获取主表id
	 *
	 * @return 主表id.ID
	 */
	public Long getBankreconciliation() {
		return get("bankreconciliation");
	}

	/**
	 * 设置主表id
	 *
	 * @param bankreconciliation 主表id.ID
	 */
	public void setBankreconciliation(Long bankreconciliation) {
		set("bankreconciliation", bankreconciliation);
	}

	/**
	 * 获取关联业务单据类型
	 *
	 * @return 关联业务单据类型
	 */
	public Short getBilltype() {
		return getShort("billtype");
	}

	/**
	 * 设置关联业务单据类型
	 *
	 * @param billtype 关联业务单据类型
	 */
	public void setBilltype(Short billtype) {
		set("billtype", billtype);
	}

	/**
	 * 获取异构系统业务单据类型
	 *
	 * @return 异构系统业务单据类型
	 */
	public String getOutbilltypename() {
		return get("outbilltypename");
	}

	/**
	 * 设置异构系统业务单据类型
	 *
	 * @param outbilltypename 异构系统业务单据类型
	 */
	public void setOutbilltypename(String outbilltypename) {
		set("outbilltypename", outbilltypename);
	}

	/**
	 * 获取业务单据日期
	 *
	 * @return 业务单据日期
	 */
	public java.util.Date getVouchdate() {
		return get("vouchdate");
	}

	/**
	 * 设置业务单据日期
	 *
	 * @param vouchdate 业务单据日期
	 */
	public void setVouchdate(java.util.Date vouchdate) {
		set("vouchdate", vouchdate);
	}

	/**
	 * 获取业务单据主表id
	 *
	 * @return 业务单据主表id
	 */
	public Long getSrcbillid() {
		return get("srcbillid");
	}

	/**
	 * 设置业务单据主表id
	 *
	 * @param srcbillid 业务单据主表id
	 */
	public void setSrcbillid(Long srcbillid) {
		set("srcbillid", srcbillid);
	}

	/**
	 * 获取业务单据子表id
	 *
	 * @return 业务单据子表id
	 */
	public Long getBillid() {
		return get("billid");
	}

	/**
	 * 设置业务单据子表id
	 *
	 * @param billid 业务单据子表id
	 */
	public void setBillid(Long billid) {
		set("billid", billid);
	}

	/**
	 * 获取业务单据编号
	 *
	 * @return 业务单据编号
	 */
	public String getBillcode() {
		return get("billcode");
	}

	/**
	 * 设置业务单据编号
	 *
	 * @param billcode 业务单据编号
	 */
	public void setBillcode(String billcode) {
		set("billcode", billcode);
	}

	/**
	 * 获取业务单元
	 *
	 * @return 业务单元.ID
	 */
	public String getAccentity() {
		return get("accentity");
	}

	/**
	 * 设置业务单元
	 *
	 * @param accentity 业务单元.ID
	 */
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}

	/**
	 * 获取部门
	 *
	 * @return 部门.ID
	 */
	public String getDept() {
		return get("dept");
	}

	/**
	 * 设置部门
	 *
	 * @param dept 部门.ID
	 */
	public void setDept(String dept) {
		set("dept", dept);
	}

	/**
	 * 获取项目
	 *
	 * @return 项目.ID
	 */
	public String getProject() {
		return get("project");
	}

	/**
	 * 设置项目
	 *
	 * @param project 项目.ID
	 */
	public void setProject(String project) {
		set("project", project);
	}

	/**
	 * 获取业务单据金额
	 *
	 * @return 业务单据金额
	 */
	public java.math.BigDecimal getAmountmoney() {
		return get("amountmoney");
	}

	/**
	 * 设置业务单据金额
	 *
	 * @param amountmoney 业务单据金额
	 */
	public void setAmountmoney(java.math.BigDecimal amountmoney) {
		set("amountmoney", amountmoney);
	}

	/**
	 * 获取关联状态
	 *
	 * @return 关联状态
	 */
	public Short getRelationstatus() {
		return getShort("relationstatus");
	}

	/**
	 * 设置关联状态
	 *
	 * @param relationstatus 关联状态
	 */
	public void setRelationstatus(Short relationstatus) {
		set("relationstatus", relationstatus);
	}

	/**
	 * 获取关联类型
	 *
	 * @return 关联类型
	 */
	public Short getRelationtype() {
		return getShort("relationtype");
	}

	/**
	 * 设置关联类型
	 *
	 * @param relationtype 关联类型
	 */
	public void setRelationtype(Short relationtype) {
		set("relationtype", relationtype);
	}

	/**
	 * 获取单据编号
	 *
	 * @return 单据编号
	 */
	public String getBillnum() {
		return get("billnum");
	}

	/**
	 * 设置单据编号
	 *
	 * @param billnum 单据编号
	 */
	public void setBillnum(String billnum) {
		set("billnum", billnum);
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
	 * 获取提前入账
	 *
	 * @return 提前入账
	 */
	public Boolean getIsadvanceaccounts() {
		return getBoolean("isadvanceaccounts");
	}

	/**
	 * 设置提前入账
	 *
	 * @param isadvanceaccounts 提前入账
	 */
	public void setIsadvanceaccounts(Boolean isadvanceaccounts) {
		set("isadvanceaccounts", isadvanceaccounts);
	}

	/**
	 * 获取业务关联次数
	 *
	 * @return 业务关联次数
	 */
	public Short getAssociationcount() {
		return getShort("associationcount");
	}

	/**
	 * 设置业务关联次数
	 *
	 * @param associationcount 业务关联次数
	 */
	public void setAssociationcount(Short associationcount) {
		set("associationcount", associationcount);
	}

	/**
	 * 获取交易类型
	 *
	 * @return 交易类型.ID
	 */
	public String getTradetype() {
		return get("tradetype");
	}

	/**
	 * 设置交易类型
	 *
	 * @param tradetype 交易类型.ID
	 */
	public void setTradetype(String tradetype) {
		set("tradetype", tradetype);
	}

	/**
	 * 获取业务单据子表编号
	 *
	 * @return 业务单据子表编号
	 */
	public String getBillitemcode() {
		return get("billitemcode");
	}

	/**
	 * 设置业务单据子表编号
	 *
	 * @param billitemcode 业务单据子表编号
	 */
	public void setBillitemcode(String billitemcode) {
		set("billitemcode", billitemcode);
	}

	/**
	 * 获取主表id
	 *
	 * @return 主表id.ID
	 */
	public Long getClaimid() {
		return get("claimid");
	}

	/**
	 * 设置主表id
	 *
	 * @param claimid 主表id.ID
	 */
	public void setClaimid(Long claimid) {
		set("claimid", claimid);
	}

	/**
	 * 获取关联信息特征
	 *
	 * @return 关联信息特征.ID
	 */
	public String getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置关联信息特征
	 *
	 * @param characterDef 关联信息特征.ID
	 */
	public void setCharacterDef(String characterDef) {
		set("characterDef", characterDef);
	}

	/**
	 * 获取生单序号
	 *
	 * @return 生单序号
	 */
	public String getOrdernum() {
		return get("ordernum");
	}

	/**
	 * 设置生单序号
	 *
	 * @param ordernum 生单序号
	 */
	public void setOrdernum(String ordernum) {
		set("ordernum", ordernum);
	}
}
