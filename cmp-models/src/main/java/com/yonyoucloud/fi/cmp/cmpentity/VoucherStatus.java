package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 凭证状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum VoucherStatus {
	Created(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026206") /* "已生成" */, (short)1),
	Empty(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FED_EXAMP_0000020437") /* "未生成" */, (short)2),
	Received(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_AR_0000058968" )/* "已接收未生成" */, (short) 3),
	NONCreate(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_TP_0000673009" )/* "不生成" */, (short) 4),
	POSTING(MessageUtils.getMessage("P_YS_FI_EFA-UI_1441649469919068307") /* "过账中" */, (short) 5),
	POST_SUCCESS(MessageUtils.getMessage("P_YS_FI_SC_0001116422") /* "过账成功" */, (short) 6),
	POST_FAIL(MessageUtils.getMessage("P_YS_FI_EFA-UI_1441649469919068310") /* "过账失败" */, (short) 7),
	NO_POST(MessageUtils.getMessage("P_YS_ZNBZ_BZBX-UI_1461835532756058112") /* "不过账" */, (short) 8),
	TO_BE_POST(MessageUtils.getMessage("P_YS_CTM_CM-BE_1583473576464875526") /* "待过账" */, (short) 9);
	private String name;
	private short value;

	private VoucherStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, VoucherStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, VoucherStatus>();
		VoucherStatus[] items = VoucherStatus.values();
		for (VoucherStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static VoucherStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
