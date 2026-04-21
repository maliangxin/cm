package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 交易状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum TradeStatus {
	succeed(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026331") /* "交易成功" */, (short)0),
	fail(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026180") /* "交易失败" */, (short)1),
	unknown(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026037") /* "未明" */, (short)2);

	private String name;
	private short value;

	private TradeStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, TradeStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, TradeStatus>();
		TradeStatus[] items = TradeStatus.values();
		for (TradeStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static TradeStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
