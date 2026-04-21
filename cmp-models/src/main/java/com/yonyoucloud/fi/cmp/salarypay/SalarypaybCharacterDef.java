package com.yonyoucloud.fi.cmp.salarypay;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import org.imeta.orm.base.BizObject;

/**
 * 转账单自定义特征实体
 *
 * @author u
 * @version 1.0
 */
public class SalarypaybCharacterDef extends BizObject implements IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.salarypay.SalarypaybCharacterDef";

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

	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}

}
