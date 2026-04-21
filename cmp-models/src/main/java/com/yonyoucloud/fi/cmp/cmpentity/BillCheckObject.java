package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 结算检查对象枚举
 *
 * @author u
 * @version 1.0
 */
public enum BillCheckObject {
	paybillSave(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C04180150","资金付款单：保存") /* "资金付款单：保存" */, (short)1),
	paybillSubmit(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C04180151","资金付款单：提交") /* "资金付款单：提交" */, (short)2),
	transferSave(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418014D","转账单：保存") /* "转账单：保存" */, (short)3),
	transferSubmit(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418014E","转账单：提交") /* "转账单：提交" */, (short)4),
	foreignPaymentSave( "外汇付款：保存" , (short)5),
	foreignPaymentSubmit( "外汇付款：提交" , (short)6);
	private String name;
	private short value;

	private BillCheckObject(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, BillCheckObject> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, BillCheckObject>();
		BillCheckObject[] items = BillCheckObject.values();
		for (BillCheckObject item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static BillCheckObject find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
