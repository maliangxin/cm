package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 计息方式枚举
 *
 * @author u
 * @version 1.0
 */
public enum AgreeinterestmethodEnum {
	DAY_LAST("日末余额逐日计算", (short)1),
	DAY_AVERAGE("日均余额整期计算", (short)2);

	private String name;
	private short value;

	private AgreeinterestmethodEnum(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return MessageUtils.getMessage(name);
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, AgreeinterestmethodEnum> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, AgreeinterestmethodEnum>();
		AgreeinterestmethodEnum[] items = AgreeinterestmethodEnum.values();
		for (AgreeinterestmethodEnum item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static AgreeinterestmethodEnum find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
