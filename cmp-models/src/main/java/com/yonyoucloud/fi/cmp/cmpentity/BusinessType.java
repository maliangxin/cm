package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 业务类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum BusinessType {
	OnlineBusiness("线上自助", (short)0),
	BankMargin("银行审核", (short)1);

	private String name;
	private short value;

	private BusinessType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BusinessType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BusinessType>();
		BusinessType[] items = BusinessType.values();
		for (BusinessType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BusinessType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
