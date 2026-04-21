package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 业务关联状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum AssociationStatus {
	NoAssociated(MessageUtils.getMessage("P_YS_SD_SDOC-UI_0001178268") /* "未关联" */, (short)0),
	Associated(MessageUtils.getMessage("P_YS_SD_SDOC-UI_0001178248") /* "已关联" */, (short)1);

	private String name;
	private short value;

	private AssociationStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, AssociationStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, AssociationStatus>();
		AssociationStatus[] items = AssociationStatus.values();
		for (AssociationStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static AssociationStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
