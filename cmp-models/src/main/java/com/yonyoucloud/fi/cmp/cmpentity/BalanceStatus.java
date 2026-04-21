package com.yonyoucloud.fi.cmp.cmpentity;


import java.util.HashMap;

/**
 * 余额调节表枚举
 *
 * @author u
 * @version 1.0
 */
public enum BalanceStatus {
	not_generate("未生成", (short)0),
	balance_closed("余额已平", (short)1),
	balance_unclosed("余额未平", (short)2),
	;

	private String name;
	private short value;

	private BalanceStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BalanceStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BalanceStatus>();
		BalanceStatus[] items = BalanceStatus.values();
		for (BalanceStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BalanceStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
