package com.yonyoucloud.fi.cmp.payapplicationbill;


import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IBackWrite;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.ext.voucher.base.VouchLine;

/**
 * 付款申请工作台子表实体
 *
 * @author u
 * @version 1.0
 */
public class PayApplicationBill_b extends VouchLine implements IAuditInfo, IBackWrite, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.payapplicationbill.PayApplicationBill_b";

	/**
	 * 获取付款单id
	 *
	 * @return 付款单id.ID
	 */
	public Long getMainid() {
		return get("mainid");
	}

	/**
	 * 设置付款单id
	 *
	 * @param mainid 付款单id.ID
	 */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}

	/**
	 * 获取款项类型
	 *
	 * @return 款项类型.ID
	 */
	public Long getQuickType() {
		return get("quickType");
	}

	/**
	 * 设置款项类型
	 *
	 * @param quickType 款项类型.ID
	 */
	public void setQuickType(Long quickType) {
		set("quickType", quickType);
	}

	/**
	 * 获取来源单据号
	 *
	 * @return 来源单据号
	 */
	public String getSrcbillno() {
		return get("srcbillno");
	}

	/**
	 * 设置来源单据号
	 *
	 * @param srcbillno 来源单据号
	 */
	public void setSrcbillno(String srcbillno) {
		set("srcbillno", srcbillno);
	}

	/**
	 * 获取来源单据行号
	 *
	 * @return 来源单据行号
	 */
	public String getSrcbillitemno() {
		return get("srcbillitemno");
	}

	/**
	 * 设置来源单据行号
	 *
	 * @param srcbillitemno 来源单据行号
	 */
	public void setSrcbillitemno(String srcbillitemno) {
		set("srcbillitemno", srcbillitemno);
	}

	/**
	 * 获取来源单据行id
	 *
	 * @return 来源单据行id
	 */
	public String getSrcbillitemid() {
		return get("srcbillitemid");
	}

	/**
	 * 设置来源单据行id
	 *
	 * @param srcbillitemid 来源单据行id
	 */
	public void setSrcbillitemid(String srcbillitemid) {
		set("srcbillitemid", srcbillitemid);
	}

	/**
	 * 获取来源单据主表id
	 *
	 * @return 来源单据主表id
	 */
	public String getSrcbillitemmainid() {
		return get("srcbillitemmainid");
	}

	/**
	 * 设置来源单据主表id
	 *
	 * @param srcbillitemmainid 来源单据主表id
	 */
	public void setSrcbillitemmainid(String srcbillitemmainid) {
		set("srcbillitemmainid", srcbillitemmainid);
	}

	/**
	 * 获取源头单据行id
	 *
	 * @return 源头单据行id
	 */
	public String getTopsrcbillitemid() {
		return get("topsrcbillitemid");
	}

	/**
	 * 设置源头单据行id
	 *
	 * @param topsrcbillitemid 源头单据行id
	 */
	public void setTopsrcbillitemid(String topsrcbillitemid) {
		set("topsrcbillitemid", topsrcbillitemid);
	}

	/**
	 * 获取源头单据主表id
	 *
	 * @return 源头单据主表id
	 */
	public String getTopsrcbillitemmainid() {
		return get("topsrcbillitemmainid");
	}

	/**
	 * 设置源头单据主表id
	 *
	 * @param topsrcbillitemmainid 源头单据主表id
	 */
	public void setTopsrcbillitemmainid(String topsrcbillitemmainid) {
		set("topsrcbillitemmainid", topsrcbillitemmainid);
	}

	/**
	 * 获取来源采购发票单据行id
	 *
	 * @return 来源采购发票单据行id
	 */
	public String getSrcinvoicebillitemid() {
		return get("srcinvoicebillitemid");
	}

	/**
	 * 设置来源采购发票单据行id
	 *
	 * @param srcinvoicebillitemid 来源采购发票单据行id
	 */
	public void setSrcinvoicebillitemid(String srcinvoicebillitemid) {
		set("srcinvoicebillitemid", srcinvoicebillitemid);
	}

	/**
	 * 获取业务组织
	 *
	 * @return 业务组织.ID
	 */
	public String getOrg() {
		return get("org");
	}

	/**
	 * 设置业务组织
	 *
	 * @param org 业务组织.ID
	 */
	public void setOrg(String org) {
		set("org", org);
	}

	/**
	 * 获取业务部门
	 *
	 * @return 业务部门.ID
	 */
	public String getDept() {
		return get("dept");
	}

	/**
	 * 设置业务部门
	 *
	 * @param dept 业务部门.ID
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
	 * 获取物料SKUid
	 *
	 * @return 物料SKUid.ID
	 */
	public Long getProductsku() {
		return get("productsku");
	}

	/**
	 * 设置物料SKUid
	 *
	 * @param productsku 物料SKUid.ID
	 */
	public void setProductsku(Long productsku) {
		set("productsku", productsku);
	}

	/**
	 * 获取物料
	 *
	 * @return 物料.ID
	 */
	public Long getMaterial() {
		return get("material");
	}

	/**
	 * 设置物料
	 *
	 * @param material 物料.ID
	 */
	public void setMaterial(Long material) {
		set("material", material);
	}

	/**
	 * 获取期号
	 *
	 * @return 期号
	 */
	public String getIssue() {
		return get("issue");
	}

	/**
	 * 设置期号
	 *
	 * @param issue 期号
	 */
	public void setIssue(String issue) {
		set("issue", issue);
	}

	/**
	 * 获取合同号
	 *
	 * @return 合同号
	 */
	public String getContractNo() {
		return get("contractNo");
	}

	/**
	 * 设置合同号
	 *
	 * @param contractNo 合同号
	 */
	public void setContractNo(String contractNo) {
		set("contractNo", contractNo);
	}

	/**
	 * 获取订单号
	 *
	 * @return 订单号
	 */
	public String getOrderNo() {
		return get("orderNo");
	}

	/**
	 * 设置订单号
	 *
	 * @param orderNo 订单号
	 */
	public void setOrderNo(String orderNo) {
		set("orderNo", orderNo);
	}

	/**
	 * 获取发票号
	 *
	 * @return 发票号
	 */
	public String getInvoiceNo() {
		return get("invoiceNo");
	}

	/**
	 * 设置发票号
	 *
	 * @param invoiceNo 发票号
	 */
	public void setInvoiceNo(String invoiceNo) {
		set("invoiceNo", invoiceNo);
	}

	/**
	 * 获取付款申请金额
	 *
	 * @return 付款申请金额
	 */
	public java.math.BigDecimal getPaymentApplyAmount() {
		return get("paymentApplyAmount");
	}

	/**
	 * 设置付款申请金额
	 *
	 * @param paymentApplyAmount 付款申请金额
	 */
	public void setPaymentApplyAmount(java.math.BigDecimal paymentApplyAmount) {
		set("paymentApplyAmount", paymentApplyAmount);
	}

	/**
	 * 获取付款预占金额
	 *
	 * @return 付款预占金额
	 */
	public java.math.BigDecimal getPaymentPreemptAmount() {
		return get("paymentPreemptAmount");
	}

	/**
	 * 设置付款预占金额
	 *
	 * @param paymentPreemptAmount 付款预占金额
	 */
	public void setPaymentPreemptAmount(java.math.BigDecimal paymentPreemptAmount) {
		set("paymentPreemptAmount", paymentPreemptAmount);
	}

	/**
	 * 获取已付款金额
	 *
	 * @return 已付款金额
	 */
	public java.math.BigDecimal getPaidAmount() {
		return get("paidAmount");
	}

	/**
	 * 设置已付款金额
	 *
	 * @param paidAmount 已付款金额
	 */
	public void setPaidAmount(java.math.BigDecimal paidAmount) {
		set("paidAmount", paidAmount);
	}

	/**
	 * 获取未付款金额
	 *
	 * @return 未付款金额
	 */
	public java.math.BigDecimal getUnpaidAmount() {
		return get("unpaidAmount");
	}

	/**
	 * 设置未付款金额
	 *
	 * @param unpaidAmount 未付款金额
	 */
	public void setUnpaidAmount(java.math.BigDecimal unpaidAmount) {
		set("unpaidAmount", unpaidAmount);
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
	 * 获取行号
	 *
	 * @return 行号
	 */
	public Integer getRowno() {
		return get("rowno");
	}

	/**
	 * 设置行号
	 *
	 * @param rowno 行号
	 */
	public void setRowno(Integer rowno) {
		set("rowno", rowno);
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
	 * 获取来源采购订单计划单据行id
	 *
	 * @return 来源采购订单计划单据行id
	 */
	public String getSrcpurchaseplanitemid() {
		return get("srcpurchaseplanitemid");
	}

	/**
	 * 设置来源采购订单计划单据行id
	 *
	 * @param srcpurchaseplanitemid 来源采购订单计划单据行id
	 */
	public void setSrcpurchaseplanitemid(String srcpurchaseplanitemid) {
		set("srcpurchaseplanitemid", srcpurchaseplanitemid);
	}
	/**
	 * 获取来源采购订单物料明细行id
	 *
	 * @return 来源采购订单物料明细行id
	 */
	public String getSrcPurchaseOrderMaterialLineId() {
		return get("srcPurchaseOrderMaterialLineId");
	}

	/**
	 * 设置来源采购订单物料明细行id
	 *
	 * @param srcPurchaseOrderMaterialLineId 来源采购订单物料明细行id
	 */
	public void setSrcPurchaseOrderMaterialLineId(String srcPurchaseOrderMaterialLineId) {
		set("srcPurchaseOrderMaterialLineId", srcPurchaseOrderMaterialLineId);
	}

	/**
	 * 获取源头单据类型
	 *
	 * @return 源头单据类型
	 */
	public Short getSourceOrderType() {
		return getShort("sourceOrderType");
	}

	/**
	 * 设置源头单据类型
	 *
	 * @param sourceOrderType 源头单据类型
	 */
	public void setSourceOrderType(Short sourceOrderType) {
		set("sourceOrderType", sourceOrderType);
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
	 * 获取付款申请单子表特征
	 *
	 * @return 付款申请单子表特征.ID
	 */
	public String getCharacterDefb() {
		return get("characterDefb");
	}

	/**
	 * 设置付款申请单子表特征
	 *
	 * @param characterDefb 付款申请单子表特征.ID
	 */
	public void setCharacterDefb(String characterDefb) {
		set("characterDefb", characterDefb);
	}

}
