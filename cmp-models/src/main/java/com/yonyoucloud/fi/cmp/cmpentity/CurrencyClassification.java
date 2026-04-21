package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 币种分类枚举
 *
 * @author u
 * @version 1.0
 */
public enum CurrencyClassification {
	Home_Currency("本币", (short) 1),
	Ori_Currency("原币", (short) 2),
	OriAndHome("原币本币", (short) 3),
	Org_Currency("组织币", (short) 4),
	Group_Currency("集团币", (short) 5),
	Global_Currency("全局币", (short) 6);


	private String name;
	private short value;

	private CurrencyClassification(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, CurrencyClassification> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, CurrencyClassification>();
		CurrencyClassification[] items = CurrencyClassification.values();
		for (CurrencyClassification item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static CurrencyClassification find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
