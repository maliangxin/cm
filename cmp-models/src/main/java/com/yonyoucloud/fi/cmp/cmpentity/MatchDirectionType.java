package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 匹配方向类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum MatchDirectionType {
	None("无", (short)0),
	Same("相同", (short)1),
	Opposite("相反", (short)2);

	private String name;
	private short value;

	private MatchDirectionType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, MatchDirectionType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, MatchDirectionType>();
		MatchDirectionType[] items = MatchDirectionType.values();
		for (MatchDirectionType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static MatchDirectionType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
