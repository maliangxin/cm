package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 核销状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum WriteOffStatus {
	Complete(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026075") /* "已核销" */, (short)1),
	Incomplete(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026321") /* "未核销" */, (short)2),
	Partial(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026222") /* "部分核销" */, (short)3);

	private String name;
	private short value;

	private WriteOffStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, WriteOffStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, WriteOffStatus>();
		WriteOffStatus[] items = WriteOffStatus.values();
		for (WriteOffStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static WriteOffStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
