package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 审批状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum SynergyStatus {
	Confirm("P_YS_FI_RA_0000071687" /* "已确认" */, (short)1),
	UNConfirm("P_YS_FED_IMP_IOT0000005422" /* "未确认" */, (short)0),
	Other("P_YS_PF_PROCENTER_0000023062" /* "其他" */,(short) 2);

	private String name;
	private short value;

	private SynergyStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return MessageUtils.getMessage(name);
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, SynergyStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, SynergyStatus>();
		SynergyStatus[] items = SynergyStatus.values();
		for (SynergyStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static SynergyStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
