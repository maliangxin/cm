package com.yonyoucloud.fi.cmp.fundpayment;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IBackWrite;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.CheckPurpose;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.NoteDirection;
import org.imeta.orm.base.BizObject;

import java.math.BigDecimal;

/**
 * 资金付款子表实体
 *
 * @author u
 * @version 1.0
 */
public class FundPayment_b extends BizObject implements IBackWrite, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.fundpayment.FundPayment_b";

	private static final long serialVersionUID = -3448627345333873327L;

	// ==================== 字段常量定义 (标识符大写，值为原始字段名) ====================
	public static final String MAINID = "mainid";
	public static final String FUNDBUSINOBJTYPEID = "fundbusinobjtypeid";
	public static final String SMARTCHECKNO = "smartcheckno";
	public static final String QUICKTYPE = "quickType";
	public static final String ORISUM = "oriSum";
	public static final String NORIREMAINAMOUNT = "noriremainamount";
	public static final String ORITRANSITAMOUNT = "oritransitamount";
	public static final String BIZOBJTYPE = "bizobjtype";
	public static final String REFUNDSUM = "refundSum";
	public static final String NATSUM = "natSum";
	public static final String SETTLESUCCESSSUM = "settlesuccessSum";
	public static final String SETTLEERRORSUM = "settleerrorSum";
	public static final String TRANSNUMBER = "transNumber";
	public static final String WSETTLESTATUS = "wsettleStatus";
	public static final String SETTLERESULT = "settleResult";
	public static final String REJECTTYPE = "rejecttype";
	public static final String REJECTREMARK = "rejectremark";
	public static final String DZDATE = "dzdate";
	public static final String CURRENCY = "currency";
	public static final String NATCURRENCY = "natCurrency";
	public static final String SETTLEMODE = "settlemode";
	public static final String SETTLESTATUS = "settlestatus";
	public static final String ENTERPRISEBANKACCOUNT = "enterprisebankaccount";
	public static final String CASHACCOUNT = "cashaccount";
	public static final String NOTETYPE = "notetype";
	public static final String NOTENO = "noteno";
	public static final String CHECKNO = "checkno";
	public static final String CHECKID = "checkId";
	public static final String NOTETEXTNO = "notetextno";
	public static final String NOTEDIRECTION = "noteDirection";
	public static final String NOTESUM = "noteSum";
	public static final String CAOBJECT = "caobject";
	public static final String CUSTOMER = "customer";
	public static final String TX_NOTENO = "tx_noteno";
	public static final String SUPPLIER = "supplier";
	public static final String EMPLOYEE = "employee";
	public static final String OPPOSITEACCOUNTID = "oppositeaccountid";
	public static final String OPPOSITEOBJECTID = "oppositeobjectid";
	public static final String OPPOSITEOBJECTNAME = "oppositeobjectname";
	public static final String OPPOSITEACCOUNTNAME = "oppositeaccountname";
	public static final String OPPOSITEACCOUNTNO = "oppositeaccountno";
	public static final String OPPOSITEBANKADDR = "oppositebankaddr";
	public static final String OPPOSITEBANKADDRID = "oppositebankaddrid";
	public static final String OPPOSITEBANKTYPE = "oppositebankType";
	public static final String OPPOSITEBANKLINENO = "oppositebanklineno";
	public static final String EXCHRATE = "exchRate";
	public static final String EXCHANGERATETYPE = "exchangeRateType";
	public static final String PROJECT = "project";
	public static final String EXPENSEITEM = "expenseitem";
	public static final String DEPT = "dept";
	public static final String OPERATOR = "operator";
	public static final String TAXCATEGORY = "taxCategory";
	public static final String TAXRATE = "taxRate";
	public static final String TAXSUM = "taxSum";
	public static final String DESCRIPTION = "description";
	public static final String SRCBILLNO = "srcbillno";
	public static final String SRCBILLITEMNO = "srcbillitemno";
	public static final String TOPSRCBILLNO = "topsrcbillno";
	public static final String SRCBILLID = "srcbillid";
	public static final String PUSHSRCBILLMID = "pushsrcbillmid";
	public static final String YTENANTID = "ytenantId";
	public static final String SOURCEID = "sourceid";
	public static final String SOURCEAUTOID = "sourceautoid";
	public static final String SOURCE = "source";
	public static final String UPCODE = "upcode";
	public static final String MAKERULECODE = "makeRuleCode";
	public static final String SOURCEMAINPUBTS = "sourceMainPubts";
	public static final String GROUPTASKKEY = "groupTaskKey";
	public static final String ROWNO = "rowno";
	public static final String PUBTS = "pubts";
	public static final String TENANT = "tenant";
	public static final String CHARACTERDEFB = "characterDefb";
	public static final String SIGNATURE = "signature";
	public static final String ASSOCIATIONSTATUS = "associationStatus";
	public static final String BANKRECONCILIATIONID = "bankReconciliationId";
	public static final String BILLCLAIMID = "billClaimId";
	public static final String COSTCENTER = "costcenter";
	public static final String PROFITCENTER = "profitcenter";
	public static final String OPPOSITEOPENBANKID = "oppositeOpenBankId";
	public static final String OPPOSITEOPENBANKNAME = "oppositeOpenBankName";
	public static final String SETTLESUCCESSTIME = "settleSuccessTime";
	public static final String FUNDPLANPROJECT = "fundPlanProject";
	public static final String THIRDPARVIRTACCOUNT = "thirdParVirtAccount";
	public static final String WHETHERSETTLE = "whetherSettle";
	public static final String ENTRUSTREJECT = "entrustReject";
	public static final String SETTLEDID = "settledId";
	public static final String ISTOPUSHCSPL = "isToPushCspl";
	public static final String YTENANT = "ytenant";
	public static final String INTERESTSETTLEMENTACCOUNT = "interestSettlementAccount";
	public static final String LASTINTERESTSETTLEMENTENDDATE = "lastInterestSettlementEndDate";
	public static final String CURRENTINTERESTSETTLEMENTSTARTDATE = "currentInterestSettlementStartDate";
	public static final String CURRENTINTERESTSETTLEMENTENDDATE = "currentInterestSettlementEndDate";
	public static final String WITHHOLDINGORISUM = "withholdingOriSum";
	public static final String WITHHOLDINGNATSUM = "withholdingNatSum";
	public static final String RECEIVEPARTYSWIFT = "receivePartySwift";
	public static final String RISKPAYTYPE = "riskPayType";
	public static final String PAYMENTMODE = "paymentMode";
	public static final String UNTAXSUM = "unTaxSum";
	public static final String INCLUDETAXSUM = "includeTaxSum";
	public static final String ISSUBCONTRACT = "isSubcontract";
	public static final String ONEAVAILABLEMONEY = "oneAvailableMoney";
	public static final String PAYEXTEND = "payExtend";
	public static final String CHECKPURPOSE = "checkPurpose";
	public static final String ISOCCUPYBUDGET = "isOccupyBudget";
	public static final String LINENO = "lineno";
	public static final String SYNERGYBILLNO = "synergybillno";
	public static final String SYNERGYBILLITEMNO = "synergybillitemno";
	public static final String SYNERGYBILLID = "synergybillid";
	public static final String ISSYNERGY = "issynergy";
	public static final String PUBLICPRIVATE = "publicPrivate";
	public static final String WHETHERREFUNDANDREPAYMENT = "whetherRefundAndRepayment";
	public static final String REFUNDASSOCIATEDPAYMENTID = "refundAssociatedPaymentId";
	public static final String ISINCOMEANDEXPENDITURE = "isIncomeAndExpenditure";
	public static final String INCOMEANDEXPENDRELATIONGROUP = "incomeAndExpendRelationGroup";
	public static final String INCOMEANDEXPENDBANKACCOUNT = "incomeAndExpendBankAccount";
	public static final String SETTLECURRENCY = "settleCurrency";
	public static final String SWAPOUTEXCHANGERATETYPE = "swapOutExchangeRateType";
	public static final String SWAPOUTEXCHANGERATEESTIMATE = "swapOutExchangeRateEstimate";
	public static final String SWAPOUTAMOUNTESTIMATE = "swapOutAmountEstimate";
	public static final String ACTUALSETTLEMENTEXCHANGERATE = "actualSettlementExchangeRate";
	public static final String ACTUALSETTLEMENTAMOUNT = "actualSettlementAmount";
	public static final String ACTUALSETTLEMENTEXCHANGERATETYPE = "actualSettlementExchangeRateType";
	public static final String ACTUALSETTLEACCOUNT = "actualSettleAccount";
	public static final String NETIDENTIFICATECODE = "netIdentificateCode";
	public static final String NETSETTLECOUNT = "netSettleCount";
	public static final String AFTERNETAMT = "afterNetAmt";
	public static final String AFTERNETDIR = "afterNetDir";
	public static final String EXCHANGERATEOPS = "exchangerateOps";

	/**
     * 获取主表id
     *
     * @return 主表id.ID
     */
	public String getMainid() {
		return get("mainid");
	}

    /**
     * 设置主表id
     *
     * @param mainid 主表id.ID
     */
	public void setMainid(String mainid) {
		set("mainid", mainid);
	}

	/**
	 * 获取资金业务对象类型id
	 *
	 * @return 资金业务对象类型id
	 */
	public String getFundbusinobjtypeid() {
		return get("fundbusinobjtypeid");
	}

	/**
	 * 设置资金业务对象类型id
	 *
	 * @param fundbusinobjtypeid 资金业务对象类型id
	 */
	public void setFundbusinobjtypeid(String fundbusinobjtypeid) {
		set("fundbusinobjtypeid", fundbusinobjtypeid);
	}


	/**
	 * 获取智能对账勾兑码
	 *
	 * @return 智能对账勾兑码
	 */
	public String getSmartcheckno() {
		return get("smartcheckno");
	}

	/**
	 * 设置智能对账勾兑码
	 *
	 * @param smartcheckno 智能对账勾兑码
	 */
	public void setSmartcheckno(String smartcheckno) {
		set("smartcheckno", smartcheckno);
	}

    /**
     * 获取款项类型
     *
     * @return 款项类型.ID
     */
	public Long getQuickType() {
		return getLong("quickType");
	}

    /**
     * 设置款项类型
     *
     * @param quickType 款项类型.ID
     */
	public void setQuickType(Long quickType) {
		set("quickType", quickType);
	}

    /**
     * 获取收款金额
     *
     * @return 收款金额
     */
	public java.math.BigDecimal getOriSum() {
		return get("oriSum");
	}

    /**
     * 设置收款金额
     *
     * @param oriSum 收款金额
     */
	public void setOriSum(java.math.BigDecimal oriSum) {
		set("oriSum", oriSum);
	}


	/**
	 * 获取原币余额
	 *
	 * @return 原币余额
	 */
	public java.math.BigDecimal getnoriRemainAmount() {
		return get("noriremainamount");
	}

	/**
	 * 设置原币余额
	 *
	 * @param noriRemainAmount 原币余额
	 */
	public void setNoriRemainAmount(java.math.BigDecimal noriRemainAmount) {
		set("noriremainamount", noriRemainAmount);
	}


	/**
	 * 获取在途结算金额
	 *
	 * @return 在途结算金额
	 */
	public java.math.BigDecimal getOriTransitAmount() {
		return get("oritransitamount");
	}

	/**
	 * 设置在途结算金额
	 *
	 * @param oritransitamount 在途结算金额
	 */
	public void setOriTransitAmount(java.math.BigDecimal oritransitamount) {
		set("oritransitamount", oritransitamount);
	}

	/**
	 * 获取业务对象编码
	 *
	 * @return 业务对象编码
	 */
	public String getBizobjtype() {
		return get("bizobjtype");
	}

	/**
	 * 设置bizobjtype
	 *
	 * @param bizobjtype 业务对象编码
	 */
	public void setBizobjtype(String bizobjtype) {
		set("bizobjtype", bizobjtype);
	}


	/**
	 * 获取退票金额
	 *
	 * @return 退票金额
	 */
	public java.math.BigDecimal getRefundSum() {
		return get("refundSum");
	}

	/**
	 * 设置退票金额
	 *
	 * @param refundSum 退票金额
	 */
	public void setRefundSum(java.math.BigDecimal refundSum) {
		set("refundSum", refundSum);
	}

    /**
     * 获取本币金额
     *
     * @return 本币金额
     */
	public java.math.BigDecimal getNatSum() {
		return get("natSum");
	}

    /**
     * 设置本币金额
     *
     * @param natSum 本币金额
     */
	public void setNatSum(java.math.BigDecimal natSum) {
		set("natSum", natSum);
	}

    /**
     * 获取结算成功金额
     *
     * @return 结算成功金额
     */
	public java.math.BigDecimal getSettlesuccessSum() {
		return get("settlesuccessSum");
	}

    /**
     * 设置结算成功金额
     *
     * @param settlesuccessSum 结算成功金额
     */
	public void setSettlesuccessSum(java.math.BigDecimal settlesuccessSum) {
		set("settlesuccessSum", settlesuccessSum);
	}

    /**
     * 获取结算止付金额
     *
     * @return 结算止付金额
     */
	public java.math.BigDecimal getSettleerrorSum() {
		return get("settleerrorSum");
	}

    /**
     * 设置结算止付金额
     *
     * @param settleerrorSum 结算止付金额
     */
	public void setSettleerrorSum(java.math.BigDecimal settleerrorSum) {
		set("settleerrorSum", settleerrorSum);
	}

	/**
	 * 获取待结算数据流水号
	 *
	 * @return 待结算数据流水号
	 */
	public String getTransNumber() {
		return get("transNumber");
	}

	/**
	 * 设置待结算数据流水号
	 *
	 * @param transNumber 待结算数据流水号
	 */
	public void setTransNumber(String transNumber) {
		set("transNumber", transNumber);
	}
	/**
	 * 获取资金结算状态
	 *
	 * @return 资金结算状态
	 */
	public String getWsettleStatus() {
		return get("wsettleStatus");
	}

	/**
	 * 设置资金结算状态
	 *
	 * @param wsettleStatus 资金结算状态
	 */
	public void setWsettleStatus(String wsettleStatus) {
		set("wsettleStatus", wsettleStatus);
	}

	/**
	 * 获取资金结算结果
	 *
	 * @return 资金结算结果
	 */
	public String getSettleResult() {
		return get("settleResult");
	}

	/**
	 * 设置资金结算结果
	 *
	 * @param settleResult 资金结算结果
	 */
	public void setSettleResult(String settleResult) {
		set("settleResult", settleResult);
	}

	/**
	 * 获取止付类型
	 *
	 * @return 止付类型
	 */
	public String getRejecttype() {
		return get("rejecttype");
	}

	/**
	 * 设置止付类型
	 *
	 * @param rejecttype 止付类型
	 */
	public void setRejecttype(String rejecttype) {
		set("rejecttype", rejecttype);
	}

	/**
	 * 获取驳回备注
	 *
	 * @return 驳回备注
	 */
	public String getRejectremark() {
		return get("rejectremark");
	}

	/**
	 * 设置驳回备注
	 *
	 * @param rejectremark 驳回备注
	 */
	public void setRejectremark(String rejectremark) {
		set("rejectremark", rejectremark);
	}

	/**
     * 获取登账日期
     *
     * @return 登账日期
     */
	public java.util.Date getDzdate() {
		return get("dzdate");
	}

    /**
     * 设置登账日期
     *
     * @param dzdate 登账日期
     */
	public void setDzdate(java.util.Date dzdate) {
		set("dzdate", dzdate);
	}

    /**
     * 获取币种id
     *
     * @return 币种id.ID
     */
	public String getCurrency() {
		return get("currency");
	}

    /**
     * 设置币种id
     *
     * @param currency 币种id.ID
     */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

    /**
     * 获取本币币种id
     *
     * @return 本币币种id.ID
     */
	public String getNatCurrency() {
		return get("natCurrency");
	}

    /**
     * 设置本币币种id
     *
     * @param natCurrency 本币币种id.ID
     */
	public void setNatCurrency(String natCurrency) {
		set("natCurrency", natCurrency);
	}

    /**
     * 获取结算方式
     *
     * @return 结算方式.ID
     */
	public Long getSettlemode() {
		return get("settlemode");
	}

    /**
     * 设置结算方式
     *
     * @param settlemode 结算方式.ID
     */
	public void setSettlemode(Long settlemode) {
		set("settlemode", settlemode);
	}

    /**
     * 获取结算状态
     *
     * @return 结算状态
     */
	public FundSettleStatus getFundSettlestatus() {
		Number v = getShort("settlestatus");
		return FundSettleStatus.find(v);
	}

    /**
     * 设置结算状态
     *
     * @param settlestatus 结算状态
     */
	public void setFundSettlestatus(FundSettleStatus settlestatus) {
		if (settlestatus != null) {
			set("settlestatus", settlestatus.getValue());
		} else {
			set("settlestatus", null);
		}
	}

    /**
     * 获取企业银行账户
     *
     * @return 企业银行账户.ID
     */
	public String getEnterprisebankaccount() {
		return get("enterprisebankaccount");
	}

    /**
     * 设置企业银行账户
     *
     * @param enterprisebankaccount 企业银行账户.ID
     */
	public void setEnterprisebankaccount(String enterprisebankaccount) {
		set("enterprisebankaccount", enterprisebankaccount);
	}

    /**
     * 获取收款现金账户
     *
     * @return 收款现金账户.ID
     */
	public String getCashaccount() {
		return get("cashaccount");
	}

    /**
     * 设置收款现金账户
     *
     * @param cashaccount 收款现金账户.ID
     */
	public void setCashaccount(String cashaccount) {
		set("cashaccount", cashaccount);
	}

    /**
     * 获取票据类型
     *
     * @return 票据类型.ID
     */
	public Long getNotetype() {
		return get("notetype");
	}

    /**
     * 设置票据类型
     *
     * @param notetype 票据类型.ID
     */
	public void setNotetype(Long notetype) {
		set("notetype", notetype);
	}

    /**
     * 获取票据号
     *
     * @return 票据号.ID
     */
	public Long getNoteno() {
		return get("noteno");
	}

    /**
     * 设置票据号
     *
     * @param noteno 票据号.ID
     */
	public void setNoteno(Long noteno) {
		set("noteno", noteno);
	}

	/**
	 * 获取支票号
	 *
	 * @return 支票号.ID
	 */
	public String getCheckno() {
		return get("checkno");
	}

	/**
	 * 设置支票号
	 *
	 * @param checkno 支票.ID
	 */
	public void setCheckno(String checkno) {
		set("checkno", checkno);
	}

	/**
	 * 获取支票号
	 *
	 * @return 支票
	 */
	public String getCheckId() {
		return get("checkId");
	}

	/**
	 * 设置支票号
	 *
	 * @param checkId 支票
	 */
	public void setCheckId(String checkId) {
		set("checkId", checkId);
	}

    /**
     * 获取票据文本号
     *
     * @return 票据文本号
     */
	public String getNotetextno() {
		return get("notetextno");
	}

    /**
     * 设置票据文本号
     *
     * @param notetextno 票据文本号
     */
	public void setNotetextno(String notetextno) {
		set("notetextno", notetextno);
	}

    /**
     * 获取票据方向
     *
     * @return 票据方向
     */
	public NoteDirection getNoteDirection() {
		Number v = getShort("noteDirection");
		return NoteDirection.find(v);
	}

    /**
     * 设置票据方向
     *
     * @param noteDirection 票据方向
     */
	public void setNoteDirection(NoteDirection noteDirection) {
		if (noteDirection != null) {
			set("noteDirection", noteDirection.getValue());
		} else {
			set("noteDirection", null);
		}
	}

    /**
     * 获取票据金额
     *
     * @return 票据金额
     */
	public BigDecimal getNoteSum() {
		return get("noteSum");
	}

    /**
     * 设置票据金额
     *
     * @param noteSum 票据金额
     */
	public void setNoteSum(BigDecimal noteSum) {
		set("noteSum", noteSum);
	}

	/**
	 * 获取收款对象
	 *
	 * @return 收款对象
	 */
	public CaObject getCaobject() {
		Number v = getShort("caobject");
		return CaObject.find(v);
	}

    /**
     * 设置收款对象
     *
     * @param caobject 收款对象
     */
	public void setCaobject(CaObject caobject) {
		if (caobject != null) {
			set("caobject", caobject.getValue());
		} else {
			set("caobject", null);
		}
	}
	/**
	 * 获取客户
	 *
	 * @return 客户.ID
	 */
	public Long getCustomer() {
		return get("customer");
	}

	/**
	 * 获取贴现票据号
	 *
	 * @return 贴现票据号.ID
	 */
	public Long getTx_noteno() {
		return get("tx_noteno");
	}

	/**
	 * 设置贴现票据号
	 *
	 * @param tx_noteno 贴现票据号.ID
	 */
	public void setTx_noteno(Long tx_noteno) {
		set("tx_noteno", tx_noteno);
	}

	/**
	 * 设置客户
	 *
	 * @param customer 客户.ID
	 */
	public void setCustomer(Long customer) {
		set("customer", customer);
	}
	/**
	 * 获取供应商
	 *
	 * @return 供应商.ID
	 */
	public Long getSupplier() {
		return get("supplier");
	}

	/**
	 * 设置供应商
	 *
	 * @param supplier 供应商.ID
	 */
	public void setSupplier(Long supplier) {
		set("supplier", supplier);
	}

	/**
	 * 获取员工
	 *
	 * @return 员工.ID
	 */
	public String getEmployee() {
		return get("employee");
	}

	/**
	 * 设置员工
	 *
	 * @param employee 员工.ID
	 */
	public void setEmployee(String employee) {
		set("employee", employee);
	}
	/**
	 * 获取收款方账户id
	 *
	 * @return 收款方账户id
	 */
	public String getOppositeaccountid() {
		return get("oppositeaccountid");
	}

	/**
	 * 设置收款方账户id
	 *
	 * @param oppositeaccountid 收款方账户id
	 */
	public void setOppositeaccountid(String oppositeaccountid) {
		set("oppositeaccountid", oppositeaccountid);
	}
	/**
	 * 获取收款单位名称id
	 *
	 * @return 收款单位名称id
	 */
	public String getOppositeobjectid() {
		return get("oppositeobjectid");
	}

	/**
	 * 设置收款单位名称id
	 *
	 * @param oppositeobjectid 收款单位名称id
	 */
	public void setOppositeobjectid(String oppositeobjectid) {
		set("oppositeobjectid", oppositeobjectid);
	}
	/**
	 * 获取收款单位名称
	 *
	 * @return 收单位名称
	 */
	public String getOppositeobjectname() {
		return get("oppositeobjectname");
	}

	/**
	 * 设置收款单位名称
	 *
	 * @param oppositeobjectname 收款单位名称
	 */
	public void setOppositeobjectname(String oppositeobjectname) {
		set("oppositeobjectname", oppositeobjectname);
	}

	/**
	 * 获取收款方账户名称
	 *
	 * @return 收款方账户名称
	 */
	public String getOppositeaccountname() {
		return get("oppositeaccountname");
	}

	/**
	 * 设置收款方账户名称
	 *
	 * @param oppositeaccountname 收款方账户名称
	 */
	public void setOppositeaccountname(String oppositeaccountname) {
		set("oppositeaccountname", oppositeaccountname);
	}

	/**
	 * 获取收款方账户号
	 *
	 * @return 收款方账户号
	 */
	public String getOppositeaccountno() {
		return get("oppositeaccountno");
	}

	/**
	 * 设置收款方账户号
	 *
	 * @param oppositeaccountno 收款方账户号
	 */
	public void setOppositeaccountno(String oppositeaccountno) {
		set("oppositeaccountno", oppositeaccountno);
	}

	/**
	 * 获取收款方银行网点
	 *
	 * @return 收款方银行网点
	 */
	public String getOppositebankaddr() {
		return get("oppositebankaddr");
	}

	/**
	 * 设置收款方银行网点
	 *
	 * @param oppositebankaddr 收款方银行网点
	 */
	public void setOppositebankaddr(String oppositebankaddr) {
		set("oppositebankaddr", oppositebankaddr);
	}


	/**
	 * 获取收款方开户行id
	 *
	 * @return 收款方银行网点
	 */
	public String getOppositebankaddrid() {
		return get("oppositebankaddrid");
	}

	/**
	 * 设置收款方开户行id
	 *
	 * @param oppositebankaddrid 收款方银行网点
	 */
	public void setOppositebankaddrid(String oppositebankaddrid) {
		set("oppositebankaddrid", oppositebankaddrid);
	}

	/**
	 * 获取收款方银行类别
	 *
	 * @return 收款方银行类别
	 */
	public String getOppositebankType() {
		return get("oppositebankType");
	}

	/**
	 * 设置收款方银行类别
	 *
	 * @param oppositebankType 收款方银行类别
	 */
	public void setOppositebankType(String oppositebankType) {
		set("oppositebankType", oppositebankType);
	}


	/**
	 * 获取收款方开户行联行号
	 *
	 * @return 收款方开户行联行号
	 */
	public String getOppositebanklineno() {
		return get("oppositebanklineno");
	}

	/**
	 * 设置收款方开户行联行号
	 *
	 * @param oppositebanklineno 收款方开户行联行号
	 */
	public void setOppositebanklineno(String oppositebanklineno) {
		set("oppositebanklineno", oppositebanklineno);
	}


	/**
     * 获取汇率
     *
     * @return 汇率
     */
	public java.math.BigDecimal getExchRate() {
		return get("exchRate");
	}

    /**
     * 设置汇率
     *
     * @param exchRate 汇率
     */
	public void setExchRate(java.math.BigDecimal exchRate) {
		set("exchRate", exchRate);
	}

    /**
     * 获取汇率类型
     *
     * @return 汇率类型.ID
     */
	public String getExchangeRateType() {
		return get("exchangeRateType");
	}

    /**
     * 设置汇率类型
     *
     * @param exchangeRateType 汇率类型.ID
     */
	public void setExchangeRateType(String exchangeRateType) {
		set("exchangeRateType", exchangeRateType);
	}

    /**
     * 获取项目
     *
     * @return 项目.ID
     */
	public String getProject() {
		return get("project");
	}

    /**
     * 设置项目
     *
     * @param project 项目.ID
     */
	public void setProject(String project) {
		set("project", project);
	}

    /**
     * 获取费用项目
     *
     * @return 费用项目.ID
     */
	public Long getExpenseitem() {
		return get("expenseitem");
	}

    /**
     * 设置费用项目
     *
     * @param expenseitem 费用项目.ID
     */
	public void setExpenseitem(Long expenseitem) {
		set("expenseitem", expenseitem);
	}

    /**
     * 获取部门
     *
     * @return 部门.ID
     */
	public String getDept() {
		return get("dept");
	}

    /**
     * 设置部门
     *
     * @param dept 部门.ID
     */
	public void setDept(String dept) {
		set("dept", dept);
	}

	/**
	 * 获取业务员
	 *
	 * @return 业务员.ID
	 */
	public String getOperator() {
		return get("operator");
	}

	/**
	 * 设置业务员
	 *
	 * @param operator 业务员.ID
	 */
	public void setOperator(String operator) {
		set("operator", operator);
	}

	/**
	 * 获取税种
	 *
	 * @return 税种.ID
	 */
	public String getTaxCategory() {
		return get("taxCategory");
	}

	/**
	 * 设置税种
	 *
	 * @param taxCategory 税种.ID
	 */
	public void setTaxCategory(String taxCategory) {
		set("taxCategory", taxCategory);
	}


    /**
     * 获取税率
     *
     * @return 税率
     */
	public java.math.BigDecimal getTaxRate() {
		return get("taxRate");
	}

    /**
     * 设置税率
     *
     * @param taxRate 税率
     */
	public void setTaxRate(java.math.BigDecimal taxRate) {
		set("taxRate", taxRate);
	}

    /**
     * 获取税额
     *
     * @return 税额
     */
	public java.math.BigDecimal getTaxSum() {
		return get("taxSum");
	}

    /**
     * 设置税额
     *
     * @param taxSum 税额
     */
	public void setTaxSum(java.math.BigDecimal taxSum) {
		set("taxSum", taxSum);
	}

    /**
     * 获取摘要
     *
     * @return 摘要
     */
	public String getDescription() {
		return get("description");
	}

    /**
     * 设置摘要
     *
     * @param description 摘要
     */
	public void setDescription(String description) {
		set("description", description);
	}

    /**
     * 获取来源单据号
     *
     * @return 来源单据号
     */
	public String getSrcbillno() {
		return get("srcbillno");
	}

    /**
     * 设置来源单据号
     *
     * @param srcbillno 来源单据号
     */
	public void setSrcbillno(String srcbillno) {
		set("srcbillno", srcbillno);
	}

    /**
     * 获取来源单据行号
     *
     * @return 来源单据行号
     */
	public String getSrcbillitemno() {
		return get("srcbillitemno");
	}

    /**
     * 设置来源单据行号
     *
     * @param srcbillitemno 来源单据行号
     */
	public void setSrcbillitemno(String srcbillitemno) {
		set("srcbillitemno", srcbillitemno);
	}

    /**
     * 获取源头单据号
     *
     * @return 源头单据号
     */
	public String getTopsrcbillno() {
		return get("topsrcbillno");
	}

    /**
     * 设置源头单据号
     *
     * @param topsrcbillno 源头单据号
     */
	public void setTopsrcbillno(String topsrcbillno) {
		set("topsrcbillno", topsrcbillno);
	}

    /**
     * 获取来源单据id
     *
     * @return 来源单据id
     */
	public String getSrcbillid() {
		return get("srcbillid");
	}

    /**
     * 设置来源单据id
     *
     * @param srcbillid 来源单据id
     */
	public void setSrcbillid(String srcbillid) {
		set("srcbillid", srcbillid);
	}

    /**
     * 获取生单关联主表id
     *
     * @return 生单关联主表id
     */
	public String getPushsrcbillmid() {
		return get("pushsrcbillmid");
	}

    /**
     * 设置生单关联主表id
     *
     * @param pushsrcbillmid 生单关联主表id
     */
	public void setPushsrcbillmid(String pushsrcbillmid) {
		set("pushsrcbillmid", pushsrcbillmid);
	}

    /**
     * 获取Y租户Id
     *
     * @return Y租户Id
     */
	public String getYtenantId() {
		return get("ytenantId");
	}

    /**
     * 设置Y租户Id
     *
     * @param ytenantId Y租户Id
     */
	public void setYtenantId(String ytenantId) {
		set("ytenantId", ytenantId);
	}

    /**
     * 获取上游单据主表id
     *
     * @return 上游单据主表id
     */
	public Long getSourceid() {
		return get("sourceid");
	}

    /**
     * 设置上游单据主表id
     *
     * @param sourceid 上游单据主表id
     */
	public void setSourceid(Long sourceid) {
		set("sourceid", sourceid);
	}

    /**
     * 获取上游单据子表id
     *
     * @return 上游单据子表id
     */
	public Long getSourceautoid() {
		return get("sourceautoid");
	}

    /**
     * 设置上游单据子表id
     *
     * @param sourceautoid 上游单据子表id
     */
	public void setSourceautoid(Long sourceautoid) {
		set("sourceautoid", sourceautoid);
	}

    /**
     * 获取上游单据类型
     *
     * @return 上游单据类型
     */
	public String getSource() {
		return get("source");
	}

    /**
     * 设置上游单据类型
     *
     * @param source 上游单据类型
     */
	public void setSource(String source) {
		set("source", source);
	}

    /**
     * 获取上游单据号
     *
     * @return 上游单据号
     */
	public String getUpcode() {
		return get("upcode");
	}

    /**
     * 设置上游单据号
     *
     * @param upcode 上游单据号
     */
	public void setUpcode(String upcode) {
		set("upcode", upcode);
	}

    /**
     * 获取生单规则编号
     *
     * @return 生单规则编号
     */
	public String getMakeRuleCode() {
		return get("makeRuleCode");
	}

    /**
     * 设置生单规则编号
     *
     * @param makeRuleCode 生单规则编号
     */
	public void setMakeRuleCode(String makeRuleCode) {
		set("makeRuleCode", makeRuleCode);
	}

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
	public java.util.Date getSourceMainPubts() {
		return get("sourceMainPubts");
	}

    /**
     * 设置时间戳
     *
     * @param sourceMainPubts 时间戳
     */
	public void setSourceMainPubts(java.util.Date sourceMainPubts) {
		set("sourceMainPubts", sourceMainPubts);
	}

    /**
     * 获取分组任务KEY
     *
     * @return 分组任务KEY
     */
	public String getGroupTaskKey() {
		return get("groupTaskKey");
	}

    /**
     * 设置分组任务KEY
     *
     * @param groupTaskKey 分组任务KEY
     */
	public void setGroupTaskKey(String groupTaskKey) {
		set("groupTaskKey", groupTaskKey);
	}

    /**
     * 获取序号
     *
     * @return 序号
     */
	public Integer getRowno() {
		return get("rowno");
	}

    /**
     * 设置序号
     *
     * @param rowno 序号
     */
	public void setRowno(Integer rowno) {
		set("rowno", rowno);
	}

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
	public java.util.Date getPubts() {
		return get("pubts");
	}

    /**
     * 设置时间戳
     *
     * @param pubts 时间戳
     */
	public void setPubts(java.util.Date pubts) {
		set("pubts", pubts);
	}
	/**
	 * 获取租户
	 *
	 * @return 租户.ID
	 */
	public Long getTenant() {
		return get("tenant");
	}

	/**
	 * 设置租户
	 *
	 * @param tenant 租户.ID
	 */
	public void setTenant(Long tenant) {
		set("tenant", tenant);
	}

	/**
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public BizObject getCharacterDefb() {
		return get("characterDefb");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param characterDefb 自定义项特征属性组.ID
	 */
	public void setCharacterDefb(BizObject characterDefb) {
		set("characterDefb", characterDefb);
	}

	/**
	 * 获取签名
	 *
	 * @return 签名
	 */
	public String getSignature() {
		return get("signature");
	}

	/**
	 * 设置签名
	 *
	 * @param signature 签名
	 */
	public void setSignature(String signature) {
		set("signature", signature);
	}

	/**
	 * 获取关联状态
	 *
	 * @return 关联状态
	 */
	public Short getAssociationStatus() {
		return getShort("associationStatus");
	}

	/**
	 * 设置关联状态
	 *
	 * @param associationStatus 关联状态
	 */
	public void setAssociationStatus(Short associationStatus) {
		set("associationStatus", associationStatus);
	}


	/**
	 * 获取银行对账单ID
	 *
	 * @return 银行对账单ID
	 */
	public String getBankReconciliationId() {
		return get("bankReconciliationId");
	}

	/**
	 * 设置银行对账单ID
	 *
	 * @param bankReconciliationId 银行对账单ID
	 */
	public void setBankReconciliationId(String bankReconciliationId) {
		set("bankReconciliationId", bankReconciliationId);
	}

	/**
	 * 获取认领单ID
	 *
	 * @return 认领单ID
	 */
	public String getBillClaimId() {
		return get("billClaimId");
	}

	/**
	 * 设置认领单ID
	 *
	 * @param billClaimId 认领单ID
	 */
	public void setBillClaimId(String billClaimId) {
		set("billClaimId", billClaimId);
	}

	/**
	 * 获取成本中心
	 *
	 * @return 成本中心.ID
	 */
	public Long getCostcenter() {
		return get("costcenter");
	}

	/**
	 * 设置成本中心
	 *
	 * @param costcenter 成本中心.ID
	 */
	public void setCostcenter(Long costcenter) {
		set("costcenter", costcenter);
	}

	/**
	 * 获取利润中心
	 *
	 * @return 利润中心.ID
	 */
	public String getProfitcenter() {
		return get("profitcenter");
	}

	/**
	 * 设置利润中心
	 *
	 * @param profitcenter 利润中心.ID
	 */
	public void setProfitcenter(String profitcenter) {
		set("profitcenter", profitcenter);
	}


	/**
	 * 获取收款方开户行id
	 *
	 * @return 收款方开户行id
	 */
	public String getOppositeOpenBankId() {
		return get("oppositeOpenBankId");
	}

	/**
	 * 设置收款方开户行id
	 *
	 * @param oppositeOpenBankId 收款方开户行id
	 */
	public void setOppositeOpenBankId(String oppositeOpenBankId) {
		set("oppositeOpenBankId", oppositeOpenBankId);
	}

	/**
	 * 获取收款方开户行名称
	 *
	 * @return 收款方开户行名称
	 */
	public String getOppositeOpenBankName() {
		return get("oppositeOpenBankName");
	}

	/**
	 * 设置收款方开户行名称
	 *
	 * @param oppositeOpenBankName 收款方开户行名称
	 */
	public void setOppositeOpenBankName(String oppositeOpenBankName) {
		set("oppositeOpenBankName", oppositeOpenBankName);
	}

	/**
	 * 获取结算成功时间
	 *
	 * @return 结算成功时间
	 */
	public java.util.Date getSettleSuccessTime() {
		return get("settleSuccessTime");
	}

	/**
	 * 设置结算成功时间
	 *
	 * @param settleSuccessTime 结算成功时间
	 */
	public void setSettleSuccessTime(java.util.Date settleSuccessTime) {
		set("settleSuccessTime", settleSuccessTime);
	}

	/**
	 * 获取资金计划项目
	 *
	 * @return 资金计划项目
	 */
	public String getFundPlanProject() {
		if(this.get("fundPlanProject") instanceof Long){
			return this.get("fundPlanProject").toString();
		}
		return this.get("fundPlanProject");
	}


	/**
	 * 设置资金计划项目
	 *
	 * @param fundPlanProject 资金计划项目
	 */
	public void setFundPlanProject(String fundPlanProject) {
		this.set("fundPlanProject", fundPlanProject);
	}


	/**
	 * 获取付款虚拟账户
	 *
	 * @return 付款虚拟账户.ID
	 */
	public String getThirdParVirtAccount() {
		return get("thirdParVirtAccount");
	}

	/**
	 * 设置付款虚拟账户
	 *
	 * @param thirdParVirtAccount 付款虚拟账户.ID
	 */
	public void setThirdParVirtAccount(String thirdParVirtAccount) {
		set("thirdParVirtAccount", thirdParVirtAccount);
	}

	/**
	 * 获取是否结算
	 *
	 * @return 是否结算
	 */
	public Integer getWhetherSettle() {
		return get("whetherSettle");
	}

	/**
	 * 设置是否结算
	 *
	 * @param whetherSettle 是否结算
	 */
	public void setWhetherSettle(Integer whetherSettle) {
		set("whetherSettle", whetherSettle);
	}

	/**
	 * 获取是否委托驳回
	 *
	 * @return 是否委托驳回
	 */
	public Integer getEntrustReject() {
		return get("entrustReject");
	}

	/**
	 * 设置是否委托驳回
	 *
	 * @param entrustReject 是否委托驳回
	 */
	public void setEntrustReject(Integer entrustReject) {
		set("entrustReject", entrustReject);
	}

	/**
	 * 获取资金结算明细id
	 *
	 * @return 资金结算明细id
	 */
	public String getSettledId() {
		return get("settledId");
	}

	/**
	 * 设置资金结算明细id
	 *
	 * @param settledId 资金结算明细id
	 */
	public void setSettledId(String settledId) {
		set("settledId", settledId);
	}

	/**
	 * 获取是否要占用资金计划
	 *
	 * @return 是否要占用资金计划
	 */
	public Integer getIsToPushCspl() {
		return get("isToPushCspl");
	}

	/**
	 * 设置是否要占用资金计划
	 *
	 * @param isToPushCspl 是否要占用资金计划
	 */
	public void setIsToPushCspl(Integer isToPushCspl) {
		set("isToPushCspl", isToPushCspl);
	}


	/**
	 * 获取租户id
	 *
	 * @return 租户id
	 */
	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	/**
	 * 设置租户id
	 *
	 * @param ytenant 租户id
	 */
	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}


	/**
	 * 获取结息账户
	 *
	 * @return 结息账户.ID
	 */
	public String getInterestSettlementAccount() {
		return get("interestSettlementAccount");
	}

	/**
	 * 设置结息账户
	 *
	 * @param interestSettlementAccount 结息账户.ID
	 */
	public void setInterestSettlementAccount(String interestSettlementAccount) {
		set("interestSettlementAccount", interestSettlementAccount);
	}

	/**
	 * 获取上次结息结束日
	 *
	 * @return 上次结息结束日
	 */
	public java.util.Date getLastInterestSettlementEndDate() {
		return get("lastInterestSettlementEndDate");
	}

	/**
	 * 设置上次结息结束日
	 *
	 * @param lastInterestSettlementEndDate 上次结息结束日
	 */
	public void setLastInterestSettlementEndDate(java.util.Date lastInterestSettlementEndDate) {
		set("lastInterestSettlementEndDate", lastInterestSettlementEndDate);
	}

	/**
	 * 获取本次结息开始日
	 *
	 * @return 本次结息开始日
	 */
	public java.util.Date getCurrentInterestSettlementStartDate() {
		return get("currentInterestSettlementStartDate");
	}

	/**
	 * 设置本次结息开始日
	 *
	 * @param currentInterestSettlementStartDate 本次结息开始日
	 */
	public void setCurrentInterestSettlementStartDate(java.util.Date currentInterestSettlementStartDate) {
		set("currentInterestSettlementStartDate", currentInterestSettlementStartDate);
	}

	/**
	 * 获取本次结息结束日
	 *
	 * @return 本次结息结束日
	 */
	public java.util.Date getCurrentInterestSettlementEndDate() {
		return get("currentInterestSettlementEndDate");
	}

	/**
	 * 设置本次结息结束日
	 *
	 * @param currentInterestSettlementEndDate 本次结息结束日
	 */
	public void setCurrentInterestSettlementEndDate(java.util.Date currentInterestSettlementEndDate) {
		set("currentInterestSettlementEndDate", currentInterestSettlementEndDate);
	}

	/**
	 * 获取已预提金额
	 *
	 * @return 已预提金额
	 */
	public java.math.BigDecimal getWithholdingOriSum() {
		return get("withholdingOriSum");
	}

	/**
	 * 设置已预提金额
	 *
	 * @param withholdingOriSum 已预提金额
	 */
	public void setWithholdingOriSum(java.math.BigDecimal withholdingOriSum) {
		set("withholdingOriSum", withholdingOriSum);
	}

	/**
	 * 获取已预提本币金额
	 *
	 * @return 已预提本币金额
	 */
	public java.math.BigDecimal getWithholdingNatSum() {
		return get("withholdingNatSum");
	}

	/**
	 * 设置已预提本币金额
	 *
	 * @param withholdingNatSum 已预提本币金额
	 */
	public void setWithholdingNatSum(java.math.BigDecimal withholdingNatSum) {
		set("withholdingNatSum", withholdingNatSum);
	}

	/**
	 * 获取收款方swift码
	 *
	 * @return 收款方swift码
	 */
	public String getReceivePartySwift() {
		return get("receivePartySwift");
	}

	/**
	 * 设置收款方swift码
	 *
	 * @param receivePartySwift 收款方swift码
	 */
	public void setReceivePartySwift(String receivePartySwift) {
		set("receivePartySwift", receivePartySwift);
	}


	/**
	 * 获取风险类型
	 *
	 * @return 风险类型
	 */
	public String getRiskPayType() {
		return get("riskPayType");
	}

	/**
	 * 设置风险类型
	 *
	 * @param riskPayType 风险类型
	 */
	public void setRiskPayType(String riskPayType) {
		set("riskPayType", riskPayType);
	}
	/**
	 * 付款模式
	 *
	 * @return 付款模式
	 */
	public Short getPaymentMode() {
		return getShort("paymentMode");
	}

	/**
	 * 付款模式
	 *
	 * @param paymentMode 付款模式
	 */
	public void setPaymentMode(Short paymentMode) {
		set("paymentMode", paymentMode);
	}

	/**
	 * 获取无税金额(付款含税)
	 *
	 * @return 无税金额(付款含税)
	 */
	public java.math.BigDecimal getUnTaxSum() {
		return get("unTaxSum");
	}

	/**
	 * 设置无税金额(付款含税)
	 *
	 * @param unTaxSum 无税金额(付款含税)
	 */
	public void setUnTaxSum(java.math.BigDecimal unTaxSum) {
		set("unTaxSum", unTaxSum);
	}

	/**
	 * 获取税额(付款含税)
	 *
	 * @return 税额(付款含税)
	 */
	public java.math.BigDecimal getIncludeTaxSum() {
		return get("includeTaxSum");
	}

	/**
	 * 设置税额(付款含税)
	 *
	 * @param includeTaxSum 税额(付款含税)
	 */
	public void setIncludeTaxSum(java.math.BigDecimal includeTaxSum) {
		set("includeTaxSum", includeTaxSum);
	}

	/**
	 * 获取是否分包流转
	 *
	 * @return 是否分包流转
	 */
	public Boolean getIsSubcontract() {
		return getBoolean("isSubcontract");
	}

	/**
	 * 设置是否分包流转
	 *
	 * @param isSubcontract 是否分包流转
	 */
	public void setIsSubcontract(Boolean isSubcontract) {
		set("isSubcontract", isSubcontract);
	}

	/**
	 * 获取单笔可操作金额
	 *
	 * @return 单笔可操作金额
	 */
	public BigDecimal getOneAvailableMoney() {
		return get("oneAvailableMoney");
	}

	/**
	 * 设置单笔可操作金额
	 *
	 * @param oneAvailableMoney 单笔可操作金额
	 */
	public void setOneAvailableMoney(BigDecimal oneAvailableMoney) {
		set("oneAvailableMoney", oneAvailableMoney);
	}

	/**
	 * 获取支付扩展信息
	 *
	 * @return 支付扩展信息
	 */
	public String getPayExtend() {
		return get("payExtend");
	}

	/**
	 * 设置支付扩展信息
	 *
	 * @param payExtend 支付扩展信息
	 */
	public void setPayExtend(String payExtend) {
		set("payExtend", payExtend);
	}

	/**
	 * 获取支票用途
	 *
	 * @return 支票用途
	 */
	public CheckPurpose getCheckPurpose() {
		Number v = getShort("checkPurpose");
		return CheckPurpose.find(v);
	}

	/**
	 * 设置支票用途
	 *
	 * @param checkPurpose 支票用途
	 */
	public void setCheckPurpose(CheckPurpose checkPurpose) {
		if (checkPurpose != null) {
			set("checkPurpose", checkPurpose.getValue());
		} else {
			set("checkPurpose", null);
		}
	}

	/**
	 * 获取是否占预算
	 *
	 * @return 是否占预算
	 */
	public Short getIsOccupyBudget() {
		return getShort("isOccupyBudget");
	}

	/**
	 * 设置是否占预算
	 *
	 * @param isOccupyBudget 是否占预算
	 */
	public void setIsOccupyBudget(Short isOccupyBudget) {
		set("isOccupyBudget", isOccupyBudget);
	}

	/**
	 * 获取行号
	 *
	 * @return 行号
	 */
	public java.math.BigDecimal getLineno() {
		return get("lineno");
	}

	/**
	 * 设置行号
	 *
	 * @param lineno 行号
	 */
	public void setLineno(java.math.BigDecimal lineno) {
		set("lineno", lineno);
	}

	/**
	 * 获取协同单据编号
	 *
	 * @return 协同单据编号
	 */
	public String getSynergybillno() {
		return get("synergybillno");
	}

	/**
	 * 设置协同单据编号
	 *
	 * @param synergybillno 协同单据编号
	 */
	public void setSynergybillno(String synergybillno) {
		set("synergybillno", synergybillno);
	}

	/**
	 * 获取协同单据行号
	 *
	 * @return 协同单据行号
	 */
	public String getSynergybillitemno() {
		return get("synergybillitemno");
	}

	/**
	 * 设置协同单据行号
	 *
	 * @param synergybillitemno 协同单据行号
	 */
	public void setSynergybillitemno(String synergybillitemno) {
		set("synergybillitemno", synergybillitemno);
	}

	/**
	 * 获取协同单据id
	 *
	 * @return 协同单据id
	 */
	public String getSynergybillid() {
		return get("synergybillid");
	}

	/**
	 * 设置协同单据id
	 *
	 * @param synergybillid 协同单据id
	 */
	public void setSynergybillid(String synergybillid) {
		set("synergybillid", synergybillid);
	}

	/**
	 * 获取是否协同发起方
	 *
	 * @return 是否协同发起方
	 */
	public Boolean getIssynergy() {
		return getBoolean("issynergy");
	}

	/**
	 * 设置是否协同发起方
	 *
	 * @param issynergy 是否协同发起方
	 */
	public void setIssynergy(Boolean issynergy) {
		set("issynergy", issynergy);
	}


	/**
	 * 获取对公对私
	 *
	 * @return 对公对私
	 */
	public String getPublicPrivate() {
		return getString("publicPrivate");
	}

	/**
	 * 设置对公对私
	 *
	 * @param publicPrivate 对公对私
	 */
	public void setPublicPrivate(String publicPrivate) {
		set("publicPrivate", publicPrivate);
	}

	/**
	 * 获取是否已退票重付
	 *
	 * @return 是否已退票重付
	 */
	public Integer getWhetherRefundAndRepayment() {
		return get("whetherRefundAndRepayment");
	}

	/**
	 * 设置是否已退票重付
	 *
	 * @param whetherRefundAndRepayment 是否已退票重付
	 */
	public void setWhetherRefundAndRepayment(Integer whetherRefundAndRepayment) {
		set("whetherRefundAndRepayment", whetherRefundAndRepayment);
	}

	/**
	 * 获取退票关联付款单id
	 *
	 * @return 退票关联付款单id
	 */
	public String getRefundAssociatedPaymentId() {
		return get("refundAssociatedPaymentId");
	}

	/**
	 * 设置退票关联付款单id
	 *
	 * @param refundAssociatedPaymentId 退票关联付款单id
	 */
	public void setRefundAssociatedPaymentId(String refundAssociatedPaymentId) {
		set("refundAssociatedPaymentId", refundAssociatedPaymentId);
	}

	/**
	 * 获取是否统收统支
	 *
	 * @return 是否统收统支
	 */
	public Boolean getIsIncomeAndExpenditure() {
		return getBoolean("isIncomeAndExpenditure");
	}

	/**
	 * 设置是否统收统支
	 *
	 * @param isIncomeAndExpenditure 是否统收统支
	 */
	public void setIsIncomeAndExpenditure(Boolean isIncomeAndExpenditure) {
		set("isIncomeAndExpenditure", isIncomeAndExpenditure);
	}

	/**
	 * 获取统收统支关系组
	 *
	 * @return 统收统支关系组.ID
	 */
	public Long getIncomeAndExpendRelationGroup() {
		return get("incomeAndExpendRelationGroup");
	}

	/**
	 * 设置统收统支关系组
	 *
	 * @param incomeAndExpendRelationGroup 统收统支关系组.ID
	 */
	public void setIncomeAndExpendRelationGroup(Long incomeAndExpendRelationGroup) {
		set("incomeAndExpendRelationGroup", incomeAndExpendRelationGroup);
	}

	/**
	 * 获取统收统支银行账户
	 *
	 * @return 统收统支银行账户.ID
	 */
	public String getIncomeAndExpendBankAccount() {
		return get("incomeAndExpendBankAccount");
	}

	/**
	 * 设置统收统支银行账户
	 *
	 * @param incomeAndExpendBankAccount 统收统支银行账户.ID
	 */
	public void setIncomeAndExpendBankAccount(String  incomeAndExpendBankAccount) {
		set("incomeAndExpendBankAccount", incomeAndExpendBankAccount);
	}

	/**
	 * 获取结算币种id
	 *
	 * @return 结算币种id.ID
	 */
	public String getSettleCurrency() {
		return get("settleCurrency");
	}

	/**
	 * 设置结算币种id
	 *
	 * @param settleCurrency 结算币种id.ID
	 */
	public void setSettleCurrency(String settleCurrency) {
		set("settleCurrency", settleCurrency);
	}

	/**
	 * 获取换出汇率类型
	 *
	 * @return 换出汇率类型.ID
	 */
	public String getSwapOutExchangeRateType() {
		return get("swapOutExchangeRateType");
	}

	/**
	 * 设置换出汇率类型
	 *
	 * @param swapOutExchangeRateType 换出汇率类型.ID
	 */
	public void setSwapOutExchangeRateType(String swapOutExchangeRateType) {
		set("swapOutExchangeRateType", swapOutExchangeRateType);
	}

	/**
	 * 获取换出汇率预估
	 *
	 * @return 换出汇率预估
	 */
	public java.math.BigDecimal getSwapOutExchangeRateEstimate() {
		return get("swapOutExchangeRateEstimate");
	}

	/**
	 * 设置换出汇率预估
	 *
	 * @param swapOutExchangeRateEstimate 换出汇率预估
	 */
	public void setSwapOutExchangeRateEstimate(java.math.BigDecimal swapOutExchangeRateEstimate) {
		set("swapOutExchangeRateEstimate", swapOutExchangeRateEstimate);
	}

	/**
	 * 获取换出金额预估
	 *
	 * @return 换出金额预估
	 */
	public java.math.BigDecimal getSwapOutAmountEstimate() {
		return get("swapOutAmountEstimate");
	}

	/**
	 * 设置换出金额预估
	 *
	 * @param swapOutAmountEstimate 换出金额预估
	 */
	public void setSwapOutAmountEstimate(java.math.BigDecimal swapOutAmountEstimate) {
		set("swapOutAmountEstimate", swapOutAmountEstimate);
	}

	/**
	 * 获取实际结算汇率
	 *
	 * @return 实际结算汇率
	 */
	public java.math.BigDecimal getActualSettlementExchangeRate() {
		return get("actualSettlementExchangeRate");
	}

	/**
	 * 设置实际结算汇率
	 *
	 * @param actualSettlementExchangeRate 实际结算汇率
	 */
	public void setActualSettlementExchangeRate(java.math.BigDecimal actualSettlementExchangeRate) {
		set("actualSettlementExchangeRate", actualSettlementExchangeRate);
	}

	/**
	 * 获取实际结算金额
	 *
	 * @return 实际结算金额
	 */
	public java.math.BigDecimal getActualSettlementAmount() {
		return get("actualSettlementAmount");
	}

	/**
	 * 设置实际结算金额
	 *
	 * @param actualSettlementAmount 实际结算金额
	 */
	public void setActualSettlementAmount(java.math.BigDecimal actualSettlementAmount) {
		set("actualSettlementAmount", actualSettlementAmount);
	}

	/**
	 * 获取实际结算汇率类型
	 *
	 * @return 实际结算汇率类型
	 */
	public String getActualSettlementExchangeRateType() {
		return get("actualSettlementExchangeRateType");
	}

	/**
	 * 设置实际结算汇率类型
	 *
	 * @param actualSettlementExchangeRateType 实际结算汇率类型
	 */
	public void setActualSettlementExchangeRateType(String actualSettlementExchangeRateType) {
		set("actualSettlementExchangeRateType", actualSettlementExchangeRateType);
	}

	/**
	 * 获取实际结算账户
	 *
	 * @return 实际结算账户.ID
	 */
	public String getActualSettleAccount() {
		return get("actualSettleAccount");
	}

	/**
	 * 设置实际结算账户
	 *
	 * @param actualSettleAccount 实际结算账户.ID
	 */
	public void setActualSettleAccount(String actualSettleAccount) {
		set("actualSettleAccount", actualSettleAccount);
	}

	/**
	 * 获取轧差识别码
	 *
	 * @return 轧差识别码
	 */
	private String netIdentificateCode;



	/**
	 * 获取轧差结算总笔数
	 *
	 * @return 轧差结算总笔数
	 */
	private Short netSettleCount;


	/**
	 * 获取轧差后金额
	 *
	 * @return 轧差后金额
	 */
	private java.math.BigDecimal afterNetAmt;


	/**
	 * 获取轧差后收付方向
	 *
	 * @return 轧差后收付方向
	 */
	private Short afterNetDir;

	public String getNetIdentificateCode() {
		return get("netIdentificateCode");
	}

	public void setNetIdentificateCode(String netIdentificateCode) {
		set("netIdentificateCode", netIdentificateCode);
	}

	public Short getNetSettleCount() {
		return get("netSettleCount");
	}

	public void setNetSettleCount(Short netSettleCount) {
		set("netSettleCount", netSettleCount);
	}

	public BigDecimal getAfterNetAmt() {
		return get("afterNetAmt");
	}

	public void setAfterNetAmt(BigDecimal afterNetAmt) {
		set("afterNetAmt", afterNetAmt);
	}

	public Short getAfterNetDir() {
		return get("afterNetDir");
	}

	public void setAfterNetDir(Short afterNetDir) {
		set("afterNetDir", afterNetDir);
	}

	/**
	 * 获取汇率折算方式
	 *
	 * @return 汇率折算方式
	 */
	public Short getExchangerateOps() {
		return getShort("exchangerateOps");
	}

	/**
	 * 设置汇率折算方式
	 *
	 * @param exchangerateOps 汇率折算方式
	 */
	public void setExchangerateOps(Short exchangerateOps) {
		set("exchangerateOps", exchangerateOps);
	}

}
