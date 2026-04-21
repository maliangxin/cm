package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 变更支付状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum UpdatePayStatus {
	Success(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026156") /* "支付成功" */, (short)3),
	Fail(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026089") /* "支付失败" */, (short)4);

	private String name;
	private short value;

	private UpdatePayStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, UpdatePayStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, UpdatePayStatus>();
		UpdatePayStatus[] items = UpdatePayStatus.values();
		for (UpdatePayStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static UpdatePayStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
