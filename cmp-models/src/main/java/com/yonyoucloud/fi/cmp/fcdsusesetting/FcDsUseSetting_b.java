package com.yonyoucloud.fi.cmp.fcdsusesetting;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IEnable;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 流水认领数据源使用设置子表实体
 * table cmp_fc_datasource_use_setting_b
 * @author guoxh
 * @version 1.0
 */
public class FcDsUseSetting_b extends BizObject implements ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.fcdsusesetting.FcDsUseSetting_b";

    /**
     * 获取业务对象
     *
     * @return 业务对象.ID
     */
	public String getBizObject() {
		return get("bizObject");
	}

    /**
     * 设置业务对象
     *
     * @param bizObject 业务对象.ID
     */
	public void setBizObject(String bizObject) {
		set("bizObject", bizObject);
	}

	/**
	 * 获取业务对象编码
	 *
	 * @return 业务对象编码
	 */
	public String getBizObjectCode() {
		return get("bizObjectCode");
	}

	/**
	 * 设置业务对象编码
	 *
	 * @param bizObjectCode 业务对象编码
	 */
	public void setBizObjectCode(String bizObjectCode) {
		set("bizObjectCode", bizObjectCode);
	}

	/**
	 * 获取业务对象名称
	 *
	 * @return 业务对象名称
	 */
	public String getBizObjectName() {
		return get("bizObjectName");
	}

	/**
	 * 设置业务对象名称
	 *
	 * @param bizObjectName 业务对象名称
	 */
	public void setBizObjectName(String bizObjectName) {
		set("bizObjectName", bizObjectName);
	}

    /**
     * 获取数据源
     *
     * @return 数据源.ID
     */
	public String getCdp() {
		return get("cdp");
	}

    /**
     * 设置数据源
     *
     * @param cdp 数据源.ID
     */
	public void setCdp(String cdp) {
		set("cdp", cdp);
	}

    /**
     * 获取适用收付方向
     *
     * @return 适用收付方向
     */
	public String getDcFlag() {
		return get("dcFlag");
	}

    /**
     * 设置适用收付方向
     *
     * @param dcFlag 适用收付方向
     */
	public void setDcFlag(String dcFlag) {
		set("dcFlag", dcFlag);
	}

    /**
     * 获取停用时间
     *
     * @return 停用时间
     */
	public java.util.Date getDisablets() {
		return get("disablets");
	}

    /**
     * 设置停用时间
     *
     * @param disablets 停用时间
     */
	public void setDisablets(java.util.Date disablets) {
		set("disablets", disablets);
	}

    /**
     * 获取所属领域
     *
     * @return 所属领域
     */
	public String getDomain() {
		return get("domain");
	}

    /**
     * 设置所属领域
     *
     * @param domain 所属领域
     */
	public void setDomain(String domain) {
		set("domain", domain);
	}

    /**
     * 获取启用
     *
     * @return 启用
     */
	public String getEnable() {
		return get("enable");
	}

    /**
     * 设置启用
     *
     * @param enable 启用
     */
	public void setEnable(String enable) {
		set("enable", enable);
	}

    /**
     * 获取启用时间
     *
     * @return 启用时间
     */
	public java.util.Date getEnablets() {
		return get("enablets");
	}

    /**
     * 设置启用时间
     *
     * @param enablets 启用时间
     */
	public void setEnablets(java.util.Date enablets) {
		set("enablets", enablets);
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
     * 获取使用对象名称
     *
     * @return 使用对象名称
     */
	public String getName() {
		return get("name");
	}

    /**
     * 设置使用对象名称
     *
     * @param name 使用对象名称
     */
	public void setName(String name) {
		set("object", name);
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
     * 获取交易类型
     *
     * @return 交易类型.ID
     */
	public String getTradeType() {
		return get("tradeType");
	}

    /**
     * 设置交易类型
     *
     * @param tradeType 交易类型.ID
     */
	public void setTradeType(String tradeType) {
		set("tradeType", tradeType);
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
