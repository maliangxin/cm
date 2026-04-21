package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 支票处置单据类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum CheckManageEnum {
	transfer(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B80025","支票处置") /* "支票处置" */, (short)1);

	private String name;
	private short value;

	private CheckManageEnum(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, CheckManageEnum> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, CheckManageEnum>();
		CheckManageEnum[] items = CheckManageEnum.values();
		for (CheckManageEnum item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static CheckManageEnum find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
