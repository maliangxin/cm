package com.yonyoucloud.fi.cmp.cmpentity;


import java.util.HashMap;

/**
 * 对账状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum ReconciliationStatus {
	unchecked("未对符", (short)0),
	checked("已对符", (short)1),
	Unreconciled("未对账", (short)2);

	private String name;
	private short value;

	private ReconciliationStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, ReconciliationStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, ReconciliationStatus>();
		ReconciliationStatus[] items = ReconciliationStatus.values();
		for (ReconciliationStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static ReconciliationStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
