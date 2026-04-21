package com.yonyoucloud.fi.cmp.checkmanage;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalFlow;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.ext.voucher.base.Vouch;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;

/**
 * 支票处置主表实体
 *
 * @author u
 * @version 1.0
 */
public class CheckManage extends Vouch implements IAuditInfo, ITenant, IApprovalInfo, IApprovalFlow, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.checkmanage.CheckManage";

	/**
	 * 获取单据类型
	 *
	 * @return 单据类型
	 */
	public Short getBilltype() {
		return getShort("billtype");
	}

	/**
	 * 设置单据类型
	 *
	 * @param billtype 单据类型
	 */
	public void setBilltype(Short billtype) {
		set("billtype", billtype);
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
	 * 获取单据日期
	 *
	 * @return 单据日期
	 */
	public java.util.Date getBilldate() {
		return get("billdate");
	}

	/**
	 * 设置单据日期
	 *
	 * @param billdate 单据日期
	 */
	public void setBilldate(java.util.Date billdate) {
		set("billdate", billdate);
	}

	/**
	 * 获取单据编号
	 *
	 * @return 编码
	 */
	public String getCode() {
		return get("code");
	}

	/**
	 * 设置单据编号
	 *
	 * @param code 编码
	 */
	public void setCode(String code) {
		set("code", code);
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
	 * 获取处置说明
	 *
	 * @return *处置说明
	 */
	public String getDescription() {
		return get("description");
	}

	/**
	 * 设置处置说明
	 *
	 * @param description *处置说明
	 */
	public void setDescription(String description) {
		set("description", description);
	}

	/**
	 * 获取生成方式
	 *
	 * @return 生成方式
	 */
	public Short getGenerateType() {
		return getShort("generateType");
	}

	/**
	 * 设置生成方式
	 *
	 * @param generateType 生成方式
	 */
	public void setGenerateType(Short generateType) {
		set("generateType", generateType);
	}

	/**
	 * 获取处置数量(张)
	 *
	 * @return 处置数量(张)
	 */
	public Integer getHandleNum() {
		return get("handleNum");
	}

	/**
	 * 设置处置数量(张)
	 *
	 * @param handleNum 处置数量(张)
	 */
	public void setHandleNum(Integer handleNum) {
		set("handleNum", handleNum);
	}

	/**
	 * 获取处置类型
	 *
	 * @return 处置类型
	 */
	public String getHandletype() {
		return get("handletype");
	}

	/**
	 * 设置处置类型
	 *
	 * @param handletype 处置类型
	 */
	public void setHandletype(String handletype) {
		set("handletype", handletype);
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
	 * 获取支票工作台来源单据号
	 *
	 * @return 支票工作台来源单据号
	 */
	public String getSourceckBillNo() {
		return get("sourceckBillNo");
	}

	/**
	 * 设置支票工作台来源单据号
	 *
	 * @param sourceckBillNo 支票工作台来源单据号
	 */
	public void setSourceckBillNo(String sourceckBillNo) {
		set("sourceckBillNo", sourceckBillNo);
	}

	/**
	 * 获取结算变更来源单据号
	 *
	 * @return 结算变更来源单据号
	 */
	public String getSourcejsBillNo() {
		return get("sourcejsBillNo");
	}

	/**
	 * 设置结算变更来源单据号
	 *
	 * @param sourcejsBillNo 结算变更来源单据号
	 */
	public void setSourcejsBillNo(String sourcejsBillNo) {
		set("sourcejsBillNo", sourcejsBillNo);
	}

	/**
	 * 获取单据状态
	 *
	 * @return 单据状态
	 */
	public Status getStatus() {
		return Status.find(getShort("status"));
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
	 * 获取支票处置子表集合
	 *
	 * @return 支票处置子表集合
	 */
	public java.util.List<CheckManageDetail> CheckManageDetail() {
		return getBizObjects("CheckManageDetail", CheckManageDetail.class);
	}

	/**
	 * 设置支票处置子表集合
	 *
	 * @param CheckManageDetail 支票处置子表集合
	 */
	public void setCheckManageDetail(java.util.List<CheckManageDetail> CheckManageDetail) {
		setBizObjects("CheckManageDetail", CheckManageDetail);
	}

	/**
	 * 获取租户id
	 *
	 * @return 租户id.ID
	 */
	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	/**
	 * 设置租户id
	 *
	 * @param s 租户id.ID
	 */
	@Override
	public void setYTenant(String s) {
		set("ytenant", s);
	}

	/**
	 * 获取支票处置特征
	 *
	 * @return 支票处置特征.ID
	 */
	public String getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置支票处置特征
	 *
	 * @param characterDef 支票处置特征.ID
	 */
	public void setCharacterDef(String characterDef) {
		set("characterDef", characterDef);
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
}
