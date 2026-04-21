package com.yonyoucloud.fi.cmp.cashinventory;

import org.imeta.orm.base.BizObject;

/**
 * 现金盘点子表实体
 *
 * @author u
 * @version 1.0
 */
public class CashInventory_b extends BizObject {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.cashinventory.CashInventory_b";

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
	 * 获取货币面额
	 *
	 * @return 货币面额
	 */
	public String getDenomination() {
		return get("denomination");
	}

	/**
	 * 设置货币面额
	 *
	 * @param denomination 货币面额
	 */
	public void setDenomination(String denomination) {
		set("denomination", denomination);
	}

    /**
     * 获取数量
     *
     * @return 数量
     */
	public Integer getQuantity() {
		return get("quantity");
	}

    /**
     * 设置数量
     *
     * @param quantity 数量
     */
	public void setQuantity(Integer quantity) {
		set("quantity", quantity);
	}

    /**
     * 获取金额
     *
     * @return 金额
     */
	public java.math.BigDecimal getAmountmoney() {
		return get("amountmoney");
	}

    /**
     * 设置金额
     *
     * @param amountmoney 金额
     */
	public void setAmountmoney(java.math.BigDecimal amountmoney) {
		set("amountmoney", amountmoney);
	}

    /**
     * 获取换算基本单位值
     *
     * @return 换算基本单位值
     */
	public java.math.BigDecimal getConvertvalue() {
		return get("convertvalue");
	}

    /**
     * 设置换算基本单位值
     *
     * @param convertvalue 换算基本单位值
     */
	public void setConvertvalue(java.math.BigDecimal convertvalue) {
		set("convertvalue", convertvalue);
	}

    /**
     * 获取友互通Id
     *
     * @return 友互通Id
     */
	public String getYtenantId() {
		return get("ytenantId");
	}

    /**
     * 设置友互通Id
     *
     * @param ytenantId 友互通Id
     */
	public void setYtenantId(String ytenantId) {
		set("ytenantId", ytenantId);
	}

    /**
     * 获取序号
     *
     * @return 序号
     */
	public Integer getRowno() {
		return get("rowno");
	}

    /**
     * 设置序号
     *
     * @param rowno 序号
     */
	public void setRowno(Integer rowno) {
		set("rowno", rowno);
	}

	/**
	 * 获取货币面额组件子表id
	 *
	 * @return 货币面额组件子表id
	 */
	public Long getDenominationSettingbId() {
		return get("denominationSettingbId");
	}

	/**
	 * 设置货币面额组件子表id
	 *
	 * @param denominationSettingbId 货币面额组件子表id
	 */
	public void setDenominationSettingbId(Long denominationSettingbId) {
		set("denominationSettingbId", denominationSettingbId);
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
