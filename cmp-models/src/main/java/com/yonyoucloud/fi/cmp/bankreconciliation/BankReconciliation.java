package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReFundType;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;

import java.math.BigDecimal;

/**
 * 银行对账单实体
 *
 * @author u
 * @version 1.0
 */
public class BankReconciliation extends BizObject implements IAuditInfo, ITenant, AccentityRawInterface {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankreconciliation.BankReconciliation";

	public BankReconciliation(Object id) {
		setId(id);
	}

	public BankReconciliation() {
	}

	// 字段常量
    public static final String DETAIL_RECEIPT_RELATION_CODE = "detailReceiptRelationCode";
    public static final String FROZENCOUNT = "frozencount";
    public static final String ACCENTITY_RAW = "accentityRaw";
    public static final String ACCENTITY = "accentity";
    public static final String ACCT_NAME = "acct_name";
    public static final String BANKOPEN = "bankopen";
    public static final String DZDATE = "dzdate";
    public static final String BANKRECONCILIATIONSCHEME = "bankreconciliationscheme";
    public static final String INITFLAG = "initflag";
    public static final String LIBRARYFLAG = "libraryflag";
    public static final String ISVIRTUALFLOW = "isvirtualflow";
    public static final String VIRTUALFLOWID = "virtualflowid";
    public static final String SRCITEM = "srcitem";
    public static final String BANKACCOUNT = "bankaccount";
    public static final String CURRENCY = "currency";
    public static final String STATEMENTNO = "statementno";
    public static final String BANK_SEQ_NO = "bank_seq_no";
    public static final String THIRDSERIALNO = "thirdserialno";
    public static final String TRAN_DATE = "tran_date";
    public static final String TRAN_TIME = "tran_time";
    public static final String DEBITAMOUNT = "debitamount";
    public static final String CREDITAMOUNT = "creditamount";
    public static final String DC_FLAG = "dc_flag";
    public static final String CASH_FLAG = "cash_flag";
    public static final String TRAN_AMT = "tran_amt";
    public static final String ACCT_BAL = "acct_bal";
    public static final String NOTENO = "noteno";
    public static final String TO_ACCT = "to_acct";
    public static final String TO_ACCT_NO = "to_acct_no";
    public static final String TO_ACCT_NAME = "to_acct_name";
    public static final String TO_ACCT_BANK = "to_acct_bank";
    public static final String TO_ACCT_BANK_NAME = "to_ACCT_BANK_NAME";
    public static final String OPER = "oper";
    public static final String VALUE_DATE = "value_date";
    public static final String USE_NAME = "use_name";
    public static final String REMARK = "remark";
    public static final String CHECKFLAG = "checkflag";
    public static final String OTHER_CHECKFLAG = "other_checkflag";
    public static final String CHECKMAN = "checkman";
    public static final String CHECKDATE = "checkdate";
    public static final String OTHER_CHECKDATE = "other_checkdate";
    public static final String CHECKNO = "checkno";
    public static final String OTHER_CHECKNO = "other_checkno";
    public static final String BANKRECONCILIATIONSETTINGID = "bankreconciliationsettingid";
    public static final String GL_BANKRECONCILIATIONSETTINGID = "gl_bankreconciliationsettingid";
    public static final String RECEIVELINE = "receiveline";
    public static final String PAYMENTLINE = "paymentline";
    public static final String DATA_ORIGIN = "dataOrigin";
    public static final String ENTRUSTEDTYPE = "entrustedtype";
    public static final String ENTRUSTEDUNIT = "entrustedunit";
    public static final String CREATE_TIME = "createTime";
    public static final String CREATE_DATE = "createDate";
    public static final String MODIFY_TIME = "modifyTime";
    static final String MODIFY_DATE = "modifyDate";
    public static final String CREATOR = "creator";
    public static final String MODIFIER = "modifier";
    public static final String CREATOR_ID = "creatorId";
    public static final String MODIFIER_ID = "modifierId";
    public static final String TENANT = "tenant";
    public static final String AUTOBILL = "autobill";
    public static final String BANKCHECKNO = "bankcheckno";
    public static final String IDENTIFICATIONMARK = "identificationmark";
    public static final String AMOUNTTOBECLAIMED = "amounttobeclaimed";
    public static final String ASSOCIATIONSTATUS = "associationstatus";
    public static final String AUTOASSOCIATION = "autoassociation";
    public static final String BILLCLAIMSTATUS = "billclaimstatus";
    public static final String CLAIMAMOUNT极速快3 = "claimamount";
    public static final String ISPUBLISH = "ispublish";
    public static final String PUBLISHMAN = "publishman";
    public static final String PUBLISH_TIME = "publish_time";
    public static final String CHARACTER_DEF = "characterDef";
    public static final String ISAUTOCREATEBILL = "isautocreatebill";
    public static final String AUTOCREATEBILLCODE = "autocreatebillcode";
    public static final String ISCHOOSEBILL = "ischoosebill";
    public static final String RELATIONSTATUS = "relationstatus";
    public static final String SMARTCHECKNO = "smartcheckno";
    public static final String OPPOSITEOBJECTID = "oppositeobjectid";
    public static final String OPPOSITEOBJECTNAME = "oppositeobjectname";
    public static final String OPPOSITETYPE = "oppositetype";
    public static final String REFUNDRELATIONID = "refundrelationid";
    public static final String REFUNDREJECTRELATIONID = "refundrejectrelationid";
    public static final String REFUNDSTATUS = "refundstatus";
    public static final String BILLPROCESSFLAG = "billprocessflag";
    public static final String DISTRIBUTESTATUS = "distributestatus";
    public static final String PUBLISHDISTRIBUTESTATUS = "publishdistributestatus";
    public static final String OPPOSITEIDENTIFYSOURCE = "oppositeidentifysource";
    public static final String OPPOSITEIDENTIFYSTATUS = "oppositeidentifystatus";
    public static final String ENTERACCOUNTTYPE = "enteraccounttype";
    public static final String ENTERACCOUNTTYPEIDFSTATUS = "enteraccounttypeidfstatus";
    public static final String ENTERACCOUNTCODE = "enteraccountcode";
    public static final String ENTERACCOUNTCODEIDFSTATUS = "enteraccountcodeidfstatus";
    public static final String GROUP = "group";
    public static final String GROUPIDFSTATUS = "groupidfstatus";
    public static final String ENTERACCOUNTNAME = "enteraccountname";
    public static final String ASSISTIDENTIFY = "assistidentify";
    public static final String ISRUNIDENTIFY = "isrunidentify";
    public static final String FROZENSTATUS = "frozenstatus";
    public static final String TRIPLE_SYNCHRON_STATUS = "tripleSynchronStatus";
    public static final String SEALFLAG = "sealflag";
    public static final String ISADVANCEACCOUNTS = "isadvanceaccounts";
    public static final String ASSOCIATIONCOUNT = "associationcount";
    public static final String ISRETURNED = "isreturned";
    public static final String ISOVERDUE = "isoverdue";
    public static final String COUNTERPART = "counterpart";
    public static final String BUSSCOUNTERPART = "busscounterpart";
    public static final String GENERATBILLTYPE = "genertbilltype";
    public static final String NOTE = "note";
    public static final String RECEIPTASSOCIATION = "receiptassociation";
    public static final String BANKTYPE = "banktype";
    public static final String YTENANT_ID = "ytenantId";
    public static final String ISIMPUTATION = "isimputation";
    public static final String PROJECT = "project";
    public static final String EXPENSE_ITEM = "expenseItem";
    public static final String ENTRYTYPE = "entrytype";
    public static final String VIRTUALENTRYTYPE = "virtualentrytype";
    public static final String ISINNERACCOUNTING = "isinneraccounting";
    public static final String IMPINNERACCOUNT = "impinneraccount";
    public static final String EARLYENTRYFLAG = "earlyentryflag";
    public static final String QUICK_TYPE = "quickType";
    public static final String ELIMINATE_REASON_TYPE = "eliminateReasonType";
    public static final String ELIMINATE_STATUS = "eliminateStatus";
    public static final String REMOVEREASONS = "removereasons";
    public static final String ELIMINATE_AMT = "eliminate_amt";
    public static final String AFTER_ELIMINATE_AMT = "after_eliminate_amt";
    public static final String UNIQUE_NO = "unique_no";
    public static final String CONCAT_INFO = "concat_info";
    public static final String CONCAT_INFO_4 = "concat_info_4";
    public static final String CONCAT_INFO_DEFINE = "concat_info_define";
    public static final String ACTIVITY = "activity";
    public static final String REMARK01 = "remark01";
    public static final String INTEREST = "interest";
    public static final String ISREPEAT = "isrepeat";
    public static final String ORGID = "orgid";
    public static final String CONFIRMBILL = "confirmbill";
    public static final String CONFIRMSTATUS = "confirmstatus";
    public static final String RPAIMPORT = "rpaimport";
    public static final String REFUNDRULESCODE = "refundrulescode";
    public static final String REFUNDAUTO = "refundauto";
    public static final String PUBLISHRULESCODE = "publishrulescode";
    public static final String PUBLISHDATE = "publishdate";
    public static final String SERIALAUTO = "serialauto";
    public static final String PUBLISHED_ROLE = "published_role";
    public static final String PUBLISHED_DEPT = "published_dept";
    public static final String EMPLOYEE_FINANCIAL = "employee_financial";
    public static final String PUBLISHED_TYPE = "published_type";
    public static final String PUBLISHED_USER = "published_user";
    public static final String REJECTFLAG = "rejectflag";
    public static final String BILLRULESCODE = "billrulescode";
    public static final String BILLRULESDATE = "billrulesdate";
    public static final String DATASOURCENAME = "datasourcename";
    public static final String DATASOURCESYSTEM = "datasourcesystem";
    public static final String DATASOURCESYSTEMCODE = "datasourcesystemcode";
    public static final String DATASOURCETYPE = "datasourcetype";
    public static final String BILLOPERATOR = "billoperator";
    public static final String BILLASSOCIATIONDATE = "billassociationdate";
    public static final String AUTODEALSTATE = "autodealstate";
    public static final String SERIALDEALENDSTATE = "serialdealendstate";
    public static final String SERIALDEALTYPE = "serialdealtype";
    public static final String DEALUSER = "dealuser";
    public static final String DEALDATE = "dealdate";
    public static final String PROCESSSTATUS = "processstatus";
    public static final String ISAUTOSUBMIT = "isautosubmit";
    public static final String REFUNDFLAG = "refundflag";
    public static final String ORIGINBANKSEQNO = "originbankseqno";
    public static final String NEED_ROLLBACK = "needRollback";
    public static final String MERCHANT = "merchant";
    public static final String STAFF = "staff";
    public static final String VENDOR = "vendor";
    public static final String INNERORG = "innerorg";
    public static final String ISPARSE极速快3SMARTCHECKNO = "isparsesmartcheckno";
    public static final String REFUNDCONFIRMSTAFF = "refundconfirmstaff";
    public static final String CHECKTIME = "checktime";
    public static final String OTHER_CHECKTIME = "other_checktime";
    public static final String TRADETYPE = "tradetype";
    public static final String CASH_DIRECT_LINK = "cashDirectLink";
    public static final String ENTERCOUNTRY = "entercountry";

    // 子表集合字段
    public static final String BANK_RECONCILIATION_BUS_RELATION_B = "BankReconciliationbusrelation_b";
    public static final String ITEMS = "items";
    public static final String DETAILS = "details";
    public static final String BANK_RECONCILIATION_PUBLISHED_USER = "bankReconciliationPublishedUser";
    public static final String BANK_RECONCILIATION_PUBLISHED_ROLE = "bankReconciliationPublishedRole";
    public static final String BANK_RECONCILIATION_PUBLISHED_DEPT = "bankReconciliationPublishedDept";
    public static final String BANK_RECONCILIATION_PUBLISHED_STAFF = "bankReconciliationPublishedStaff";
    public static final String BANK_RECONCILIATION_PUBLISHED_ASSIGN_ORG = "bankReconciliationPublishedAssignOrg";

	/**
	 * 获取流水回单关联码
	 *
	 * @return 流水回单关联码
	 */
	public String getDetailReceiptRelationCode() {
		return get("detailReceiptRelationCode");
	}

	/**
	 * 设置流水回单关联码
	 *
	 * @param detailReceiptRelationCode 流水回单关联码
	 */
	public void setDetailReceiptRelationCode(String detailReceiptRelationCode) {
		set("detailReceiptRelationCode", detailReceiptRelationCode);
	}

	/**
	 * 获取冻结次数
	 *
	 * @return 冻结次数
	 */
	public Integer getFrozencount() {
		return get("frozencount");
	}

	/**
	 * 设置冻结次数
	 *
	 * @param frozencount 冻结次数
	 */
	public void setFrozencount(Integer frozencount) {
		set("frozencount", frozencount);
	}

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
	 * 获取银行账户名称
	 *
	 * @return 银行账户名称
	 */
	public String getAcct_name() {
		return get("acct_name");
	}

	/**
	 * 设置银行账户名称
	 *
	 * @param acct_name
	 */
	public void setAcct_name(String acct_name) {
		set("acct_name", acct_name);
	}

	/**
	 * 获取开户行
	 *
	 * @return 开户行
	 */
	public String getBankopen() {
		return get("bankopen");
	}

	/**
	 * 设置开户行
	 *
	 * @param bankopen 开户行
	 */
	public void setBankopen(String bankopen) {
		set("bankopen", bankopen);
	}

	/**
	 * 获取对账单日期
	 *
	 * @return 对账单日期
	 */
	public java.util.Date getDzdate() {
		return get("dzdate");
	}

	/**
	 * 设置对账单日期
	 *
	 * @param dzdate 对账单日期
	 */
	public void setDzdate(java.util.Date dzdate) {
		set("dzdate", dzdate);
	}

	/**
	 * 获取对账方案
	 *
	 * @return 对账方案.ID
	 */
	public Long getBankreconciliationscheme() {
		return get("bankreconciliationscheme");
	}

	/**
	 * 设置对账方案
	 *
	 * @param bankreconciliationscheme 对账方案.ID
	 */
	public void setBankreconciliationscheme(Long bankreconciliationscheme) {
		set("bankreconciliationscheme", bankreconciliationscheme);
	}

	/**
	 * 获取是否期初
	 *
	 * @return 是否期初
	 */
	public Boolean getInitflag() {
		return getBoolean("initflag");
	}

	/**
	 * 设置是否期初
	 *
	 * @param initflag 是否期初
	 */
	public void setInitflag(Boolean initflag) {
		set("initflag", initflag);
	}

	/**
	 * 获取是否来自事项库
	 *
	 * @return 是否来自事项库
	 */
	public Boolean getLibraryflag() {
		return getBoolean("libraryflag");
	}

	/**
	 * 设置是否来自事项库
	 *
	 * @param libraryflag 是否来自事项库
	 */
	public void setLibraryflag(Boolean libraryflag) {
		set("libraryflag", libraryflag);
	}

	/**
	 * 获取是否生成虚拟流水
	 *
	 * @return 是否生成虚拟流水
	 */
	public Boolean getIsvirtualflow() {
		return getBoolean("isvirtualflow");
	}

	/**
	 * 设置是否生成虚拟流水
	 *
	 * @param isvirtualflow 是否生成虚拟流水
	 */
	public void setIsvirtualflow(Boolean isvirtualflow) {
		set("isvirtualflow", isvirtualflow);
	}

	/**
	 * 关联生成虚拟流水id
	 *
	 * @return 关联生成虚拟流水id
	 */
	public String getVirtualflowid() {
		return get("virtualflowid");
	}

	/**
	 * 设置关联生成虚拟流水id
	 *
	 * @param virtualflowid 关联生成虚拟流水id
	 */
	public void setVirtualflowid(String virtualflowid) {
		set("virtualflowid", virtualflowid);
	}

	/**
	 * 获取事项来源
	 *
	 * @return 事项来源
	 */
	public EventSource getSrcitem() {
		Number v = get("srcitem");
		return EventSource.find(v);
	}

	/**
	 * 获取流水处理修改的来源
	 *
	 * @return 事项来源
	 */
	public void setOperateSourceEnum(OperateSourceEnum operateSourceEnum) {
		set("operateSourceEnum", operateSourceEnum);
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
	 * 获取银行账户
	 *
	 * @return 银行账户.ID
	 */
	public String getBankaccount() {
		return get("bankaccount");
	}

	/**
	 * 设置银行账户
	 *
	 * @param bankaccount 银行账户.ID
	 */
	public void setBankaccount(String bankaccount) {
		set("bankaccount", bankaccount);
	}

	/**
	 * 获取币种
	 *
	 * @return 币种.ID
	 */
	public String getCurrency() {
		return get("currency");
	}

	/**
	 * 设置币种
	 *
	 * @param currency 币种.ID
	 */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

	/**
	 * 获取对账单行号
	 *
	 * @return 对账单行号
	 */
	public String getStatementno() {
		return get("statementno");
	}

	/**
	 * 设置对账单行号
	 *
	 * @param statementno 对账单行号
	 */
	public void setStatementno(String statementno) {
		set("statementno", statementno);
	}

	/**
	 * 获取银行交易流水号
	 *
	 * @return 银行交易流水号
	 */
	public String getBank_seq_no() {
		return get("bank_seq_no");
	}

	/**
	 * 设置银行交易流水号
	 *
	 * @param bank_seq_no 银行交易流水号
	 */
	public void setBank_seq_no(String bank_seq_no) {
		set("bank_seq_no", bank_seq_no);
	}

	/**
	 * 获取第三方流水号
	 *
	 * @return 第三方流水号
	 */
	public String getThirdserialno() {
		return get("thirdserialno");
	}

	/**
	 * 设置第三方流水号
	 *
	 * @param thirdserialno 第三方流水号
	 */
	public void setThirdserialno(String thirdserialno) {
		set("thirdserialno", thirdserialno);
	}

	/**
	 * 获取交易日期
	 *
	 * @return 交易日期
	 */
	public java.util.Date getTran_date() {
		return get("tran_date");
	}

	/**
	 * 设置交易日期
	 *
	 * @param tran_date 交易日期
	 */
	public void setTran_date(java.util.Date tran_date) {
		set("tran_date", tran_date);
	}

	/**
	 * 获取交易时间
	 *
	 * @return 交易时间
	 */
	public java.util.Date getTran_time() {
		return get("tran_time");
	}

	/**
	 * 设置交易时间
	 *
	 * @param tran_time 交易时间
	 */
	public void setTran_time(java.util.Date tran_time) {
		set("tran_time", tran_time);
	}

	/**
	 * 获取借方金额
	 *
	 * @return 借方金额
	 */
	public java.math.BigDecimal getDebitamount() {
		return get("debitamount");
	}

	/**
	 * 设置借方金额
	 *
	 * @param debitamount 借方金额
	 */
	public void setDebitamount(java.math.BigDecimal debitamount) {
		set("debitamount", debitamount);
	}

	/**
	 * 获取贷方金额
	 *
	 * @return 贷方金额
	 */
	public java.math.BigDecimal getCreditamount() {
		return get("creditamount");
	}

	/**
	 * 设置贷方金额
	 *
	 * @param creditamount 贷方金额
	 */
	public void setCreditamount(java.math.BigDecimal creditamount) {
		set("creditamount", creditamount);
	}

	/**
	 * 获取借贷标
	 *
	 * @return 借贷标
	 */
	public Direction getDc_flag() {
		if (get("dc_flag") instanceof String) {
			Number v = Integer.valueOf(get("dc_flag"));
			return Direction.find(v);
		}else {
			Number v = get("dc_flag");
			return Direction.find(v);
		}
	}

	/**
	 * 设置借贷标
	 *
	 * @param dc_flag 借贷标
	 */
	public void setDc_flag(Direction dc_flag) {
		if (dc_flag != null) {
			set("dc_flag", dc_flag.getValue());
		} else {
			set("dc_flag", null);
		}
	}

	/**
	 * 获取钞汇标志
	 *
	 * @return 钞汇标志
	 */
	public String getCash_flag() {
		return get("cash_flag");
	}

	/**
	 * 设置钞汇标志
	 *
	 * @param cash_flag 钞汇标志
	 */
	public void setCash_flag(String cash_flag) {
		set("cash_flag", cash_flag);
	}

	/**
	 * 获取交易金额
	 *
	 * @return 交易金额
	 */
	public java.math.BigDecimal getTran_amt() {
		return get("tran_amt");
	}

	/**
	 * 设置交易金额
	 *
	 * @param tran_amt 交易金额
	 */
	public void setTran_amt(java.math.BigDecimal tran_amt) {
		set("tran_amt", tran_amt);
	}

	/**
	 * 获取余额
	 *
	 * @return 余额
	 */
	public java.math.BigDecimal getAcct_bal() {
		return get("acct_bal");
	}

	/**
	 * 设置余额
	 *
	 * @param acct_bal 余额
	 */
	public void setAcct_bal(java.math.BigDecimal acct_bal) {
		set("acct_bal", acct_bal);
	}

	/**
	 * 获取票据号
	 *
	 * @return 票据号
	 */
	public String getNoteno() {
		return get("noteno");
	}

	/**
	 * 设置票据号
	 *
	 * @param noteno 票据号
	 */
	public void setNoteno(String noteno) {
		set("noteno", noteno);
	}

	/**
	 * 获取对方账号id
	 *
	 * @return 对方账号id
	 */
	public String getTo_acct() {
		return get("to_acct");
	}

	/**
	 * 设置对方账号id
	 *
	 * @param to_acct 对方账号id
	 */
	public void setTo_acct(String to_acct) {
		set("to_acct", to_acct);
	}


	/**
	 * 获取对方账号
	 *
	 * @return 对方账号
	 */
	public String getTo_acct_no() {
		return get("to_acct_no");
	}

	/**
	 * 设置对方账号
	 *
	 * @param to_acct_no 对方账号
	 */
	public void setTo_acct_no(String to_acct_no) {
		set("to_acct_no", to_acct_no);
	}

	/**
	 * 获取对方户名
	 *
	 * @return 对方户名
	 */
	public String getTo_acct_name() {
		return get("to_acct_name");
	}

	/**
	 * 设置对方户名
	 *
	 * @param to_acct_name 对方户名
	 */
	public void setTo_acct_name(String to_acct_name) {
		set("to_acct_name", to_acct_name);
	}

	/**
	 * 获取对方开户行
	 *
	 * @return 对方开户行
	 */
	public String getTo_acct_bank() {
		return get("to_acct_bank");
	}

	/**
	 * 设置对方开户行
	 *
	 * @param to_acct_bank 对方开户行
	 */
	public void setTo_acct_bank(String to_acct_bank) {
		set("to_acct_bank", to_acct_bank);
	}

	/**
	 * 获取对方开户行名
	 *
	 * @return 对方开户行名
	 */
	public String getTo_acct_bank_name() {
		return get("to_acct_bank_name");
	}

	/**
	 * 设置对方开户行名
	 *
	 * @param to_acct_bank_name 对方开户行名
	 */
	public void setTo_acct_bank_name(String to_acct_bank_name) {
		set("to_acct_bank_name", to_acct_bank_name);
	}

	/**
	 * 获取操作人
	 *
	 * @return 操作人
	 */
	public String getOper() {
		return get("oper");
	}

	/**
	 * 设置操作人
	 *
	 * @param oper 操作人
	 */
	public void setOper(String oper) {
		set("oper", oper);
	}

	/**
	 * 获取起息日
	 *
	 * @return 起息日
	 */
	public java.util.Date getValue_date() {
		return get("value_date");
	}

	/**
	 * 设置起息日
	 *
	 * @param value_date 起息日
	 */
	public void setValue_date(java.util.Date value_date) {
		set("value_date", value_date);
	}

	/**
	 * 获取用途
	 *
	 * @return 用途
	 */
	public String getUse_name() {
		return get("use_name");
	}

	/**
	 * 设置用途
	 *
	 * @param use_name 用途
	 */
	public void setUse_name(String use_name) {
		set("use_name", use_name);
	}

	/**
	 * 获取摘要
	 *
	 * @return 摘要
	 */
	public String getRemark() {
		return get("remark");
	}

	/**
	 * 设置摘要
	 *
	 * @param remark 摘要
	 */
	public void setRemark(String remark) {
		set("remark", remark);
	}

	/**
	 * 获取是否已勾对
	 *
	 * @return 是否已勾对
	 */
	public Boolean getCheckflag() {
		return getBoolean("checkflag");
	}

	/**
	 * 设置是否已勾对
	 *
	 * @param checkflag 是否已勾对
	 */
	public void setCheckflag(Boolean checkflag) {
		set("checkflag", checkflag);
	}


	/**
	 * 获取其他模块是否已勾对
	 *
	 * @return 其他模块是否已勾对
	 */
	public Boolean getOther_checkflag() {
		if (getBoolean("other_checkflag") == null){
			return false;
		}
		return getBoolean("other_checkflag");
	}

	/**
	 * 设置其他模块是否已勾对
	 *
	 * @param other_checkflag 其他模块是否已勾对
	 */
	public void setOther_checkflag(Boolean other_checkflag) {
		set("other_checkflag", other_checkflag);
	}

	/**
	 * 获取勾对人
	 *
	 * @return 勾对人.ID
	 */
	public Long getCheckman() {
		return get("checkman");
	}

	/**
	 * 设置勾对人
	 *
	 * @param checkman 勾对人.ID
	 */
	public void setCheckman(Long checkman) {
		set("checkman", checkman);
	}

	/**
	 * 获取勾对日期
	 *
	 * @return 勾对日期
	 */
	public java.util.Date getCheckdate() {
		return get("checkdate");
	}

	/**
	 * 设置勾对日期
	 *
	 * @param checkdate 勾对日期
	 */
	public void setCheckdate(java.util.Date checkdate) {
		set("checkdate", checkdate);
	}

	/**
	 * 获取总账勾对日期
	 *
	 * @return 总账勾对日期
	 */
	public java.util.Date getOther_checkdate() {
		return get("other_checkdate");
	}

	/**
	 * 设置总账勾对日期
	 *
	 * @param checkdate 总账勾对日期
	 */
	public void setOther_checkdate(java.util.Date checkdate) {
		set("other_checkdate", checkdate);
	}

	/**
	 * 获取勾对号
	 *
	 * @return 勾对号
	 */
	public String getCheckno() {
		return get("checkno");
	}

	/**
	 * 设置勾对号
	 *
	 * @param checkno 勾对号
	 */
	public void setCheckno(String checkno) {
		set("checkno", checkno);
	}


	/**
	 * 获取总账勾对号
	 *
	 * @return 总帐勾对号
	 */
	public String getOther_checkno() {
		return get("other_checkno");
	}


	/**
	 * 设置总账勾对号
	 *
	 * @param other_checkno 总账勾对号
	 */
	public void setOther_checkno(String other_checkno) {
		set("other_checkno", other_checkno);
	}

	/**
	 * 获取对账方案id
	 *
	 * @return 对账方案id
	 */
	public String getBankreconciliationsettingid() {
		return get("bankreconciliationsettingid");
	}

	/**
	 * 设置对账方案id
	 *
	 * @param bankreconciliationsettingid 对账方案id
	 */
	public void setBankreconciliationsettingid(String bankreconciliationsettingid) {
		set("bankreconciliationsettingid", bankreconciliationsettingid);
	}

	/**
	 * 获取总账对账方案id
	 *
	 * @return 总账对账方案id
	 */
	public String getGl_bankreconciliationsettingid() {
		return get("gl_bankreconciliationsettingid");
	}

	/**
	 * 设置总账对账方案id
	 *
	 * @param gl_bankreconciliationsettingid 总账对账方案id
	 */
	public void setGl_bankreconciliationsettingid(String gl_bankreconciliationsettingid) {
		set("gl_bankreconciliationsettingid", gl_bankreconciliationsettingid);
	}

	/**
	 * 获取生成收款单线索
	 *
	 * @return 生成收款单线索
	 */
	public String getReceiveline() {
		return get("receiveline");
	}

	/**
	 * 设置生成收款单线索
	 *
	 * @param receiveline 生成收款单线索
	 */
	public void setReceiveline(String receiveline) {
		set("receiveline", receiveline);
	}

	/**
	 * 获取生成付款单线索
	 *
	 * @return 生成付款单线索
	 */
	public String getPaymentline() {
		return get("paymentline");
	}

	/**
	 * 设置生成付款单线索
	 *
	 * @param paymentline 生成付款单线索
	 */
	public void setPaymentline(String paymentline) {
		set("paymentline", paymentline);
	}


	/**
	 * 获取数据来源
	 *
	 * @return 数据来源
	 */
	public DateOrigin getDataOrigin() {
		Number v = get("dataOrigin");
		return DateOrigin.find(v);
	}

	/**
	 * 设置数据来源
	 *
	 * @param dataOrigin 数据来源
	 */
	public void setDataOrigin(DateOrigin dataOrigin) {
		if (dataOrigin != null) {
			set("dataOrigin", dataOrigin.getValue());
		} else {
			set("dataOrigin", null);
		}
	}

	/**
	 * 获取被委托方类型
	 *
	 * @return 被委托方类型
	 */
	public String getEntrustedtype() {
		return get("entrustedtype");
	}

	/**
	 * 设置被委托方类型
	 *
	 * @param entrustedtype 被委托方类型
	 */
	public void setEntrustedtype(String entrustedtype) {
		set("entrustedtype", entrustedtype);
	}

	/**
	 * 获取被委托方单位
	 *
	 * @return 被委托方单位
	 */
	public String getEntrustedunit() {
		return get("entrustedunit");
	}

	/**
	 * 设置被委托方单位
	 *
	 * @param entrustedunit 被委托方单位
	 */
	public void setEntrustedunit(String entrustedunit) {
		set("entrustedunit", entrustedunit);
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
		return get("creatorId");
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
	 * 获取是否已自动生单
	 *
	 * @return 是否已自动生单
	 */
	public Boolean getAutobill() {
		return getBoolean("autobill");
	}

	/**
	 * 设置是否期初
	 *
	 * @param autobill 是否已自动生单
	 */
	public void setAutobill(Boolean autobill) {
		set("autobill", autobill);
	}

	/**
	 * 获取银行对账编号
	 *
	 * @return 银行对账编号
	 */
	public String getBankcheckno() {
		return get("bankcheckno");
	}

	/**
	 * 设置银行对账编号
	 *
	 * @param bankcheckno 银行对账编号
	 */
	public void setBankcheckno(String bankcheckno) {
		set("bankcheckno", bankcheckno);
	}

	/**
	 * 获取判重标识字段
	 *
	 * @return 判重标识字段
	 */
	public String getIdentificationmark() {
		return get("identificationmark");
	}

	/**
	 * 设置判重标识字段
	 *
	 * @param identificationmark 判重标识字段
	 */
	public void setIdentificationmark(String identificationmark) {
		set("identificationmark", identificationmark);
	}

	/**
	 * 获取待认领金额
	 *
	 * @return 待认领金额
	 */
	public java.math.BigDecimal getAmounttobeclaimed() {
		return get("amounttobeclaimed");
	}

	/**
	 * 设置待认领金额
	 *
	 * @param amounttobeclaimed 待认领金额
	 */
	public void setAmounttobeclaimed(java.math.BigDecimal amounttobeclaimed) {
		set("amounttobeclaimed", amounttobeclaimed);
	}

	/**
	 * 获取业务关联状态
	 *
	 * @return 业务关联状态
	 */
	public Short getAssociationstatus() {
		return getShort("associationstatus");
	}

	/**
	 * 设置业务关联状态
	 *
	 * @param associationstatus 业务关联状态
	 */
	public void setAssociationstatus(Short associationstatus) {
		if (AssociationStatus.Associated.getValue() == associationstatus){
			set("processstatus", (short)25);
		}else if(AssociationStatus.NoAssociated.getValue() == associationstatus){
			set("processstatus", (short)1);
		}
		set("associationstatus", associationstatus);
	}

	/**
	 * 获取自动关联标志
	 *
	 * @return 自动关联标志
	 */
	public Boolean getAutoassociation() {
		return getBoolean("autoassociation");
	}

	/**
	 * 设置自动关联标志
	 *
	 * @param autoassociation 自动关联标志
	 */
	public void setAutoassociation(Boolean autoassociation) {
		set("autoassociation", autoassociation);
	}
	/**
	 * 获取认领状态
	 *
	 * @return 认领状态
	 */
	public Short getBillclaimstatus() {
		return getShort("billclaimstatus");
	}

	/**
	 * 设置认领状态
	 *
	 * @param billclaimstatus 认领状态
	 */
	public void setBillclaimstatus(Short billclaimstatus) {
		set("billclaimstatus", billclaimstatus);
	}

	/**
	 * 获取认领金额
	 *
	 * @return 认领金额
	 */
	public java.math.BigDecimal getClaimamount() {
		return get("claimamount");
	}

	/**
	 * 设置认领金额
	 *
	 * @param claimamount 认领金额
	 */
	public void setClaimamount(java.math.BigDecimal claimamount) {
		set("claimamount", claimamount);
	}

	/**
	 * 获取是否发布
	 *
	 * @return 是否发布
	 */
	public Boolean getIspublish() {
		return getBoolean("ispublish");
	}

	/**
	 * 设置是否发布
	 *
	 * @param ispublish 是否发布
	 */
	public void setIspublish(Boolean ispublish) {
		set("ispublish", ispublish);
	}

	/**
	 * 获取发布人
	 *
	 * @return 发布人.ID
	 */
	public Long getPublishman() {
		return get("publishman");
	}

	/**
	 * 设置发布人
	 *
	 * @param publishman 发布人.ID
	 */
	public void setPublishman(Long publishman) {
		set("publishman", publishman);
	}

	/**
	 * 获取发布时间
	 *
	 * @return 发布时间
	 */
	public java.util.Date getPublish_time() {
		return get("publish_time");
	}

	/**
	 * 设置发布时间
	 *
	 * @param publish_time 发布时间
	 */
	public void setPublish_time(java.util.Date publish_time) {
		set("publish_time", publish_time);
	}

	/**
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public Object getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param characterDef 自定义项特征属性组.ID
	 */
	public void setCharacterDef(Object characterDef) {
		set("characterDef", characterDef);
	}

	/**
	 * 获取是否自动生单
	 *
	 * @return 是否自动生单
	 */
	public Boolean getIsautocreatebill() {
		return getBoolean("isautocreatebill");
	}

	/**
	 * 设置是否自动生单
	 *
	 * @param isautocreatebill 是否自动生单
	 */
	public void setIsautocreatebill(Boolean isautocreatebill) {
		set("isautocreatebill", isautocreatebill);
	}

	/**
	 * 获取自动生单关联单据code
	 *
	 * @return 自动生单关联单据code
	 */
	public String getAutocreatebillcode() {
		return get("autocreatebillcode");
	}

	/**
	 * 设置自动生单关联单据code
	 *
	 * @param autocreatebillcode 自动生单关联单据code
	 */
	public void setAutocreatebillcode(String autocreatebillcode) {
		set("autocreatebillcode", autocreatebillcode);
	}

	/**
	 * 获取是否选择生单单据
	 *
	 * @return 是否选择生单单据
	 */
	public Boolean getIschoosebill() {
		return getBoolean("ischoosebill");
	}

	/**
	 * 设置是否选择生单单据
	 *
	 * @param ischoosebill 是否选择生单单据
	 */
	public void setIschoosebill(Boolean ischoosebill) {
		set("ischoosebill", ischoosebill);
	}

	/**
	 * 获取是否确认标识：1待确认；2已确认
	 *
	 * @return 是否确认标识：1待确认；2已确认
	 */
	public Short getRelationstatus() {
		return getShort("relationstatus");
	}

	/**
	 * 设置是否确认标识：1待确认；2已确认
	 *
	 * @param relationstatus 是否确认标识：1待确认；2已确认
	 */
	public void setRelationstatus(Short relationstatus) {
		set("relationstatus", relationstatus);
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
	 * 获取对方单位
	 *
	 * @return 对方单位
	 */
	public String getOppositeobjectid() {
		return get("oppositeobjectid");
	}

	/**
	 * 设置对方单位
	 *
	 * @param oppositeobjectid 对方单位
	 */
	public void setOppositeobjectid(String oppositeobjectid) {
		set("oppositeobjectid", oppositeobjectid);
	}

	/**
	 * 获取对方单位名称
	 *
	 * @return 对方单位名称
	 */
	public String getOppositeobjectname() {
		return get("oppositeobjectname");
	}

	/**
	 * 设置对方单位名称
	 *
	 * @param oppositeobjectname 对方单位名称
	 */
	public void setOppositeobjectname(String oppositeobjectname) {
		set("oppositeobjectname", oppositeobjectname);
	}

	/**
	 * 获取对方类型
	 *
	 * @return 对方类型
	 */
	public Short getOppositetype() {
		return getShort("oppositetype");
	}

	/**
	 * 设置对方类型
	 *
	 * @param oppositetype 对方类型
	 */
	public void setOppositetype(Short oppositetype) {
		set("oppositetype", oppositetype);
	}

	/**
	 * 获取退票关联ID
	 *
	 * @return 退票关联ID
	 */
	public String getRefundrelationid() {
		return get("refundrelationid");
	}

	/**
	 * 设置退票关联ID
	 *
	 * @param refundrelationid 退票关联ID
	 */
	public void setRefundrelationid(String refundrelationid) {
		set("refundrelationid", refundrelationid);
	}

	/**
	 * 获取退票拒绝关联ID
	 *
	 * @return 退票拒绝关联ID
	 */
	public String getRefundrejectrelationid() {
		return get("refundrejectrelationid");
	}

	/**
	 * 设置退票拒绝关联ID
	 *
	 * @param refundrejectrelationid 退票拒绝关联ID
	 */
	public void setRefundrejectrelationid(String refundrejectrelationid) {
		set("refundrejectrelationid", refundrejectrelationid);
	}

	/**
	 * 获取退票状态
	 *
	 * @return 退票状态
	 */
	public Short getRefundstatus() {
		return getShort("refundstatus");
	}

	/**
	 * 设置退票状态
	 *
	 * @param refundstatus 退票状态
	 */
	public void setRefundstatus(Short refundstatus) {
		set("refundstatus", refundstatus);
	}

	/**
	 * 获取回单处理标识
	 *
	 * @return 回单处理标识
	 */
	public Short getBillprocessflag() {
		return getShort("billprocessflag");
	}

	/**
	 * 设置回单处理标识
	 *
	 * @param billprocessflag 回单处理标识
	 */
	public void setBillprocessflag(Short billprocessflag) {
		set("billprocessflag", billprocessflag);
	}

	/**
	 * 获取分配财务人员状态
	 *
	 * @return 分配财务人员状态
	 */
	public Short getDistributestatus() {
		return getShort("distributestatus");
	}

	/**
	 * 设置分配财务人员状态
	 *
	 * @param distributestatus 分配财务人员状态
	 */
	public void setDistributestatus(Short distributestatus) {
		set("distributestatus", distributestatus);
	}

	/**
	 * 获取分配业务人员状态
	 *
	 * @return 分配业务人员状态
	 */
	public Short getPublishdistributestatus() {
		return getShort("publishdistributestatus");
	}

	/**
	 * 设置分配业务人员状态
	 *
	 * @param publishdistributestatus 分配业务人员状态
	 */
	public void setPublishdistributestatus(Short publishdistributestatus) {
		set("publishdistributestatus", publishdistributestatus);
	}

	/**
	 * 获取对方单位辨识来源
	 *
	 * @return 对方单位辨识来源
	 */
	public String getOppositeidentifysource() {
		return get("oppositeidentifysource");
	}

	/**
	 * 设置对方单位辨识来源
	 *
	 * @param oppositeidentifysource 对方单位辨识来源
	 */
	public void setOppositeidentifysource(String oppositeidentifysource) {
		set("oppositeidentifysource", oppositeidentifysource);
	}

	/**
	 * 获取对方类型辨识状态
	 *
	 * @return 对方类型辨识状态
	 */
	public Short getOppositeidentifystatus() {
		return getShort("oppositeidentifystatus");
	}

	/**
	 * 设置对方类型辨识状态
	 *
	 * @param oppositeidentifystatus 对方类型辨识状态
	 */
	public void setOppositeidentifystatus(Short oppositeidentifystatus) {
		set("oppositeidentifystatus", oppositeidentifystatus);
	}

	/**
	 * 获取入账方类型
	 *
	 * @return 入账方类型
	 */
	public String getEnteraccounttype() {
		return get("enteraccounttype");
	}

	/**
	 * 设置入账方类型
	 *
	 * @param enteraccounttype 入账方类型
	 */
	public void setEnteraccounttype(String enteraccounttype) {
		set("enteraccounttype", enteraccounttype);
	}

	/**
	 * 获取入账方类型辨识状态
	 *
	 * @return 入账方类型辨识状态
	 */
	public Short getEnteraccounttypeidfstatus() {
		return getShort("enteraccounttypeidfstatus");
	}

	/**
	 * 设置入账方类型辨识状态
	 *
	 * @param enteraccounttypeidfstatus 入账方类型辨识状态
	 */
	public void setEnteraccounttypeidfstatus(Short enteraccounttypeidfstatus) {
		set("enteraccounttypeidfstatus", enteraccounttypeidfstatus);
	}

	/**
	 * 获取入账方编码
	 *
	 * @return 入账方编码
	 */
	public String getEnteraccountcode() {
		return get("enteraccountcode");
	}

	/**
	 * 设置入账方编码
	 *
	 * @param enteraccountcode 入账方编码
	 */
	public void setEnteraccountcode(String enteraccountcode) {
		set("enteraccountcode", enteraccountcode);
	}

	/**
	 * 获取入账方编码辨识状态
	 *
	 * @return 入账方编码辨识状态
	 */
	public Short getEnteraccountcodeidfstatus() {
		return getShort("enteraccountcodeidfstatus");
	}

	/**
	 * 设置入账方编码辨识状态
	 *
	 * @param enteraccountcodeidfstatus 入账方编码辨识状态
	 */
	public void setEnteraccountcodeidfstatus(Short enteraccountcodeidfstatus) {
		set("enteraccountcodeidfstatus", enteraccountcodeidfstatus);
	}

	/**
	 * 获取组别
	 *
	 * @return 组别.ID
	 */
	public String getGroup() {
		return get("group");
	}

	/**
	 * 设置组别
	 *
	 * @param group 组别.ID
	 */
	public void setGroup(String group) {
		set("group", group);
	}

	/**
	 * 获取组别辨识状态
	 *
	 * @return 组别辨识状态
	 */
	public Short getGroupidfstatus() {
		return getShort("groupidfstatus");
	}

	/**
	 * 设置组别辨识状态
	 *
	 * @param groupidfstatus 组别辨识状态
	 */
	public void setGroupidfstatus(Short groupidfstatus) {
		set("groupidfstatus", groupidfstatus);
	}

	/**
	 * 获取入账方名称
	 *
	 * @return 入账方名称
	 */
	public String getEnteraccountname() {
		return get("enteraccountname");
	}

	/**
	 * 设置入账方名称
	 *
	 * @param enteraccountname 入账方名称
	 */
	public void setEnteraccountname(String enteraccountname) {
		set("enteraccountname", enteraccountname);
	}

	/**
	 * 获取辅助辨识
	 *
	 * @return 辅助辨识
	 */
	public String getAssistidentify() {
		return get("assistidentify");
	}

	/**
	 * 设置辅助辨识
	 *
	 * @param assistidentify 辅助辨识
	 */
	public void setAssistidentify(String assistidentify) {
		set("assistidentify", assistidentify);
	}

	/**
	 * 获取是否已执行辨识规则
	 *
	 * @return 是否已执行辨识规则
	 */
	public Boolean getIsrunidentify() {
		return getBoolean("isrunidentify");
	}

	/**
	 * 设置是否已执行辨识规则
	 *
	 * @param isrunidentify 是否已执行辨识规则
	 */
	public void setIsrunidentify(Boolean isrunidentify) {
		set("isrunidentify", isrunidentify);
	}

	/**
	 * 获取冻结状态
	 *
	 * @return 冻结状态
	 */
	public Short getFrozenstatus() {
		return getShort("frozenstatus");
	}

	/**
	 * 设置冻结状态
	 *
	 * @param frozenstatus 冻结状态
	 */
	public void setFrozenstatus(Short frozenstatus) {
		set("frozenstatus", frozenstatus);
	}

	/**
	 * 获取三方平台同步状态
	 *
	 * @return 三方平台同步状态
	 */
	public Short getTripleSynchronStatus() {
		return getShort("tripleSynchronStatus");
	}

	/**
	 * 设置三方平台同步状态
	 *
	 * @param tripleSynchronStatus 三方平台同步状态
	 */
	public void setTripleSynchronStatus(Short tripleSynchronStatus) {
		set("tripleSynchronStatus", tripleSynchronStatus);
	}

	/**
	 * 获取是否已封存
	 *
	 * @return 是否已封存
	 */
	public Boolean getSealflag() {
		return getBoolean("sealflag");
	}

	/**
	 * 设置是否已封存
	 *
	 * @param sealflag 是否已封存
	 */
	public void setSealflag(Boolean sealflag) {
		set("sealflag", sealflag);
	}

	/**
	 * 获取提前入账
	 *
	 * @return 提前入账
	 */
	public Boolean getIsadvanceaccounts() {
		if (getBoolean("isadvanceaccounts") == null){
			return  false;
		}
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
	 * 获取退回标识
	 *
	 * @return 退回标识
	 */
	public Boolean getIsreturned() {
		return getBoolean("isreturned");
	}

	/**
	 * 设置退回标识
	 *
	 * @param isreturned 退回标识
	 */
	public void setIsreturned(Boolean isreturned) {
		set("isreturned", isreturned);
	}

	/**
	 * 获取是否超期
	 *
	 * @return 是否超期
	 */
	public Boolean getIsoverdue() {
		return getBoolean("isoverdue");
	}

	/**
	 * 设置是否超期
	 *
	 * @param isoverdue 是否超期
	 */
	public void setIsoverdue(Boolean isoverdue) {
		set("isoverdue", isoverdue);
	}



	/**
	 * 获取规则返回财务对接人
	 *
	 * @return 规则返回财务对接人
	 */
	public String getCounterpart() {
		return get("counterpart");
	}

	/**
	 * 设置规则返回财务对接人
	 *
	 * @param counterpart 规则返回财务对接人
	 */
	public void setCounterpart(String counterpart) {
		set("counterpart", counterpart);
	}

	/**
	 * 获取规则返回业务对接人
	 *
	 * @return 规则返回业务对接人
	 */
	public String getBusscounterpart() {
		return get("busscounterpart");
	}

	/**
	 * 设置规则返回业务对接人
	 *
	 * @param busscounterpart 规则返回业务对接人
	 */
	public void setBusscounterpart(String busscounterpart) {
		set("busscounterpart", busscounterpart);
	}

	/**
	 * 获取生单类型
	 *
	 * @return 生单类型
	 */
	public Short getGenertbilltype() {
		return getShort("genertbilltype");
	}

	/**
	 * 设置生单类型
	 *
	 * @param genertbilltype 生单类型
	 */
	public void setGenertbilltype(Short genertbilltype) {
		set("genertbilltype", genertbilltype);
	}

	/**
	 * 获取备注
	 *
	 * @return 备注
	 */
	public String getNote() {
		return get("note");
	}

	/**
	 * 设置备注
	 *
	 * @param note 备注
	 */
	public void setNote(String note) {
		set("note", note);
	}

	/**
	 * 获取回单关联状态
	 *
	 * @return 回单关联状态
	 */
	public Short getReceiptassociation() {
		return getShort("receiptassociation");
	}

	/**
	 * 设置回单关联状态
	 *
	 * @param receiptassociation 回单关联状态
	 */
	public void setReceiptassociation(Short receiptassociation) {
		set("receiptassociation", receiptassociation);
	}

	/**
	 * 获取对账单业务单据关联集合
	 *
	 * @return 对账单业务单据关联集合
	 */
	public java.util.List<BankReconciliationbusrelation_b> BankReconciliationbusrelation_b() {
		return getBizObjects("BankReconciliationbusrelation_b", BankReconciliationbusrelation_b.class);
	}

	/**
	 * 设置对账单业务单据关联集合
	 *
	 * @param BankReconciliationbusrelation_b 对账单业务单据关联集合
	 */
	public void setBankReconciliationbusrelation_b(java.util.List<BankReconciliationbusrelation_b> BankReconciliationbusrelation_b) {
		setBizObjects("BankReconciliationbusrelation_b", BankReconciliationbusrelation_b);
	}

	/**
	 * 获取认领单明细子表集合
	 *
	 * @return 认领单明细子表集合
	 */
	public java.util.List<BillClaimItem> items() {
		return getBizObjects("items", BillClaimItem.class);
	}

	/**
	 * 设置认领单明细子表集合
	 *
	 * @param items 认领单明细子表集合
	 */
	public void setItems(java.util.List<BillClaimItem> items) {
		setBizObjects("items", items);
	}

	/**
	 * 获取银行对账单组件明细集合
	 *
	 * @return 银行对账单组件明细集合
	 */
	public java.util.List<BankReconciliationDetail> details() {
		return getBizObjects("details", BankReconciliationDetail.class);
	}

	/**
	 * 设置银行对账单组件明细集合
	 *
	 * @param details 银行对账单组件明细集合
	 */
	public void setDetails(java.util.List<BankReconciliationDetail> details) {
		setBizObjects("details", details);
	}


	/**
	 * 获取银行类别
	 *
	 * @return 银行类别.ID
	 */
	public String getBanktype() {
		return get("banktype");
	}

	/**
	 * 设置银行类别
	 *
	 * @param banktype 银行类别.ID
	 */
	public void setBanktype(String banktype) {
		set("banktype", banktype);
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
	 * 获取是否触发归集
	 *
	 * @return 是否触发归集
	 */
	public Boolean getIsimputation() {
		return getBoolean("isimputation");
	}

	/**
	 * 设置是否触发归集
	 *
	 * @param isimputation 是否触发归集
	 */
	public void setIsimputation(Boolean isimputation) {
		set("isimputation", isimputation);
	}

	/**
	 * 获取项目
	 *
	 * @return 项目
	 */
	public String getProject() {
		return get("project");
	}

	/**
	 * 设置项目
	 *
	 * @param project 项目
	 */
	public void setProject(String project) {
		set("project", project);
	}

	/**
	 * 获取费用项目
	 *
	 * @return 费用项目
	 */
	public Long getExpenseItem() {
		return get("expenseItem");
	}

	/**
	 * 设置费用项目
	 *
	 * @param expenseItem 费用项目
	 */
	public void setExpenseItem(Long expenseItem) {
		set("expenseItem", expenseItem);
	}


	/**
	 * 获取对账单入账类型
	 *
	 * @return 对账单入账类型
	 */
	public Short getEntrytype() {
		return getShort("entrytype");
	}

	/**
	 * 设置对账单入账类型
	 *
	 * @param entrytype 对账单入账类型
	 */
	public void setEntrytype(Short entrytype) {
		set("entrytype", entrytype);
	}

	/**
	 * 获取入账类型
	 *
	 * @return 入账类型
	 */
	public Short getVirtualEntryType() {
		return getShort("virtualentrytype");
	}

	/**
	 * 设置入账类型
	 *
	 * @param virtualentrytype 入账类型
	 */
	public void setVirtualEntryType(Short virtualentrytype) {
		set("virtualentrytype", virtualentrytype);
	}

	/**
	 * 获取内部账户是否记账
	 *
	 * @return 内部账户是否记账
	 */
	public Boolean getIsinneraccounting() {
		return getBoolean("isinneraccounting");
	}

	/**
	 * 设置内部账户是否记账
	 *
	 * @param isinneraccounting 内部账户是否记账
	 */
	public void setIsinneraccounting(Boolean isinneraccounting) {
		set("isinneraccounting", isinneraccounting);
	}

	/**
	 * 获取归集内部账户
	 *
	 * @return 归集内部账户
	 */
	public String getImpinneraccount() {
		return get("impinneraccount");
	}

	/**
	 * 设置归集内部账户
	 *
	 * @param impinneraccount 归集内部账户
	 */
	public void setImpinneraccount(String impinneraccount) {
		set("impinneraccount", impinneraccount);
	}

	/**
	 * set提前自动入账标识
	 * @param earlyEntryFlag
	 */
	public void setEarlyEntryFlag(Boolean earlyEntryFlag){
		set("earlyentryflag",earlyEntryFlag);
	}
	/**
	 * 提前自动入账标识
	 * @return
	 */
	public Boolean getEarlyEntryFlag(){return getBoolean("earlyentryflag");}

	/**
	 * 款项类型set
	 * @param quickType
	 */
	public void setQuickType(String quickType){set("quickType",quickType);}

	/**
	 * 款项类型get
	 * @return
	 */
	public String getQuickType(){return get("quickType");}

	/**
	 * 获取剔除原因类型
	 *
	 * @return 剔除原因类型
	 */
	public String getEliminateReasonType() {
		return get("eliminateReasonType");
	}

	/**
	 * 设置剔除原因类型
	 *
	 * @param eliminateReasonType 剔除原因类型
	 */
	public void setEliminateReasonType(String eliminateReasonType) {
		set("eliminateReasonType", eliminateReasonType);
	}

	/**
	 * 获取剔除状态
	 *
	 * @return 剔除状态
	 */
	public Short getEliminateStatus() {
		return getShort("eliminateStatus");
	}

	/**
	 * 设置剔除状态
	 *
	 * @param eliminateStatus 剔除状态
	 */
	public void setEliminateStatus(Short eliminateStatus) {
		set("eliminateStatus", eliminateStatus);
	}

	/**
	 * 获取剔除原因
	 *
	 * @return 剔除原因
	 */
	public String getRemovereasons() {
		return get("removereasons");
	}

	/**
	 * 设置剔除原因
	 *
	 * @param removereasons 剔除原因
	 */
	public void setRemovereasons(String removereasons) {
		set("removereasons", removereasons);
	}

	/**
	 * 获取剔除金额
	 *
	 * @return 剔除金额
	 */
	public java.math.BigDecimal getEliminate_amt() {
		return get("eliminate_amt");
	}

	/**
	 * 设置剔除金额
	 *
	 * @param eliminate_amt 剔除金额
	 */
	public void setEliminate_amt(java.math.BigDecimal eliminate_amt) {
		set("eliminate_amt", eliminate_amt);
	}

	/**
	 * 获取剔除后余额
	 *
	 * @return 剔除后余额
	 */
	public java.math.BigDecimal getAfter_eliminate_amt() {
		return get("after_eliminate_amt");
	}

	/**
	 * 设置剔除后余额
	 *
	 * @param after_eliminate_amt 剔除后余额
	 */
	public void setAfter_eliminate_amt(java.math.BigDecimal after_eliminate_amt) {
		set("after_eliminate_amt", after_eliminate_amt);
	}

	/**
	 * 唯一标识码
	 *
	 * @return 唯一标识码
	 */
	public String getUnique_no() {
		return get("unique_no");
	}

	/**
	 * 唯一标识码
	 *
	 * @param unique_no 唯一标识码
	 */
	public void setUnique_no(String unique_no) {
		set("unique_no", unique_no);
	}

	/**
	 * 字段唯一标识码
	 *
	 * @return 字段唯一标识码
	 */
	public String getConcat_info() {
		return get("concat_info");
	}

	/**
	 * 字段唯一标识码
	 *
	 * @param concat_info 字段唯一标识码
	 */
	public void setConcat_info(String concat_info) {
		set("concat_info", concat_info);
	}

	/**
	 * 关联回单id
	 *
	 * @return 关联回单id
	 */
	public String getReceiptId() {
		return get("receiptId");
	}

	/**
	 * 关联回单id
	 *
	 * @param receiptId 关联回单id
	 */
	public void setReceiptId(String receiptId) {
		set("receiptId", receiptId);
	}


	/**
	 * 临时疑重规则标识（疑重规则后字段不计入库）
	 *
	 * @return 临时疑重规则标识
	 */
	public String getConcat_info_define() {
		return get("concat_info_define");
	}

	/**
	 * 字段四要素标识码
	 *
	 * @param concat_info_define 字段四要素标识码
	 */
	public void setConcat_info_define(String concat_info_define) {
		set("concat_info_define", concat_info_define);
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
	 * 获取新增附言
	 *
	 * @return 新增附言
	 */
	public String getRemark01() {
		return get("remark01");
	}

	/**
	 * 设置新增附言
	 *
	 * @param remark01 新增附言
	 */
	public void setRemark01(String remark01) {
		set("remark01", remark01);
	}





	/**
	 * 利息
	 *
	 * @return 利息
	 */
	public java.math.BigDecimal getInterest() {
		return get("interest");
	}

	/**
	 * 利息
	 *
	 * @param interest 利息
	 */
	public void setInterest(java.math.BigDecimal interest) {
		set("interest", interest);
	}




	/**
	 * 获取是否疑重
	 *
	 * @return 是否疑重
	 */
	public Short getIsRepeat() {
		return getShort("isrepeat");
	}

	/**
	 * 设置是否疑重
	 *
	 * @param isrepeat 是否疑重
	 */
	public void setIsRepeat(Short isrepeat) {
		set("isrepeat", isrepeat);
	}




	/**
	 * 获取多要素组合字段
	 *
	 * @return 新增多要素组合字段
	 */
	public String getConcatInfo() {
		return get("concat_info");
	}

	/**
	 * 设置多要素组合字段
	 *
	 * @param concat_info 新增多要素组合字段
	 */
	private void setConcatInfo(String concat_info) {
		set("concat_info", concat_info);
	}

	/**
	 * 8要素组合
	 * @param concat_info
	 */
	public void fillConcatInfo(String concat_info){
		set("concat_info", concat_info);
	}

	/**
	 * 获取所属组织
	 *
	 * @return 所属组织.ID
	 */
	public String getOrgid() {
		return get("orgid");
	}

	/**
	 * 设置所属组织
	 *
	 * @param orgid 所属组织.ID
	 */
	public void setOrgid(String orgid) {
		set("orgid", orgid);
	}

	/**
	 * 获取授权组织确认节点
	 *
	 * @return 授权组织确认节点
	 */
	public String getConfirmbill() {
		return get("confirmbill");
	}

	/**
	 * 设置授权组织确认节点
	 *
	 * @param confirmbill 授权组织确认节点
	 */
	public void setConfirmbill(String confirmbill) {
		set("confirmbill", confirmbill);
	}

	/**
	 * 获取确认状态
	 *
	 * @return 确认状态
	 */
	public String getConfirmstatus() {
		return get("confirmstatus");
	}

	/**
	 * 设置确认状态
	 *
	 * @param confirmstatus 确认状态
	 */
	public void setConfirmstatus(String confirmstatus) {
		set("confirmstatus", confirmstatus);
	}

	/**
	 * 获取RPA导入
	 *
	 * @return RPA导入
	 */
	public Boolean getRpaimport() {
		return getBoolean("rpaimport");
	}

	/**
	 * 设置RPA导入
	 *
	 * @param rpaimport RPA导入
	 */
	public void setRpaimport(Boolean rpaimport) {
		set("rpaimport", rpaimport);
	}

	/**
	 * 获取退票辨识规则编号
	 *
	 * @return 退票辨识规则编号
	 */
	public String getRefundrulescode() {
		return get("refundrulescode");
	}

	/**
	 * 设置退票辨识规则编号
	 *
	 * @param refundrulescode 退票辨识规则编号
	 */
	public void setRefundrulescode(String refundrulescode) {
		set("refundrulescode", refundrulescode);
	}

	/**
	 * 获取退票自动辨识
	 *
	 * @return 退票自动辨识
	 */
	public Boolean getRefundauto() {
		return getBoolean("refundauto");
	}

	/**
	 * 设置退票自动辨识
	 *
	 * @param refundauto 退票自动辨识
	 */
	public void setRefundauto(Boolean refundauto) {
		set("refundauto", refundauto);
	}

	/**
	 * 获取发布对象辨识规则编号
	 *
	 * @return 发布对象辨识规则编号
	 */
	public String getPublishrulescode() {
		return get("publishrulescode");
	}

	/**
	 * 设置发布对象辨识规则编号
	 *
	 * @param publishrulescode 发布对象辨识规则编号
	 */
	public void setPublishrulescode(String publishrulescode) {
		set("publishrulescode", publishrulescode);
	}

	/**
	 * 获取发布对象辨识时间
	 *
	 * @return 发布对象辨识时间
	 */
	public java.util.Date getPublishdate() {
		return get("publishdate");
	}

	/**
	 * 设置发布对象辨识时间
	 *
	 * @param publishdate 发布对象辨识时间
	 */
	public void setPublishdate(java.util.Date publishdate) {
		set("publishdate", publishdate);
	}

	/**
	 * 获取流水自动发布
	 *
	 * @return 流水自动发布
	 */
	public Boolean getSerialauto() {
		return getBoolean("serialauto");
	}

	/**
	 * 设置流水自动发布
	 *
	 * @param serialauto 流水自动发布
	 */
	public void setSerialauto(Boolean serialauto) {
		set("serialauto", serialauto);
	}

	/**
	 * 获取发布角色ID
	 *
	 * @return 发布角色ID.ID
	 */
	public String getPublished_role() {
		return get("published_role");
	}

	/**
	 * 设置发布角色ID
	 *
	 * @param published_role 发布角色ID.ID
	 */
	public void setPublished_role(String published_role) {
		set("published_role", published_role);
	}

	/**
	 * 获取发布部门ID
	 *
	 * @return 发布部门ID.ID
	 */
	public String getPublished_dept() {
		return get("published_dept");
	}

	/**
	 * 设置发布部门ID
	 *
	 * @param published_dept 发布部门ID.ID
	 */
	public void setPublished_dept(String published_dept) {
		set("published_dept", published_dept);
	}

	/**
	 * 获取发布员工ID
	 *
	 * @return 发布员工ID.ID
	 */
	public String getEmployee_financial() {
		return get("employee_financial");
	}

	/**
	 * 设置发布员工ID
	 *
	 * @param employee_financial 发布员工ID.ID
	 */
	public void setEmployee_financial(String employee_financial) {
		set("employee_financial", employee_financial);
	}

	/**
	 * 获取发布对象
	 *
	 * @return 发布对象
	 */
	public Short getPublished_type() {
		return getShort("published_type");
	}

	/**
	 * 设置发布对象
	 *
	 * @param published_type 发布对象
	 */
	public void setPublished_type(Short published_type) {
		set("published_type", published_type);
	}

	/**
	 * 获取发布用户ID
	 *
	 * @return 发布用户ID.ID
	 */
	public String getPublished_user() {
		return get("published_user");
	}

	/**
	 * 设置发布用户ID
	 *
	 * @param published_user 发布用户ID.ID
	 */
	public void setPublished_user(String published_user) {
		set("published_user", published_user);
	}

	/**
	 * 获取退回标识
	 *
	 * @return 退回标识
	 */
	public Boolean getRejectflag() {
		return getBoolean("rejectflag");
	}

	/**
	 * 设置退回标识
	 *
	 * @param rejectflag 退回标识
	 */
	public void setRejectflag(Boolean rejectflag) {
		set("rejectflag", rejectflag);
	}

	/**
	 * 获取收付单据匹配规则编号
	 *
	 * @return 收付单据匹配规则编号
	 */
	public String getBillrulescode() {
		return get("billrulescode");
	}

	/**
	 * 设置收付单据匹配规则编号
	 *
	 * @param billrulescode 收付单据匹配规则编号
	 */
	public void setBillrulescode(String billrulescode) {
		set("billrulescode", billrulescode);
	}

	/**
	 * 获取收付单据关联匹配时间
	 *
	 * @return 收付单据关联匹配时间
	 */
	public java.util.Date getBillrulesdate() {
		return get("billrulesdate");
	}

	/**
	 * 设置收付单据关联匹配时间
	 *
	 * @param billrulesdate 收付单据关联匹配时间
	 */
	public void setBillrulesdate(java.util.Date billrulesdate) {
		set("billrulesdate", billrulesdate);
	}

	/**
	 * 获取数据源名称
	 *
	 * @return 数据源名称
	 */
	public String getDatasourcename() {
		return get("datasourcename");
	}

	/**
	 * 设置数据源名称
	 *
	 * @param datasourcename 数据源名称
	 */
	public void setDatasourcename(String datasourcename) {
		set("datasourcename", datasourcename);
	}

	/**
	 * 获取数据源来源系统
	 *
	 * @return 数据源来源系统
	 */
	public String getDatasourcesystem() {
		return get("datasourcesystem");
	}

	/**
	 * 设置数据源来源系统
	 *
	 * @param datasourcesystem 数据源来源系统
	 */
	public void setDatasourcesystem(String datasourcesystem) {
		set("datasourcesystem", datasourcesystem);
	}

	/**
	 * 获取数据源来源系统编码
	 *
	 * @return 数据源来源系统编码
	 */
	public String getDatasourcesystemcode() {
		return get("datasourcesystemcode");
	}

	/**
	 * 设置数据源来源系统编码
	 *
	 * @param datasourcesystemcode 数据源来源系统编码
	 */
	public void setDatasourcesystemcode(String datasourcesystemcode) {
		set("datasourcesystemcode", datasourcesystemcode);
	}

	/**
	 * 获取数据源类别
	 *
	 * @return 数据源类别.ID
	 */
	public String getDatasourcetype() {
		return get("datasourcetype");
	}

	/**
	 * 设置数据源类别
	 *
	 * @param datasourcetype 数据源类别.ID
	 */
	public void setDatasourcetype(String datasourcetype) {
		set("datasourcetype", datasourcetype);
	}

	/**
	 * 获取收付单据关联操作人
	 *
	 * @return 收付单据关联操作人.ID
	 */
	public String getBilloperator() {
		return get("billoperator");
	}

	/**
	 * 设置收付单据关联操作人
	 *
	 * @param billoperator 收付单据关联操作人.ID
	 */
	public void setBilloperator(String billoperator) {
		set("billoperator", billoperator);
	}

	/**
	 * 获取收付单据关联时间
	 *
	 * @return 收付单据关联时间
	 */
	public java.util.Date getBillassociationdate() {
		return get("billassociationdate");
	}

	/**
	 * 设置收付单据关联时间
	 *
	 * @param billassociationdate 收付单据关联时间
	 */
	public void setBillassociationdate(java.util.Date billassociationdate) {
		set("billassociationdate", billassociationdate);
	}

	/**
	 * 获取自动处理状态
	 *
	 * @return 自动处理状态
	 */
	public Short getAutodealstate() {
		return getShort("autodealstate");
	}

	/**
	 * 设置自动处理状态
	 *
	 * @param autodealstate 自动处理状态
	 */
	public void setAutodealstate(Short autodealstate) {
		set("autodealstate", autodealstate);
	}

	/**
	 * 获取流水处理完结状态
	 *
	 * @return 流水处理完结状态
	 */
	public Short getSerialdealendstate() {
		return getShort("serialdealendstate");
	}

	/**
	 * 设置流水处理完结状态
	 *
	 * @param serialdealendstate 流水处理完结状态
	 */
	public void setSerialdealendstate(Short serialdealendstate) {
		set("serialdealendstate", serialdealendstate);
	}

	/**
	 * 判断是否疑似重复
	 * @param bankReconciliation
	 * @return
	 */
	public Boolean checkRefund(BankReconciliation bankReconciliation){
		boolean isSuspectedRefund = false;
		String bankaccount = this.getBankaccount();
		String currency = this.getCurrency();
		short dc_flag = this.getDc_flag().getValue();
		BigDecimal tranAmt = this.getTran_amt();
		String to_acct_no = this.getTo_acct_no();
		if((StringUtils.isEmpty(to_acct_no)) || StringUtils.isEmpty(bankReconciliation.getTo_acct_no())){
			return isSuspectedRefund;
		}
		/**
		 * 1. 交易方向相同，则判断金额、本方账户、对方账户、币种是否相同
		 * 2. 交易方向不同，则判断金额绝对值、本方账户、对方账户、币种是否相同
		 */
		if(dc_flag == bankReconciliation.getDc_flag().getValue()){
			if(bankaccount.equals(bankReconciliation.getBankaccount()) &&
					currency.equals(bankReconciliation.getCurrency()) &&
					tranAmt.compareTo(bankReconciliation.getTran_amt()) == 0 &&
					to_acct_no.equals(bankReconciliation.getTo_acct_no()))
			{
				isSuspectedRefund = true;
			}
		}else{
			if(bankaccount.equals(bankReconciliation.getBankaccount()) &&
					currency.equals(bankReconciliation.getCurrency()) &&
					tranAmt.abs().compareTo(bankReconciliation.getTran_amt().abs()) == 0 &&
					to_acct_no.equals(bankReconciliation.getTo_acct_no()))
			{
				isSuspectedRefund = true;
			}
		}
		if(isSuspectedRefund){
			this.setRefundrelationid(bankReconciliation.getId().toString());
			this.setRefundstatus(ReFundType.SUSPECTEDREFUND.getValue());
			bankReconciliation.setRefundstatus(ReFundType.SUSPECTEDREFUND.getValue());
			bankReconciliation.setRefundrelationid(this.getId().toString());
		}
		return isSuspectedRefund;
	}

	/**
	 * 获取流水认领处理方式
	 *
	 * @return 流水认领处理方式
	 */
	public Short getSerialdealtype() {
		return getShort("serialdealtype");
	}

	/**
	 * 设置流水认领处理方式
	 *
	 * @param serialdealtype 流水认领处理方式
	 */
	public void setSerialdealtype(Short serialdealtype) {
		set("serialdealtype", serialdealtype);
	}

	/**
	 * 获取处理用户
	 *
	 * @return 处理用户.ID
	 */
	public String getDealuser() {
		return get("dealuser");
	}

	/**
	 * 设置处理用户
	 *
	 * @param dealuser 处理用户.ID
	 */
	public void setDealuser(String dealuser) {
		set("dealuser", dealuser);
	}

	/**
	 * 获取处理时间
	 *
	 * @return 处理时间
	 */
	public java.util.Date getDealdate() {
		return get("dealdate");
	}

	/**
	 * 设置处理时间
	 *
	 * @param dealdate 处理时间
	 */
	public void setDealdate(java.util.Date dealdate) {
		set("dealdate", dealdate);
	}

	/**
	 * 获取流水处理状态
	 *
	 * @return 流水处理状态
	 */
	public Short getProcessstatus() {
		return getShort("processstatus");
	}

	/**
	 * 设置流水处理状态
	 *
	 * @param processstatus 流水处理状态
	 */
	public void setProcessstatus(Short processstatus) {
		set("processstatus", processstatus);
	}

	/**
	 * 获取提前挂账自动提交
	 *
	 * @return 提前挂账自动提交
	 */
	public Short getIsautosubmit() {
		return getShort("isautosubmit");
	}

	/**
	 * 设置提前挂账自动提交
	 *
	 * @param isautosubmit 提前挂账自动提交
	 */
	public void setIsautosubmit(Short isautosubmit) {
		set("isautosubmit", isautosubmit);
	}

	/**
	 * 获取退票
	 *
	 * @return 退票
	 */
	public Boolean getRefundFlag() {
		return get("refundflag");
	}

	/**
	 * 设置退票
	 *
	 * @param refundFlag 退票
	 */
	public void setRefundFlag(Boolean refundFlag) {
		set("refundflag",refundFlag);
	}

	/**
	 * 获取原交易流水号
	 *
	 * @return 原交易流水号
	 */
	public String getOriginBankseqno() {
		return get("originbankseqno");
	}

	/**
	 * 原交易流水
	 *
	 * @param orignBankseqno 原交易流水
	 */
	public void setOrignBankseqno(String orignBankseqno) {
		set("originbankseqno",orignBankseqno);
	}

	/**
	 * 获取银行对账单发布用户子表集合
	 *
	 * @return 银行对账单发布用户子表集合
	 */
	public java.util.List<BankReconciliationPublishedUser> bankReconciliationPublishedUser() {
		return getBizObjects("bankReconciliationPublishedUser", BankReconciliationPublishedUser.class);
	}

	/**
	 * 设置银行对账单发布用户子表集合
	 *
	 * @param bankReconciliationPublishedUser 银行对账单发布用户子表集合
	 */
	public void setBankReconciliationPublishedUser(java.util.List<BankReconciliationPublishedUser> bankReconciliationPublishedUser) {
		setBizObjects("bankReconciliationPublishedUser", bankReconciliationPublishedUser);
	}

	/**
	 * 获取银行对账单发布角色子表集合
	 *
	 * @return 银行对账单发布角色子表集合
	 */
	public java.util.List<BankReconciliationPublishedRole> bankReconciliationPublishedRole() {
		return getBizObjects("bankReconciliationPublishedRole", BankReconciliationPublishedRole.class);
	}

	/**
	 * 设置银行对账单发布角色子表集合
	 *
	 * @param bankReconciliationPublishedRole 银行对账单发布角色子表集合
	 */
	public void setBankReconciliationPublishedRole(java.util.List<BankReconciliationPublishedRole> bankReconciliationPublishedRole) {
		setBizObjects("bankReconciliationPublishedRole", bankReconciliationPublishedRole);
	}

	/**
	 * 获取银行对账单发布部门子表集合
	 *
	 * @return 银行对账单发布部门子表集合
	 */
	public java.util.List<BankReconciliationPublishedDept> bankReconciliationPublishedDept() {
		return getBizObjects("bankReconciliationPublishedDept", BankReconciliationPublishedDept.class);
	}

	/**
	 * 设置银行对账单发布部门子表集合
	 *
	 * @param bankReconciliationPublishedDept 银行对账单发布部门子表集合
	 */
	public void setBankReconciliationPublishedDept(java.util.List<BankReconciliationPublishedDept> bankReconciliationPublishedDept) {
		setBizObjects("bankReconciliationPublishedDept", bankReconciliationPublishedDept);
	}

	/**
	 * 获取银行对账单发布员工子表集合
	 *
	 * @return 银行对账单发布员工子表集合
	 */
	public java.util.List<BankReconciliationPublishedStaff> bankReconciliationPublishedStaff() {
		return getBizObjects("bankReconciliationPublishedStaff", BankReconciliationPublishedStaff.class);
	}

	/**
	 * 设置银行对账单发布员工子表集合
	 *
	 * @param bankReconciliationPublishedStaff 银行对账单发布员工子表集合
	 */
	public void setBankReconciliationPublishedStaff(java.util.List<BankReconciliationPublishedStaff> bankReconciliationPublishedStaff) {
		setBizObjects("bankReconciliationPublishedStaff", bankReconciliationPublishedStaff);
	}

	/**
	 * 获取是否需要回滚
	 *
	 */
	public Boolean isNeedRollback() {
		return getBoolean("needRollback");
	}

	/**
	 * 设置是否需要回滚
	 *
	 * @param needRollback
	 */
	public void setNeedRollback(Boolean needRollback) {
		set("needRollback", needRollback);
	}

	/**
	 * 获取客户
	 *
	 * @return 客户.ID
	 */
	public Long getMerchant() {
		return get("merchant");
	}

	/**
	 * 设置客户
	 *
	 * @param merchant 客户.ID
	 */
	public void setMerchant(Long merchant) {
		set("merchant", merchant);
	}

	/**
	 * 获取员工
	 *
	 * @return 员工.ID
	 */
	public String getStaff() {
		return get("staff");
	}

	/**
	 * 设置员工
	 *
	 * @param staff 员工.ID
	 */
	public void setStaff(String staff) {
		set("staff", staff);
	}

	/**
	 * 获取供应商
	 *
	 * @return 供应商.ID
	 */
	public Long getVendor() {
		return get("vendor");
	}

	/**
	 * 设置供应商
	 *
	 * @param vendor 供应商.ID
	 */
	public void setVendor(Long vendor) {
		set("vendor", vendor);
	}

	/**
	 * 获取内部单位
	 *
	 * @return 内部单位.ID
	 */
	public String getInnerorg() {
		return get("innerorg");
	}

	/**
	 * 设置内部单位
	 *
	 * @param innerorg 内部单位.ID
	 */
	public void setInnerorg(String innerorg) {
		set("innerorg", innerorg);
	}


	/**
	 * 获取统一对账码是否解析生成
	 *
	 * @return 统一对账码是否解析生成
	 */
	public Boolean getIsparsesmartcheckno() {
		if (this.get("isparsesmartcheckno") == null ){
			return false;
		}
		return getBoolean("isparsesmartcheckno");
	}

	/**
	 * 设置统一对账码是否解析生成 - 需要进行格式化校验
	 *
	 * @param isparsesmartcheckno 统一对账码是否解析生成
	 */
	public void setIsparsesmartcheckno(Boolean isparsesmartcheckno) {
		set("isparsesmartcheckno", isparsesmartcheckno);
	}

	/**
	 * 获取退票确认人
	 *
	 * @return 退票确认人
	 */
	public String getRefundconfirmstaff() {
		return get("refundconfirmstaff");
	}

	/**
	 * 设置退票确认人
	 *
	 * @param refundconfirmstaff 退票确认人
	 */
	public void setRefundconfirmstaff(String refundconfirmstaff) {
		set("refundconfirmstaff", refundconfirmstaff);
	}

	/**
	 * 获取日记账勾对时间
	 *
	 * @return 日记账勾对时间
	 */
	public java.util.Date getChecktime() {
		return get("checktime");
	}

	/**
	 * 设置日记账勾对时间
	 *
	 * @param checktime 日记账勾对时间
	 */
	public void setChecktime(java.util.Date checktime) {
		set("checktime", checktime);
	}

	/**
	 * 获取总账勾对时间
	 *
	 * @return 总账勾对时间
	 */
	public java.util.Date getOther_checktime() {
		return get("other_checktime");
	}

	/**
	 * 设置总账勾对时间
	 *
	 * @param other_checktime 总账勾对时间
	 */
	public void setOther_checktime(java.util.Date other_checktime) {
		set("other_checktime", other_checktime);
	}
	/**
	 * 获取交易类型
	 *
	 * @return 交易类型
	 */
	public String getTradetype() {return get("tradetype");}
	/**
	 * 设置交易类型
	 *
	 * @param tradetype 交易类型
	 */
	public void setTradetype(String tradetype) {set("tradetype", tradetype);}

	/**
	 * 获取银行对账单发布指定组织子表集合
	 *
	 * @return 银行对账单发布指定组织子表集合
	 */
	public java.util.List<BankReconciliationPublishedAssignOrg> bankReconciliationPublishedAssignOrg() {
		return getBizObjects("bankReconciliationPublishedAssignOrg", BankReconciliationPublishedAssignOrg.class);
	}

	/**
	 * 设置银行对账单发布指定组织子表集合
	 *
	 * @param bankReconciliationPublishedAssignOrg 银行对账单发布指定组织子表集合
	 */
	public void setBankReconciliationPublishedAssignOrg(java.util.List<BankReconciliationPublishedAssignOrg> bankReconciliationPublishedAssignOrg) {
		setBizObjects("bankReconciliationPublishedAssignOrg", bankReconciliationPublishedAssignOrg);
	}

	/**
	 * 获取是否直联
	 *
	 * @return 是否直联
	 */
	public Short getCashDirectLink() {
		return getShort("cashDirectLink");
	}

	/**
	 * 设置是否直联
	 *
	 * @param cashDirectLink 是否直联
	 */
	public void setCashDirectLink(Short cashDirectLink) {
		set("cashDirectLink", cashDirectLink);
	}

	/**
	 * 获取是否入境
	 *
	 * @return 是否入境
	 */
	public Short getEntercountry() {
		return getShort("entercountry");
	}

	/**
	 * 设置是否入境
	 *
	 * @param entercountry 是否入境
	 */
	public void setEntercountry(Short entercountry) {
		set("entercountry", entercountry);
	}
}
