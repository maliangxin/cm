package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 三方转账类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum VirtualBank {
	VirtualToBank("虚拟账户转银行账户", (short)0),
	BankToVirtual("银行账户转虚拟账户", (short)1);

	private String name;
	private short value;

	private VirtualBank(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, VirtualBank> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, VirtualBank>();
		VirtualBank[] items = VirtualBank.values();
		for (VirtualBank item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static VirtualBank find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
