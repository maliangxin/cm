package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 外币兑换枚举
 *
 * @author u
 * @version 1.0
 */
public enum ExchangeType {
	Buy(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026220") /* "买入外汇" */, (short)0),
	Sell(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026246") /* "卖出外汇" */, (short)1),
	Exchange(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153332") /* "外币兑换" */, (short)2);

	private String name;
	private short value;

	private ExchangeType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, ExchangeType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, ExchangeType>();
		ExchangeType[] items = ExchangeType.values();
		for (ExchangeType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static ExchangeType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
