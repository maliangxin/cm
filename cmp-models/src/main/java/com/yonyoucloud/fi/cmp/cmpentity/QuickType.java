package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 款项类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum QuickType {
	advancesReceived(MessageUtils.getMessage("P_YS_FI_AR_0000058990") /* "预收款" */, (short)1),
	accountReceivable (MessageUtils.getMessage("P_YS_FI_AR_0000059081") /* "应收款" */, (short)2),

	cost(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026199") /* "费用" */, (short)3),
	discount(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026132") /* "折扣" */, (short)4),

	advancePayment(MessageUtils.getMessage("P_YS_FI_AR_0000058907") /* "预付款" */, (short)5),
	accountPayable(MessageUtils.getMessage("P_YS_FI_AR_0000058964") /* "应付款" */, (short)6),

	serviceCharge(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026165") /* "手续费" */, (short)7),
	interest(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026241") /* "利息" */, (short)8),
	sundry(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026249") /* "杂项" */, (short)9),

	cashDeposit(MessageUtils.getMessage("P_YS_FI_AR_0000154390") /* "保证金" */, (short)10),
	qualityGuarantee(MessageUtils.getMessage("P_YS_FI_AR_0000154337") /* "质保金" */, (short)11),
	cashPledge(MessageUtils.getMessage("P_YS_FI_AR_0000154335") /* "押金" */, (short)12);

	private String name;
	private short value;

	private QuickType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, QuickType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, QuickType>();
		QuickType[] items = QuickType.values();
		for (QuickType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static QuickType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
