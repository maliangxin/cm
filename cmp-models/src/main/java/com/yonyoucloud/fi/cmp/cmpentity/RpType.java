package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 收付款类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum RpType {
	ReceiveBill(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012711") /* "收款单" */, (short)1),
	PayBill(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012535") /* "付款单" */, (short)2);

	private String name;
	private short value;

	private RpType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, RpType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, RpType>();
		RpType[] items = RpType.values();
		for (RpType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static RpType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
