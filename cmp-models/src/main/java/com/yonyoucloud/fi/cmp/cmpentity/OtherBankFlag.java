package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;


/**
 * 收方跨行标识枚举
 *
 * @author u
 * @version 1.0
 */
public enum OtherBankFlag {
	SameBank(MessageUtils.getMessage("P_YS_FI_CM_0001123719") /* "本行" */, (short)0),
	OtherBank(MessageUtils.getMessage("P_YS_FI_CM_0001123720") /* "跨行" */, (short)1);

	private String name;
	private short value;

	private OtherBankFlag(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, OtherBankFlag> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, OtherBankFlag>();
		OtherBankFlag[] items = OtherBankFlag.values();
		for (OtherBankFlag item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static OtherBankFlag find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
