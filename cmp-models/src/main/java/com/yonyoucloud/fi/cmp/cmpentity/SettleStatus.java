package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 结算状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum SettleStatus {
	noSettlement(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418013A","待结算") /* "待结算" */, (short) 1),
	SettleProssing(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418013B","结算中") /* "结算中" */, (short) 3),
	alreadySettled(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C04180134","结算成功") /* "结算成功" */, (short) 2),
	SettleFailed(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000FF", "结算止付") /* "结算止付" */, (short)6),
	SettledRep(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C04180193","已结算补单") /* "已结算补单" */, (short) 8);

	private String name;
	private short value;

	private SettleStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
	}

	public short getValue() {
		return value;
	}

	// 在声明时直接初始化
	private static final HashMap<Short, SettleStatus> map = new HashMap<>();

	static {
		SettleStatus[] items = SettleStatus.values();
		for (SettleStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static SettleStatus find(Number value) {
		if (value == null) {
			return null;
		}
		return map.get(value.shortValue());
	}
}
