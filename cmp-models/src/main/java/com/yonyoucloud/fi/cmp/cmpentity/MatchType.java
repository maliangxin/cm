package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 匹配类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum MatchType {
	NotMatch(MessageUtils.getMessage("P_YS_SD_SDMB_0000093197") /* "不匹配" */, (short)0),
	Same(MessageUtils.getMessage("P_YS_PLM_PSDM-FE_0001106862") /* "相同" */, (short)1),
	PartMatch(MessageUtils.getMessage("P_YS_SD_UDHLIB_0000134144") /* "模糊匹配" */, (short)2);

	private String name;
	private short value;

	private MatchType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, MatchType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, MatchType>();
		MatchType[] items = MatchType.values();
		for (MatchType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static MatchType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
