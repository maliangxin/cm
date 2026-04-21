package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 外币兑换枚举
 *
 * @author u
 * @version 1.0
 */
public enum OtherCustType {
	CUSTOMER("客户", (short)0),
	SUPPLIER("供应商", (short)1),
	PERSONNEL("人员", (short)2),
	INTERNALUNIT("内部客户", (short)3);

	private String name;
	private short value;

	private OtherCustType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, OtherCustType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, OtherCustType>();
		OtherCustType[] items = OtherCustType.values();
		for (OtherCustType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static OtherCustType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
