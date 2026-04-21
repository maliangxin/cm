package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 处置类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum HandleType {
	Loss (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B80026","挂失") /* "挂失" */, (short)1),
	Delete (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B80027","作废") /* "作废" */, (short)2),
	Cancel(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B80028","退回") /* "退回" */, (short)3);

	private String name;
	private short value;

	private HandleType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, HandleType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, HandleType>();
		HandleType[] items = HandleType.values();
		for (HandleType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static HandleType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
