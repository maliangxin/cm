package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 记账状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum AccountStatus {
	Created(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026267") /* "已记账" */, (short)1),
	Empty(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026290") /* "未记账" */, (short)2);

	private String name;
	private short value;

	private AccountStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, AccountStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, AccountStatus>();
		AccountStatus[] items = AccountStatus.values();
		for (AccountStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static AccountStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
