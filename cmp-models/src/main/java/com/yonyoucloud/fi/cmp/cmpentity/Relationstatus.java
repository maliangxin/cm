package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 关联状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum Relationstatus {
	Confirm("待确认", (short)1),
	Confirmed("已确认", (short)2);

	private String name;
	private short value;

	private Relationstatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, Relationstatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, Relationstatus>();
		Relationstatus[] items = Relationstatus.values();
		for (Relationstatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static Relationstatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
