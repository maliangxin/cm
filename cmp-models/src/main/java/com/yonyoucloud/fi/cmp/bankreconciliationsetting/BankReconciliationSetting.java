package com.yonyoucloud.fi.cmp.bankreconciliationsetting;

import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 对账方案设置实体
 *
 * @author u
 * @version 1.0
 */
public class BankReconciliationSetting extends BizObject implements IAuditInfo, ITenant {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankreconciliationsetting.BankReconciliationSetting";


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
     * 获取银行对账方案编码
     *
     * @return 银行对账方案编码
     */
	public String getBankreconciliationschemecode() {
		return get("bankreconciliationschemecode");
	}

    /**
     * 设置银行对账方案编码
     *
     * @param bankreconciliationschemecode 银行对账方案编码
     */
	public void setBankreconciliationschemecode(String bankreconciliationschemecode) {
		set("bankreconciliationschemecode", bankreconciliationschemecode);
	}

    /**
     * 获取银行对账方案名称
     *
     * @return 银行对账方案名称
     */
	public String getBankreconciliationschemename() {
		return get("bankreconciliationschemename");
	}

    /**
     * 设置银行对账方案名称
     *
     * @param bankreconciliationschemename 银行对账方案名称
     */
	public void setBankreconciliationschemename(String bankreconciliationschemename) {
		set("bankreconciliationschemename", bankreconciliationschemename);
	}

    /**
     * 获取对账数据源
     *
     * @return 对账数据源
     */
	public ReconciliationDataSource getReconciliationdatasource() {
		Number v = get("reconciliationdatasource");
		return ReconciliationDataSource.find(v);
	}

    /**
     * 设置对账数据源
     *
     * @param reconciliationdatasource 对账数据源
     */
	public void setReconciliationdatasource(ReconciliationDataSource reconciliationdatasource) {
		if (reconciliationdatasource != null) {
			set("reconciliationdatasource", reconciliationdatasource.getValue());
		} else {
			set("reconciliationdatasource", null);
		}
	}

    /**
     * 获取对账财务账簿
     *
     * @return 对账财务账簿.ID
     */
	public String getAccbook() {
		return get("accbook");
	}

    /**
     * 设置对账财务账簿
     *
     * @param accbook 对账财务账簿.ID
     */
	public void setAccbook(String accbook) {
		set("accbook", accbook);
	}

    /**
     * 获取是否包含未记账
     *
     * @return 是否包含未记账
     */
	public Boolean getContainunjournal() {
	    return getBoolean("containunjournal");
	}

    /**
     * 设置是否包含未记账
     *
     * @param containunjournal 是否包含未记账
     */
	public void setContainunjournal(Boolean containunjournal) {
		set("containunjournal", containunjournal);
	}

    /**
     * 获取是否包含未结算
     *
     * @return 是否包含未结算
     */
	public Boolean getContainunsettle() {
	    return getBoolean("containunsettle");
	}

    /**
     * 设置是否包含未结算
     *
     * @param containunsettle 是否包含未结算
     */
	public void setContainunsettle(Boolean containunsettle) {
		set("containunsettle", containunsettle);
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
	 * 获取启用状态
	 *
	 * @return 启用状态
	 */
	public EnableStatus getEnableStatus(){
		Number v = get("enableStatus");
		return EnableStatus.find(v);
	}

	/**
	 * 设置启用状态
	 *
	 * @param enableStatus 是否启用状态
	 */
	public void setEnableStatus(EnableStatus enableStatus){
		if (enableStatus != null) {
			set("enableStatus", enableStatus.getValue());
		} else {
			set("enableStatus", null);
		}
	}

	/**
	 * 获取停用日期
	 *
	 * @return 停用日期
	 */
	public java.util.Date getStopDate() {
		return get("stopDate");
	}

	/**
	 * 设置停用日期
	 *
	 * @param stopDate 停用日期
	 */
	public void setStopDate(java.util.Date stopDate) {
		set("stopDate", stopDate);
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
     * 获取对账方案设置子表集合
     *
     * @return 对账方案设置子表集合
     */
	public java.util.List<BankReconciliationSetting_b> BankReconciliationSetting_b() {
		return getBizObjects("BankReconciliationSetting_b", BankReconciliationSetting_b.class);
	}

    /**
     * 设置对账方案设置子表集合
     *
     * @param BankReconciliationSetting_b 对账方案设置子表集合
     */
	public void setBankReconciliationSetting_b(java.util.List<BankReconciliationSetting_b> BankReconciliationSetting_b) {
		setBizObjects("BankReconciliationSetting_b", BankReconciliationSetting_b);
	}


}
