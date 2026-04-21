package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;

import java.util.HashMap;

/**
 * 余额调节表原有审批状态字段枚举
 *
 * @author u
 * @version 1.0
 */
public enum BalanceAuditStatus {
	Complete(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801BD","已审批") /* "已审批" */, (short) 1),
	Incomplete(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801BE","未审批") /* "未审批" */, (short) 2),
	SUBMITED("审批中", (short)3);

	private String name;
	private short value;

	private BalanceAuditStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return InternationalUtils.getMessageWithDefault(name,name) /* name */;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BalanceAuditStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BalanceAuditStatus>();
		BalanceAuditStatus[] items = BalanceAuditStatus.values();
		for (BalanceAuditStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BalanceAuditStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}

	public static String getName(short value) {
		if (map == null) {
			initMap();
		}

		if (1==value) {
			return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801BD", "已审批");
		} else if (2==value) {
			return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801BE", "未审批");
		} else if (3==value) {
			return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00003", "已驳回");
		} else if (4==value) {
			return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00004", "已通过");
		} else if (5==value) {
			return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00005", "已终止");
		} else {
			return map.get(value) == null ? null : ((BalanceAuditStatus)map.get(value)).getName();
		}
	}

}
