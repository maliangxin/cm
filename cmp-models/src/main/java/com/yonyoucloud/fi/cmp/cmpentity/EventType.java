package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * 事项类型枚举
 *
 * @author u
 * @version 1.0
 */
public enum EventType {
	InitDate(MessageUtils.getMessage("P_YS_FI_AR_0001065998") /* "现金期初日记账未达" */,(short)0),
	SaleInvoice(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012527") /* "销售发票" */, (short)1),
	OtherAREvent(MessageUtils.getMessage("P_YS_FI_CM_0000026304") /* "其它应收事项" */, (short)2),
	OrderDailyReport(MessageUtils.getMessage("P_YS_FI_CM_0000026042") /* "订单日报" */, (short)5),
	InterSettlement(MessageUtils.getMessage("P_YS_FI_CM_0000026113") /* "内部交易结算单" */, (short)6),
	ReceiveBill(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012711") /* "收款单" */, (short)7),
	OtherAPEvent(MessageUtils.getMessage("P_YS_FI_CM_0000026166") /* "其它应付事项" */, (short)8),
	ArRefund(MessageUtils.getMessage("P_YS_FI_AR_0000059061") /* "客户退款单" */, (short)9),
	PayMent(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012535") /* "付款单" */, (short)10),
	ApRefund(MessageUtils.getMessage("P_YS_FI_AR_0000059125") /* "供应商退款单" */, (short)11),
	TransferAccount(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22DBE83804300009","同名账户划转") /* "同名账户划转" */, (short)12),
	ExchangeBill(MessageUtils.getMessage("P_YS_FI_AR_0000059029") /* "汇率损益单" */, (short)13),
	CurrencyExchangeBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22DBE8D604380008", "货币兑换单") /* "货币兑换单" */, (short)14),
	SalaryPayment(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_YYFI-UI_0001112175","薪资支付单") /* "薪资支付单" */, (short)15),
	CashMark(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000013132") /* "银行对账单" */, (short)16),
	FundCollection(MessageUtils.getMessage("P_YS_CTM_CM-BE_0001533498") /* "资金收款单" */, (short)17),
	FundPayment(MessageUtils.getMessage("P_YS_CTM_CM-BE_0001533499") /* "资金付款单" */, (short)18),
	CmpInventory(MessageUtils.getMessage("P_YS_CTM_CM-UI_0001575400") /* "现金盘点" */, (short)19),
	CmpCheckStockApply(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001612487") /* "支票入库" */, (short)20),
	CmpCheckTable(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001612486") /* "支票工作台" */, (short)21),
	CurrencyExchangeApply(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CTM-CMP-MD_18D4535C05400013","外币兑换申请") /* "外币兑换申请" */, (short) 22),
    JournalBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218B04AE04300007","日记账录入") /* "日记账录入" */, (short) 23),

	ReceivableNoteInit(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001097688") /* "应收票据期初" */, (short)50),
	Register(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001097617") /* "收票登记" */, (short)51),
	ConsignBank(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001097649") /* "银行托收" */, (short)52),
	ExpireCash(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001097608") /* "到期兑付" */, (short)53),
	SignNoteInit(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001097718") /* "应付票据期初" */, (short)54),
	SignNote(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001097701") /* "票据签发" */, (short)55),
	PayBillRegister(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001097602") /* "付票登记" */, (short)56),
	Discount(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001098110") /* "票据贴现" */, (short)57),
	Endore(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001098127") /* "票据背书" */, (short)58),
	PayApplyBill(MessageUtils.getMessage("P_YS_FI_YYFI-UI_0001145853") /* "付款申请单" */, (short)59),
	StwbSettleMentDetails(MessageUtils.getMessage("P_YS_FI_AR_0001162621") /* "资金结算明细" */, (short)60),

	SALES_EXPENSE_AR(MessageUtils.getMessage("P_YS_FI_RP_0000125842") /* "销售费用" */, (short)70),
	CHAIN_EXPENSE_AR(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001146527") /* "供应链费用" */, (short)71),
	SALES_EXPENSE_AP(MessageUtils.getMessage("P_YS_FI_RP_0000125842") /* "销售费用" */, (short)72),
	CHAIN_EXPENSE_AP(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001146527") /* "供应链费用" */, (short)73),

	INTERNALTRADE_PRE_AR(MessageUtils.getMessage("P_YS_FI_AR_0001224649") /* "内部交易待结算单" */, (short)72),
	INTERNALTRADE_PRE_AP(MessageUtils.getMessage("P_YS_FI_AR_0001224649") /* "内部交易待结算单" */, (short)74),

	Collect_receipts(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001217528") /* "归集收款单" */, (short)75),
	IHand_out_receipt(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001217548") /* "下拨收款单" */, (short)76),
	Collect_payment_slips(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001217549") /* "归集付款单" */, (short)77),
	Disburse_payment_slip(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001217530") /* "下拨付款单" */, (short)78),

	stct_interestsettlement(MessageUtils.getMessage("P_YS_CTM_STWB-UI_1566331657632874572") /* "内部账户结息单" */, (short)79),

	BillClaim(MessageUtils.getMessage("P_YS_CTM_CM-UI_1472171371533959174") /* "认领单" */, (short)80),
	Drft_Discount(MessageUtils.getMessage("P_YS_CTM_CM-FE_1528677498964410370") /* "贴现办理单" */, (short)81),
	Drft_Consignbank(MessageUtils.getMessage("P_YS_CTM_CM-FE_1545661225579315209") /* "银行托收单" */, (short)82),
	stct_agentpayment(MessageUtils.getMessage("P_YS_CTM_STCT-UI_0001368239") /* "代理付款单" */, (short)83),
	stct_agentCollection(MessageUtils.getMessage("P_YS_CTM_STCT-BE_0001315174") /* "代理收款单" */, (short)84),
	eap_apRefund(MessageUtils.getMessage("P_YS_CTM_STWB-UI_0001567162") /* "付款退款单" */, (short)85),
	eap_arRefund(MessageUtils.getMessage("P_YS_CTM_STWB-UI_0001567165") /* "收款退款单" */, (short)86),
	ear_collection(MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012711") /* "收款单" */, (short)87),

	cooperate_fund_collection(MessageUtils.getMessage("P_YS_CTM_CM-BE_1572948965274419226") /* "协同资金收款单" */, (short)88),
	MarginAccount(MessageUtils.getMessage("P_YS_CTM_CM-BE_1659195095457464352") /* "保证金存入支取单" */, (short)89),

	FinancingRegistration(MessageUtils.getMessage("P_YS_CTM_GRM-BE_1575400420923146246") /* "融资登记" */, (short)90),
	LendingRegistration(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001587019") /* "融入登记" */, (short)91),
	Financingrollover(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001587012") /* "融资展期" */, (short)92),
	Financingrepayment(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001586677") /* "融资还本" */, (short)93),

	CreditContract(MessageUtils.getMessage("P_YS_CTM_CAM-BE_1570172200182349993") /* "授信合同" */, (short)94),
	CreditChange(MessageUtils.getMessage("P_YS_CTM_CAM-UI_1591698498981986622") /* "授信变更" */, (short)95),

	Billissuance(MessageUtils.getMessage("P_YS_CTM_STCT-UI_1586495051598397446") /* "票据签发" */, (short)96),
	Initialpayable(MessageUtils.getMessage("P_YS_CTM_CM-BE_1670496691987939337") /* "期初应付票据" */, (short)97),
	DuePmt(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001097608") /* "到期兑付" */, (short)98),
	Margincall(MessageUtils.getMessage("P_YS_CTM_CM-BE_1670497095737933829") /* "保证金追加" */, (short)99),


	Unified_Synergy(MessageUtils.getMessage("P_YS_CTM_STWB-UI_1481672474919174174") /* "统收统支协同单" */, (short)100),

	ThreePartyReconciliation(MessageUtils.getMessage("P_YS_FI_AAI-BE_1672566093131546668") /* "三方对账" */, (short)110),
	tlm_payinterest(MessageUtils.getMessage("P_YS_PF_GZTSYS_0001587080") /* "融资付息" */, (short) 111),
	tlm_financepayfee(MessageUtils.getMessage("P_YS_CTM_TLM-FE_1500454478566916098") /* "融资付费" */, (short) 112),


	tlm_interestcollection(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18774A2C05B0000F","投资收息") /* "投资收息" */, (short) 113),
	tlm_purchaseregister(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18774A2C05B00010","申购登记") /* "申购登记" */, (short) 114),
	tlm_investredem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18774A2C05B0000C","投资赎回") /* "投资赎回" */, (short) 115),
	tlm_investpayment(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18774A2C05B0000D","投资付费") /* "投资付费" */, (short) 116),
	tlm_investprofitsharing(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18774A2C05B0000E","投资分红") /* "投资分红" */, (short) 117),
	InternalTransferProtocol("内转协议", (short) 118),
	BuildingCloudReceipt("收款单", (short)119),
	EarnestMoneyReclaimOrder("保证金收购回单",(short)120),
	EarnestMoneyCollectOrder("保证金收取单",(short)121),
	znbzbx_returnbill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_198F49CE0408000A","还款单") /* "还款单" */, (short) 122),
	AccountDetailExclusion("账户收支明细剔除",(short)123),
	marketingbill_PaymentVoucherReceipt("来款记录",(short)124),
	lgm_guaranteefee(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19C43D4404780009","保函费用") /* "保函费用" */, (short) 125),
	BankDealDetail("银行交易明细" , (short) 130),
	BankElectronicReceipt("银行交易回单" , (short) 131),
	tlm_tradedelivery("交易交割单" , (short) 132),
	tlm_derivativesclose("交易平仓单" , (short) 133),
	tlm_traderolloverregister("交易展期登记单" , (short) 134),
	tlm_addbond("交易保证金登记单" , (short) 135),
	znbzbx_refundbill("退款单", (short) 136),
	eap_bill_payment("付款单", (short) 137),
	tlm_Payinterestadjust("利息调整付息单", (short) 138),
	tlm_Payadjustcollection("利息调整收息单", (short) 139),
	tlm_ABSRepayment("专项还款登记单", (short) 140),
	znbzbx_pubprepay("对公预付单", (short) 141),
	znbzbx_expensebill("通用报销单", (short) 142),
	sfa_tenderguaranteecard("投标保证金退回单", (short) 143),
	sfa_performguaranteecard("履约保证金退回单", (short) 144),
	fdtr_fund_collect_serial("资金归集", (short) 145),
	fdtr_fund_allocation("资金下拨", (short) 146),
	refund_and_reissue("退票重付", (short) 147),
	;

	private String name;
	private short value;

	private EventType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	// 在声明时直接初始化
	private static final HashMap<Short, EventType> map = new HashMap<>();

	static {
		EventType[] items = EventType.values();
		for (EventType item : items) {
			map.put(item.getValue(), item);
		}
	}


	public static EventType find(Number value) {
		if (value == null) {
			return null;
		}
		return map.get(value.shortValue());
	}
}
