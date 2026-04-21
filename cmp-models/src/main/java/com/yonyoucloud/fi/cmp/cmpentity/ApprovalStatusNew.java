package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 审批状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum ApprovalStatusNew {
	Saved(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B80021","已保存") /* "已保存" */, (short)1),
	Approving(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B80022","审批中") /* "审批中" */, (short)2),
	Approved(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B80023","审批通过") /* "审批通过" */, (short)3),
	Rejected(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B80024","已驳回") /* "已驳回" */, (short)4),
	Terminated(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18903CFA05B80020","已终止") /* "已终止" */, (short)5);

	private String name;
	private short value;

	private ApprovalStatusNew(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, ApprovalStatusNew> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, ApprovalStatusNew>();
		ApprovalStatusNew[] items = ApprovalStatusNew.values();
		for (ApprovalStatusNew item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static ApprovalStatusNew find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
