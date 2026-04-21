package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 结算检查校验枚举
 *
 * @author u
 * @version 1.0
 */
public enum BillCheck {
	duplication(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000AE", "疑重校验") /* "疑重校验" */, (short)1),
	balance(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D4A220205E00004", "直联账户余额校验") /* "直联账户余额校验" */, (short)2),
	blockTrade(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000A7", "大额交易") /* "大额交易" */, (short)3),
	blackList(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000A8", "黑名单") /* "黑名单" */, (short)4),
	sensitiveWord(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000AA", "敏感词") /* "敏感词" */, (short)5),
	paymentLimit(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000AB", "账户支付限额") /* "账户支付限额" */, (short)6),
	availableBalance(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000AC", "关联内部户可用余额") /* "关联内部户可用余额" */, (short)7),
	highFrequency(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000AD", "高频交易") /* "高频交易" */, (short)8),
	grayList(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1862E3BA05D00006", "灰名单") /* "灰名单" */,(short)9),
	noDirAccBalance(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D4A224005E00006", "非直联账户余额校验") /* "非直联账户余额校验" */,(short)10),
	;

	private String name;
	private short value;

	private BillCheck(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name, name) /* name */;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BillCheck> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BillCheck>();
		BillCheck[] items = BillCheck.values();
		for (BillCheck item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BillCheck find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
