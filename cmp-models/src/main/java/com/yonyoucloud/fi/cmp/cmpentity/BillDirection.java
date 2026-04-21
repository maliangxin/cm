package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 单据方向枚举
 *
 * @author u
 * @version 1.0
 */
public enum BillDirection {
	red(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026168") /* "红字" */, (short)1),
	blue (com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026014") /* "蓝字" */, (short)2);

	private String name;
	private short value;

	private BillDirection(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BillDirection> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BillDirection>();
		BillDirection[] items = BillDirection.values();
		for (BillDirection item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BillDirection find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
