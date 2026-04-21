package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 是否包含未结算单据
 *
 * @author xudya
 * @version 1.0
 */
public enum Containunsettle {
	contain(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FED_FW_0000021785") /* "包含" */, (short)1),
	uncontain(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FED_FW_0000021777") /* "不包含" */, (short)2);

	private String name;
	private short value;

	private Containunsettle(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, Containunsettle> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, Containunsettle>();
		Containunsettle[] items = Containunsettle.values();
		for (Containunsettle item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static Containunsettle find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
