package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 账单认领状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum BillClaimStatus {
	ToBeClaim(MessageUtils.getMessage("P_YS_HR_HRYGF_0000055739") /* "待认领" */, (short)0),
	Claimed(MessageUtils.getMessage("P_YS_CTM_CM-UI_1431408360912060452") /* "认领完成" */, (short)1);

	private String name;
	private short value;

	private BillClaimStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BillClaimStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BillClaimStatus>();
		BillClaimStatus[] items = BillClaimStatus.values();
		for (BillClaimStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BillClaimStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
