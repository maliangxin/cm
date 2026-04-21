package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;

import java.util.HashMap;

/**
 * 资金收付款单结算状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum FundSettleStatus {
    WaitSettle(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418013A","待结算") /* "待结算" */, (short) 1),
    SettleProssing(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418013B","结算中") /* "结算中" */, (short) 2),
    SettleSuccess(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C04180134","结算成功") /* "结算成功" */, (short) 3),
    SettleFailed(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C04180135","结算止付") /* "结算止付" */, (short) 4),
    PartSuccess(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C04180136","部分成功") /* "部分成功" */, (short) 5),
    Refund(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C04180137","退票") /* "退票" */, (short) 6),
    SettlementSupplement(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C04180138","已结算补单") /* "已结算补单" */, (short) 7);

	private String name;
	private short value;

	private FundSettleStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return InternationalUtils.getMessageWithDefault(name,name) /* name */;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, FundSettleStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, FundSettleStatus>();
		FundSettleStatus[] items = FundSettleStatus.values();
		for (FundSettleStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static FundSettleStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
