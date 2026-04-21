package com.yonyoucloud.fi.cmp.autocorrsettings;

import java.util.HashMap;

/**
 * 业务单据类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum BussDocumentType {
	settlebench("资金结算单", (short)2),
	fundcollection("资金收款单", (short)0),
	fundpayment("资金付款单", (short)1),
	transferaccount("转账单", (short)3),
	currencyexchange("外币兑换单", (short)4),
	Payment("付款单", (short)5),
	ReceiveBill("收款单", (short)6),
	;

	private String name;
	private short value;

	private BussDocumentType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BussDocumentType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BussDocumentType>();
		BussDocumentType[] items = BussDocumentType.values();
		for (BussDocumentType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BussDocumentType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
