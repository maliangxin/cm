package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 对方类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum OppositeType {
	Customer("客户", (short)1),
	Supplier("供应商", (short)2),
	Employee("员工", (short)3),
	InnerOrg("内部单位", (short)4),
	Other("其他",(short) 5),
	CapBizObj("资金业务对象", (short) 6);

	private String name;
	private short value;

	private OppositeType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, OppositeType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, OppositeType>();
		OppositeType[] items = OppositeType.values();
		for (OppositeType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static OppositeType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
