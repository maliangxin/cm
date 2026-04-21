package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 单据状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum MoneyTypeNum {
	Init(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_PF_GZTSYS_0000015926") /* "期初" */, (short)0),
	Detail(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FED_FW_0000021867") /* "明细" */, (short)1),
	Sum(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FED_FW_0000021247") /* "小计" */, (short)2),
	MaxSum(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FED_FW_0000021124") /* "合计" */, (short)3);

	private String name;
	private short value;

	private MoneyTypeNum(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, MoneyTypeNum> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, MoneyTypeNum>();
		MoneyTypeNum[] items = MoneyTypeNum.values();
		for (MoneyTypeNum item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static MoneyTypeNum find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return (MoneyTypeNum)map.get(value.shortValue());
	}
}
