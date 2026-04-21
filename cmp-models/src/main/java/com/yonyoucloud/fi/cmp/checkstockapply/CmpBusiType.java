package com.yonyoucloud.fi.cmp.checkstockapply;

import java.util.HashMap;

/**
 * 交易类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum CmpBusiType {
	Black("空白支票", (short)0),
	Rec("收入支票", (short)1);

	private String name;
	private short value;

	private CmpBusiType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, CmpBusiType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, CmpBusiType>();
		CmpBusiType[] items = CmpBusiType.values();
		for (CmpBusiType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static CmpBusiType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
