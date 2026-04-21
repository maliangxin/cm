package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 网银支付状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum PaymentStatus {
	NoPay(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026295") /* "未支付" */, (short)1),
	PayDone(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026156") /* "支付成功" */, (short)2),
	PayFail(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026089") /* "支付失败" */, (short)3),
	UnkownPay(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026294") /* "支付状态不明" */, (short)4);

	private String name;
	private short value;

	private PaymentStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	// 在声明时直接初始化
	private static final HashMap<Short, PaymentStatus> map = new HashMap<>();

	static {
		PaymentStatus[] items = PaymentStatus.values();
		for (PaymentStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static PaymentStatus find(Number value) {
		if (value == null) {
			return null;
		}
		return map.get(value.shortValue());
	}
}
