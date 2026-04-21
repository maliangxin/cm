package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 保证金账户选择标志枚举
 *
 * @author u
 * @version 1.0
 */
public enum MarginAccountFlag {
	ForeignMargin("冻结该笔交易外币账户可用余额作为保证金", (short)0),
	FreezeMargin("冻结该笔交易人民币账户可用余额作为保证金", (short)1);

	private String name;
	private short value;

	private MarginAccountFlag(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, MarginAccountFlag> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, MarginAccountFlag>();
		MarginAccountFlag[] items = MarginAccountFlag.values();
		for (MarginAccountFlag item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static MarginAccountFlag find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
