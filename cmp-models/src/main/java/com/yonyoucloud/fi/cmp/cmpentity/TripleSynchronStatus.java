package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 三方平台同步状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum TripleSynchronStatus {
	Undone(MessageUtils.getMessage("P_YS_FI_YYFI-UI_0000161540") /* "未同步" */, (short)0),
	AlreadyAuto(MessageUtils.getMessage("P_YS_CTM_CM-BE_1671060183107239945") /* "已自动同步" */, (short)1),
	AlreadyManual(MessageUtils.getMessage("P_YS_CTM_CM-BE_1671060183107239942") /* "已手工同步" */, (short)2),
	AutoSendBack(MessageUtils.getMessage("P_YS_CTM_CM-BE_1671060183107239948") /* "自动同步退回" */, (short)3),
	ManualSendBack(MessageUtils.getMessage("P_YS_CTM_CM-BE_1671060183107239951") /* "手工同步退回" */, (short)4);

	private String name;
	private short value;

	private TripleSynchronStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, TripleSynchronStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, TripleSynchronStatus>();
		TripleSynchronStatus[] items = TripleSynchronStatus.values();
		for (TripleSynchronStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static TripleSynchronStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
