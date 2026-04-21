package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 散户收款类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum RetailerAccountType {
	forPrivate(MessageUtils.getMessage("P_YS_FI_AR_0000058918") /* "对私" */, (short)1),
	toPublic(MessageUtils.getMessage("P_YS_FI_AR_0000058899") /* "对公" */, (short)2),
	bankAccount(MessageUtils.getMessage("P_YS_FI_AR_0000059054") /* "银行内部户" */, (short)3);

	private String name;
	private short value;

	private RetailerAccountType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, RetailerAccountType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, RetailerAccountType>();
		RetailerAccountType[] items = RetailerAccountType.values();
		for (RetailerAccountType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static RetailerAccountType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
