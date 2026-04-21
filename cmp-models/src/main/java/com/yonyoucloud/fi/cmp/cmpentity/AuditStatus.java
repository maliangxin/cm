package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;

import java.util.HashMap;

/**
 * 审批状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum AuditStatus {

	Complete(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801BD","已审批") /* "已审批" */, (short) 1),
	Incomplete(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801BE","未审批") /* "未审批" */, (short) 2),
	Retured(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00003", "已驳回") /* "已驳回" */, (short)3),
	Passed(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00004", "已通过") /* "已通过" */, (short)4),
	Stopped(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00005", "已终止") /* "已终止" */, (short)5);

	private String name;
	private short value;

	private AuditStatus(String name, short value) {
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
	private static final HashMap<Short, AuditStatus> map = new HashMap<>();

	static {
		AuditStatus[] items = AuditStatus.values();
		for (AuditStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static AuditStatus find(Number value) {
		if (value == null) {
			return null;
		}
		return map.get(value.shortValue());
	}

	public static String getName(short value) {

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
			return map.get(value) == null ? null : ((AuditStatus)map.get(value)).getName();
		}
	}

}
