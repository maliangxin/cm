package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 外币兑换买卖状态
 *
 * @author u
 * @version 1.0
 */
public enum PricingParty {
	Buy("UID:P_CM-BE_1811A04805B00039", (short)1),
	Sell("UID:P_CM-BE_1811A04805B0003A" , (short)2);

	private String name;
	private short value;

	private PricingParty(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name, name) /* name */;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, PricingParty> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, PricingParty>();
		PricingParty[] items = PricingParty.values();
		for (PricingParty item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static PricingParty find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
