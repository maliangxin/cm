package com.yonyoucloud.fi.cmp.bankaccountsetting;


import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 银企联账号实体
 *
 * @author u
 * @version 1.0
 */
public class BankAccountSetting extends BizObject implements IAuditInfo, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankaccountsetting.BankAccountSetting";

	/**
	 * 获取状态
	 *
	 * @return 状态
	 */
	public String getAccStatus() {
		return get("accStatus");
	}

	/**
	 * 设置状态
	 *
	 * @param accStatus 状态
	 */
	public void setAccStatus(String accStatus) {
		set("accStatus", accStatus);
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
     * 获取企业银行账户
     *
     * @return 企业银行账户.ID
     */
	public String getEnterpriseBankAccount() {
		return get("enterpriseBankAccount");
	}

    /**
     * 设置企业银行账户
     *
     * @param enterpriseBankAccount 企业银行账户.ID
     */
	public void setEnterpriseBankAccount(String enterpriseBankAccount) {
		set("enterpriseBankAccount", enterpriseBankAccount);
	}

    /**
     * 获取开通银企连服务
     *
     * @return 开通银企连服务
     */
	public Boolean getOpenFlag() {
	    return getBoolean("openFlag");
	}

    /**
     * 设置开通银企连服务
     *
     * @param openFlag 开通银企连服务
     */
	public void setOpenFlag(Boolean openFlag) {
		set("openFlag", openFlag);
	}

    /**
     * 获取客户号
     *
     * @return 客户号
     */
	public String getCustomNo() {
		return get("customNo");
	}

    /**
     * 设置客户号
     *
     * @param customNo 客户号
     */
	public void setCustomNo(String customNo) {
		set("customNo", customNo);
	}

    /**
     * 获取开通电票服务
     *
     * @return 开通电票服务
     */
	public Boolean getOpenTicketService() {
	    return getBoolean("openTicketService");
	}

    /**
     * 设置开通电票服务
     *
     * @param openTicketService 开通电票服务
     */
	public void setOpenTicketService(Boolean openTicketService) {
		set("openTicketService", openTicketService);
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
	 * 获取启用日期
	 *
	 * @return 启用日期
	 */
	public java.util.Date getEnableDate() {
		return get("enableDate");
	}

	/**
	 * 设置启用日期
	 *
	 * @param enableDate 启用日期
	 */
	public void setEnableDate(java.util.Date enableDate) {
		set("enableDate", enableDate);
	}


    /**
     * 获取直联授权
     *
     * @return 直联授权
     */
    public String getEmpower() {
        return get("empower");
    }

    /**
     * 设置直联授权
     *
     * @param empower 直联授权
     */
    public void setEmpower(String empower) {
        set("empower", empower);
    }


}
