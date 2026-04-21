package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 关联类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum Relationtype {
	AutoAssociated(MessageUtils.getMessage("P_YS_SD_SDOC-UI_0000170490") /* "自动关联" */, (short)0), //收付单据自动关联
	ManualAssociated(MessageUtils.getMessage("P_YS_TBS_TBS-UI_0001110849") /* "手动关联" */, (short)1),//收付单据手工关联
	MakeBillAssociated(MessageUtils.getMessage("P_YS_CTM_CM-UI_1461838650902315008") /* "生单关联" */, (short)2),//收付单据自动生单
	ThreePartyAssociated(MessageUtils.getMessage("P_YS_CTM_CM-BE_1681322861441581059") /* "三方对账关联" */, (short)3),
	HeterogeneousAssociated(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1862FE3C05D0001E","异构系统关联") /* "异构系统关联" */, (short) 4),
	Manualgenerationreceiptpaymentdocuments( "收付单据手工生单" , (short)5);


	private String name;
	private short value;

	private Relationtype(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, Relationtype> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, Relationtype>();
		Relationtype[] items = Relationtype.values();
		for (Relationtype item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static Relationtype find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
