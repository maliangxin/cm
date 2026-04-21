package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 余额对比结果枚举
 *
 * @author u
 * @version 1.0
 */
public enum BalanceContrast {
	Unequal("不相等", (short)0),
	Equal("相等", (short)1);

	private String name;
	private short value;

	private BalanceContrast(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BalanceContrast> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BalanceContrast>();
		BalanceContrast[] items = BalanceContrast.values();
		for (BalanceContrast item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BalanceContrast find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
