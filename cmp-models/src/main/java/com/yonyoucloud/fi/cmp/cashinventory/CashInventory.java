package com.yonyoucloud.fi.cmp.cashinventory;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.*;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 现金盘点主表实体
 *
 * @author u
 * @version 1.0
 */
public class CashInventory extends BizObject implements IAuditInfo, ITenant, IApprovalInfo, ICurrency, IApprovalFlow, IPrintCount, IYTenant, AccentityRawInterface {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.cashinventory.CashInventory";
	private static final long serialVersionUID = -6186389564894737310L;

	/**
	 * 获取核算会计
	 *
	 * @return 核算会计.ID
	 */
	@Override
	public String getAccentityRaw() {
		return get("accentityRaw");
	}

	/**
	 * 设置核算会计
	 *
	 * @param accentityRaw 核算会计.ID
	 */
	@Override
	public void setAccentityRaw(String accentityRaw) {
		set("accentityRaw", accentityRaw);
	}

	/**
	 * 获取资金组织
	 *
	 * @return 资金组织.ID
	 */
	@Override
	public String getAccentity() {
		return get("accentity");
	}

	/**
	 * 设置资金组织
	 *
	 * @param accentity 资金组织.ID
	 */
	@Override
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}
	public String getCashaccount() {
		return get("cashaccount");
	}

    /**
     * 设置现金账户
     *
     * @param cashaccount 现金账户.ID
     */
	public void setCashaccount(String cashaccount) {
		set("cashaccount", cashaccount);
	}

	/**
	 * 获取审批流控制
	 *
	 * @return 审批流控制
	 */
	@Override
	public Boolean getIsWfControlled() {
		return getBoolean("isWfControlled");
	}

	/**
	 * 设置审批流控制
	 *
	 * @param isWfControlled 审批流控制
	 */
	@Override
	public void setIsWfControlled(Boolean isWfControlled) {
		set("isWfControlled", isWfControlled);
	}

    /**
     * 获取盘点人
     *
     * @return 盘点人
     */
	public String getInventorystaff() {
		return get("inventorystaff");
	}

    /**
     * 设置盘点人
     *
     * @param inventorystaff 盘点人
     */
	public void setInventorystaff(String inventorystaff) {
		set("inventorystaff", inventorystaff);
	}

    /**
     * 获取基本单位
     *
     * @return 基本单位
     */
	public String getBasicunit() {
		return get("basicunit");
	}

    /**
     * 设置基本单位
     *
     * @param basicunit 基本单位
     */
	public void setBasicunit(String basicunit) {
		set("basicunit", basicunit);
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
	public String getDescription() {
		return get("description");
	}

    /**
     * 设置备注
     *
     * @param description 备注
     */
	public void setDescription(String description) {
		set("description", description);
	}

    /**
     * 获取摘要
     *
     * @return 摘要
     */
	public String getSummary() {
		return get("summary");
	}

    /**
     * 设置摘要
     *
     * @param summary 摘要
     */
	public void setSummary(String summary) {
		set("summary", summary);
	}

	/**
	 * 获取汇率类型
	 *
	 * @return 汇率类型.ID
	 */
	public String getExchangeRateType() {
		return get("exchangeRateType");
	}

	/**
	 * 设置汇率类型
	 *
	 * @param exchangeRateType 汇率类型.ID
	 */
	public void setExchangeRateType(String exchangeRateType) {
		set("exchangeRateType", exchangeRateType);
	}
	/**
	 * 获取长款本币金额
	 *
	 * @return 长款本币金额
	 */
	public java.math.BigDecimal getLonglocalamount() {
		return get("longlocalamount");
	}

	/**
	 * 设置长款本币金额
	 *
	 * @param longlocalamount 长款本币金额
	 */
	public void setLonglocalamount(java.math.BigDecimal longlocalamount) {
		set("longlocalamount", longlocalamount);
	}

	/**
	 * 获取短款本币金额
	 *
	 * @return 短款本币金额
	 */
	public java.math.BigDecimal getShortielocalamount() {
		return get("shortielocalamount");
	}

	/**
	 * 设置短款本币金额
	 *
	 * @param shortielocalamount 短款本币金额
	 */
	public void setShortielocalamount(java.math.BigDecimal shortielocalamount) {
		set("shortielocalamount", shortielocalamount);
	}


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
     * 获取盘点日现金日记账余额
     *
     * @return 盘点日现金日记账余额
     */
	public java.math.BigDecimal getJournalbalance() {
		return get("journalbalance");
	}

    /**
     * 设置盘点日现金日记账余额
     *
     * @param journalbalance 盘点日现金日记账余额
     */
	public void setJournalbalance(java.math.BigDecimal journalbalance) {
		set("journalbalance", journalbalance);
	}

    /**
     * 获取调整后日记账余额
     *
     * @return 调整后日记账余额
     */
	public java.math.BigDecimal getAdjustjournalbalance() {
		return get("adjustjournalbalance");
	}

    /**
     * 设置调整后日记账余额
     *
     * @param adjustjournalbalance 调整后日记账余额
     */
	public void setAdjustjournalbalance(java.math.BigDecimal adjustjournalbalance) {
		set("adjustjournalbalance", adjustjournalbalance);
	}

    /**
     * 获取已登账 未付款
     *
     * @return 已登账 未付款
     */
	public java.math.BigDecimal getRegisteredunpaid() {
		return get("registeredunpaid");
	}

    /**
     * 设置已登账 未付款
     *
     * @param registeredunpaid 已登账 未付款
     */
	public void setRegisteredunpaid(java.math.BigDecimal registeredunpaid) {
		set("registeredunpaid", registeredunpaid);
	}

    /**
     * 获取已登账 未收款
     *
     * @return 已登账 未收款
     */
	public java.math.BigDecimal getRegisteredunreceived() {
		return get("registeredunreceived");
	}

    /**
     * 设置已登账 未收款
     *
     * @param registeredunreceived 已登账 未收款
     */
	public void setRegisteredunreceived(java.math.BigDecimal registeredunreceived) {
		set("registeredunreceived", registeredunreceived);
	}

    /**
     * 获取未登账 已付款
     *
     * @return 未登账 已付款
     */
	public java.math.BigDecimal getUnregisteredpaid() {
		return get("unregisteredpaid");
	}

    /**
     * 设置未登账 已付款
     *
     * @param unregisteredpaid 未登账 已付款
     */
	public void setUnregisteredpaid(java.math.BigDecimal unregisteredpaid) {
		set("unregisteredpaid", unregisteredpaid);
	}

    /**
     * 获取未登账 已收款
     *
     * @return 未登账 已收款
     */
	public java.math.BigDecimal getUnregisteredreceived() {
		return get("unregisteredreceived");
	}

    /**
     * 设置未登账 已收款
     *
     * @param unregisteredreceived 未登账 已收款
     */
	public void setUnregisteredreceived(java.math.BigDecimal unregisteredreceived) {
		set("unregisteredreceived", unregisteredreceived);
	}

    /**
     * 获取现金实点金额
     *
     * @return 现金实点金额
     */
	public java.math.BigDecimal getActualamount() {
		return get("actualamount");
	}

    /**
     * 设置现金实点金额
     *
     * @param actualamount 现金实点金额
     */
	public void setActualamount(java.math.BigDecimal actualamount) {
		set("actualamount", actualamount);
	}

    /**
     * 获取长款
     *
     * @return 长款
     */
	public java.math.BigDecimal getLongstyle() {
		return get("longstyle");
	}

    /**
     * 设置长款
     *
     * @param longstyle 长款
     */
	public void setLongstyle(java.math.BigDecimal longstyle) {
		set("longstyle", longstyle);
	}

    /**
     * 获取短款
     *
     * @return 短款
     */
	public java.math.BigDecimal getShortie() {
		return get("shortie");
	}

    /**
     * 设置短款
     *
     * @param shortie 短款
     */
	public void setShortie(java.math.BigDecimal shortie) {
		set("shortie", shortie);
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
	 * 获取凭证状态
	 *
	 * @return 凭证状态
	 */
	public Short getVoucherstatus() {
		return getShort("voucherstatus");
	}

	/**
	 * 设置凭证状态
	 *
	 * @param voucherstatus 凭证状态
	 */
	public void setVoucherstatus(Short voucherstatus) {
		set("voucherstatus", voucherstatus);
	}

    /**
     * 获取结算方式
     *
     * @return 结算方式.ID
     */
	public Long getSettlemode() {
		return get("settlemode");
	}

    /**
     * 设置结算方式
     *
     * @param settlemode 结算方式.ID
     */
	public void setSettlemode(Long settlemode) {
		set("settlemode", settlemode);
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
     * 获取单据日期---盘点日期
     *
     * @return 单据日期
     */
	public java.util.Date getVouchdate() {
		return get("vouchdate");
	}

    /**
     * 设置单据日期---盘点日期
     *
     * @param vouchdate 单据日期---盘点日期
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
     * 获取现金盘点子表集合
     *
     * @return 现金盘点子表集合
     */
	public java.util.List<CashInventory_b> CashInventory_b() {
		return getBizObjects("CashInventory_b", CashInventory_b.class);
	}

    /**
     * 设置现金盘点子表集合
     *
     * @param CashInventory_b 现金盘点子表集合
     */
	public void setCashInventory_b(java.util.List<CashInventory_b> CashInventory_b) {
		setBizObjects("CashInventory_b", CashInventory_b);
	}
	/**
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public String getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param characterDef 自定义项特征属性组.ID
	 */
	public void setCharacterDef(String characterDef) {
		set("characterDef", characterDef);
	}
	/**
	 * 获取货币面额组件id
	 *
	 * @return 货币面额组件id
	 */
	public Long getDenominationSettingId() {
		return get("denominationSettingId");
	}

	/**
	 * 设置货币面额组件id
	 *
	 * @param denominationSettingId 货币面额组件id
	 */
	public void setDenominationSettingId(Long denominationSettingId) {
		set("denominationSettingId", denominationSettingId);
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
