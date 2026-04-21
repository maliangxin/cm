package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 协定利率机械天数枚举
 *
 * @author u
 * @version 1.0
 */
public enum AgreeinterestdaysEnum {
	D_30_360("30/360", "360"),
	D_NL_365("NL/365", "365"),
	D_ACT_360("ACT/360", "ACT_360"),
	D_ACT_365("ACT/365", "ACT_365"),
	D_ACT_ACT("ACT/ACT", "ACT_ACT");

	private String name;
	private String value;

	private AgreeinterestdaysEnum(String name, String value) {
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
