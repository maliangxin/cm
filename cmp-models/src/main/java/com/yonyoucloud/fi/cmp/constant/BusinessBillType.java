package com.yonyoucloud.fi.cmp.constant;

import java.util.HashMap;

/**
 * 业务单据类型
 */
public enum BusinessBillType {
    TransferPayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804C2","转账付款单") /* "转账付款单" */, (short)0),
    TransferRecBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804C3","转账收款单") /* "转账收款单" */, (short)1),
    Reimbursement(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804C5","通用报销单") /* "通用报销单" */, (short)2),
    RefundBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804C7","退款单") /* "退款单" */, (short)3),
    OtherRec(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804C8","其他收款单") /* "其他收款单（第三方专用）" */, (short)6),
    RecBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804C9","收款单") /* "收款单" */, (short)7),
    OtherSinglePay(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804CA","其他单笔付款单") /* "其他单笔付款单（第三方专用）" */,(short)8),
    OtherBatchPay(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804CB","其他批量付款单") /* "其他批量付款单" */,(short)9),
    PayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804CC","付款单") /* "付款单" */, (short)10),
    ClReimbursement(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804CD","差旅费报销单") /* "差旅费报销单" */, (short)11),
    DgRePayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804CE","对公预付单") /* "对公预付单" */, (short)12),
    Hkd(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804CF","还款单") /* "还款单" */, (short)13),
    PersonaDebit(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804D0","个人借款单") /* "个人借款单" */, (short)14),
    SalaryPaymentBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804D1","薪资发放单") /* "薪资发放单" */, (short)15),
    AgentPayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804D3","代理付款单") /* "代理付款单" */,(short)16),
    AgentRecBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804D4","代理收款单") /* "代理收款单" */,(short)17),
    InnerSettleBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804D5","内部账户结息单") /* "内部账户结息单" */,(short)18),

    InvestmentRecBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804D6","投资收款单") /* "投资收款单" */,(short)161),
    InvestmentPayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804D7","投资付款单") /* "投资付款单" */ ,(short)162),
    FinanCollectBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804D8","融资收款单") /* "融资收款单" */,(short)163),
    FinancingPayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804D9","融资付款单") /* "融资付款单" */,(short)164),
    DerivativeRecBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804DA","衍生品收款单") /* "衍生品收款单" */,(short)165),
    DerivativePayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804DB","衍生品付款单") /* "衍生品付款单" */,(short)166),
    //20230330新增
    IntegrateRegistBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804DC","融入登记单") /* "融入登记单" */,(short)167),//收款
    FinanPrinRepayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804DD","融资还本单") /* "融资还本单" */ ,(short)168),//付款
    FinanInterPayNoteBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804DE","融资付息单") /* "融资付息单" */,(short)169),//付款
    FinancingBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804DF","融资付费单") /* "融资付费单" */,(short)170),//付款
    SubscriptRegistBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804E0","申购登记单") /* "申购登记单" */,(short)171),//付款
    InvestRedempteBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804E1","投资赎回单") /* "投资赎回单" */,(short)172),//收款
    InvestInterReceBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804E2","投资收息单") /* "投资收息单" */,(short)173),//收款
    InvestPaySlipBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804E3","投资付费单") /* "投资付费单" */,(short)174),//付款
    InvestDividNoteBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804E4","投资分红单") /* "投资分红单" */,(short)175),//收款
    TransRegistBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804E5","交易成交登记单") /* "交易成交登记单" */,(short)176),//付款
    IransClosRegistPayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804E6","交易平仓登记付款单") /* "交易平仓登记付款单" */,(short)177),//付款
    IransClosRegistReceBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804E7","交易平仓登记收款单") /* "交易平仓登记收款单" */,(short)178),//收款
    AddMarginRegistBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804E8","追加保证金登记单") /* "追加保证金登记单" */,(short)179),//付款
    TransSettleRegistPayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804E9","交易交割登记付款单") /* "交易交割登记付款单" */,(short)180),//付款
    TransSettleRegistReceBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804EA","交易交割登记收款单") /* "交易交割登记收款单" */,(short)181),//收款
    InterestAdjustmentPayment("利息调整付息",(short)182),
    InterestAdjustmentReceivebill("利息调整收息",(short)183),
    SpecialRepaymentRegistration ("专项还款登记" , (short) 184),

    //付款
    TradeDealBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804EB", "交易成交单") /* "交易成交单" */, (short) 30),
    //收款
    TradeCloseOutBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804EC", "交易平仓单") /* "交易平仓单" */, (short) 31),
    //付款
    AddBondBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804ED", "追加保证金单") /* "追加保证金单" */, (short) 32),


    //现金管理
    CashRecBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804EE", "资金收款单") /* "资金收款单" */, (short) 25),
    CashPayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804EF", "资金付款单") /* "资金付款单" */, (short) 26),
    GuarantyDepBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804F0", "保证金存入支取单") /* "保证金存入支取单" */, (short) 81),
    TransferBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804F1", "转账单") /* "转账单" */, (short) 82),
    PaymentMarginManagement(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_19529C1604F0001E", "支付保证金台账管理") /* 支付保证金台账管理 */, (short) 83),
    ReceiptMarginManagement(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_19529C1604F00021", "收到保证金台账管理") /* 收到保证金台账管理 */, (short) 84),
    ForeignPayment("外汇付款", (short) 85),

    PressForPayment("追索申请", (short) 86),

    AgreeToRepayTheApplication("同意清偿申请", (short) 87),

    InternalTransferApplication("内部转让申请", (short) 88),

    InternalTransferAcceptance("内部转让受理", (short) 89),
    //应收应付
    RecRefundBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804F2", "收款退款单") /* "收款退款单" */, (short) 27),
    PayRefundBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804F3", "付款退款单") /* "付款退款单" */, (short) 28),


    //项目云
    ProjectPayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804F4", "项目付款单") /* "项目付款单" */, (short) 29),

    //资金调度
    CollectPaybill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804F5", "资金调度归集付款单") /* "资金调度归集付款单" */, (short) 101),
    CollectRecebill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804F6", "资金调度归集收款单") /* "资金调度归集收款单" */, (short) 102),
    AllocatePayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804F7", "资金调度下拨付款单") /* "资金调度下拨付款单" */, (short) 106),
    AllocateRecBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804F8", "资金调度下拨收款单") /* "资金调度下拨收款单" */, (short) 107),
    FundtransferPayment(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804F9", "资金调度调拨付款单") /* "资金调度调拨付款单" */, (short) 109),
    FundtransferReceive(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804FA", "资金调度调拨收款单") /* "资金调度调拨收款单" */, (short) 110),

    //资金结算-统收统支(收付款)
    IncomeAndExpenditure(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804FB", "统收统支协同单") /* "统收统支协同单" */, (short) 188),

    //费控系统
    MarketingexpensebizBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804FC","营销费用业务核销单") /* "营销费用业务核销单" */,(short)190),
    MarketingexpensefdBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804FD","营销费用财务核销单") /* "营销费用财务核销单" */,(short)191),
    FkPayBill("挂账付款申请单",(short)210),


    //商业汇票
    DrftRecBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804FF","商业汇票收款单") /* "商业汇票收款单" */,(short)41),
    DrftPayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180500","商业汇票付款单") /* "商业汇票付款单" */ ,(short)42),
    BankCollection(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_19529C1604F0001D","银行托收") /* "银行托收" */,(short)43),
    DuePayment(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_19529C1604F0001F","到期兑付") /* "到期兑付" */,(short)44),
    DiscountHandling(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_19529C1604F00022","贴现办理") /* "贴现办理" */,(short)45),
    BillIssuance(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_19529C1604F00024","票据签发") /* "票据签发" */,(short)46),
    BulkDiscount("批量贴现办理",(short) 47),

    //境外
    CollectionPayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180501","境外归集付款单") /* "境外归集付款单" */ ,(short)200),
    CollectionRecBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180502","境外归集收款单") /* "境外归集收款单" */ ,(short)201),
    StirPayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180503","境外下拨付款单") /* "境外下拨付款单" */ ,(short)202),
    StirRecBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180504","境外下拨收款单") /* "境外下拨收款单" */ ,(short)203),
    TransfersPayBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180505","境外调拨付款单") /* "境外调拨付款单" */ ,(short)204),
    TransfersRecBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180506","境外调拨收款单") /* "境外调拨收款单" */ ,(short)205),
    LoanRegisterBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180507","境外放款登记单") /* "境外放款登记单" */,(short)206),
    BorrowRegisterBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180508","外债借入登记单") /* "外债借入登记单" */,(short)207),
    FundCollectionForm(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180509","跨境资金归集单") /* "跨境资金归集单" */,(short)208),
    FundAllocateForm(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418050A","跨境资金下拨单") /* "跨境资金下拨单" */,(short)209),


    //汽车云-销售财务
    PrePayApplyBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418050B","预付款申请") /* "预付款申请" */,(short)231),//付
    SalesPayApplyBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418050C","销售应付单申请") /* "销售应付单申请" */,(short)232),//付
    OtherPaymentBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418050D","其它付款申请") /* "其它付款申请" */,(short)233),//付
    PreRecRefundBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418050E","预收款退款") /* "预收款退款" */,(short)234),//付
    SalesRefundBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418050F","装潢销售退款") /* "装潢销售退款" */,(short)235),//付
    SalesSettleRecBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180510","销售结算收款") /* "销售结算收款" */,(short)236),//收
    AdvanceCollectionBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180511","预收款收款") /* "预收款收款" */,(short)237),//收
    FinancialRecAndInvBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180512","金融对账开票") /* "金融对账开票" */,(short)238),//收
    //汽车云-售后财务
    AfterSalesApplyBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804BA","售后应付申请") /* "售后应付申请" */,(short)241),//付
    AfterSalesSettleBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804BB","售后结算收款") /* "售后结算收款" */,(short)242),//收
    AfterSalesRecAndInvBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804BC","售后对账开票") /* "售后对账开票" */,(short)243),//收
    //汽车云-其他结算
    OtherReviceBill(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804BD","其他收款单(汽车云)") /* "其他收款单(汽车云)" */,(short)251),//收

    //0530 担保 授信 费用
    CreditFeeSheet(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804BE","授信费用单") /* "授信费用单" */,(short)255),
    GuaranteeFeeSheet(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804BF","担保费用单") /* "担保费用单" */,(short)256),
    ChangeSettlement(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804C0","结算信息变更单") /* "结算信息变更单" */,(short)257),
    GuaranteeFeeAdjustment(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804C1","担保费用调整单") /* "担保费用调整单" */,(short)258),
    //信用证
    PaymentUponReceipt(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804C4","到单付款单") /* "到单付款单" */,(short)301),
    PaymentReceipt(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041804C6","交单收款单") /* "交单收款单" */,(short)302),

    //信用证管理 ，保函管理，交易展期登记
    LetterCreditManagement(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_19529C1604F00025","信用证手续费") /* "信用证手续费" */,(short)310),
    GuaranteeManagement(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_19529C1604F00026","保函手续费") /* "保函手续费" */,(short)311),
    RegistrationTransactionExtension(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_19529C1604F00027","交易展期登记") /* "交易展期登记" */,(short)312),
    GuaranteeFee("保函费用",(short)313),

    //建筑云付款单、建筑云收款单
    ConstructionReceipt(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_19529C1604F00020","建筑云收款单") /* "建筑云收款单" */,(short)320),
    ConstructionPayment(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_19529C1604F00023","建筑云付款单") /* "建筑云付款单" */,(short)321),
    ConstructionReceiptRefund(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-UI_1A0D5C2005B80008","建筑云收款退款单") /* "建筑云收款退款单" */,(short)322),
    ConstructionPaymentRefund(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-UI_1A0D5CB405B80007","建筑云付款退款单") /* "建筑云付款退款单" */,(short)323),

    /*B2C订单中心*/
    DailyPaymentReport_B2C("收款日报(B2C订单中心)",(short)330),
    DailyPaymentReport_RetailServices("收款日报(零售服务)",(short)340);

    private String name;
    private short value;

    BusinessBillType(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
    }

    public void setName(String name) {
        this.name = name;
    }

    public short getValue() {
        return value;
    }

    public void setValue(short value) {
        this.value = value;
    }

    private static HashMap<Short, BusinessBillType> map = null;
    private static HashMap<String, BusinessBillType> nameMap = null;

    private synchronized static void initMap() {
        if (map != null||nameMap!=null) {
            return;
        }
        map = new HashMap<Short, BusinessBillType>();
        nameMap = new HashMap<String, BusinessBillType>();
        BusinessBillType[] items = BusinessBillType.values();
        for (BusinessBillType item : items) {
            map.put(item.getValue(), item);
            nameMap.put(item.getName(), item);
        }
    }

    public static BusinessBillType find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
    public static BusinessBillType findName(String name) {
        if (name == null) {
            return null;
        }
        if (nameMap == null) {
            initMap();
        }
        return nameMap.get(name);
    }
}
