package com.yonyoucloud.fi.cmp.fundpayment;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalFlow;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IPrintCount;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 资金付款主表实体
 *
 * @author u
 * @version 1.0
 */
public class FundPayment extends BizObject implements IAuditInfo, ITenant, ICurrency, IApprovalFlow, IPrintCount, IYTenant, AccentityRawInterface {
	/**
	 * 实体全称
	 */
	public static final String ENTITY_NAME = "cmp.fundpayment.FundPayment";
	private static final long serialVersionUID = -4823545931635284839L;
	// 业务对象编码
	public static final String BUSI_OBJ_CODE = "ctm-cmp.cmp_fundpayment";
	public static final String SIGNATURE = "signature";
	public static final String MAINID = "mainid";
	public static final String FUNDBUSINOBJTYPEID = "fundbusinobjtypeid";
	public static final String QUICKTYPE = "quickType";
	public static final String QUICKTYPECODE = "quickTypeCode";
	public static final String SMARTCHECKNO = "smartcheckno";
	public static final String ROWNO = "rowno";
	public static final String ORISUM = "oriSum";
	public static final String NORIREMAINAMOUNT = "noriremainamount";
	public static final String ORITRANSITAMOUNT = "oritransitamount";
	public static final String NATSUM = "natSum";
	public static final String SETTLESUCCESSSUM = "settlesuccessSum";
	public static final String SETTLEERRORSUM = "settleerrorSum";
	public static final String DZDATE = "dzdate";
	public static final String CURRENCY = "currency";
	public static final String BIZOBJTYPE = "bizobjtype";
	public static final String NATCURRENCY = "natcurrency";
	public static final String SETTLEMODE = "settlemode";
	public static final String ENTERPRISEBANKACCOUNT = "enterprisebankaccount";
	public static final String CASHACCOUNT = "cashaccount";
	public static final String NOTETYPE = "notetype";
	public static final String NOTENO = "noteno";
	public static final String NOTETEXTNO = "notetextno";
	public static final String NOTEDIRECTION = "noteDirection";
	public static final String NOTESUM = "noteSum";
	public static final String CAOBJECT = "caobject";
	public static final String CUSTOMER = "customer";
	public static final String SUPPLIER = "supplier";
	public static final String EMPLOYEE = "employee";
	public static final String OPPOSITEACCOUNTID = "oppositeaccountid";
	public static final String OPPOSITEOBJECTID = "oppositeobjectid";
	public static final String OPPOSITEOBJECTNAME = "oppositeobjectname";
	public static final String OPPOSITEACCOUNTNAME = "oppositeaccountname";
	public static final String OPPOSITEBANKADDRID = "oppositebankaddrid";
	public static final String OPPOSITEACCOUNTNO = "oppositeaccountno";
	public static final String OPPOSITEBANKADDR = "oppositebankaddr";
	public static final String OPPOSITEBANKTYPE = "oppositebankType";
	public static final String OPPOSITEBANKLINENO = "oppositebanklineno";
	public static final String TRANSNUMBER = "transNumber";
	public static final String SETTLESTATUS = "settlestatus";
	public static final String REJECTTYPE = "rejecttype";
	public static final String REJECTREMARK = "rejectremark";
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
	public static final String SOURCEID = "sourceid";
	public static final String SOURCEAUTOID = "sourceautoid";
	public static final String SOURCE = "source";
	public static final String UPCODE = "upcode";
	public static final String MAKERULECODE = "makeRuleCode";
	public static final String SOURCEMAINPUBTS = "sourceMainPubts";
	public static final String GROUPTASKKEY = "groupTaskKey";
	public static final String PUBTS = "pubts";
	public static final String TENANT = "tenant";
	public static final String ASSOCIATIONSTATUS = "associationStatus";
	public static final String BANKRECONCILIATIONID = "bankReconciliationId";
	public static final String BILLCLAIMID = "billClaimId";
	public static final String CHARACTERDEFB = "characterDefb";
	public static final String COSTCENTER = "costcenter";
	public static final String PROFITCENTER = "profitcenter";
	public static final String OPPOSITEOPENBANKID = "oppositeOpenBankId";
	public static final String OPPOSITEOPENBANKNAME = "oppositeOpenBankName";
	public static final String SETTLESUCCESSTIME = "settleSuccessTime";
	public static final String FUNDPLANPROJECT = "fundPlanProject";
	public static final String THIRDPARVIRTACCOUNT = "thirdParVirtAccount";
	public static final String WHETHERSETTLE = "whetherSettle";
	public static final String ISTOPUSHCSPL = "isToPushCspl";
	public static final String YTENANT = "ytenant";
	public static final String ENTRUSTREJECT = "entrustReject";
	public static final String SETTLEDID = "settledId";
	public static final String INTERESTSETTLEMENTACCOUNT = "interestSettlementAccount";
	public static final String LASTINTERESTSETTLEMENTENDDATE = "lastInterestSettlementEndDate";
	public static final String CURRENTINTERESTSETTLEMENTSTARTDATE = "currentInterestSettlementStartDate";
	public static final String CURRENTINTERESTSETTLEMENTENDDATE = "currentInterestSettlementEndDate";
	public static final String WITHHOLDINGORISUM = "withholdingOriSum";
	public static final String WITHHOLDINGNATSUM = "withholdingNatSum";
	public static final String RECEIVEPARTYSWIFT = "receivePartySwift";
	public static final String UNTAXSUM = "unTaxSum";
	public static final String INCLUDETAXSUM = "includeTaxSum";
	public static final String CHECKPURPOSE = "checkPurpose";
	public static final String CHECKNO = "checkno";
	public static final String CHECKID = "checkId";
	public static final String LINENO = "lineno";
	public static final String SYNERGYBILLNO = "synergybillno";
	public static final String SYNERGYBILLITEMNO = "synergybillitemno";
	public static final String SYNERGYBILLID = "synergybillid";
	public static final String ISSYNERGY = "issynergy";
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
	public static final String FUNDPLANPROJECTDETAIL = "fundPlanProjectDetail";
	public static final String FUNDPLANPROJECTDETAILPAY = "fundPlanProjectDetailPay";
	public static final String ISOCCUPYBUDGET = "isOccupyBudget";
	public static final String EXCHANGERATEOPS = "exchangerateOps";


	/**
	 * 获取核算会计
	 *
	 * @return 核算会计.ID
	 */
	@Override
	public String getAccentityRaw() {
		return get("accentityRaw");
	}

	/**
	 * 设置核算会计
	 *
	 * @param accentityRaw 核算会计.ID
	 */
	@Override
	public void setAccentityRaw(String accentityRaw) {
		set("accentityRaw", accentityRaw);
	}

	/**
	 * 获取资金组织
	 *
	 * @return 资金组织.ID
	 */
	@Override
	public String getAccentity() {
		return get("accentity");
	}

	/**
	 * 设置资金组织
	 *
	 * @param accentity 资金组织.ID
	 */
	@Override
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}

    /**
     * 获取事项来源
     *
     * @return 事项来源
     */
	public EventSource getSrcitem() {
		Number v = getShort("srcitem");
		return EventSource.find(v);
	}

    /**
     * 设置事项来源
     *
     * @param srcitem 事项来源
     */
	public void setSrcitem(EventSource srcitem) {
		if (srcitem != null) {
			set("srcitem", srcitem.getValue());
		} else {
			set("srcitem", null);
		}
	}

    /**
     * 获取事项类型
     *
     * @return 事项类型
     */
	public EventType getBilltype() {
		Number v = getShort("billtype");
		return EventType.find(v);
	}

    /**
     * 设置事项类型
     *
     * @param billtype 事项类型
     */
	public void setBilltype(EventType billtype) {
		if (billtype != null) {
			set("billtype", billtype.getValue());
		} else {
			set("billtype", null);
		}
	}

    /**
     * 获取业务组织
     *
     * @return 业务组织.ID
     */
	public String getOrg() {
		return get("org");
	}

    /**
     * 设置业务组织
     *
     * @param org 业务组织.ID
     */
	public void setOrg(String org) {
		set("org", org);
	}

	/**
	 * 获取确认状态
	 *
	 * @return 确认状态
	 */
	public Short getConfirmstatus() {
		return getShort("confirmstatus");
	}

	/**
	 * 设置确认状态
	 *
	 * @param confirmstatus 确认状态
	 */
	public void setConfirmstatus(Short confirmstatus) {
		set("confirmstatus", confirmstatus);
	}


	/**
     * 获取交易类型
     *
     * @return 交易类型.ID
     */
	public String getTradetype() {
		return get("tradetype");
	}

    /**
     * 设置交易类型
     *
     * @param tradetype 交易类型.ID
     */
	public void setTradetype(String tradetype) {
		set("tradetype", tradetype);
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
     * 获取审批状态
     *
     * @return 审批状态
     */
	public AuditStatus getAuditstatus() {
		Number v = getShort("auditstatus");
		return AuditStatus.find(v);
	}

    /**
     * 设置审批状态
     *
     * @param auditstatus 审批状态
     */
	public void setAuditstatus(AuditStatus auditstatus) {
		if (auditstatus != null) {
			set("auditstatus", auditstatus.getValue());
		} else {
			set("auditstatus", null);
		}
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
     * 获取凭证状态
     *
     * @return 凭证状态
     */
	public VoucherStatus getVoucherstatus() {
		Number v = getShort("voucherstatus");
		return VoucherStatus.find(v);
	}

    /**
     * 设置凭证状态
     *
     * @param voucherstatus 凭证状态
     */
	public void setVoucherstatus(VoucherStatus voucherstatus) {
		if (voucherstatus != null) {
			set("voucherstatus", voucherstatus.getValue());
		} else {
			set("voucherstatus", null);
		}
	}

	/**
	 * 获取收付款对象类型
	 *
	 * @return 收付款对象类型
	 */
	public CaObject getCaobject() {
		Number v = getShort("caobject");
		return CaObject.find(v);
	}

	/**
	 * 设置收付款对象类型
	 *
	 * @param caobject 收付款对象类型
	 */
	public void setCaobject(CaObject caobject) {
		if (caobject != null) {
			set("caobject", caobject.getValue());
		} else {
			set("caobject", null);
		}
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
     * 获取收款银行账户
     *
     * @return 收款银行账户.ID
     */
	public String getEnterprisebankaccount() {
		return get("enterprisebankaccount");
	}

    /**
     * 设置收款银行账户
     *
     * @param enterprisebankaccount 收款银行账户.ID
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
     * 获取客户
     *
     * @return 客户.ID
     */
	public Long getCustomer() {
		return get("customer");
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
		return get("employee(");
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
     * 获取审批流控制
     *
     * @return 审批流控制
     */
	public Boolean getIsWfControlled() {
		return getBoolean("isWfControlled");
	}

    /**
     * 设置审批流控制
     *
     * @param isWfControlled 审批流控制
     */
	public void setIsWfControlled(Boolean isWfControlled) {
		set("isWfControlled", isWfControlled);
	}

    /**
     * 获取审批流状态
     *
     * @return 审批流状态
     */
	public Short getVerifystate() {
		return get("verifystate");
	}

    /**
     * 设置审批流状态
     *
     * @param verifystate 审批流状态
     */
	public void setVerifystate(Short verifystate) {
		set("verifystate", verifystate);
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
	 * 获取付款金额
	 *
	 * @return 付款金额
	 */
	public java.math.BigDecimal getOriSum() {
		return get("oriSum");
	}

	/**
	 * 设置付款金额
	 *
	 * @param oriSum 付款金额
	 */
	public void setOriSum(java.math.BigDecimal oriSum) {
		set("oriSum", oriSum);
	}
	/**
	 * 获取审批人名称
	 *
	 * @return 审批人名称
	 */
	public String getAuditor() {
		return get("auditor");
	}

	/**
	 * 设置审批人名称
	 *
	 * @param auditor 审批人名称
	 */
	public void setAuditor(String auditor) {
		set("auditor", auditor);
	}

	/**
	 * 获取审批人
	 *
	 * @return 审批人.ID
	 */
	public Long getAuditorId() {
		return get("auditorId");
	}

	/**
	 * 设置审批人
	 *
	 * @param auditorId 审批人.ID
	 */
	public void setAuditorId(Long auditorId) {
		set("auditorId", auditorId);
	}

	/**
	 * 获取审批时间
	 *
	 * @return 审批时间
	 */
	public java.util.Date getAuditTime() {
		return get("auditTime");
	}

	/**
	 * 设置审批时间
	 *
	 * @param auditTime 审批时间
	 */
	public void setAuditTime(java.util.Date auditTime) {
		set("auditTime", auditTime);
	}

	/**
	 * 获取审批日期
	 *
	 * @return 审批日期
	 */
	public java.util.Date getAuditDate() {
		return get("auditDate");
	}

	/**
	 * 设置审批日期
	 *
	 * @param auditDate 审批日期
	 */
	public void setAuditDate(java.util.Date auditDate) {
		set("auditDate", auditDate);
	}

    /**
     * 获取备注
     *
     * @return 备注
     */
	public String getDescription() {
		return get("description");
	}

    /**
     * 设置备注
     *
     * @param description 备注
     */
	public void setDescription(String description) {
		set("description", description);
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
     * 获取返回总数
     *
     * @return 返回总数
     */
	public Short getReturncount() {
		return get("returncount");
	}

    /**
     * 设置返回总数
     *
     * @param returncount 返回总数
     */
	public void setReturncount(Short returncount) {
		set("returncount", returncount);
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
     * 获取创建时间
     *
     * @return 创建时间
     */
	public java.util.Date getCreateTime() {
		return get("createTime");
	}

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
	public void setCreateTime(java.util.Date createTime) {
		set("createTime", createTime);
	}

    /**
     * 获取创建日期
     *
     * @return 创建日期
     */
	public java.util.Date getCreateDate() {
		return get("createDate");
	}

    /**
     * 设置创建日期
     *
     * @param createDate 创建日期
     */
	public void setCreateDate(java.util.Date createDate) {
		set("createDate", createDate);
	}

    /**
     * 获取修改时间
     *
     * @return 修改时间
     */
	public java.util.Date getModifyTime() {
		return get("modifyTime");
	}

    /**
     * 设置修改时间
     *
     * @param modifyTime 修改时间
     */
	public void setModifyTime(java.util.Date modifyTime) {
		set("modifyTime", modifyTime);
	}

    /**
     * 获取修改日期
     *
     * @return 修改日期
     */
	public java.util.Date getModifyDate() {
		return get("modifyDate");
	}

    /**
     * 设置修改日期
     *
     * @param modifyDate 修改日期
     */
	public void setModifyDate(java.util.Date modifyDate) {
		set("modifyDate", modifyDate);
	}

    /**
     * 获取创建人名称
     *
     * @return 创建人名称
     */
	public String getCreator() {
		return get("creator");
	}

    /**
     * 设置创建人名称
     *
     * @param creator 创建人名称
     */
	public void setCreator(String creator) {
		set("creator", creator);
	}

    /**
     * 获取修改人名称
     *
     * @return 修改人名称
     */
	public String getModifier() {
		return get("modifier");
	}

    /**
     * 设置修改人名称
     *
     * @param modifier 修改人名称
     */
	public void setModifier(String modifier) {
		set("modifier", modifier);
	}

    /**
     * 获取创建人
     *
     * @return 创建人.ID
     */
	public Long getCreatorId() {
		return getLong("creatorId");
	}

    /**
     * 设置创建人
     *
     * @param creatorId 创建人.ID
     */
	public void setCreatorId(Long creatorId) {
		set("creatorId", creatorId);
	}

    /**
     * 获取修改人
     *
     * @return 修改人.ID
     */
	public Long getModifierId() {
		return get("modifierId");
	}

    /**
     * 设置修改人
     *
     * @param modifierId 修改人.ID
     */
	public void setModifierId(Long modifierId) {
		set("modifierId", modifierId);
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
     * 获取本币
     *
     * @return 本币.ID
     */
	public String getNatCurrency() {
		return get("natCurrency");
	}

    /**
     * 设置本币
     *
     * @param natCurrency 本币.ID
     */
	public void setNatCurrency(String natCurrency) {
		set("natCurrency", natCurrency);
	}

    /**
     * 获取原币
     *
     * @return 原币.ID
     */
	public String getCurrency() {
		return get("currency");
	}

    /**
     * 设置原币
     *
     * @param currency 原币.ID
     */
	public void setCurrency(String currency) {
		set("currency", currency);
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
     * 获取单据日期
     *
     * @return 单据日期
     */
	public java.util.Date getVouchdate() {
		return get("vouchdate");
	}

    /**
     * 设置单据日期
     *
     * @param vouchdate 单据日期
     */
	public void setVouchdate(java.util.Date vouchdate) {
		set("vouchdate", vouchdate);
	}

    /**
     * 获取模板id
     *
     * @return 模板id
     */
	public Long getTplid() {
		return get("tplid");
	}

    /**
     * 设置模板id
     *
     * @param tplid 模板id
     */
	public void setTplid(Long tplid) {
		set("tplid", tplid);
	}

    /**
     * 获取单据状态
     *
     * @return 单据状态
     */
	public Short getStatus() {
	    return getShort("status");
	}

    /**
     * 设置单据状态
     *
     * @param status 单据状态
     */
	public void setStatus(Short status) {
		set("status", status);
	}

    /**
     * 获取编码
     *
     * @return 编码
     */
	public String getCode() {
		return get("code");
	}

    /**
     * 设置编码
     *
     * @param code 编码
     */
	public void setCode(String code) {
		set("code", code);
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
     * 获取资金付款子表集合
     *
     * @return 资金付款子表集合
     */
	public java.util.List<FundPayment_b> FundPayment_b() {
		return getBizObjects("FundPayment_b", FundPayment_b.class);
	}

    /**
     * 设置资金付款子表集合
     *
     * @param FundPayment_b 资金付款子表集合
     */
	public void setFundPayment_b(java.util.List<FundPayment_b> FundPayment_b) {
		setBizObjects("FundPayment_b", FundPayment_b);
	}

	@Override
	public Integer getPrintCount() {
		return get("printCount");
	}

	@Override
	public void setPrintCount(Integer printCount) {
		set("printCount", printCount);
	}

	/**
	 * 获取是否启用商业汇票
	 *
	 * @return 是否启用商业汇票
	 */
	public Boolean getIsEnabledBsd() {
		return getBoolean("isEnabledBsd");
	}

	/**
	 * 设置是否启用商业汇票
	 *
	 * @param isEnabledBsd 是否启用商业汇票
	 */
	public void setIsEnabledBsd(Boolean isEnabledBsd) {
		set("isEnabledBsd", isEnabledBsd);
	}


	/**
	 * 获取凭证号
	 *
	 * @return 凭证号
	 */
	public String getVoucherNo() {
		return get("voucherNo");
	}

	/**
	 * 设置凭证号
	 *
	 * @param voucherNo 凭证号
	 */
	public void setVoucherNo(String voucherNo) {
		set("voucherNo", voucherNo);
	}

	/**
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public BizObject getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param characterDef 自定义项特征属性组.ID
	 */
	public void setCharacterDef(BizObject characterDef) {
		set("characterDef", characterDef);
	}

	/**
	 * 获取凭证期间
	 *
	 * @return 凭证期间
	 */
	public String getVoucherPeriod() {
		return get("voucherPeriod");
	}

	/**
	 * 设置凭证期间
	 *
	 * @param voucherPeriod 凭证期间
	 */
	public void setVoucherPeriod(String voucherPeriod) {
		set("voucherPeriod", voucherPeriod);
	}

	/**
	 * 获取提前入账
	 *
	 * @return 提前入账
	 */
	public Boolean getIsadvanceaccounts() {
		return getBoolean("isadvanceaccounts");
	}

	/**
	 * 设置提前入账
	 *
	 * @param isadvanceaccounts 提前入账
	 */
	public void setIsadvanceaccounts(Boolean isadvanceaccounts) {
		set("isadvanceaccounts", isadvanceaccounts);
	}

	/**
	 * 获取业务关联次数
	 *
	 * @return 业务关联次数
	 */
	public Short getAssociationcount() {
		return getShort("associationcount");
	}

	/**
	 * 设置业务关联次数
	 *
	 * @param associationcount 业务关联次数
	 */
	public void setAssociationcount(Short associationcount) {
		set("associationcount", associationcount);
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
	 * 获取事项分录ID|凭证ID
	 *
	 * @return 事项分录ID|凭证ID
	 */
	public String getVoucherId() {
		return get("voucherId");
	}

	/**
	 * 设置事项分录ID|凭证ID
	 *
	 * @param voucherId 事项分录ID|凭证ID
	 */
	public void setVoucherId(String voucherId) {
		set("voucherId", voucherId);
	}

	/**
	 * 获取委托单位
	 *
	 * @return 委托单位.ID
	 */
	public String getEntrustedUnit() {
		return get("entrustedUnit");
	}

	/**
	 * 设置委托单位
	 *
	 * @param entrustedUnit 委托单位.ID
	 */
	public void setEntrustedUnit(String entrustedUnit) {
		set("entrustedUnit", entrustedUnit);
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
	 * 获取是否存在支付风险
	 *
	 * @return 是否存在支付风险
	 */
	public Boolean getRiskPayFlag() {
		return getBoolean("riskPayFlag");
	}

	/**
	 * 设置是否存在支付风险
	 *
	 * @param riskPayFlag 是否存在支付风险
	 */
	public void setRiskPayFlag(Boolean riskPayFlag) {
		set("riskPayFlag", riskPayFlag);
	}

	/**
	 * 事项生成类型
	 *
	 * @return 事项生成类型
	 */
	public String getClassifier() {
		return get("classifier");
	}

	/**
	 * 事项生成类型
	 *
	 * @param classifier 事项生成类型
	 */
	public void setClassifier(String classifier) {
		set("classifier", classifier);
	}

	/**
	 * 获取是否结算成功后过账
	 *
	 * @return 是否结算成功后过账
	 */
	public Integer getSettleSuccessPost() {
		return get("settleSuccessPost");
	}

	/**
	 * 设置是否结算成功后过账
	 *
	 * @param settleSuccessPost 是否结算成功后过账
	 */
	public void setSettleSuccessPost(Integer settleSuccessPost) {
		set("settleSuccessPost", settleSuccessPost);
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

	public Long getFilesCount() {
		return get("filesCount");
	}

	public void setFilesCount(Long filesCount) {
		set("filesCount", filesCount);
	}

	/**
	 * 获取入账类型
	 *
	 * @return 入账类型
	 */
	public Short getEntrytype() {
		return getShort("entrytype");
	}

	/**
	 * 设置入账类型
	 *
	 * @param entrytype 入账类型
	 */
	public void setEntrytype(Short entrytype) {
		set("entrytype", entrytype);
	}

	/**
	 * 获取是否传资金结算
	 *
	 * @return 是否传资金结算
	 */
	public Short getSettleflag() {
		return getShort("settleflag");
	}

	/**
	 * 设置是否传资金结算
	 *
	 * @param settleflag 是否传资金结算
	 */
	public void setSettleflag(Short settleflag) {
		set("settleflag", settleflag);
	}

	/**
	 * 获取WBS
	 *
	 * @return WBS.ID
	 */
	public String getWbs() {
		return get("wbs");
	}

	/**
	 * 设置WBS
	 *
	 * @param wbs WBS.ID
	 */
	public void setWbs(String wbs) {
		set("wbs", wbs);
	}

	/**
	 * 获取活动
	 *
	 * @return 活动.ID
	 */
	public String getActivity() {
		return get("activity");
	}

	/**
	 * 设置活动
	 *
	 * @param activity 活动.ID
	 */
	public void setActivity(String activity) {
		set("activity", activity);
	}

	/**
	 * 获取升级标识id
	 *
	 * @return 升级标识id
	 */
	public String getMigradeid() {
		return get("migradeid");
	}

	/**
	 * 设置升级标识id
	 *
	 * @param migradeid 升级标识id
	 */
	public void setMigradeid(String migradeid) {
		set("migradeid", migradeid);
	}


	/**
	 * 获取用户的id
	 *
	 * @return 用户的id
	 */
	public String getUserId() {
		return get("userId");
	}

	/**
	 * 设置用户的id
	 *
	 * @param userId 用户的id
	 */
	public void setUserId(String userId) {
		set("userId", userId);
	}

    public Long getVoucherVersion(){return getLong("voucherVersion");}
	public void setVoucherVersion(Long voucherVersion){set("voucherVersion", voucherVersion);}
	public Long getfiEventDataVersion(){return getLong("fiEventDataVersion");}
	public void setFiEventDataVersion(Long fiEventDataVersion) {
		set("fiEventDataVersion", fiEventDataVersion);
	}

}
