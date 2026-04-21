package com.yonyoucloud.fi.cmp.payapplicationbill;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.*;
import com.yonyou.ucf.mdd.ext.voucher.base.Vouch;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyoucloud.fi.cmp.cmpentity.RetailerAccountType;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.BillType;

/**
 * 付款申请工作台主表实体
 *
 * @author u
 * @version 1.0
 */
public class PayApplicationBill extends Vouch implements IApprovalInfo, IApprovalFlow, IBackWrite, IPrintCount, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.payapplicationbill.PayApplicationBill";

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
	 * 获取单据类型
	 *
	 * @return 单据类型
	 */
	public BillType getBilltype() {
		Number v = get("billtype");
		return BillType.find(v);
	}

	/**
	 * 设置单据类型
	 *
	 * @param billtype 单据类型
	 */
	public void setBilltype(BillType billtype) {
		if (billtype != null) {
			set("billtype", billtype.getValue());
		} else {
			set("billtype", null);
		}
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
	 * 获取付款申请组织
	 *
	 * @return 付款申请组织.ID
	 */
	public String getOrg() {
		return get("org");
	}

	/**
	 * 设置付款申请组织
	 *
	 * @param org 付款申请组织.ID
	 */
	public void setOrg(String org) {
		set("org", org);
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
	 * 获取业务员
	 *
	 * @return 业务员.ID
	 */
	public String getOperator() {
		return get("operator");
	}

	/**
	 * 设置业务员
	 *
	 * @param operator 业务员.ID
	 */
	public void setOperator(String operator) {
		set("operator", operator);
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
	 * 获取费用项目
	 *
	 * @return 费用项目.ID
	 */
	public Long getExpenseitem() {
		return get("expenseitem");
	}

	/**
	 * 设置费用项目
	 *
	 * @param expenseitem 费用项目.ID
	 */
	public void setExpenseitem(Long expenseitem) {
		set("expenseitem", expenseitem);
	}

	/**
	 * 获取付款方式
	 *
	 * @return 付款方式
	 */
	public String getPaymentMode() {
		return get("paymentMode");
	}

	/**
	 * 设置付款方式
	 *
	 * @param paymentMode 付款方式
	 */
	public void setPaymentMode(String paymentMode) {
		set("paymentMode", paymentMode);
	}

	/**
	 * 获取付款日期
	 *
	 * @return 付款日期
	 */
	public java.util.Date getProposePaymentDate() {
		return get("proposePaymentDate");
	}

	/**
	 * 设置付款日期
	 *
	 * @param proposePaymentDate 付款日期
	 */
	public void setProposePaymentDate(java.util.Date proposePaymentDate) {
		set("proposePaymentDate", proposePaymentDate);
	}

	/**
	 * 获取距付款日天数
	 *
	 * @return 距付款日天数
	 */
	public Integer getDistanceProposePaymentDateDays() {
		return get("distanceProposePaymentDateDays");
	}

	/**
	 * 设置距付款日天数
	 *
	 * @param distanceProposePaymentDateDays 距付款日天数
	 */
	public void setDistanceProposePaymentDateDays(Integer distanceProposePaymentDateDays) {
		set("distanceProposePaymentDateDays", distanceProposePaymentDateDays);
	}

	/**
	 * 获取收付款对象类型
	 *
	 * @return 收付款对象类型
	 */
	public PaymentObject getCaobject() {
		Number v = get("caobject");
		return PaymentObject.find(v);
	}

	/**
	 * 设置收付款对象类型
	 *
	 * @param caobject 收付款对象类型
	 */
	public void setCaobject(PaymentObject caobject) {
		if (caobject != null) {
			set("caobject", caobject.getValue());
		} else {
			set("caobject", null);
		}
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
	 * 获取供应商银行账户
	 *
	 * @return 供应商银行账户.ID
	 */
	public Long getSupplierbankaccount() {
		return get("supplierbankaccount");
	}

	/**
	 * 设置供应商银行账户
	 *
	 * @param supplierbankaccount 供应商银行账户.ID
	 */
	public void setSupplierbankaccount(Long supplierbankaccount) {
		set("supplierbankaccount", supplierbankaccount);
	}

	/**
	 * 获取供应商银行名称
	 *
	 * @return 供应商银行名称
	 */
	public String getSupplierbankname() {
		return get("supplierbankname");
	}

	/**
	 * 设置供应商银行名称
	 *
	 * @param supplierbankname 供应商银行名称
	 */
	public void setSupplierbankname(String supplierbankname) {
		set("supplierbankname", supplierbankname);
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
	 * 获取客户银行账户
	 *
	 * @return 客户银行账户.ID
	 */
	public Long getCustomerbankaccount() {
		return get("customerbankaccount");
	}

	/**
	 * 设置客户银行账户
	 *
	 * @param customerbankaccount 客户银行账户.ID
	 */
	public void setCustomerbankaccount(Long customerbankaccount) {
		set("customerbankaccount", customerbankaccount);
	}

	/**
	 * 获取客户银行名称
	 *
	 * @return 客户银行名称
	 */
	public String getCustomerbankname() {
		return get("customerbankname");
	}

	/**
	 * 设置客户银行名称
	 *
	 * @param customerbankname 客户银行名称
	 */
	public void setCustomerbankname(String customerbankname) {
		set("customerbankname", customerbankname);
	}

	/**
	 * 获取员工
	 *
	 * @return 员工.ID
	 */
	public String getEmployee() {
		return get("employee");
	}

	/**
	 * 设置员工
	 *
	 * @param employee 员工.ID
	 */
	public void setEmployee(String employee) {
		set("employee", employee);
	}

	/**
	 * 获取员工银行账户
	 *
	 * @return 员工银行账户.ID
	 */
	public String getStaffBankAccount() {
		return get("staffBankAccount");
	}

	/**
	 * 设置员工银行账户
	 *
	 * @param staffBankAccount 员工银行账户.ID
	 */
	public void setStaffBankAccount(String staffBankAccount) {
		set("staffBankAccount", staffBankAccount);
	}

	/**
	 * 获取散户
	 *
	 * @return 散户
	 */
	public String getRetailer() {
		return get("retailer");
	}

	/**
	 * 设置散户
	 *
	 * @param retailer 散户
	 */
	public void setRetailer(String retailer) {
		set("retailer", retailer);
	}

	/**
	 * 获取散户账户名称
	 *
	 * @return 散户账户名称
	 */
	public String getRetailerAccountName() {
		return get("retailerAccountName");
	}

	/**
	 * 设置散户账户名称
	 *
	 * @param retailerAccountName 散户账户名称
	 */
	public void setRetailerAccountName(String retailerAccountName) {
		set("retailerAccountName", retailerAccountName);
	}

	/**
	 * 获取散户账号
	 *
	 * @return 散户账号
	 */
	public String getRetailerAccountNo() {
		return get("retailerAccountNo");
	}

	/**
	 * 设置散户账号
	 *
	 * @param retailerAccountNo 散户账号
	 */
	public void setRetailerAccountNo(String retailerAccountNo) {
		set("retailerAccountNo", retailerAccountNo);
	}

	/**
	 * 获取散户账户联行号
	 *
	 * @return 散户账户联行号
	 */
	public String getRetailerLineNumber() {
		return get("retailerLineNumber");
	}

	/**
	 * 设置散户账户联行号
	 *
	 * @param retailerLineNumber 散户账户联行号
	 */
	public void setRetailerLineNumber(String retailerLineNumber) {
		set("retailerLineNumber", retailerLineNumber);
	}

	/**
	 * 获取散户收款类型
	 *
	 * @return 散户收款类型
	 */
	public RetailerAccountType getRetailerAccountType() {
		Number v = get("retailerAccountType");
		return RetailerAccountType.find(v);
	}

	/**
	 * 设置散户收款类型
	 *
	 * @param retailerAccountType 散户收款类型
	 */
	public void setRetailerAccountType(RetailerAccountType retailerAccountType) {
		if (retailerAccountType != null) {
			set("retailerAccountType", retailerAccountType.getValue());
		} else {
			set("retailerAccountType", null);
		}
	}

	/**
	 * 获取散户账户银行类别
	 *
	 * @return 散户账户银行类别.ID
	 */
	public String getRetailerBankType() {
		return get("retailerBankType");
	}

	/**
	 * 设置散户账户银行类别
	 *
	 * @param retailerBankType 散户账户银行类别.ID
	 */
	public void setRetailerBankType(String retailerBankType) {
		set("retailerBankType", retailerBankType);
	}

	/**
	 * 获取付款申请金额合计
	 *
	 * @return 付款申请金额合计
	 */
	public java.math.BigDecimal getPaymentApplyAmountSum() {
		return get("paymentApplyAmountSum");
	}

	/**
	 * 设置付款申请金额合计
	 *
	 * @param paymentApplyAmountSum 付款申请金额合计
	 */
	public void setPaymentApplyAmountSum(java.math.BigDecimal paymentApplyAmountSum) {
		set("paymentApplyAmountSum", paymentApplyAmountSum);
	}

	/**
	 * 获取付款预占金额合计
	 *
	 * @return 付款预占金额合计
	 */
	public java.math.BigDecimal getPaymentPreemptAmountSum() {
		return get("paymentPreemptAmountSum");
	}

	/**
	 * 设置付款预占金额合计
	 *
	 * @param paymentPreemptAmountSum 付款预占金额合计
	 */
	public void setPaymentPreemptAmountSum(java.math.BigDecimal paymentPreemptAmountSum) {
		set("paymentPreemptAmountSum", paymentPreemptAmountSum);
	}

	/**
	 * 获取已付款金额合计
	 *
	 * @return 已付款金额合计
	 */
	public java.math.BigDecimal getPaidAmountSum() {
		return get("paidAmountSum");
	}

	/**
	 * 设置已付款金额合计
	 *
	 * @param paidAmountSum 已付款金额合计
	 */
	public void setPaidAmountSum(java.math.BigDecimal paidAmountSum) {
		set("paidAmountSum", paidAmountSum);
	}

	/**
	 * 获取未付款金额合计
	 *
	 * @return 未付款金额合计
	 */
	public java.math.BigDecimal getUnpaidAmountSum() {
		return get("unpaidAmountSum");
	}

	/**
	 * 设置未付款金额合计
	 *
	 * @param unpaidAmountSum 未付款金额合计
	 */
	public void setUnpaidAmountSum(java.math.BigDecimal unpaidAmountSum) {
		set("unpaidAmountSum", unpaidAmountSum);
	}

	/**
	 * 获取付款币种
	 *
	 * @return 付款币种.ID
	 */
	public String getCurrency() {
		return get("currency");
	}

	/**
	 * 设置付款币种
	 *
	 * @param currency 付款币种.ID
	 */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

	/**
	 * 获取本币币种
	 *
	 * @return 本币币种.ID
	 */
	public String getNatCurrency() {
		return get("natCurrency");
	}

	/**
	 * 设置本币币种
	 *
	 * @param natCurrency 本币币种.ID
	 */
	public void setNatCurrency(String natCurrency) {
		set("natCurrency", natCurrency);
	}

	/**
	 * 获取来源系统
	 *
	 * @return 来源系统
	 */
	public SourceSystem getSourceSystem() {
		Number v = get("sourceSystem");
		return SourceSystem.find(v);
	}

	/**
	 * 设置来源系统
	 *
	 * @param sourceSystem 来源系统
	 */
	public void setSourceSystem(SourceSystem sourceSystem) {
		if (sourceSystem != null) {
			set("sourceSystem", sourceSystem.getValue());
		} else {
			set("sourceSystem", null);
		}
	}

	/**
	 * 获取来源事项
	 *
	 * @return 来源事项
	 */
	public SourceMatters getSrcitem() {
		Number v = get("srcitem");
		return SourceMatters.find(v);
	}

	/**
	 * 设置来源事项
	 *
	 * @param srcitem 来源事项
	 */
	public void setSrcitem(SourceMatters srcitem) {
		if (srcitem != null) {
			set("srcitem", srcitem.getValue());
		} else {
			set("srcitem", null);
		}
	}

	/**
	 * 获取申请单状态
	 *
	 * @return 申请单状态
	 */
	public PayBillStatus getPayBillStatus() {
		Number v = get("payBillStatus");
		return PayBillStatus.find(v);
	}

	/**
	 * 设置申请单状态
	 *
	 * @param payBillStatus 申请单状态
	 */
	public void setPayBillStatus(PayBillStatus payBillStatus) {
		if (payBillStatus != null) {
			set("payBillStatus", payBillStatus.getValue());
		} else {
			set("payBillStatus", null);
		}
	}

	/**
	 * 获取申请审批状态
	 *
	 * @return 申请审批状态
	 */
	public ApprovalStatus getApprovalStatus() {
		Number v = get("approvalStatus");
		return ApprovalStatus.find(v);
	}

	/**
	 * 设置申请审批状态
	 *
	 * @param approvalStatus 申请审批状态
	 */
	public void setApprovalStatus(ApprovalStatus approvalStatus) {
		if (approvalStatus != null) {
			set("approvalStatus", approvalStatus.getValue());
		} else {
			set("approvalStatus", null);
		}
	}

	/**
	 * 获取预占金额是否已满（0：未满，1：已满）
	 *
	 * @return 预占金额是否已满（0：未满，1：已满）
	 */
	public Integer getPreemptAmountFull() {
		return get("preemptAmountFull");
	}

	/**
	 * 设置预占金额是否已满（0：未满，1：已满）
	 *
	 * @param preemptAmountFull 预占金额是否已满（0：未满，1：已满）
	 */
	public void setPreemptAmountFull(Integer preemptAmountFull) {
		set("preemptAmountFull", preemptAmountFull);
	}


	/**
	 * 获取是否关闭
	 *
	 * @return 是否关闭
	 */
	public CloseStatus getCloseStatus() {
		Number v = get("closeStatus");
		return CloseStatus.find(v);
	}

	/**
	 * 设置是否关闭
	 *
	 * @param closeStatus 是否关闭
	 */
	public void setCloseStatus(CloseStatus closeStatus) {
		if (closeStatus != null) {
			set("closeStatus", closeStatus.getValue());
		} else {
			set("closeStatus", null);
		}
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
	 * 获取上游单据主表id
	 *
	 * @return 上游单据主表id
	 */
	public Long getSourceid() {
		return get("sourceid");
	}

	/**
	 * 设置上游单据主表id
	 *
	 * @param sourceid 上游单据主表id
	 */
	public void setSourceid(Long sourceid) {
		set("sourceid", sourceid);
	}

	/**
	 * 获取上游单据子表id
	 *
	 * @return 上游单据子表id
	 */
	public Long getSourceautoid() {
		return get("sourceautoid");
	}

	/**
	 * 设置上游单据子表id
	 *
	 * @param sourceautoid 上游单据子表id
	 */
	public void setSourceautoid(Long sourceautoid) {
		set("sourceautoid", sourceautoid);
	}

	/**
	 * 获取上游单据类型
	 *
	 * @return 上游单据类型
	 */
	public String getSource() {
		return get("source");
	}

	/**
	 * 设置上游单据类型
	 *
	 * @param source 上游单据类型
	 */
	public void setSource(String source) {
		set("source", source);
	}

	/**
	 * 获取上游单据号
	 *
	 * @return 上游单据号
	 */
	public String getUpcode() {
		return get("upcode");
	}

	/**
	 * 设置上游单据号
	 *
	 * @param upcode 上游单据号
	 */
	public void setUpcode(String upcode) {
		set("upcode", upcode);
	}

	/**
	 * 获取生单规则编号
	 *
	 * @return 生单规则编号
	 */
	public String getMakeRuleCode() {
		return get("makeRuleCode");
	}

	/**
	 * 设置生单规则编号
	 *
	 * @param makeRuleCode 生单规则编号
	 */
	public void setMakeRuleCode(String makeRuleCode) {
		set("makeRuleCode", makeRuleCode);
	}

	/**
	 * 获取时间戳
	 *
	 * @return 时间戳
	 */
	public java.util.Date getSourceMainPubts() {
		return get("sourceMainPubts");
	}

	/**
	 * 设置时间戳
	 *
	 * @param sourceMainPubts 时间戳
	 */
	public void setSourceMainPubts(java.util.Date sourceMainPubts) {
		set("sourceMainPubts", sourceMainPubts);
	}

	/**
	 * 获取分组任务KEY
	 *
	 * @return 分组任务KEY
	 */
	public String getGroupTaskKey() {
		return get("groupTaskKey");
	}

	/**
	 * 设置分组任务KEY
	 *
	 * @param groupTaskKey 分组任务KEY
	 */
	public void setGroupTaskKey(String groupTaskKey) {
		set("groupTaskKey", groupTaskKey);
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
	public Status getStatus() {
		Short v = get("status");
		return Status.find(v);
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
	 * 获取付款申请工作台子表集合
	 *
	 * @return 付款申请工作台子表集合
	 */
	public java.util.List<PayApplicationBill_b> payApplicationBill_b() {
		return getBizObjects("payApplicationBill_b", PayApplicationBill_b.class);
	}

	/**
	 * 设置付款申请工作台子表集合
	 *
	 * @param payApplicationBill_b 付款申请工作台子表集合
	 */
	public void setPayApplicationBill_b(java.util.List<PayApplicationBill_b> payApplicationBill_b) {
		setBizObjects("payApplicationBill_b", payApplicationBill_b);
	}
	/**
	 * 获取提交时间
	 *
	 * @return 提交时间
	 */
	public java.util.Date getSubmitTime() {
		return get("submitTime");
	}

	/**
	 * 设置提交时间
	 *
	 * @param submitTime 提交时间
	 */
	public void setSubmitTime(java.util.Date submitTime) {
		set("submitTime", submitTime);
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
	 * 获取付款申请单特征
	 *
	 * @return 付款申请单特征.ID
	 */
	public String getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置付款申请单特征
	 *
	 * @param characterDef 付款申请单特征.ID
	 */
	public void setCharacterDef(String characterDef) {
		set("characterDef", characterDef);
	}

}

