package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 业务类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum BusiTypeEnum {

	PAY_WAGES("工资", (short)1),
	PAY_EXPENSE("报销", (short)2);

	private String name;
	private short value;

	private BusiTypeEnum(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BusiTypeEnum> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BusiTypeEnum>();
		BusiTypeEnum[] items = BusiTypeEnum.values();
		for (BusiTypeEnum item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BusiTypeEnum find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
