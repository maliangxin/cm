package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 关联类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum IsEnable {
	ENABLE("启用", (short)0),
	DISENABLE("禁用", (short)1);

	private String name;
	private short value;

	private IsEnable(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, IsEnable> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, IsEnable>();
		IsEnable[] items = IsEnable.values();
		for (IsEnable item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static IsEnable find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
