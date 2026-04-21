package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 业务关联状态枚举
 *
 * @author yp
 * @version 1.0
 */
public enum BillProcessFlag {

	/**无需回单中台处理*/
	NoNeedDeal(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933116") /* "无需回单中台处理" */, (short)0),
	/**需回单中台处理*/
	NeedDeal(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933092") /* "需回单中台处理" */, (short)1),
	/**需人工确认*/
	ArtificialDeal(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933134") /* "需人工确认" */, (short)2);

	private String name;
	private short value;

	private BillProcessFlag(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BillProcessFlag> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BillProcessFlag>();
		BillProcessFlag[] items = BillProcessFlag.values();
		for (BillProcessFlag item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BillProcessFlag find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
