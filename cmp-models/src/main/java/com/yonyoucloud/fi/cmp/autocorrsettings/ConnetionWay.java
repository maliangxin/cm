package com.yonyoucloud.fi.cmp.autocorrsettings;

import java.util.HashMap;
/**
 * 关联对比方式枚举
 *
 * @author u
 * @version 1.0
 */
public enum ConnetionWay {
	like("模糊匹配", (short)0),
	common("相同", (short)1);

	private String name;
	private short value;

	private ConnetionWay(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, ConnetionWay> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, ConnetionWay>();
		ConnetionWay[] items = ConnetionWay.values();
		for (ConnetionWay item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static ConnetionWay find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
