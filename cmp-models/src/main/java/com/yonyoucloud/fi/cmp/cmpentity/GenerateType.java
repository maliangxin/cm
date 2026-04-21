package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 生成方式枚举
 *
 * @author u
 * @version 1.0
 */
public enum GenerateType {
	ManualInput (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B80029","手工录入") /* "手工录入" */, (short)1),
	FundSettlementGeneration(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B8002A","资金结算生成") /* "资金结算生成" */, (short)2),
	TransferAccountGeneration(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B8002B","转账工作台生成") /* "转账工作台生成" */, (short)3);

	private String name;
	private short value;

	private GenerateType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, GenerateType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, GenerateType>();
		GenerateType[] items = GenerateType.values();
		for (GenerateType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static GenerateType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
