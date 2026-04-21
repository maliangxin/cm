package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 支付方式枚举
 *
 * @author u
 * @version 1.0
 */
public enum Payway {
	onlinepay(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012286") /* "线上支付" */, (short)0),
	offlinepay (com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012014") /* "线下支付" */, (short)1);

	private String name;
	private short value;

	private Payway(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, Payway> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, Payway>();
		Payway[] items = Payway.values();
		for (Payway item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static Payway find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
