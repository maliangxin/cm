package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 退票状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum RefundStatus {
	MaybeRefund(MessageUtils.getMessage("P_YS_FI_CM_1492827896541085701") /* "疑似退票" */, (short)1),
	Refunded(MessageUtils.getMessage("P_YS_CTM_CM-BE_1544812222195695619") /* "退票" */, (short)2);

	private String name;
	private short value;

	private RefundStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, RefundStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, RefundStatus>();
		RefundStatus[] items = RefundStatus.values();
		for (RefundStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static RefundStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
