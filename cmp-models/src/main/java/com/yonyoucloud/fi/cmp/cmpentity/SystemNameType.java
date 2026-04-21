package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 关联类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum SystemNameType {
	MoneyScheduling(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001228852") /* "资金调度" */, (short)1),
	CashManagement(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012884") /* "现金管理" */, (short)2),
	SettlementCenter(MessageUtils.getMessage("P_YS_PF_METADATA_0000087238") /* "结算中心" */, (short)3)
	;

	private String name;
	private short value;

	private SystemNameType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, SystemNameType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, SystemNameType>();
		SystemNameType[] items = SystemNameType.values();
		for (SystemNameType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static SystemNameType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
