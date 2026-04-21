package com.yonyoucloud.fi.cmp.flowhandlesetting;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 子表_关联业务凭据范围实体
 * table cmp_fhs_bill_range
 * @author guoxh
 * @version 1.0
 */
public class FhsBillRange extends BizObject implements IYTenant, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.flowhandlesetting.FhsBillRange";

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
     * 获取关联凭据类型
     *
     * @return 关联凭据类型
     */
	public String getCredentialType() {
		return get("credentialType");
	}

    /**
     * 设置关联凭据类型
     *
     * @param credentialType 关联凭据类型
     */
	public void setCredentialType(String credentialType) {
		set("credentialType", credentialType);
	}

    /**
     * 获取业务凭据关联后流程
     *
     * @return 业务凭据关联后流程
     */
	public Short getFinishAfterFlow() {
	    return getShort("finishAfterFlow");
	}

    /**
     * 设置业务凭据关联后流程
     *
     * @param finishAfterFlow 业务凭据关联后流程
     */
	public void setFinishAfterFlow(Short finishAfterFlow) {
		set("finishAfterFlow", finishAfterFlow);
	}

    /**
     * 获取是否业务凭据关联即完结
     *
     * @return 是否业务凭据关联即完结
     */
	public Short getIsFinishOver() {
	    return getShort("isFinishOver");
	}

    /**
     * 设置是否业务凭据关联即完结
     *
     * @param isFinishOver 是否业务凭据关联即完结
     */
	public void setIsFinishOver(Short isFinishOver) {
		set("isFinishOver", isFinishOver);
	}

    /**
     * 获取主表id
     *
     * @return 主表id.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置主表id
     *
     * @param mainid 主表id.ID
     */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
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
     * 获取条件(规则引擎信息)
     *
     * @return 条件(规则引擎信息)
     */
	public String getRuleEngineConfig() {
		return get("ruleEngineConfig");
	}

    /**
     * 设置条件(规则引擎信息)
     *
     * @param ruleEngineConfig 条件(规则引擎信息)
     */
	public void setRuleEngineConfig(String ruleEngineConfig) {
		set("ruleEngineConfig", ruleEngineConfig);
	}

	/**
	 * 获取条件
	 *
	 * @return 条件
	 */
	public String getRuleEngineDisplayName() {
		return get("ruleEngineDisplayName");
	}

	/**
	 * 设置条件
	 *
	 * @param ruleEngineDisplayName 条件
	 */
	public void setRuleEngineDisplayName(String ruleEngineDisplayName) {
		set("ruleEngineDisplayName", ruleEngineDisplayName);
	}

	/**
	 * 获取条件表达式
	 *
	 * @return 条件表达式
	 */
	public String getRuleEngineExpression() {
		return get("ruleEngineExpression");
	}

	/**
	 * 设置条件表达式
	 *
	 * @param ruleEngineExpression 条件表达式
	 */
	public void setRuleEngineExpression(String ruleEngineExpression) {
		set("ruleEngineExpression", ruleEngineExpression);
	}

    /**
     * 获取来源系统
     *
     * @return 来源系统
     */
	public String getSourceSystem() {
		return get("sourceSystem");
	}

    /**
     * 设置来源系统
     *
     * @param sourceSystem 来源系统
     */
	public void setSourceSystem(String sourceSystem) {
		set("sourceSystem", sourceSystem);
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
	public String getYTenant() {
		return get("ytenant");
	}

    /**
     * 设置租户id
     *
     * @param ytenant 租户id.ID
     */
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}

}
