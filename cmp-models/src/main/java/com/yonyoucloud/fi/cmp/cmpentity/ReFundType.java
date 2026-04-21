package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 关联类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum ReFundType {
	SUSPECTEDREFUND("疑似退票", (short)1),
	REFUND("退票", (short)2);

	private String name;
	private short value;

	private ReFundType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, ReFundType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, ReFundType>();
		ReFundType[] items = ReFundType.values();
		for (ReFundType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static ReFundType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
