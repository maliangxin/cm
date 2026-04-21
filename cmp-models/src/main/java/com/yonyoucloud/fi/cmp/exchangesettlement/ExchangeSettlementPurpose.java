package com.yonyoucloud.fi.cmp.exchangesettlement;

import org.imeta.orm.base.BizObject;

/**
 * 结售汇用途实体
 *
 * @author u
 * @version 1.0
 */
public class ExchangeSettlementPurpose extends BizObject {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.exchangesettlementpurpose.ExchangeSettlementPurpose";

	/**
	 * 获取用途编码
	 *
	 * @return 用途编码
	 */
	public String getPurposecode() {
		return get("purposecode");
	}

	/**
	 * 设置用途编码
	 *
	 * @param purposecode 用途编码
	 */
	public void setPurposecode(String purposecode) {
		set("purposecode", purposecode);
	}

	/**
	 * 获取用途名称
	 *
	 * @return 用途名称
	 */
	public String getPurposename() {
		return get("purposename");
	}

	/**
	 * 设置用途名称
	 *
	 * @param purposename 用途名称
	 */
	public void setPurposename(String purposename) {
		set("purposename", purposename);
	}

	/**
	 * 获取多语类型
	 *
	 * @return 多语类型
	 */
	public Integer getMultilangtype() {
		return get("multilangtype");
	}

	/**
	 * 设置多语类型
	 *
	 * @param multilangtype 多语类型
	 */
	public void setMultilangtype(Integer multilangtype) {
		set("multilangtype", multilangtype);
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

}
