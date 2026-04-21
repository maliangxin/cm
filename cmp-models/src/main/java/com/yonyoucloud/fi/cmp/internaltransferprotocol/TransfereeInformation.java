package com.yonyoucloud.fi.cmp.internaltransferprotocol;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import org.imeta.orm.base.BizObject;

/**
 * 转出方信息实体
 *
 * @author u
 * @version 1.0
 */
public class TransfereeInformation extends BizObject implements IAuditInfo, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.internaltransferprotocol.TransfereeInformation";
	private static final long serialVersionUID = -5618165744272635353L;

	/**
	 * 获取是否自动
	 *
	 * @return 是否自动
	 */
	public Boolean getIsauto() {
		return getBoolean("isauto");
	}

	/**
	 * 设置是否自动
	 *
	 * @param isauto 是否自动
	 */
	public void setIsauto(Boolean isauto) {
		set("isauto", isauto);
	}


	/**
     * 获取主表id
     *
     * @return 主表id.ID
     */
	public String getMainid() {
		return get("mainid");
	}

    /**
     * 设置主表id
     *
     * @param mainid 主表id.ID
     */
	public void setMainid(String mainid) {
		set("mainid", mainid);
	}

    /**
     * 获取转入资金组织
     *
     * @return 转入资金组织.ID
     */
	public String getIntoAccentity() {
		return get("intoAccentity");
	}

    /**
     * 设置转入资金组织
     *
     * @param intoAccentity 转入资金组织.ID
     */
	public void setIntoAccentity(String intoAccentity) {
		set("intoAccentity", intoAccentity);
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
     * 获取分摊方式
     *
     * @return 分摊方式
     */
	public Short getApportionmentMethod() {
	    return getShort("apportionmentMethod");
	}

    /**
     * 设置分摊方式
     *
     * @param apportionmentMethod 分摊方式
     */
	public void setApportionmentMethod(Short apportionmentMethod) {
		set("apportionmentMethod", apportionmentMethod);
	}

    /**
     * 获取分摊比例（%）
     *
     * @return 分摊比例（%）
     */
	public java.math.BigDecimal getApportionmentRatio() {
		return get("apportionmentRatio");
	}

    /**
     * 设置分摊比例（%）
     *
     * @param apportionmentRatio 分摊比例（%）
     */
	public void setApportionmentRatio(java.math.BigDecimal apportionmentRatio) {
		set("apportionmentRatio", apportionmentRatio);
	}

    /**
     * 获取固定金额
     *
     * @return 固定金额
     */
	public java.math.BigDecimal getFixedAmount() {
		return get("fixedAmount");
	}

    /**
     * 设置固定金额
     *
     * @param fixedAmount 固定金额
     */
	public void setFixedAmount(java.math.BigDecimal fixedAmount) {
		set("fixedAmount", fixedAmount);
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
     * 获取转出方信息特征
     *
     * @return 转出方信息特征.ID
     */
	public String getTransfereeInformationCharacterDef() {
		return get("transfereeInformationCharacterDef");
	}

    /**
     * 设置转出方信息特征
     *
     * @param transfereeInformationCharacterDef 转出方信息特征.ID
     */
	public void setTransfereeInformationCharacterDef(String transfereeInformationCharacterDef) {
		set("transfereeInformationCharacterDef", transfereeInformationCharacterDef);
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
	 * 获取开户类型
	 *
	 * @return 开户类型
	 */
	public Short getAcctOpenType() {
		return getShort("acctOpenType");
	}

	/**
	 * 设置开户类型
	 *
	 * @param acctOpenType 开户类型
	 */
	public void setAcctOpenType(Short acctOpenType) {
		set("acctOpenType", acctOpenType);
	}


}
