package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 银行对账数据源
 *
 * @author u
 * @version 1.0
 */
public enum ReconciliationDataSource {
	Voucher(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418010E","凭证") /* "凭证" */, (short)1),
	BankJournal(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418010F","银行日记账") /* "银行日记账" */, (short)2);

	private String name;
	private short value;

	private ReconciliationDataSource(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, ReconciliationDataSource> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, ReconciliationDataSource>();
		ReconciliationDataSource[] items = ReconciliationDataSource.values();
		for (ReconciliationDataSource item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static ReconciliationDataSource find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
