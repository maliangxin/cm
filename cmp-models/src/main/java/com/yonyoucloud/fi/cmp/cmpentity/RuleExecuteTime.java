package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 执行时机枚举
 *
 * @author u
 * @version 1.0
 */
public enum RuleExecuteTime {
	BeforeTripleCheck(MessageUtils.getMessage("P_YS_CTM_CM-BE_1671059513115410438") /* "三方对账前" */, (short)1),
	AfterTripleCheck(MessageUtils.getMessage("P_YS_CTM_CM-BE_1671059513115410441") /* "三方对账后" */, (short)2);

	private String name;
	private short value;

	private RuleExecuteTime(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, RuleExecuteTime> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, RuleExecuteTime>();
		RuleExecuteTime[] items = RuleExecuteTime.values();
		for (RuleExecuteTime item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static RuleExecuteTime find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
