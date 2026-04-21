package com.yonyoucloud.fi.cmp.flowhandlesetting;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 子表_关联单据范围实体
 * table cmp_fhs_association_bill_range
 * @author guoxh
 * @version 1.0
 */
public class FhsAssociationRange extends BizObject implements IYTenant, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.flowhandlesetting.FhsAssociationRange";

    /**
     * 获取可关联单据
     *
     * @return 可关联单据.ID
     */
	public Long getBill() {
		return get("bill");
	}

    /**
     * 设置可关联单据
     *
     * @param bill 可关联单据.ID
     */
	public void setBill(Long bill) {
		set("bill", bill);
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
     * 获取备注
     *
     * @return 备注
     */
	public String getRemark() {
		return get("remark");
	}

    /**
     * 设置备注
     *
     * @param remark 备注
     */
	public void setRemark(String remark) {
		set("remark", remark);
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
