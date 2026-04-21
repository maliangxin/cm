package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 自动处理状态
 * aa_enum: aa_autodealstatus
 * 生单待确认
 * 关联单据待确认
 * 关联业务凭据待确认
 * 生单已确认
 * 关联单据已确认
 * 关联业务凭据已确认
 * 收付单据自动关联
 * 收付单据自动生单
 */
public enum AutoDealState {
	ToBeMakeBill(MessageUtils.getMessage("UID:P_CM-UI_1C35078E04880006") /* "生单待确认" */, (short)1),
	ToBeLinks(MessageUtils.getMessage("UID:P_CM-UI_1C3507C404480009") /* "关联单据待确认" */, (short)2),
	ToBeVoucher(MessageUtils.getMessage("UID:P_CM-UI_1C3507FA04880008") /* "关联业务凭据待确认" */, (short)3),
	MakeBillConfirmed(MessageUtils.getMessage("UID:P_CM-UI_1C35082E04880007") /* "生单已确认" */, (short)4),
	LinkConfirmed(MessageUtils.getMessage("UID:P_CM-UI_1C35086204880003") /* "关联单据已确认" */, (short)5),
	VoucherConfirmed(MessageUtils.getMessage("UID:P_CM-UI_1C35089204880009") /* "关联业务凭据已确认" */, (short)6),
	AutoLink(MessageUtils.getMessage("UID:P_CM-UI_1C27D1D604380006") /* "收付单据自动关联" */, (short)7),
	AutoMakeBill(MessageUtils.getMessage("UID:P_CM-UI_1C27D0CC04B00003") /* "收付单据自动生单" */, (short)8)
	;

	private String name;
	private short value;

	private AutoDealState(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, AutoDealState> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, AutoDealState>();
		AutoDealState[] items = AutoDealState.values();
		for (AutoDealState item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static AutoDealState find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
