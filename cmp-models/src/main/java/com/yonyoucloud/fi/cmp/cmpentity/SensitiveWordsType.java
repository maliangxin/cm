package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 关联类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum SensitiveWordsType {
	ALL("包含全部敏感词", (short)1),
	NOCHECK("不校验", (short)3),
	PART("包含任意敏感词", (short)2);

	private String name;
	private short value;

	private SensitiveWordsType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, SensitiveWordsType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, SensitiveWordsType>();
		SensitiveWordsType[] items = SensitiveWordsType.values();
		for (SensitiveWordsType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static SensitiveWordsType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
