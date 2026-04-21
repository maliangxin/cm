package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 外币兑换买卖状态
 *
 * @author u
 * @version 1.0
 */
public enum Bsflag {
	Buy(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000045628") /* "买" */, (short)0),
	Sell(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000045634") /* "卖" */, (short)1),
	Exchange(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000045617") /* "兑换" */, (short)2);

	private String name;
	private short value;

	private Bsflag(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, Bsflag> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, Bsflag>();
		Bsflag[] items = Bsflag.values();
		for (Bsflag item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static Bsflag find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
