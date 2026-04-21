package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 利率类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum IRTypeEnum {
    FIEXD("固定", (short) 1),
    FLOAT("浮动", (short) 2);

	private String name;
	private short value;

	private IRTypeEnum(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, IRTypeEnum> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, IRTypeEnum>();
		IRTypeEnum[] items = IRTypeEnum.values();
		for (IRTypeEnum item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static IRTypeEnum find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
