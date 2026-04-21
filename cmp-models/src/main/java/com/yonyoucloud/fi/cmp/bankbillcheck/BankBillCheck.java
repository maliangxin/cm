package com.yonyoucloud.fi.cmp.bankbillcheck;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalFlow;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 银企对账直联确认实体
 *
 * @author u
 * @version 1.0
 */
public class BankBillCheck extends BizObject implements IAuditInfo, IApprovalFlow, ITenant, IYTenant, IApprovalInfo {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankbillcheck.bankBillCheck";

	/**
	 * 获取账户所属组织
	 *
	 * @return 账户所属组织.ID
	 */
	public String getAccentity() {
		return get("accentity");
	}

	/**
	 * 设置账户所属组织
	 *
	 * @param accentity 账户所属组织.ID
	 */
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}

	/**
	 * 获取调整后余额
	 *
	 * @return 调整后余额
	 */
	public java.math.BigDecimal getAdjustBalance() {
		return get("adjustBalance");
	}

	/**
	 * 设置调整后余额
	 *
	 * @param adjustBalance 调整后余额
	 */
	public void setAdjustBalance(java.math.BigDecimal adjustBalance) {
		set("adjustBalance", adjustBalance);
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
	 * 获取银行余额
	 *
	 * @return 银行余额
	 */
	public java.math.BigDecimal getBankBalance() {
		return get("bankBalance");
	}

	/**
	 * 设置银行余额
	 *
	 * @param bankBalance 银行余额
	 */
	public void setBankBalance(java.math.BigDecimal bankBalance) {
		set("bankBalance", bankBalance);
	}

	/**
	 * 获取银行响应码
	 *
	 * @return 银行响应码
	 */
	public String getBankResCode() {
		return get("bankResCode");
	}

	/**
	 * 设置银行响应码
	 *
	 * @param bankResCode 银行响应码
	 */
	public void setBankResCode(String bankResCode) {
		set("bankResCode", bankResCode);
	}

	/**
	 * 获取银行响应信息
	 *
	 * @return 银行响应信息
	 */
	public String getBankResMsg() {
		return get("bankResMsg");
	}

	/**
	 * 设置银行响应信息
	 *
	 * @param bankResMsg 银行响应信息
	 */
	public void setBankResMsg(String bankResMsg) {
		set("bankResMsg", bankResMsg);
	}

	/**
	 * 获取账单开始日期
	 *
	 * @return 账单开始日期
	 */
	public java.util.Date getBeginDate() {
		return get("beginDate");
	}

	/**
	 * 设置账单开始日期
	 *
	 * @param beginDate 账单开始日期
	 */
	public void setBeginDate(java.util.Date beginDate) {
		set("beginDate", beginDate);
	}

	/**
	 * 获取账单生成日期
	 *
	 * @return 账单生成日期
	 */
	public java.util.Date getBillCreateDate() {
		return get("billCreateDate");
	}

	/**
	 * 设置账单生成日期
	 *
	 * @param billCreateDate 账单生成日期
	 */
	public void setBillCreateDate(java.util.Date billCreateDate) {
		set("billCreateDate", billCreateDate);
	}

	/**
	 * 获取对账单编号
	 *
	 * @return 对账单编号
	 */
	public String getCheckBillCode() {
		return get("checkBillCode");
	}

	/**
	 * 设置对账单编号
	 *
	 * @param checkBillCode 对账单编号
	 */
	public void setCheckBillCode(String checkBillCode) {
		set("checkBillCode", checkBillCode);
	}

	/**
	 * 获取对账单标识
	 *
	 * @return 对账单标识
	 */
	public String getCheckBillSign() {
		return get("checkBillSign");
	}

	/**
	 * 设置对账单标识
	 *
	 * @param checkBillSign 对账单标识
	 */
	public void setCheckBillSign(String checkBillSign) {
		set("checkBillSign", checkBillSign);
	}

	/**
	 * 获取对账日期
	 *
	 * @return 对账日期
	 */
	public java.util.Date getCheckDate() {
		return get("checkDate");
	}

	/**
	 * 设置对账日期
	 *
	 * @param checkDate 对账日期
	 */
	public void setCheckDate(java.util.Date checkDate) {
		set("checkDate", checkDate);
	}

	/**
	 * 获取对账结果
	 *
	 * @return 对账结果
	 */
	public Short getCheckResult() {
		return getShort("checkResult");
	}

	/**
	 * 设置对账结果
	 *
	 * @param checkResult 对账结果
	 */
	public void setCheckResult(Short checkResult) {
		set("checkResult", checkResult);
	}

	/**
	 * 获取对账状态
	 *
	 * @return 对账状态
	 */
	public Short getCheckStatus() {
		return getShort("checkStatus");
	}

	/**
	 * 设置对账状态
	 *
	 * @param checkStatus 对账状态
	 */
	public void setCheckStatus(Short checkStatus) {
		set("checkStatus", checkStatus);
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
	 * 获取币种
	 *
	 * @return 币种.ID
	 */
	public String getCurrency() {
		return get("currency");
	}

	/**
	 * 设置币种
	 *
	 * @param currency 币种.ID
	 */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

	/**
	 * 获取账单结束日期
	 *
	 * @return 账单结束日期
	 */
	public java.util.Date getEndDate() {
		return get("endDate");
	}

	/**
	 * 设置账单结束日期
	 *
	 * @param endDate 账单结束日期
	 */
	public void setEndDate(java.util.Date endDate) {
		set("endDate", endDate);
	}

	/**
	 * 获取企业余额
	 *
	 * @return 企业余额
	 */
	public java.math.BigDecimal getEnterpriseBalance() {
		return get("enterpriseBalance");
	}

	/**
	 * 设置企业余额
	 *
	 * @param enterpriseBalance 企业余额
	 */
	public void setEnterpriseBalance(java.math.BigDecimal enterpriseBalance) {
		set("enterpriseBalance", enterpriseBalance);
	}

	/**
	 * 获取银行账号
	 *
	 * @return 银行账号.ID
	 */
	public String getEnterpriseBankAccount() {
		return get("enterpriseBankAccount");
	}

	/**
	 * 设置银行账号
	 *
	 * @param enterpriseBankAccount 银行账号.ID
	 */
	public void setEnterpriseBankAccount(String enterpriseBankAccount) {
		set("enterpriseBankAccount", enterpriseBankAccount);
	}

	/**
	 * 获取指令状态
	 *
	 * @return 指令状态
	 */
	public Short getInstructStatus() {
		return getShort("instructStatus");
	}

	/**
	 * 设置指令状态
	 *
	 * @param instructStatus 指令状态
	 */
	public void setInstructStatus(Short instructStatus) {
		set("instructStatus", instructStatus);
	}

	/**
	 * 获取指令提交时间
	 *
	 * @return 指令提交时间
	 */
	public java.util.Date getInstructSubmitTime() {
		return get("instructSubmitTime");
	}

	/**
	 * 设置指令提交时间
	 *
	 * @param instructSubmitTime 指令提交时间
	 */
	public void setInstructSubmitTime(java.util.Date instructSubmitTime) {
		set("instructSubmitTime", instructSubmitTime);
	}

	/**
	 * 获取指令提交人
	 *
	 * @return 指令提交人.ID
	 */
	public String getInstructSubmiter() {
		return get("instructSubmiter");
	}

	/**
	 * 设置指令提交人
	 *
	 * @param instructSubmiter 指令提交人.ID
	 */
	public void setInstructSubmiter(String instructSubmiter) {
		set("instructSubmiter", instructSubmiter);
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
	 * 获取联系电话
	 *
	 * @return 联系电话
	 */
	public String getPhone() {
		return get("phone");
	}

	/**
	 * 设置联系电话
	 *
	 * @param phone 联系电话
	 */
	public void setPhone(String phone) {
		set("phone", phone);
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
	 * 获取银企对账直联确认自定义项
	 *
	 * @return 银企对账直联确认自定义项.ID
	 */
	public String getBankBillCheckCharacterDef() {
		return get("bankBillCheckCharacterDef");
	}

	/**
	 * 设置银企对账直联确认自定义项
	 *
	 * @param bankBillCheckCharacterDef 银企对账直联确认自定义项.ID
	 */
	public void setBankBillCheckCharacterDef(String bankBillCheckCharacterDef) {
		set("bankBillCheckCharacterDef", bankBillCheckCharacterDef);
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
	 * 获取审批流状态
	 *
	 * @return 审批流状态
	 */
	public Short getVerifystate() {
		return getShort("verifystate");
	}

	/**
	 * 设置审批流状态
	 *
	 * @param verifystate 审批流状态
	 */
	public void setVerifystate(Short verifystate) {
		set("verifystate", verifystate);
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
