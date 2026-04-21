package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 是否存在客商档案
 *
 * @author u
 * @version 1.0
 */
public enum MerchantFlag {
	EXIST(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026146") /* "存在" */, (short)1),
	NOT_EXIST(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026118") /* "不存在" */, (short)2);

	private String name;
	private short value;

	private MerchantFlag(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, MerchantFlag> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, MerchantFlag>();
		MerchantFlag[] items = MerchantFlag.values();
		for (MerchantFlag item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static MerchantFlag find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
