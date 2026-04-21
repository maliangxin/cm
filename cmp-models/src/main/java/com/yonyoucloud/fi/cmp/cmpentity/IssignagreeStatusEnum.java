package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 审批状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum IssignagreeStatusEnum {
	SIGNAGREE("已签约", (short)1),
	UN_SIGNAGREE("未签约", (short)0);

	private String name;
	private short value;

	private IssignagreeStatusEnum(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return MessageUtils.getMessage(name);
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, IssignagreeStatusEnum> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, IssignagreeStatusEnum>();
		IssignagreeStatusEnum[] items = IssignagreeStatusEnum.values();
		for (IssignagreeStatusEnum item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static IssignagreeStatusEnum find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
