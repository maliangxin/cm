package com.yonyoucloud.fi.cmp.checkstock;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IPrintCount;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 支票库存实体
 *
 * @author u
 * @version 1.0
 */
public class CheckStock extends BizObject implements IAuditInfo, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.checkstock.CheckStock";



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
     * 获取使用组织
     *
     * @return 使用组织.ID
     */
	public String getCustNo() {
		return get("custNo");
	}

    /**
     * 设置使用组织
     *
     * @param custNo 使用组织.ID
     */
	public void setCustNo(String custNo) {
		set("custNo", custNo);
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
     * 获取支票状态
     *
     * @return 支票状态
     */
	public String getCheckBillStatus() {
		return get("checkBillStatus");
	}

    /**
     * 设置支票状态
     *
     * @param checkBillStatus 支票状态
     */
	public void setCheckBillStatus(String checkBillStatus) {
		set("checkBillStatus", checkBillStatus);
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
     * 获取支票簿编号
     *
     * @return 支票簿编号
     */
	public String getCheckBookNo() {
		return get("checkBookNo");
	}

    /**
     * 设置支票簿编号
     *
     * @param checkBookNo 支票簿编号
     */
	public void setCheckBookNo(String checkBookNo) {
		set("checkBookNo", checkBookNo);
	}

    /**
     * 获取出票人账户
     *
     * @return 出票人账户
     */
	public String getDrawerAcct() {
		return get("drawerAcct");
	}

    /**
     * 设置出票人账户
     *
     * @param drawerAcct 出票人账户
     */
	public void setDrawerAcct(String drawerAcct) {
		set("drawerAcct", drawerAcct);
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
     * 获取出票人名称
     *
     * @return 出票人名称
     */
	public String getDrawerName() {
		return get("drawerName");
	}

    /**
     * 设置出票人名称
     *
     * @param drawerName 出票人名称
     */
	public void setDrawerName(String drawerName) {
		set("drawerName", drawerName);
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
     * 获取收款人名称
     *
     * @return 收款人名称
     */
	public String getPayeeName() {
		return get("payeeName");
	}

    /**
     * 设置收款人名称
     *
     * @param payeeName 收款人名称
     */
	public void setPayeeName(String payeeName) {
		set("payeeName", payeeName);
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
     * 获取背书日期
     *
     * @return 背书日期
     */
	public java.util.Date getEndorseDate() {
		return get("endorseDate");
	}

    /**
     * 设置背书日期
     *
     * @param endorseDate 背书日期
     */
	public void setEndorseDate(java.util.Date endorseDate) {
		set("endorseDate", endorseDate);
	}

    /**
     * 获取被背书人
     *
     * @return 被背书人
     */
	public String getEndorsee() {
		return get("endorsee");
	}

    /**
     * 设置被背书人
     *
     * @param endorsee 被背书人
     */
	public void setEndorsee(String endorsee) {
		set("endorsee", endorsee);
	}

    /**
     * 获取入库日期
     *
     * @return 入库日期
     */
	public java.util.Date getBusiDate() {
		return get("busiDate");
	}

    /**
     * 设置入库日期
     *
     * @param busiDate 入库日期
     */
	public void setBusiDate(java.util.Date busiDate) {
		set("busiDate", busiDate);
	}

    /**
     * 获取登记人
     *
     * @return 登记人
     */
	public String getCreater() {
		return get("creater");
	}

    /**
     * 设置登记人
     *
     * @param creater 登记人
     */
	public void setCreater(String creater) {
		set("creater", creater);
	}

    /**
     * 获取兑付日期
     *
     * @return 兑付日期
     */
	public java.util.Date getCashDate() {
		return get("cashDate");
	}

    /**
     * 设置兑付日期
     *
     * @param cashDate 兑付日期
     */
	public void setCashDate(java.util.Date cashDate) {
		set("cashDate", cashDate);
	}

	/**
	 * 获取支票用途
	 *
	 * @return 支票用途
	 */
	public Short getCheckpurpose() {
		return getShort("checkpurpose");
	}

	/**
	 * 设置支票用途
	 *
	 * @param checkpurpose 支票用途
	 */
	public void setCheckpurpose(Short checkpurpose) {
		set("checkpurpose", checkpurpose);
	}

    /**
     * 获取作废日期
     *
     * @return 作废日期
     */
	public java.util.Date getCancelDate() {
		return get("cancelDate");
	}

    /**
     * 设置作废日期
     *
     * @param cancelDate 作废日期
     */
	public void setCancelDate(java.util.Date cancelDate) {
		set("cancelDate", cancelDate);
	}

    /**
     * 获取作废人
     *
     * @return 作废人
     */
	public String getCancelPerson() {
		return get("cancelPerson");
	}

    /**
     * 设置作废人
     *
     * @param cancelPerson 作废人
     */
	public void setCancelPerson(String cancelPerson) {
		set("cancelPerson", cancelPerson);
	}

    /**
     * 获取挂失日期
     *
     * @return 挂失日期
     */
	public java.util.Date getLossDate() {
		return get("lossDate");
	}

    /**
     * 设置挂失日期
     *
     * @param lossDate 挂失日期
     */
	public void setLossDate(java.util.Date lossDate) {
		set("lossDate", lossDate);
	}

    /**
     * 获取挂失人
     *
     * @return 挂失人
     */
	public String getLossPerson() {
		return get("lossPerson");
	}

    /**
     * 设置挂失人
     *
     * @param lossPerson 挂失人
     */
	public void setLossPerson(String lossPerson) {
		set("lossPerson", lossPerson);
	}

	/**
	 * 获取处置说明
	 *
	 * @return 处置说明
	 */
	public String getFailReason() {
		return get("failReason");
	}

	/**
	 * 设置处置说明
	 *
	 * @param failReason 处置说明
	 */
	public void setFailReason(String failReason) {
		set("failReason", failReason);
	}

	/**
	 * 获取处置类型
	 *
	 * @return 处置类型
	 */
	public Short getHandletype() {
		return getShort("handletype");
	}

	/**
	 * 设置处置类型
	 *
	 * @param handletype 处置类型
	 */
	public void setHandletype(Short handletype) {
		set("handletype", handletype);
	}

	/**
	 * 获取处置人
	 *
	 * @return 处置人
	 */
	public String getDisposer() {
		return get("disposer");
	}

	/**
	 * 设置处置人
	 *
	 * @param disposer 处置人
	 */
	public void setDisposer(String disposer) {
		set("disposer", disposer);
	}

	/**
	 * 获取处置日期
	 *
	 * @return 处置日期
	 */
	public java.util.Date getDisposalDate() {
		return get("disposalDate");
	}

	/**
	 * 设置处置日期
	 *
	 * @param disposalDate 处置日期
	 */
	public void setDisposalDate(java.util.Date disposalDate) {
		set("disposalDate", disposalDate);
	}

	/**
	 * 获取处置前支票状态
	 *
	 * @return 处置前支票状态
	 */
	public String getBeforeCheckStatus() {
		return get("beforeCheckStatus");
	}

	/**
	 * 设置处置前支票状态
	 *
	 * @param beforeCheckStatus 处置前支票状态
	 */
	public void setBeforeCheckStatus(String beforeCheckStatus) {
		set("beforeCheckStatus", beforeCheckStatus);
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
     * 获取申请编号
     *
     * @return 申请编号.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置申请编号
     *
     * @param mainid 申请编号.ID
     */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}

    /**
     * 获取是否锁定
     *
     * @return 是否锁定
     */
	public String getIsLock() {
		return get("isLock");
	}

    /**
     * 设置是否锁定
     *
     * @param isLock 是否锁定
     */
	public void setIsLock(String isLock) {
		set("isLock", isLock);
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
	 * 获取是否可使用
	 *
	 * @return 是否可使用
	 */
	public Short getIsUsed() {
		return get("isUsed");
	}

	/**
	 * 设置是否可使用
	 *
	 * @param isUsed 是否可使用
	 */
	public void setIsUsed(Short isUsed) {
		set("isUsed", isUsed);
	}

    /**
     * 获取业务系统
     *
     * @return 业务系统
     */
	public String getSysNo() {
		return get("sysNo");
	}

    /**
     * 设置业务系统
     *
     * @param sysNo 业务系统
     */
	public void setSysNo(String sysNo) {
		set("sysNo", sysNo);
	}

	/**
	 * 获取是否关联盘点
	 *
	 * @return 是否关联盘点
	 */
	public Short getIsInventory() {
		return getShort("isInventory");
	}

	/**
	 * 设置是否关联盘点
	 *
	 * @param isInventory 是否关联盘点
	 */
	public void setIsInventory(Short isInventory) {
		set("isInventory", isInventory);
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
     * 获取单据编号
     *
     * @return 单据编号
     */
	public String getInputBillNo() {
		return get("inputBillNo");
	}

    /**
     * 设置单据编号
     *
     * @param inputBillNo 单据编号
     */
	public void setInputBillNo(String inputBillNo) {
		set("inputBillNo", inputBillNo);
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
	 * 获取是否已预占
	 *
	 * @return 是否已预占
	 */
	public Short getOccupy() {
		return getShort("occupy");
	}

	/**
	 * 设置是否已预占
	 *
	 * @param occupy 是否已预占
	 */
	public void setOccupy(Short occupy) {
		set("occupy", occupy);
	}

	/**
	 * 获取兑付前支票状态
	 *
	 * @return 兑付前支票状态
	 */
	public String getBeforeCashStatus() {
		return get("beforeCashStatus");
	}

	/**
	 * 设置兑付前支票状态
	 *
	 * @return 兑付前支票状态
	 */
	public void setBeforeCashStatus(String beforeCashStatus) {
		set("beforeCashStatus", beforeCashStatus);
	}

	/**
	 * 获取兑付方式
	 *
	 * @return 兑付方式
	 */
	public Short getCashType() {
		return getShort("cashType");
	}

	/**
	 * 设置兑付方式
	 *
	 * @param cashType 兑付方式
	 */
	public void setCashType(Short cashType) {
		set("cashType", cashType);
	}

	/**
	 * 获取兑付人
	 *
	 * @return 兑付人
	 */
	public String getCashPerson() {
		return get("cashPerson");
	}

	/**
	 * 设置兑付人
	 *
	 * @return 兑付人
	 */
	public void setCashPerson(String cashPerson) {
		set("cashPerson", cashPerson);
	}

	/**
	 * 获取领用日期
	 *
	 * @return 领用日期
	 */
	public java.util.Date getReceivedate() {
		return get("receivedate");
	}

	/**
	 * 设置领用日期
	 *
	 * @param receivedate 领用日期
	 */
	public void setReceivedate(java.util.Date receivedate) {
		set("receivedate", receivedate);
	}

	/**
	 * 获取领用人
	 *
	 * @return 领用人.ID
	 */
	public String getRecipient() {
		return get("recipient");
	}

	/**
	 * 设置领用人
	 *
	 * @param recipient 领用人.ID
	 */
	public void setRecipient(String recipient) {
		set("recipient", recipient);
	}

	/**
	 * 获取领用人
	 *
	 * @return 领用人
	 */
	public String getRecipientname() {
		return get("recipientname");
	}

	/**
	 * 设置领用人
	 *
	 * @param recipientname 领用人
	 */
	public void setRecipientname(String recipientname) {
		set("recipientname", recipientname);
	}

}
