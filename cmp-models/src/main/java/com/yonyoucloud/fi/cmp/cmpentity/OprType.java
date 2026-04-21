package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 分配信息 操作类型 枚举
 *
 * @date 2022-10-12
 * @author xujhn
 * @version 1.0
 */
public enum OprType {
	//cmp_bankreconciliation_oprType	text	1	zh-cn	自动分派
	//cmp_bankreconciliation_oprType	text	2	zh-cn	手工分派
	//cmp_bankreconciliation_oprType	text	3	zh-cn	自动发布分派
	//cmp_bankreconciliation_oprType	text	4	zh-cn	手工发布分派
	AutoFinance(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933155") /* "自动分配财务人员" */, "1"),
	ManualFinance(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933149") /* "手工分配财务人员" */, "2"),
	AutoBusiness(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933215") /* "自动分配业务人员" */,"3"),
	ManualBusiness(MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933206") /* "手工分配业务人员" */,"4"),
	Publish(  "发布" , "5"),
	Claim( "认领" , "6"),
	Return( "退回" ,"7");
	private String name;
	private String value;



	private OprType(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	private static HashMap<String, OprType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<String, OprType>();
		OprType[] items = OprType.values();
		for (OprType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static OprType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value);
	}
}
