package com.yonyoucloud.fi.cmp.bankreconciliationsetting;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 对账方案设置子表实体
 *
 * @author u
 * @version 1.0
 */
public class BankReconciliationSetting_b extends BizObject implements IAuditInfo, ITenant, AccentityRawInterface {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankreconciliationsetting.BankReconciliationSetting_b";
	// 资金组织有特殊处理
    /**
     * 获取对账方案设置id
     *
     * @return 对账方案设置id.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置对账方案设置id
     *
     * @param mainid 对账方案设置id.ID
     */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}

    /**
     * 获取科目
     *
     * @return 科目.ID
     */
	public String getSubject() {
		return get("subject");
	}

    /**
     * 设置科目
     *
     * @param subject 科目.ID
     */
	public void setSubject(String subject) {
		set("subject", subject);
	}

    /**
     * 获取辅助类型
     *
     * @return 辅助类型
     */
	public String getDoctype() {
		return get("doctype");
	}

    /**
     * 设置辅助类型
     *
     * @param doctype 辅助类型
     */
	public void setDoctype(String doctype) {
		set("doctype", doctype);
	}
	/**
	 * 获取辅助核算
	 *
	 * @return 辅助核算.ID
	 */
	public String getAssistaccounting() {
		return get("assistaccounting");
	}

	/**
	 * 设置辅助核算
	 *
	 * @param assistaccounting 辅助核算.ID
	 */
	public void setAssistaccounting(String assistaccounting) {
		set("assistaccounting", assistaccounting);
	}

	/**
	 * 获取辅助核算类型
	 *
	 * @return 辅助核算类型.ID
	 */
	public String  getAssistaccountingtype(){ return get("assistaccountingtype");}
	/**
	 * 设置辅助核算类型
	 *
	 * @param assistaccountingtype 辅助核算类型.ID
	 */
	public void    setAssistaccountingtype(String assistaccountingtype){set("assistaccountingtype", assistaccountingtype);}

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
     * 获取银行账户
     *
     * @return 银行账户.ID
     */
	public String getBankaccount() {
		return get("bankaccount");
	}

    /**
     * 设置银行账户
     *
     * @param bankaccount 银行账户.ID
     */
	public void setBankaccount(String bankaccount) {
		set("bankaccount", bankaccount);
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
	 * 获取账簿
	 */
	public String getAccbook_b() {
		return get("accbook_b");
	}

	/**
	 * 设置账簿
	 */
	public void setAccbook_b(String accbook_b) {
		set("accbook_b", accbook_b);
	}

	/**
	 * 获取启用状态
	 */
	public Short getEnableStatus_b() {
		return getShort("enableStatus_b");
	}

	/**
	 * 设置启用状态
	 */
	public void setEnableStatus_b(Short enableStatus_b) {
		set("enableStatus_b", enableStatus_b);
	}

	/**
	 * 获取核算会计 主体
	 *
	 * @return 核算会计 主体.ID
	 */
	@Override
	public String getAccentityRaw() {
		return get("accentityRaw");
	}

	/**
	 * 设置核算会计 主体
	 *
	 * @param accentityRaw 核算会计 主体.ID
	 */
	@Override
	public void setAccentityRaw(String accentityRaw) {
		set("accentityRaw", accentityRaw);
	}

	/**
	 * 获取资金组织，实际上没有Accentity字段，兼容接口而设置的，取的是使用组织
	 *
	 * @return 资金组织.ID
	 */
	@Override
	public String getAccentity() {
		return getUseorg();
	}

	/**
	 * 设置资金组织，实际上没有Accentity字段，兼容接口而设置的，取的是使用组织
	 *
	 * @param accentity 资金组织.ID
	 */
	@Override
	public void setAccentity(String accentity) {
		setUseorg(accentity);
	}

	/**
	 * 获取使用组织，实际上是资金组织
	 */
	public String getUseorg() {
		return get("useorg");
	}

	/**
	 * 获取使用组织，实际上是资金组织
	 */
	public void setUseorg(String useorg) {
		set("useorg", useorg);
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

}
