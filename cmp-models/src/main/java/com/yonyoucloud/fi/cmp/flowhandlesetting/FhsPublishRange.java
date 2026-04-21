package com.yonyoucloud.fi.cmp.flowhandlesetting;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 子表_手工发布范围实体
 * table cmp_fhs_publish_range
 * @author guoxh
 * @version 1.0
 */
public class FhsPublishRange extends BizObject implements ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.flowhandlesetting.FhsPublishRange";

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
     * 获取发布对象
     *
     * @return 发布对象
     */
	public String getPublishObject() {
		return get("publishObject");
	}

    /**
     * 设置发布对象
     *
     * @param publishObject 发布对象
     */
	public void setPublishObject(String publishObject) {
		set("publishObject", publishObject);
	}

    /**
     * 获取发布对象类型
     *
     * @return 发布对象类型
     */
	public String getPublishObjectType() {
		return get("publishObjectType");
	}

    /**
     * 设置发布对象类型
     *
     * @param publishObjectType 发布对象类型
     */
	public void setPublishObjectType(String publishObjectType) {
		set("publishObjectType", publishObjectType);
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
