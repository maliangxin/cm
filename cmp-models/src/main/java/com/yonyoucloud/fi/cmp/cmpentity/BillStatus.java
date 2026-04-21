package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 单据状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum BillStatus {
	Created(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_SCM_PU_0000028170") /* "开立" */, (short)1),
	Audit(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FED_UCFBASEDOC_0000024727") /* "审核" */, (short)2),
	Settle(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_PF_PROCENTER_0000027268") /* "结算" */, (short)3);

	private String name;
	private short value;

	private BillStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BillStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BillStatus>();
		BillStatus[] items = BillStatus.values();
		for (BillStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BillStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return (BillStatus)map.get(value.shortValue());
	}
}
