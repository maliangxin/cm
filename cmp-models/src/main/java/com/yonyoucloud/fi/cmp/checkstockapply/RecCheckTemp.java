package com.yonyoucloud.fi.cmp.checkstockapply;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 支票收票临时表实体
 *
 * @author u
 * @version 1.0
 */
public class RecCheckTemp extends BizObject implements IAuditInfo, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.checkstockapply.RecCheckTemp";

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
     * 获取付款银行名称
     *
     * @return 付款银行名称.ID
     */
	public String getPayBank() {
		return get("payBank");
	}

    /**
     * 设置付款银行名称
     *
     * @param payBank 付款银行名称.ID
     */
	public void setPayBank(String payBank) {
		set("payBank", payBank);
	}

    /**
     * 获取出票日期
     *
     * @return 出票日期
     */
	public java.util.Date getDrawerDate() {
		return get("drawerDate");
	}

    /**
     * 设置出票日期
     *
     * @param drawerDate 出票日期
     */
	public void setDrawerDate(java.util.Date drawerDate) {
		set("drawerDate", drawerDate);
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
     * 获取是否可背书
     *
     * @return 是否可背书
     */
	public Short getEnableEndorse() {
	    return getShort("enableEndorse");
	}

    /**
     * 设置是否可背书
     *
     * @param enableEndorse 是否可背书
     */
	public void setEnableEndorse(Short enableEndorse) {
		set("enableEndorse", enableEndorse);
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
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public String getCharacterDefb() {
		return get("characterDefb");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param characterDefb 自定义项特征属性组.ID
	 */
	public void setCharacterDefb(String characterDefb) {
		set("characterDefb", characterDefb);
	}

}
