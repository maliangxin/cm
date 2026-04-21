package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 询价方式枚举
 *
 * @author u
 * @version 1.0
 */
public enum InquiryType {
	OnlineInquiry("线上询价", (short)0),
	OfflineInquiry("线下询价", (short)1);

	private String name;
	private short value;

	private InquiryType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, InquiryType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, InquiryType>();
		InquiryType[] items = InquiryType.values();
		for (InquiryType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static InquiryType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
