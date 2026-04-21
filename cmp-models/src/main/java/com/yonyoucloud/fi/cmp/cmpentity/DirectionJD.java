package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 借贷方向枚举
 *
 * @author u
 * @version 1.0
 */
public enum DirectionJD {
    Debit(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418016F","借") /* "借" */, (short) 1),
    Credit(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C04180170","贷") /* "贷" */, (short) 2),
	CreditAndDebit("借贷",(short) 3);

	private String name;
	private short value;

	private DirectionJD(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, DirectionJD> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, DirectionJD>();
		DirectionJD[] items = DirectionJD.values();
		for (DirectionJD item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static DirectionJD find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
