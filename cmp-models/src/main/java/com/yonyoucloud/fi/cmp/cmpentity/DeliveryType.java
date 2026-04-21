package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 交割方式枚举
 *
 * @author u
 * @version 1.0
 */
public enum DeliveryType {
	ManualDelivery("手工交割", (short)0),
	DirectDelivery("直联交割", (short)1);

	private String name;
	private short value;

	private DeliveryType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, DeliveryType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, DeliveryType>();
		DeliveryType[] items = DeliveryType.values();
		for (DeliveryType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static DeliveryType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
