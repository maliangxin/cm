package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 保证金标识枚举
 *
 * @author u
 * @version 1.0
 */
public enum MarginFlag {
	PayMargin("支付保证金", (short)0),
	RecMargin("收到保证金", (short)1);

	private String name;
	private short value;

	private MarginFlag(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, MarginFlag> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, MarginFlag>();
		MarginFlag[] items = MarginFlag.values();
		for (MarginFlag item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static MarginFlag find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
