package com.yonyoucloud.fi.cmp.virtualFlowRuleConfig;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 资金调度虚拟流水规则实体
 *
 * @author u
 * @version 1.0
 */
public class VirtualFlowRuleConfig extends BizObject implements ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.virtualFlowRuleConfig.VirtualFlowRuleConfig";

	/**
	 * 获取银行类别
	 *
	 * @return 银行类别.ID
	 */
	public String getBanktype() {
		return get("banktype");
	}

	/**
	 * 设置银行类别
	 *
	 * @param banktype 银行类别.ID
	 */
	public void setBanktype(String banktype) {
		set("banktype", banktype);
	}

	/**
	 * 获取银行类别编码
	 *
	 * @return 银行类别编码
	 */
	public String getBanktypecode() {
		return get("banktypecode");
	}

	/**
	 * 设置银行类别编码
	 *
	 * @param banktypecode 银行类别编码
	 */
	public void setBanktypecode(String banktypecode) {
		set("banktypecode", banktypecode);
	}

	/**
	 * 获取单据id
	 *
	 * @return 单据id
	 */
	public String getBillid() {
		return get("billid");
	}

	/**
	 * 设置单据id
	 *
	 * @param billid 单据id
	 */
	public void setBillid(String billid) {
		set("billid", billid);
	}

	/**
	 * 获取银行类别名称
	 *
	 * @return 银行类别名称
	 */
	public String getBanktypename() {
		return get("banktypename");
	}

	/**
	 * 设置银行类别名称
	 *
	 * @param banktypename 银行类别名称
	 */
	public void setBanktypename(String banktypename) {
		set("banktypename", banktypename);
	}

	/**
	 * 获取是否启用
	 *
	 * @return 是否启用
	 */
	public Short getIsEnable() {
		return getShort("isEnable");
	}

	/**
	 * 设置是否启用
	 *
	 * @param isEnable 是否启用
	 */
	public void setIsEnable(Short isEnable) {
		set("isEnable", isEnable);
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
	 * 获取租户id
	 *
	 * @return 租户id
	 */
	public String getYtenant() {
		return get("ytenant");
	}

	/**
	 * 设置租户id
	 *
	 * @param ytenant 租户id
	 */
	public void setYtenant(String ytenant) {
		set("ytenant", ytenant);
	}

	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	@Override
	public void setYTenant(String ytenantId) {
		set("ytenantId", ytenantId);
	}
}
