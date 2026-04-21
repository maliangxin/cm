package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 是否自动枚举
 *
 * @author u
 * @version 1.0
 */
public enum BaseConfigEnum {
	menual(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FED_EXAMP_0000020344") /* "否" */, (short)0),
	auto(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FED_EXAMP_0000020600") /* "是" */, (short)1);

	private String name;
	private short value;

	private BaseConfigEnum(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BaseConfigEnum> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BaseConfigEnum>();
		BaseConfigEnum[] items = BaseConfigEnum.values();
		for (BaseConfigEnum item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BaseConfigEnum find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
