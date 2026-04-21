package com.yonyoucloud.fi.cmp.cmpentity;



import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 审批状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum ExpenseAuditStatus {
	unsubmit("UID:P_BAM-BE_17FC181C05C8004C", (short)0),
	approval("UID:P_DRFT-BE_18509A100590011E", (short)1),
	passed("UID:P_TLM-FE_18FB9C6404F80067", (short)2),
	retured("UID:P_CM-UI_18108E6604B820DE", (short)3),
	STOPPED("UID:P_CM-UI_18108E6604B81FC9", (short)4);

	private String name;
	private short value;

	private ExpenseAuditStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
	}

	public short getValue() {
		return value;
	}

	public static List canDelete() {
		return Arrays.asList(unsubmit.getValue(), retured.getValue(), STOPPED.getValue());
	}

	public static List canSubmit() {
		return Arrays.asList(unsubmit.getValue(), retured.getValue());
	}

	public static List canWithdraw() {
		return Arrays.asList(passed.getValue());
	}

	//联审状态待提交可审批
	public static List canApprove() {
		return Arrays.asList(approval.getValue(), unsubmit.getValue());
	}

	private static HashMap<Short, ExpenseAuditStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, ExpenseAuditStatus>();
		ExpenseAuditStatus[] items = ExpenseAuditStatus.values();
		for (ExpenseAuditStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static ExpenseAuditStatus find(Number value) {
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
		return map.get(value) == null ?  null : map.get(value).getName();
	}

}
