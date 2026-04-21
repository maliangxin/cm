package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 账单认领类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum BillClaimType {
	Merge(MessageUtils.getMessage("P_YS_CTM_CM-UI_1429730806815260693") /* "合并认领" */, (short)1),
	Whole(MessageUtils.getMessage("P_YS_CTM_CM-UI_1429730806815260714") /* "整单认领" */, (short)2),
	Part(MessageUtils.getMessage("P_YS_CTM_CM-UI_1429730806815260858") /* "部分认领" */, (short)3),
	Batch(MessageUtils.getMessage("P_YS_CTM_CM-UI_2091878853995659272") /* "批量认领" */, (short)4);

	private String name;
	private short value;

	private BillClaimType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BillClaimType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BillClaimType>();
		BillClaimType[] items = BillClaimType.values();
		for (BillClaimType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BillClaimType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
