package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 业务关联次数枚举
 *
 * @author xujhn
 * @date 2022-10-10
 * @version 1.0
 */
public enum AssociationCount {
	First(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933113") /* "第一次" */, (short)1),
	Second(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933218") /* "第二次" */, (short)2);

	private String name;
	private short value;

	private AssociationCount(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, AssociationCount> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, AssociationCount>();
		AssociationCount[] items = AssociationCount.values();
		for (AssociationCount item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static AssociationCount find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
