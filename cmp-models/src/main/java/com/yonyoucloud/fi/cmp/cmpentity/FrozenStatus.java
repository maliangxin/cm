package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 冻结状态枚举
 *
 * @date 2022-10-12
 * @author xujhn
 * @version 1.0
 */
public enum FrozenStatus {
	Normal(MessageUtils.getMessage("P_YS_OA_XTLCZX_0000030776") /* "正常" */, (short)0),
	Unfreezing(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933080") /* "在解冻" */, (short)1),
	Frozen(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933104") /* "已冻结" */,(short)2);

	private String name;
	private short value;

	private FrozenStatus(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, FrozenStatus> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, FrozenStatus>();
		FrozenStatus[] items = FrozenStatus.values();
		for (FrozenStatus item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static FrozenStatus find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
