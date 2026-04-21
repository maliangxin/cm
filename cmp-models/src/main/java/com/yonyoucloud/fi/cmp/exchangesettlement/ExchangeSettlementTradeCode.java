package com.yonyoucloud.fi.cmp.exchangesettlement;

import org.imeta.orm.base.BizObject;

/**
 * 结售汇交易编码实体
 *
 * @author u
 * @version 1.0
 */
public class ExchangeSettlementTradeCode extends BizObject {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.exchangesettlementtradecode.ExchangeSettlementTradeCode";

	/**
	 * 获取交易编码
	 *
	 * @return 交易编码
	 */
	public String getTrade_code() {
		return get("trade_code");
	}

	/**
	 * 设置交易编码
	 *
	 * @param trade_code 交易编码
	 */
	public void setTrade_code(String trade_code) {
		set("trade_code", trade_code);
	}

	/**
	 * 获取项目名称
	 *
	 * @return 项目名称
	 */
	public String getProjectname() {
		return get("projectname");
	}

	/**
	 * 设置项目名称
	 *
	 * @param projectname 项目名称
	 */
	public void setProjectname(String projectname) {
		set("projectname", projectname);
	}

	/**
	 * 获取结售汇类型
	 *
	 * @return 结售汇类型
	 */
	public String getExchangesettlement_type() {
		return get("exchangesettlement_type");
	}

	/**
	 * 设置结售汇类型
	 *
	 * @param exchangesettlement_type 结售汇类型
	 */
	public void setExchangesettlement_type(String exchangesettlement_type) {
		set("exchangesettlement_type", exchangesettlement_type);
	}

	/**
	 * 获取统计代码
	 *
	 * @return 统计代码
	 */
	public String getStatisticscode() {
		return get("statisticscode");
	}

	/**
	 * 设置统计代码
	 *
	 * @param statisticscode 统计代码
	 */
	public void setStatisticscode(String statisticscode) {
		set("statisticscode", statisticscode);
	}

	/**
	 * 获取统计项目名称
	 *
	 * @return 统计项目名称
	 */
	public String getStatistics_projectname() {
		return get("statistics_projectname");
	}

	/**
	 * 设置统计项目名称
	 *
	 * @param statistics_projectname 统计项目名称
	 */
	public void setStatistics_projectname(String statistics_projectname) {
		set("statistics_projectname", statistics_projectname);
	}

	/**
	 * 获取项目类型
	 *
	 * @return 项目类型
	 */
	public String getProjecttype() {
		return get("projecttype");
	}

	/**
	 * 设置项目类型
	 *
	 * @param projecttype 项目类型
	 */
	public void setProjecttype(String projecttype) {
		set("projecttype", projecttype);
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
	 * 获取项目类型标识
	 *
	 * @return 项目类型标识
	 */
	public Integer getProjecttypeNumber() {
		return get("projecttypeNumber");
	}

	/**
	 * 设置项目类型标识
	 *
	 * @param projecttypeNumber 项目类型标识
	 */
	public void setProjecttypeNumber(Integer projecttypeNumber) {
		set("projecttypeNumber", projecttypeNumber);
	}

	/**
	 * 获取结售汇类型标识
	 *
	 * @return 结售汇类型标识
	 */
	public Integer getExchangesettlement_typeFlag() {
		return get("exchangesettlement_typeFlag");
	}

	/**
	 * 设置结售汇类型标识
	 *
	 * @param exchangesettlement_typeFlag 结售汇类型标识
	 */
	public void setExchangesettlement_typeFlag(Integer exchangesettlement_typeFlag) {
		set("exchangesettlement_typeFlag", exchangesettlement_typeFlag);
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
	 * 获取项目归类
	 *
	 * @return 项目归类
	 */
	public String getProjectclassification() {
		return get("projectclassification");
	}

	/**
	 * 设置项目归类
	 *
	 * @param projectclassification 项目归类
	 */
	public void setProjectclassification(String projectclassification) {
		set("projectclassification", projectclassification);
	}

	/**
	 * 获取项目归类编码
	 *
	 * @return 项目归类编码
	 */
	public String getProjectclassificationcode() {
		return get("projectclassificationcode");
	}

	/**
	 * 设置项目归类编码
	 *
	 * @param projectclassificationcode 项目归类编码
	 */
	public void setProjectclassificationcode(String projectclassificationcode) {
		set("projectclassificationcode", projectclassificationcode);
	}

}
