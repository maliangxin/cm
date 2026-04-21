package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 银行对账单组件明细实体
 *
 * @author u
 * @version 1.0
 */
public class BankReconciliationDetail extends BizObject implements IAuditInfo, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankreconciliation.BankReconciliationDetail";

	/**
	 * 获取银行对账单组件明细mainid
	 *
	 * @return 银行对账单组件明细mainid.ID
	 */
	public Long getMainid() {
		return get("mainid");
	}

	/**
	 * 设置银行对账单组件明细mainid
	 *
	 * @param mainid 银行对账单组件明细mainid.ID
	 */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}

	/**
	 * 获取当前对接人
	 *
	 * @return 当前对接人.ID
	 */
	public String getAutheduser() {
		return get("autheduser");
	}

	/**
	 * 设置当前对接人
	 *
	 * @param autheduser 当前对接人.ID
	 */
	public void setAutheduser(String autheduser) {
		set("autheduser", autheduser);
	}

	/**
	 * 获取财务员工
	 *
	 * @return 财务员工.ID
	 */
	public String getEmployee_financial() {
		return get("employee_financial");
	}

	/**
	 * 设置财务员工
	 *
	 * @param employee_financial 财务员工.ID
	 */
	public void setEmployee_financial(String employee_financial) {
		set("employee_financial", employee_financial);
	}

	/**
	 * 获取业务员工
	 *
	 * @return 业务员工.ID
	 */
	public String getEmployee_business() {
		return get("employee_business");
	}

	/**
	 * 设置业务员工
	 *
	 * @param employee_business 业务员工.ID
	 */
	public void setEmployee_business(String employee_business) {
		set("employee_business", employee_business);
	}

	/**
	 * 获取退回日期
	 *
	 * @return 退回日期
	 */
	public java.util.Date getReturndate() {
		return get("returndate");
	}

	/**
	 * 设置退回日期
	 *
	 * @param returndate 退回日期
	 */
	public void setReturndate(java.util.Date returndate) {
		set("returndate", returndate);
	}

	/**
	 * 获取退回意见
	 *
	 * @return 退回意见
	 */
	public String getReturn_reason() {
		return get("return_reason");
	}

	/**
	 * 设置退回意见
	 *
	 * @param return_reason 退回意见
	 */
	public void setReturn_reason(String return_reason) {
		set("return_reason", return_reason);
	}

	/**
	 * 获取操作类型
	 *
	 * @return 操作类型
	 */
	public String getOprtype() {
		return get("oprtype");
	}

	/**
	 * 设置操作类型
	 *
	 * @param oprtype 操作类型
	 */
	public void setOprtype(String oprtype) {
		set("oprtype", oprtype);
	}

	/**
	 * 获取组别
	 *
	 * @return 组别.ID
	 */
	public String getGroup() {
		return get("group");
	}

	/**
	 * 设置组别
	 *
	 * @param group 组别.ID
	 */
	public void setGroup(String group) {
		set("group", group);
	}

	/**
	 * 获取操作人
	 *
	 * @return 操作人.ID
	 */
	public Long getOperator() {
		return get("operator");
	}

	/**
	 * 设置操作人
	 *
	 * @param operator 操作人.ID
	 */
	public void setOperator(Long operator) {
		set("operator", operator);
	}

	/**
	 * 获取操作日期
	 *
	 * @return 操作日期
	 */
	public java.util.Date getOprdate() {
		return get("oprdate");
	}

	/**
	 * 设置操作日期
	 *
	 * @param oprdate 操作日期
	 */
	public void setOprdate(java.util.Date oprdate) {
		set("oprdate", oprdate);
	}


	/**
	 * 获取已发布部门
	 *
	 * @return 已发布部门.ID
	 */
	public String getPublished_dept() {
		return get("published_dept");
	}

	/**
	 * 设置已发布部门
	 *
	 * @param published_dept 已发布部门.ID
	 */
	public void setPublished_dept(String published_dept) {
		set("published_dept", published_dept);
	}

	/**
	 * 获取已发布组织
	 *
	 * @return 已发布组织.ID
	 */
	public String getPublished_org() {
		return get("published_org");
	}

	/**
	 * 设置已发布组织
	 *
	 * @param published_org 已发布组织.ID
	 */
	public void setPublished_org(String published_org) {
		set("published_org", published_org);
	}

	/**
	 * 获取已发布角色
	 *
	 * @return 已发布角色.ID
	 */
	public String getPublished_role() {
		return get("published_role");
	}

	/**
	 * 设置已发布角色
	 *
	 * @param published_role 已发布角色.ID
	 */
	public void setPublished_role(String published_role) {
		set("published_role", published_role);
	}

	/**
	 * 获取已发布用户
	 *
	 * @return 已发布用户.ID
	 */
	public String getPublished_user() {
		return get("published_user");
	}

	/**
	 * 设置已发布用户
	 *
	 * @param published_user 已发布用户.ID
	 */
	public void setPublished_user(String published_user) {
		set("published_user", published_user);
	}

	/**
	 * 获取发布状态
	 *
	 * @return 发布状态
	 */
	public Short getPublishstatus() {
		return getShort("publishstatus");
	}

	/**
	 * 设置发布状态
	 *
	 * @param publishstatus 发布状态
	 */
	public void setPublishstatus(Short publishstatus) {
		set("publishstatus", publishstatus);
	}


	/**
	 * 获取处理时间
	 *
	 * @return 处理时间
	 */
	public java.util.Date getOprtime() {
		return get("oprtime");
	}

	/**
	 * 设置处理时间
	 *
	 * @param oprtime 处理时间
	 */
	public void setOprtime(java.util.Date oprtime) {
		set("oprtime", oprtime);
	}

	/**
	 * 获取自动处理
	 *
	 * @return 自动处理
	 */
	public Short getIs_autoopr() {
		return getShort("is_autoopr");
	}

	/**
	 * 设置自动处理
	 *
	 * @param is_autoopr 自动处理
	 */
	public void setIs_autoopr(Short is_autoopr) {
		set("is_autoopr", is_autoopr);
	}


	/**
	 * 获取分配认领人员
	 *
	 * @return 分配认领人员.ID
	 */
	public String getClaimor() {
		return get("claimor");
	}

	/**
	 * 设置分配认领人员
	 *
	 * @param claimor 分配认领人员.ID
	 */
	public void setClaimor(String claimor) {
		set("claimor", claimor);
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
	 * 获取租户id
	 *
	 * @return 租户id
	 */
	public String getYtenant() {
		return get("ytenant");
	}

	/**
	 * 设置租户id
	 *
	 * @param ytenant 租户id
	 */
	public void setYtenant(String ytenant) {
		set("ytenant", ytenant);
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
	 * 获取认领单id
	 *
	 * @return 认领单id.ID
	 */
	public Long getClaimid() {
		return get("claimid");
	}

	/**
	 * 设置认领单id
	 *
	 * @param claimid 认领单id.ID
	 */
	public void setClaimid(Long claimid) {
		set("claimid", claimid);
	}

	/**
	 * 获取已发布指定组织
	 *
	 * @return 已发布指定组织.ID
	 */
	public String getPublished_assignorg() {
		return get("published_assignorg");
	}

	/**
	 * 设置已发布指定组织
	 *
	 * @param published_assignorg 已发布指定组织.ID
	 */
	public void setPublished_assignorg(String published_assignorg) {
		set("published_assignorg", published_assignorg);
	}

}
