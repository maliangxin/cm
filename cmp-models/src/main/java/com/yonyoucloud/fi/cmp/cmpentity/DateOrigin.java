package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 数据来源枚举
 *
 * @author xudya
 * @version 1.0
 */
public enum DateOrigin {
	DownFromYQL(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026258") /* "银企联下载" */, (short)1),
	Created(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026201") /* "外部导入/录入" */, (short)2),
	AddManually(MessageUtils.getMessage("P_YS_SD_SDOC_0000166235") /* "手工新增" */, (short)3);

	private String name;
	private short value;

	private DateOrigin(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, DateOrigin> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, DateOrigin>();
		DateOrigin[] items = DateOrigin.values();
		for (DateOrigin item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static DateOrigin find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
