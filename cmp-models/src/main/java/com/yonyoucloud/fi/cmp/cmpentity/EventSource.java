package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;


/**
 * 事项来源枚举
 *
 * @author u
 * @version 1.0
 */
public enum EventSource {
    Sale(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801BF","销售管理") /* "销售管理" */, (short) 1),
    OrderCenter(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801C0","订单中心") /* "订单中心" */, (short) 2),
    Online(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801C1","在线商城") /* "在线商城" */, (short) 3),
    Retail(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801C2","零售") /* "零售" */, (short) 4),
    Stock(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801C3","库存") /* "库存" */, (short) 5),
    Manual(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801C4","应收应付") /* "应收应付" */, (short) 6),
    //	Manual("应收应付", (short)6),
    Purchase(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801C5","采购管理") /* "采购管理" */, (short) 7),
    Cmpchase(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801C6","现金管理") /* "现金管理" */, (short) 8),
    ManualImport(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801C8","导入") /* "导入" */, (short) 9),
    SystemOut(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801C9","费用管理") /* "费用管理" */, (short) 10),
    GL(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801CA","总账") /* "总账" */, (short) 11),
    INTERNALTRADE(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801CB","内部交易") /* "内部交易" */, (short) 12),
    HRSalaryChase(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801CC","薪资管理") /* "薪资管理" */, (short) 13),
    StwbSettlement(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801CD","资金结算") /* "资金结算" */, (short) 14),

	THIRD_PARTY( "第三方, 协力友商", (short) 20),

    Drftchase(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801CE","商业汇票") /* "商业汇票" */, (short) 50),

    SALES_EXPENSE_AR(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801CF","销售费用") /* "销售费用" */, (short) 70),
    SALES_EXPENSE_AP(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801CF","销售费用") /* "销售费用" */, (short) 72),
    CHAIN_EXPENSE_AR(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801D0","供应链费用") /* "供应链费用" */, (short) 71),
    CHAIN_EXPENSE_AP(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801D0","供应链费用") /* "供应链费用" */, (short) 73),
    PURINRECORD_AP(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801C3","库存") /* "库存" */, (short) 74), // 采购入库的库存
    SALES_REBATE(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801D1","销售返利") /* "销售返利" */, (short) 75), // 销售返利
    OUTSOURCING_STORAGE(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801D2","委外入库") /* "委外入库" */, (short) 76), // 委外入库
    PURCHASE_BILL(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801C5","采购管理") /* "采购管理" */, (short) 77), // 采购管理 采购结算单产生的红冲入库单据
    INTERNALTRADE_PRE_AR(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801D3","内部待结算应收") /* "内部待结算应收" */, (short) 78),
    INTERNALTRADE_PRE_AP(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801D4","内部待结算应付") /* "内部待结算应付" */, (short) 79),
    THIRD_SYS_TO_ARAP(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801D5","第三方系统") /* "第三方系统" */, (short) 80),
    RECEIVE_PLAN(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801D6","收款计划") /* "收款计划" */, (short) 81),

    FIXED_100(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801D7","核销单") /* "核销单" */, (short) 100),


    IFMANAGEMENT(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801D8","投融资管理") /* "投融资管理" */, (short) 83),
    CREDITMANAGEMENT(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801D9","授信管理") /* "授信管理" */, (short) 82),
    ThreePartyReconciliation(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801DB","三方对账") /* "三方对账" */, (short) 110),

	//保函管理
	GuaranteeManagement(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_190EA5EA05800007","保函管理") /* "保函管理" */, (short) 115),
	//信用证管理
	LetterCreditManagement(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_190EA62205800003","信用证管理") /* "信用证管理" */, (short) 120),

	//投标保证金
	BidMargin(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_197FA35404D00007","投标保证金") /* "投标保证金" */, (short) 121),
	// 外部系统 对应openApi接口产生的数据
	ExternalSystem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1D6E541805000020","外部系统") /* "外部系统" */, (short) 122),
	;

	private String name;
	private short value;

	private EventSource(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
	}

	public short getValue() {
		return value;
	}

	// 在声明时直接初始化
	private static final HashMap<Short, EventSource> map = new HashMap<>();

	static {
		EventSource[] items = EventSource.values();
		for (EventSource item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static EventSource find(Number value) {
		if (value == null) {
			return null;
		}
		return map.get(value.shortValue());
	}
}
