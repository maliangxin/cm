package com.yonyoucloud.fi.cmp.checkmanage;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IBackWrite;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.ext.voucher.base.VouchLine;

/**
 * 支票处置子表实体
 *
 * @author u
 * @version 1.0
 */
public class CheckManageDetail extends VouchLine implements ITenant, IBackWrite, IYTenant, IAuditInfo {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.checkmanage.CheckManageDetail";

	/**
	 * 获取金额
	 *
	 * @return 金额
	 */
	public java.math.BigDecimal getAmount() {
		return get("amount");
	}

	/**
	 * 设置金额
	 *
	 * @param amount 金额
	 */
	public void setAmount(java.math.BigDecimal amount) {
		set("amount", amount);
	}

	/**
	 * 获取支票方向
	 *
	 * @return 支票方向
	 */
	public String getCheckBillDir() {
		return get("checkBillDir");
	}

	/**
	 * 设置支票方向
	 *
	 * @param checkBillDir 支票方向
	 */
	public void setCheckBillDir(String checkBillDir) {
		set("checkBillDir", checkBillDir);
	}

	/**
	 * 获取支票编号
	 *
	 * @return 支票编号
	 */
	public String getCheckBillNo() {
		return get("checkBillNo");
	}

	/**
	 * 设置支票编号
	 *
	 * @param checkBillNo 支票编号
	 */
	public void setCheckBillNo(String checkBillNo) {
		set("checkBillNo", checkBillNo);
	}

	/**
	 * 获取支票类型
	 *
	 * @return 支票类型
	 */
	public Short getCheckBillType() {
		return getShort("checkBillType");
	}

	/**
	 * 设置支票类型
	 *
	 * @param checkBillType 支票类型
	 */
	public void setCheckBillType(Short checkBillType) {
		set("checkBillType", checkBillType);
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
	 * 获取出票人账户名称
	 *
	 * @return 出票人账户名称
	 */
	public String getDrawerAcctName() {
		return get("drawerAcctName");
	}

	/**
	 * 设置出票人账户名称
	 *
	 * @param drawerAcctName 出票人账户名称
	 */
	public void setDrawerAcctName(String drawerAcctName) {
		set("drawerAcctName", drawerAcctName);
	}

	/**
	 * 获取出票人银行账号
	 *
	 * @return 出票人银行账号
	 */
	public String getDrawerAcctNo() {
		return get("drawerAcctNo");
	}

	/**
	 * 设置出票人银行账号
	 *
	 * @param drawerAcctNo 出票人银行账号
	 */
	public void setDrawerAcctNo(String drawerAcctNo) {
		set("drawerAcctNo", drawerAcctNo);
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
	 * 获取处置原因
	 *
	 * @return 处置原因
	 */
	public String getHandlereason() {
		return get("handlereason");
	}

	/**
	 * 设置处置原因
	 *
	 * @param handlereason 处置原因
	 */
	public void setHandlereason(String handlereason) {
		set("handlereason", handlereason);
	}

	/**
	 * 获取处置类型(明细)
	 *
	 * @return 处置类型(明细)
	 */
	public Short getHandletypeDetail() {
		return getShort("handletypeDetail");
	}

	/**
	 * 设置处置类型(明细)
	 *
	 * @param handletypeDetail 处置类型(明细)
	 */
	public void setHandletypeDetail(Short handletypeDetail) {
		set("handletypeDetail", handletypeDetail);
	}

	/**
	 * 获取入库单主表id
	 *
	 * @return 入库单主表id.ID
	 */
	public Long getMainid() {
		return get("mainid");
	}

	/**
	 * 设置入库单主表id
	 *
	 * @param mainid 入库单主表id.ID
	 */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
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
	 * 获取付款银行
	 *
	 * @return 付款银行.ID
	 */
	public String getPayBank() {
		return get("payBank");
	}

	/**
	 * 设置付款银行
	 *
	 * @param payBank 付款银行.ID
	 */
	public void setPayBank(String payBank) {
		set("payBank", payBank);
	}

	/**
	 * 获取付款银行名称
	 *
	 * @return 付款银行名称
	 */
	public String getPayBankName() {
		return get("payBankName");
	}

	/**
	 * 设置付款银行名称
	 *
	 * @param payBankName 付款银行名称
	 */
	public void setPayBankName(String payBankName) {
		set("payBankName", payBankName);
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
	 * 获取序号
	 *
	 * @return 序号
	 */
	public Integer getRowno() {
		return get("rowno");
	}

	/**
	 * 设置序号
	 *
	 * @param rowno 序号
	 */
	public void setRowno(Integer rowno) {
		set("rowno", rowno);
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
	 * 获取入库日期
	 *
	 * @return 入库日期
	 */
	public java.util.Date getVouchdate() {
		return get("vouchdate");
	}

	/**
	 * 设置入库日期
	 *
	 * @param vouchdate 入库日期
	 */
	public void setVouchdate(java.util.Date vouchdate) {
		set("vouchdate", vouchdate);
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
	 * 获取支票处置子表特征
	 *
	 * @return 支票处置子表特征.ID
	 */
	public String getCharacterDefb() {
		return get("characterDefb");
	}

	/**
	 * 设置支票处置子表特征
	 *
	 * @param characterDefb 支票处置子表特征.ID
	 */
	public void setCharacterDefb(String characterDefb) {
		set("characterDefb", characterDefb);
	}

	/**
	 * 获取记录支票处置时的支票状态
	 *
	 * @return 记录支票处置时的支票状态
	 */
	public String getCheckBillStatus() {
		return get("checkBillStatus");
	}

	/**
	 * 设置记录支票处置时的支票状态
	 *
	 * @param checkBillStatus 记录支票处置时的支票状态
	 */
	public void setCheckBillStatus(String checkBillStatus) {
		set("checkBillStatus", checkBillStatus);
	}

	/**
	 * 获取支票id
	 *
	 * @return 支票id.ID
	 */
	public Long getCheckid() {
		return get("checkid");
	}

	/**
	 * 设置支票id
	 *
	 * @param checkid 支票id.ID
	 */
	public void setCheckid(Long checkid) {
		set("checkid", checkid);
	}
}
