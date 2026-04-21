package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 业务类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum CurrencyRateTypeCode {
	BaseCode("基准汇率", "01"),
	CustomCode("用户自定义汇率", "02");

	private String name;
	private String value;

	private CurrencyRateTypeCode(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	private static HashMap<String, CurrencyRateTypeCode> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<String, CurrencyRateTypeCode>();
		CurrencyRateTypeCode[] items = CurrencyRateTypeCode.values();
		for (CurrencyRateTypeCode item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static CurrencyRateTypeCode find(String value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value);
	}
}
