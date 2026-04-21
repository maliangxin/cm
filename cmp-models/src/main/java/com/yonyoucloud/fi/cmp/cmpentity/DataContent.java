package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 数据内容枚举
 *
 * @author u
 * @version 1.0
 */
public enum DataContent {
	DailyData(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026259") /* "日常数据" */, (short)1),
	NotHaveBankBegin(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026265") /* "银行期初未达" */, (short)2);

	private String name;
	private short value;

	private DataContent(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, DataContent> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, DataContent>();
		DataContent[] items = DataContent.values();
		for (DataContent item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static DataContent find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
