package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 发票类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum InvoiceType {
	VAT_Ordinary_Elec("P_YS_PF_GZTSYS_0000013376" /* "增值税电子普通发票" */, (short)1),
	VAT_Ordinary_Paper("P_YS_PF_GZTSYS_0000013288" /* "增值税普通发票" */, (short)2),
	VAT_Special("P_YS_PF_GZTSYS_0000013286" /* "增值税专用发票" */, (short)3),
	Bill("P_YS_FI_AR_0000059064" /* "账单" */, (short)4),
	Receipt("P_YS_PF_PROCENTER_0000023176" /* "收据" */, (short)5),
	Vehicle("P_YS_PF_GZTTMP_0000103151" /* "机动车销售统一发票" */, (short)6),
	VAT_ELEC_GENERAL_INVOICE("P_YS_SCM_UPU-UI_0000174758" /* "通行费增值税电子普通发票" */, (short)7),
	VAT_Special_Elec("P_YS_PF_GZTSYS_0001122263" /* "增值税电子专用发票" */, (short)8),

	;

	private String name;
	private short value;

	private InvoiceType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, InvoiceType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, InvoiceType>();
		InvoiceType[] items = InvoiceType.values();
		for (InvoiceType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static InvoiceType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
