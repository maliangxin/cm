package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 合格质押品占用类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum CollateralOccupation {
	DepositCollateral("缴纳保证金", (short)0),
	DeductionCollateral("扣减金融市场授信额度", (short)1);

	private String name;
	private short value;

	private CollateralOccupation(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, CollateralOccupation> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, CollateralOccupation>();
		CollateralOccupation[] items = CollateralOccupation.values();
		for (CollateralOccupation item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static CollateralOccupation find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
