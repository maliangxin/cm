package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 交割状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum DeliveryStatus {
	todoDelivery("待处理", (short)1),
	completeDelivery("手工交割完成", (short)2),
	doingDelivery("处理中", (short)3),
	waitDelivery("待交割", (short)4),
	alreadyDelivery("已交割", (short)5),
	beOverdueDelivery("逾期", (short)6),
	failDelivery("交割失败", (short)7);

	private String name;
	private short value;

	private DeliveryStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, DeliveryStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, DeliveryStatus>();
		DeliveryStatus[] items = DeliveryStatus.values();
		for (DeliveryStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static DeliveryStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
