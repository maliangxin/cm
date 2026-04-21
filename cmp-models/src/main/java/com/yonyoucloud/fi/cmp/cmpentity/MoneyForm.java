package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 资金形态枚举
 *
 * @author u
 * @version 1.0
 */
public enum MoneyForm {
	cashstock(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026082") /* "库存现金" */, (short)0),
	bankaccount (com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026127") /* "银行存款" */, (short)1);

	private String name;
	private short value;

	private MoneyForm(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, MoneyForm> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, MoneyForm>();
		MoneyForm[] items = MoneyForm.values();
		for (MoneyForm item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static MoneyForm find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
