package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 日记账类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum JournalType {
	bankjournal(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_PF_GZTSYS_0000013359") /* "银行日记账" */, (short)1),
	cashjournal(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_PF_GZTSYS_0000013437") /* "现金日记账" */, (short)2);

	private String name;
	private short value;

	private JournalType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, JournalType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, JournalType>();
		JournalType[] items = JournalType.values();
		for (JournalType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static JournalType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
