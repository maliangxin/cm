package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;


/**
 * 支付状态枚举
 *
 * @author u
 * @version 1.0
 */
public enum PayStatus {
	NoPay(MessageUtils.getMessage("P_YS_FI_CM_0000026128") /* "待支付" */, (short)0),
	PreSuccess(MessageUtils.getMessage("P_YS_FI_CM_0000026137") /* "预下单成功" */, (short)1),
	PreFail(MessageUtils.getMessage("P_YS_FI_CM_0000026210") /* "预下单失败" */, (short)2),
	Success(MessageUtils.getMessage("P_YS_FI_CM_0000026156") /* "支付成功" */, (short)3),
	Fail(MessageUtils.getMessage("P_YS_FI_CM_0000026089") /* "支付失败" */, (short)4),
	Paying(MessageUtils.getMessage("P_YS_FI_CM_0000026057") /* "支付中" */, (short)5),
	PayUnknown(MessageUtils.getMessage("P_YS_FI_CM_0000026314") /* "支付不明" */, (short)6),
	OfflinePay(MessageUtils.getMessage("P_YS_FI_AR_0000059024") /* "线下支付成功" */, (short)7),
	PartSuccess(MessageUtils.getMessage("P_YS_FI_YYFI-UI_0001112212") /* "部分成功" */, (short)8),
	SupplPaid(MessageUtils.getMessage("P_YS_CTM_CTM-CMP-MD_1671826929269342391") /* "已支付补单" */, (short)9);

	private String name;
	private short value;

	private PayStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, PayStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, PayStatus>();
		PayStatus[] items = PayStatus.values();
		for (PayStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static PayStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
