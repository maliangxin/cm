package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 委托类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum DelegationType {
	RealDelegation("实时委托", (short)0),
	DirectDelegation("询价委托", (short)1);

	private String name;
	private short value;

	private DelegationType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, DelegationType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, DelegationType>();
		DelegationType[] items = DelegationType.values();
		for (DelegationType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static DelegationType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
