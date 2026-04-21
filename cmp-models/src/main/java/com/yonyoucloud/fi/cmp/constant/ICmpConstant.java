package com.yonyoucloud.fi.cmp.constant;

import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankbillcheck.BankBillCheck;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.electronicstatementconfirm.ElectronicStatementConfirm;

import java.util.Arrays;
import java.util.List;

/**
 * <h1>常量工具类</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021/7/9 13:37
 */
public interface ICmpConstant {
	String[] arr = new String[]{"\n", "\t", "\r", "\\"};
	String BATCH_DEL_VOUCHER = "batch_delete_voucher_request";
	String MDD_DOMAIN_ORGCENTER = "ucf-org-center";
	String TO_ACCT_NO = "to_acct_no";
	String TO_ACCT_NAME = "to_acct_name";
	//流水回单关联码
	String DETAIL_RECEIPT_RELATION_CODE = "detailReceiptRelationCode";
	//流水、回单唯一码
	String UNIQUE_NO = "yqlUniqueNo";
	String IS_DISPATCH_TASK_CMP = "is_dispatch_task_cmp";
	String EFFECT = "effect";
	String RELATIONS_DEPT = "relations.dept";
	String SERIAL_DEAL_END_STATE = "serialdealendstate";//流水处理完结状态
	String CMDOMAIN = "ctm-cmp";

	String ACCENTITY = "accentity";

	String ACCENTITY_NAME = "accentity_name";

	String MAINID_ACCENTITY = "mainid.accentity";

	String MAINID_ORG = "mainid.org";

	String ACCENTITY_UNDERLINE = "accentity_";

	String ACCENT = "accent";

	String CUSTOMER = "customer";

	// 银行流水处理的客户数据权限类型
	String MERCHANT = "merchant";
	String STAFF = "staff";

	String EMPLOYEE = "employee";

	String DATA = "data";

	String NAME = "name";

	String BANKACCOUNT  = "bankaccount";

	String BILL_NUM = "billnum";

	String PRIMARY_ID = "id";

	String QUERY_IN = "in";

	String QUERY_NOTIN = "nin";

	String QUERY_EQ = "eq";

	String QUERY_NEQ = "neq";

	String QUERY_EGT = "egt";

	String QUERY_ELT = "elt";

	String CARRIAGE_RETURN = "\r\n";

	String QUERY_LIKE = "like";

	String MSG = "msg";

	String SETTLE_STATUS = "settlestatus";

    String AUDIT_STATUS = "auditstatus";

    String CMP_MODUAL_NAME = "CM";

	String CTM_MODUAL_NAME = "CTM";
	String APPCODE = "yonbip-fi-ctmcmp";

    String ORDER_ASC = "asc";

	String ORDER_DESC = "desc";

    String ENTERPRISE_BANK_ACCOUNT = "enterpriseBankAccount";

    String ORG_ID = "orgid";

    String ORGID = "orgId";

    String ORGNAME = "orgName";

    String CUSTOMERID = "customerId";

    String VOUCHDATE = "vouchdate";

    String TENANT = "tenant";

    String FAILED = "failed";

	String SELECT_TOTAL_PARAM = "*";

	String SELECT_ONE_PARAM  = "1";

	String AUTO_BILL_FLAG = "auto";

	String SERVICECODE_SALPAY = "cmp_salarypaylist";

	String CM_CMP_CHECKINVENTORY="CM.cmp_checkInventory";

	Integer CONSTANT_ONE = 1;

	Integer CONSTANT_ZERO = 0;

	String  CONSTANT_STR_ZERO = "0";

	Integer CONSTANT_FOURTEEN = 14;

	Double CONSTANT_ZERO_POINT_ONE = 0.01;

	String BILLTYPE = "billtype";

	String INSERT = "Insert";

	String SRCBILLITEMID = "srcbillitemid";

	String ORISUM = "oriSum";

	String NATSUM = "natSum";

	String EXCHRATE = "exchRate";

    String TRANSFER_ACCOUNT_EXCHRATEOPS = "exchRateOps";

	String EXCHRATEOPS = "exchangeRateOps";

	String UPDATE = "Update";

	String MAINID = "mainid";

	String PAYBILLSTATUS = "payBillStatus";

	String PURCHASEORDERS = "pu.purchaseorder.PurchaseOrders";

	String PURCHASEORDER = "pu.purchaseorder.PurchaseOrder";

	String UPU = "upu";

	String OAPDETAIL= "arap.oap.OapDetail";

	String OAPMAIN= "arap.oap.OapMain";

	String FIARAP = IDomainConstant.MDD_DOMAIN_FIARAP;

	Integer CONSTANT_EIGHT = 16;

	String BALANCE = "balance";
	//原币余额
	String ORI_BALANCE = "oribalance";
	//本币余额
	String LOCAL_BALANCE = "localbalance";

	String OCCUPYAMOUNT = "occupyamount";

    // 可用原币余额
    String ORI_AVAILABLE_BALANCE = "oriAvailableBalance";
    // 可用本币余额
    String LOCAL_AVAILABLE_BALANCE = "localAvailableBalance";

	String BANK_SEQ_NO = "bank_seq_no";
	String ID = "id";

	String ITEMS = "items";

	String BACK_SOURCE_MAP = "backSourceMap";

	String PAY_APPLICATION_BILL_B = "payApplicationBill_b";

	String PAYMENT_PREEMPT_AMOUNT = "paymentPreemptAmount";

	String PAY_APPLY_BILL = "payApplyBill";

	String OLD_INFO = "oldInfo";

	String OMAKE = "omake";

	String CMPBANKRECONCILIATION = "银行流水处理";//@notranslate
	String CMPBANKRECONCILIATION_RESID = "UID:P_CM-UI_1C122F4604D00004";

	String SOURCEDATAS = "sourceDatas";

	String OAPAPPLY="oap-apply";

	String PROVISIONALESTIMATE_FLAG="provisionalestimateflag";

	String BILL_DIRECTION="billdirection";

	String OAP_DETAIL="oapDetail";

	String AUDIT = "audit";

	String UN_AUDIT = "unaudit";

	String SETTLESUCCESS = "settleSuccess";// 结算成功

	String STOPPAY = "stopPay";// 止付

	String APPROVALSTOP = "approvalStop";//审批终止

	String ORDER_NO = "orderno";

	String TOP_SRC_BILL_ITEM_ID = "topsrcbillitemid";

	Integer CONSTANT_SEVEN = 7;

	Integer CONSTANT_SEVENTH_TWO = 72;

	Integer CONSTANT_SEVENTH_FOUR = 74;

	String ORG = "org";

	String ORG_NAME = "org_name";

	String SRC_ITEM = "srcitem";

	String SAVE = "save";

	String APPLY_PAY = "applyPay";

	String TOP_SRC_BILL_ITEM_NO = "topsrcbillitemno";


	String SRC_BILL_ITEM_NO = "srcbillitemno";

	String DZ_DATE = "dzdate";//登账日期

	String SETTLE_MODE = "settlemode";//结算方式

	String SETTLE_MODE_NAME = "settlemode_name";//结算方式名称

	String SERVICE_ATTR = "serviceAttr";//服务编码 0-银行业务 1-现金业务

	String RECEIVE_MAKEBILL_CODE = "arapToCmpReceivebill";//单据转换对应code，pub_makebill表中申明，需唯一

	String CMP_FLAG = "cmpflag"; //现金标识，控制单据是否展示  false-不展示 true-展示

	String OUT_SYSTEM = "outsystem";//非现金自制单据标识，用于删除时进行判断，对该单据不进行校验，1-不校验 非1校验

	String AUTOTEST_SIGNATURE_Constant = "AUTOTEST_SIGNATURE_Constant";//自动化测试时，付款工作台等需要传输的签名值，该签名值不验签

	String SRC_FLAG = "srcflag";

	String CMP_PAY_APPLICATION = "cmppayapplication";

	String PAYMENT_APPLY_AMOUNT_SUM = "paymentApplyAmountSum";

	String FIARAP_ARAP_OAP = "fiarap.arap_oap";

	String CURRENCY = "currency";

	String CURRENCY_UNDERLINE = "currency_";

	String CURRENCY_REf = "currencyList.currency";

	String CURRENCY_ENABLE_REf = "currencyList.enable";

	String CURRENCY_DEFAULT_REf = "currencyList.isdefault";

	String UPU_ST_PURCHASEORDER = "upu.st_purchaseorder";

	String SOURCE = "source";

	String OAP_ORI_SUM = "orisum";

	String TOTAL_PAY_APPLY_AMOUNT = "totalPayApplyAmount";

	String CURRENCY_MONEYDIGIT = "currency_moneyDigit";

	String NATCURRENCY = "natCurrency";

	String ARAP_OAP_SPLIT_AMOUNT_SQL = "balance-occupyamount as orisum, id, balance, occupyamount";

	String UPU_PURCHASEORDER_SPLIT_AMOUNT_SQL = "id, totalInvoiceMoney, qty, oriTaxUnitPrice, totalInvoiceQty, invPriceExchRate, oriSum, totalPayApplyAmount";

	String PAYMENT_APPLY_AMOUNT = "paymentApplyAmount";

	String UNPAID_AMOUNT = "unpaidAmount";

	String UNPAID_AMOUNT_SUM = "unpaidAmountSum";

	String PAID_AMOUNT_SUM = "paidAmountSum";

	String PAID_AMOUNT = "paidAmount";

	String ENTERPRISE_BANK_ACCOUNT_LOWER = "enterprisebankaccount";

	String ENTERPRISE_BANK_ACCOUNT_LOWER_NAME = "enterprisebankaccount_name";

	String CASH_ACCOUNT_LOWER = "cashaccount";

	String CASH_ACCOUNT_LOWER_NAME = "cashaccount_name";

	String PAY_STATUS = "paystatus";

	String IS_ENABLED = "isEnabled";

	String SIGNATURE_STR = "signaturestr";

	String BATCH_MODIFY_FLAG = "batchModifyFlag";

	String BATCH_MODIFY = "batchmodify";

	String CODE = "code";

	String REMARK = "remark";

	String SRC_INVOICE_BILL_ITEM_ID = "srcinvoicebillitemid";

	String GET_SHOULD_PAY = "/rest/purinvoice/getShouldPay?yht_access_token=";

	String UP_SRC_BILL_ID = "upsrcbillid";

	String USTOCK_SERVER_NAME = "ustock.servername";

	String QUICK_TYPE_MAPPER = "com.yonyoucloud.fi.cmp.mapper.QuickTypeMapper.getPayApplyBillQuickTypeCode";

	String TRADE_TYPE = "tradetype";

	String DEFAULT_TRADETYPE = "default_tradetype";

	String DEFAULT_TRADETYPE_NAME = "default_tradetype_name";

	String DEFAULT_TRADETYPE_CODE = "default_tradetype_code";

	String FICM3 = "FICM3";

	String CURRENCY_PRICEDIGIT = "currency_priceDigit";

	String PRICEDIGIT = "priceDigit";

	String MONEYDIGIT = "moneyDigit";

	String QUICK_TYPE = "quickType";

	String QUICKTYPE_NAME = "quickType_name";

	String QUICKTYPE_CODE = "quickType_code";

	String C_DEFAULT_VALUE = "cDefaultValue";

	String SUPPLIER = "supplier";

	String SUPPLIER_UNDERLINE = "supplier_";

	String SUPPLIER_BANK_ACCOUNT = "supplierbankaccount";

	String SUPPLIER_BANK_ACCOUNT_ACCOUNT_NAME = "supplierbankaccount_accountname";

	String VENDOR = "vendor";

	String DEFAULT_BANK = "defaultbank";

	String ACCOUNT_NAME = "accountname";

	String PAY_APPLY_BILL_ORI_SUM = "payApplyBillOriSum";

	String CREATE = "create";

	String DELETE = "delete";

	String CMP_PAY_APPLY_BILL = "CMP_PAY_APPLY_BILL";

	String PAY_APPLY_BILL_TO_UPU = "PayApplyBillToUpu";

	String POLINE_ID = "polineId";

	String LINE_ID = "lineId";

	String TOTAL_PAY_AMOUNT = "totalPayAmount";

	String PAYMENT_REQUEST = "payment_Request";

	String ACTION = "action";

	String TYPE = "type";

	String CHECHMATCH = "checkmatch";

	String CHECHFLAG = "checkflag";

	String CM_CMP_FUND_PAYMENT = "CM.cmp_fundpayment";
	String CM_CMP_FUND_COLLECTION = "CM.cmp_fundcollection";

	String CM_CMP_MARGINACCOUNT = "CM.cmp_marginaccount";

	String CM_CMP_CHECKSTOCKAPPLY = "CM.cmp_checkstockapply";
	String CM_CMP_CURRENCYEXCHANGE = "CM.cmp_currencyexchange";
	String CM_CMP_BANKBILLCHECKLIST = "CM.cmp_bankbillchecklist";
	String CM_CMP_BALANCEADJUSTRESULT = "CM.cmp_balanceadjustresult";
	String BALANCEADJUSTRESULTOBJECTCODE = "ctm-cmp.cmp_balanceadjustresultlist";

	String SRCBILLID = "srcbillid";

	String UPCODE = "upcode";

	String USER_OBJECT = "userObject";

	String CREATOR_ID = "creatorId";

	String CREATOR = "creator";

	String JOURNAL_DELETE_OLD_DATA = "journal_delete_old_data";

	String JOURNAL_UPDATE_OLD_DATA = "journal_update_old_data";

	String DELETE_JOURNAL_INSERT_DATA = "delete_journal_insert_data";

	String CM_CMP_INVENTORY = "CM.cmp_inventory";

	String ENABLE = "enable";

	String CMPBANKRECONCILIATIONLIST = "cmpBankReconciliationList";

	String CURRENCYEXCHANGELIST_SINGLESETTLE = "currencyexchangeList_singleSettle";

	String CURRENCYEXCHANGELIST_SINGLEUNSETTLE= "currencyexchangeList_singleUnSettle";

	String MY_BILL_CLAIM_LIST = "cmp_my_bill_claim_list";

	String OPPOSITEACCOUNTNAME = "oppositeaccountname";

	String OPPOSITEOBJECTNAME = "oppositeobjectname";

	String OPPOSITEBANKLINENO = "oppositebanklineno";

	String OPPOSITEBANKADDR = "oppositebankaddr";

	String TARLIST = "tarList";

	String PAY_BILL_B = "PayBillb";

	String BANKACCOUNT_NAME="bankaccount_name";

	String FULL_NAME="fullName";

	String STATUS="status";

	String YONBIP_FI_CTMCMP = "yonbip-fi-ctmcmp";

	String MESSAGE = "message";

	String VOUCHER_STATUS = "voucherstatus";

	String TABLE_NAME = "tableName";

	String VERTICAL_LINE = "\\|";

	String EVENT_INFO = "eventInfo";

	String SRC_SYSTEM_ID = "srcSystemId";

	String SRC_BUSI_ID = "srcBusiId";

	String SRC_EXTRA_INFO = "srcExtraInfo";

	String UPDATE_VOUCHER_STATUS = "com.yonyoucloud.fi.cmp.voucher.updateVoucherStatus";
	String UPDATE_FUND_VOUCHER_STATUS = "com.yonyoucloud.fi.cmp.voucher.updateFundVoucherStatus";


	String UPDATE_VOUCHER_NO_STATUS = "com.yonyoucloud.fi.cmp.voucher.updateVoucherNo";

	String MARAP_OAPLIST = "marap_oaplist";

	String ST_PURCHASE_ORDER_LIST = "st_purchaseorderlist";

	String YCCONTRACTLIST = "yccontractlist";

	String PO_SUBCONTRACT_ORDER_LIST = "po_subcontract_order_list";

	String SUCCESS = "success";
	String ERROR = "error";

	String DATAS="datas";

	String YEAR="year";
	String MONTH="month";
	String DAY="day";
	String CALLBACKURL="callbackUrl";
	String TRANDATE="tranDate";

	String PK = "pk";
	String EXTENDSS = "extendss";
	String FILENAME = "filename";
	String FILESIZE = "filesize";
	String TOKEN = "token";
	String FILESERVER = "fileServer";
	String BOOK = "book";
	String SHOWUPPER = "showUpper";
	String TASKID = "taskId";
	String TICKETINFO = "ticketInfo";
	String SECRETKEY = "secretKey";
	String SERVERTYPE = "serverType";
	String BUCKETNAME = "bucketName";
	String ENDPOINT = "endpoint";
	String OTHERCHECKNO = "other_checkno";
	String DOCPKLIST = "docPkList";
	String SRCSYS = "srcSys";
	String TOTALCOUNT = "totalCount";
	String ORGCODE = "orgCode";
	String DOCTYPE = "docType";
	String ACCOUNTBOOKCODE = "accountBookCode";
	String PAPERINDEX = "paperIndex";
	String YHT_ACCESS_TOKEN = "yht_access_token";
	String ABSTRACTS = "abstracts";

	String DEFALUE_MONEYDIGIT = "1000000";

	String DEFALUEORG = "defaultOrg";

	String TOP_SRC_BILL_ID = "topsrcbillid";

	String TOPSRCBILLID = "topSrcBillId";

	String SOURCE_ORDER_TYPE = "sourceOrderType";

	String STOP_STATUS = "stopstatus";

	String CMP_PAYAPPLICATIONBILL_UNDERLINE  = "yonbip-fi-ctmcmp-cmp_payapplicationbill_";

	String SUPPLIER_NAME = "supplier_name";

	String SUPPLIER_BANK_ACCOUNT_ACCOUNT = "supplierbankaccount_account";

	String SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME = "supplierbankaccount_openaccountbank_name";

	String YONBIP_MM_MFPO = "domain.yonbip-mm-mfpo";

	String GET_OUTSOURCING_ORDER_REQUEST_AMOUNT = "/subcontract/paymentCalu/caluProduct";
	String PAYMENT_TYPE = "paymentType";

	String REQUESTED_PAYMENT = "requestedPayment";

	String PRODUCT_IDS = "productIds";

	String CONTENT_TYPE = "Content-Type";

	String PRODUCT_RESULTS = "productResults";

	String CALU_MONEY_TC = "caluMoneyTc";

	String LIST_MAP = "listMap";

	String ORDER_ID = "orderId";

	String PO_LINE = "poLine";

	String ORDER = "order";

	String PRODUCT_PAYMENT_BACK_WRITE = "ProductPyamentBackWrite";

	String DELIMITER = "\r\n";

	String TRADETYPE = "tradetype";

	String TRADETYPE_NEW = "tradeType";

	String SYNTHESIZE_POST = "synthesize_post";
	String FINANCIAL_POST = "financial_post";
	String BUSINESS_POST = "business_post";

	String OAP_INIT_APPLY = "oap-init-apply";

	String MARAP_AP_OAPINIT = "marap-ap-oapinit";

	String VIRTUALBANK = "virtualBank";

	String ISSETTLE = "isSettle";

	String PAYBANKACCOUNT = "payBankAccount";
	String RECBANKACCOUNT = "recBankAccount";
	String COLLVIRTUALACCOUNT = "collVirtualAccount";
	String PAYVIRTUALACCOUNT = "payVirtualAccount";
	String IS_SUCCESS = "isSuccess";
	String IS_WFCONTROLLED = "isWfControlled";

	String VERIFY_STATE = "verifystate";

	String DEAL_TYPE = "dealtype";

	String IS_TO_PUSH_CSPL = "isToPushCspl";

	String FUND_PLAN_PROJECT = "fundPlanProject";

	String DEPT = "dept";

	String WORD_KEY = "key";
	String THIRD_PAR_VIRT_ACCOUNT = "thirdParVirtAccount";
	String THIRDPARVIRTACCOUNT_NAME = "thirdParVirtAccount_name";
	Integer CONSTANT_TEN = 10;

	Integer CONSTANT_TWO = 2;
	String TWO = "2";
	String WHETHER_SETTLE = "whetherSettle";

	String GROUP_SUM_KEY = "group_sum_key";

	String GROUP_SUM_MONEY = "group_sum_money";

	String RETURN = "return";

	String TENANTID = "tenantId";

	String ACCRUEDENDDATE = "accruedEndDate";
	String ACCRUEDSTARTDATE = "accruedStartDate";

	String BANKTYPE = "bankType";
	String BANKTYPE_LOW = "banktype";

	String CURRENCY_NAME = "currency_name";

	String BANKACCOUNT_ACCOUNT = "bankaccount_account";

	String CM_CMP_CMP_ACCRUALSWITHHOLDINGQUERY = "CM.cmp_accrualsWithholdingquery";

	String BANK_INTEREST = "bank-interest";
	String BANKACCOUNT_ACCTNAME = "bankaccount_acctName";
	String BANKACCOUNT_CODE = "bankaccount_code";
	String ACCOUNT = "account";
	String ACCTNAME = "acctName";
	String BANKTYPE_NAME = "bankType_name";
	String EXCHANGERATE = "exchangerate";
	String LASTINTERESTSETTLEMENTORACCRUEDDATE = "lastInterestSettlementOrAccruedDate";
	String ACCRUAAFTERSETTLEMENT = "accruaAfterSettlement";
	String DAILYSETTLEMENTCONTROL = "dailySettlementControl";

	String FUND_COLLECTION_B = "FundCollection_b";
	String FUND_PAYMENT_B = "FundPayment_b";
	String WITHHOLDING_ID = "withholdingid";
	String FUND_COLLECTION_SUB_ID = "fundcollectionsubid";
	String FUND_PAYMENT_SUB_ID = "fundpaymentsubid";
	String FUND_COLLECTION_SUB_WITHHOLDING_RELATION = "FundCollectionSubWithholdingRelation";
	String FUND_PAYMENT_SUB_WITHHOLDING_RELATION = "FundPaymentSubWithholdingRelation";
	String INTEREST_SETTLEMENT_ACCOUNT = "interestSettlementAccount";
	String THREE_HUNDRED_ONE = "301";
	String BANK_ACCOUNT = "bankaccount";
	//String BANK_ACCOUNT_LOWER = "bankaccount";
	String LAST_INTEREST_SETTLEMENT_END_DATE = "lastInterestSettlementEndDate";
	String CURRENT_INTEREST_SETTLEMENT_END_DATE = "currentInterestSettlementEndDate";
	String CURRENT_INTEREST_SETTLEMENT_START_DATE = "currentInterestSettlementStartDate";
	String CMD_SAVE_AND_SUBMIT = "cmdSaveAndSubmit";
	String CMD_NAME = "cmdname";

	String CMD_INTERESTRATESETTING = "CM.cmp_interestratesetting";

	String BANK_RECONCILIATION = "bankreconciliation";

	String WITHHOLDING_ORI_SUM = "withholdingOriSum";
	String WITHHOLDING_NAT_SUM = "withholdingNatSum";
	String TRAN_DATE = "tran_date";
	String BANK_RECONCILIATION_ID = "bankReconciliationId";
	String INTEREST_SETTLEMENT_ACCOUNT_NAME = "interestSettlementAccount_name";
	Integer MINUS_ONE = -1;
	String LAST_INTEREST_ACCRUED_DATE = "lastInterestAccruedDate";
	String LAST_INTEREST_SETTLEMENT_DATE = "lastInterestSettlementDate";

	String KEY_PREFIX = "yonbip-fi-ctmcmp:";

	String SELECT_HUNDRED_PARAM = "100";

	String UPDATE_VOUCHER_ID = "com.yonyoucloud.fi.cmp.voucher.updateVoucherId";

	String NEXTMONTHCOVER = "nextMonthCover";

	String ASSOCIATIONID = "associationid";

	String WFHDSY = "WFHDSY";

	String HDSY = "HDSY";

	String BEFOREDATE = "beforedate";
	String ISCOVER = "isCover";
	String Y_TENANT_ID = "ytenant";
	String REQUEST_SUCCESS_STATUS_CODE = "200";
	String REQUEST_MISSING_STATUS_CODE = "404";
	String COVERDATE = "coverDate";
	String AUDIT_TIME = "auditTime";
	String AUDIT_DATE = "auditDate";
	String SETTLE_SUCCESS_TIME = "settleSuccessTime";

	List<Short> EVENT_TYPE_FOR_CHECK_FUND_PLAN_PROJECT_LIST = Arrays.asList(
			EventType.ThreePartyReconciliation.getValue(),
			EventType.Unified_Synergy.getValue(),
			EventType.CashMark.getValue(),
			EventType.BillClaim.getValue()
	);

	String SUPPLIER_CORRESPONDENT_CODE = "supplierbankaccount_correspondentcode";
	String AUTOREFUNDFLAG = "autorefundflag";
	String REFUNDDATE = "refunddate";
	String REFUNDMARGINID = "refundmarginid";
	String TRANSTYPEIDREFUND = "transTypeIdRefund";
	String PAYMARGIN = "payMargin";
	String CM_CMP_PAYMARGIN = "CM.cmp_paymargin";
	String CM_CMP_RECEIVEMARGIN = "CM.cmp_receivemargin";
	String CM_CMP_FOREIGNPAYMENT = "CM.cmp_foreignpayment";
	String CM_CMP_TRANSFERACCOUNT = "CM.cm_transfer_account";
	String RECMARGIN = "recMargin";
	String MARGINBUSINESSNO = "marginBusinessNo";
	String MARGINAMOUNT = "marginamount";
	String NATMARGINAMOUNT = "natMarginamount";
	String CONVERSIONAMOUNT = "Conversionamount";
	String SETTLEFLAG = "settleflag";
	String NATCONVERSIONAMOUNT = "natConversionamount";
	String DBMARGINAMOUNT = "dbMarginAmount";
	String DBCONVERSIONAMOUNT = "dbConversionAmount";
	String DBNATMARGINAMOUNT = "dbNatMarginAmount";
	String DBNATCONVERSIONAMOUNT = "dbNatConversionAmount";


	String CONVERSIONMARGINFLAG = "conversionmarginflag";
	String MARGINBUSINESSNO_X = "marginbusinessno";
	String NATMARGINAMOUNT_X = "natmarginamount";
	String CONVERSIONAMOUNT_X = "conversionamount";
	String NATCONVERSIONAMOUNT_X = "natconversionamount";
	String CONVERSIONMARGINID = "conversionmarginid";
	String CONVERSIONMARGINCODE = "conversionmargincode";


	String AUDITORID = "auditorId";
	String AUDITOR = "auditor";
	String DELETEALL = "deleteAll";
	String VOUCHERNO = "voucherNo";
	String VOUCHERPERIOD = "voucherPeriod";
	String VOUCHERID = "voucherId";
	String VOUCHERSTATUS_ORIGINAL = "voucherstatus_original";
	String PUSHTIMES = "pushtimes";
	String FIRST = "first";
	String SECOND = "second";

	//是否期初
	String INITFLAG = "initflag";

	//资金业务对象类型编码
	String CAPTYPRCODE = "TBOT0007";

	//交易类型标识
	String TRADETYPEFLAG = "tradetypeflag";

	String OURBANKACCOUNT_NAME = "ourbankaccount_name";
	String PAYMENTERPRISEBANKACCOUNT_ACCOUNT = "paymenterprisebankaccount_account";
	String OURBANKACCOUNT_ACCOUNT = "ourbankaccount_account";

	String MARGINWORKBENCH_ID = "marginworkbenchId";

	String MARGINVIRTUALACCOUNT = "marginvirtualaccount";

	String VERSION_NO = "versionNo";
	String VERSION_ID = "versionId";
	String PARENT_ID = "parentId";
	String PROTOCOL_CALL_LOGS = "ProtocolCallLogs";
	String TRANSFEREE_INFORMATION = "TransfereeInformation";
	String TRANSFEREE_INFORMATION_ = "TransfereeInformation_";
	String IS_PARENT = "isParent";
	String ENTITY_STATUS = "_status";
	String CONSTANT_V = "V";

	String EXCHANGE_RATE_TYPE = "exchangeRateType";
	String EXCHANGE_RATE_TYPE_DIGIT = "exchangeRateType_digit";
	String DIGIT = "digit";
	String SETTLE_MODE_BANK_TRANSFER_CODE = "system_0001";
	String CA_OBJECT = "caobject";
	String LINE_NO = "lineno";
	String PROJECT = "project";
	String INTERNAL_TRANSFER_PROTOCOL_CONVERT_FUND_PAYMENT = "internalTransferProtocolConvertFundPayment";
	String TRANSFER_OUT_ACCOUNT_ALLOCATION = "transferOutAccountAllocation";
	String FIXED_AMOUNT = "fixedAmount";
	String APPORTIONMENT_METHOD = "apportionmentMethod";
	String APPORTIONMENT_RATIO = "apportionmentRatio";
	Integer SELECT_HUNDRED_PARAM_INTEGER = 100;
	String CMP_FUND_PAYMENT_OTHER = "cmp_fund_payment_other";
	String IS_ENABLED_TYPE = "isEnabledType";
	String ENTERPRISEBANKACCOUNT_ACCOUNT = "enterpriseBankAccount_account";
	String ENTERPRISE_BANK_ACCOUNT_ACCOUNT = "enterprisebankaccount_account";
	String ACCT_OPEN_TYPE = "acctOpenType";

	String SETTLESTATUSCHANGE = "settlestatuschange";
	String IS_DISCARD = "isDiscard";
	String INTO_ACCENTITY = "intoAccentity";
	String DATA_SOURCES = "dataSources";
	String CMP_POINT_INTERNALTRANSFERPROTOCOL = "cmp.internaltransferprotocol";
	String DESCRIPTION = "description";
	String CONTRACT_NO = "contractNo";
	String CONTRACT_NAME = "contractName";
	String INTERNAL_TRANSFER_PROTOCOL_CODE = "internalTransferProtocolCode";
	String FROM_API = "_fromApi";
	String CALLER_PROTOCOL_VERSION = "callerProtocolVersion";
	String ENABLED = "enabled";
	String UNDER_LINE = "_";
	String ENTRY_TYPE = "entrytype";
	String CMP_POINT_FUND_PAYMENT = "cmp.fundpayment";

	String QUICK_TYPE_MAPPER_FUND_PAYMENT_BILL = "com.yonyoucloud.fi.cmp.mapper.QuickTypeMapper.getFundPaymentQuickTypeCode";

	String OPPOSITEOBJECTID = "oppositeobjectid";
	String OPPOSITEACCOUNTID = "oppositeaccountid";
	String OPPOSITEACCOUNTNO = "oppositeaccountno";
	String OPPOSITEBANKADDRID = "oppositebankaddrid";
	String OPPOSITEBANKTYPE = "oppositebankType";
	String CREATE_DATE = "createDate";
	String CREATE_TIME = "createTime";
	String MODIFIER = "modifier";
	String MODIFY_TIME = "modifyTime";
	String MODIFY_DATE = "modifyDate";
	String GENERATED_DOCUMENT_ID = "generatedDocumentId";

	String CONSTANT_NZXY = "NZXY";
	//相关动作锁
	String QUERYTRANSDETAILKEY = BankDealDetail.ENTITY_NAME + "queryTransDetail";//拉取交易明细锁

	String AUTO_PUSH = BankReconciliation.ENTITY_NAME + "autopush";//自动生单

	String QUERYBALANCEBATCH =  "queryBalanceBatch";//拉取余额对账表锁

	String QUERYTRANSDETAILKEY_ZGH = BankDealDetail.ENTITY_NAME + "queryTransDetail_zgh";//中广核小核心推送数据锁

	String SYN_DATA_ZGH = "SYN_DATA_ZGH";
	String QUERYBANKRECEIPTKEY = BankElectronicReceipt.ENTITY_NAME + "queryBankReceipt";//拉取交易回单锁
	String BATCHINSERTBANKRECEIPTKEY = BankElectronicReceipt.ENTITY_NAME + "batchInsertBankReceipt";//批量新增交易明细锁
	String QUERYHISBALANCEKEY = AccountRealtimeBalance.ENTITY_NAME+"queryHistoryBalance";//查询历史余额
	String QUERYREALBALANCEKEY = AccountRealtimeBalance.ENTITY_NAME+"queryRealBalance";//查询实时余额
	String BANKACCOUNTSYNCKEY = BankAccountSetting.ENTITY_NAME+"bankAccountSync";//账户同步定时任务
	String QUERYBANKRECEIPTFILEKEY = BankElectronicReceipt.ENTITY_NAME + "queryBankReceiptFile";//拉取交易回单文件锁

	//数据迁移相关
	//付款 - 资金付
	String MIGRADEPAYTOFUNDPAYMENT = "migradePayToFundPayMent";
	//收款 - 资金收
	String MIGRADERECEIVETOFUNDCOLLECTION = "migradeReceiveToFundCollection";
	//交易类型升级key
	String MIGRADEUPDATETRADETYPE = "migradeUpdateTradetype";
	//特征升级
	String MIGRADEUPDATECHARACTERDEF = "migradeUpdateCharacterDef";

	String AUTO_ASSOCIATION_TASK_LOCK = "AUTO_ASSOCIATION_TASK_LOCK";//自动关联任务锁
	String AUTO_PUSHBILL_TASK_LOCK = "AUTO_PUSHBILL_TASK_LOCK";//自动关联任务锁
	String AUTO_CONFIRMBILL_TASK_LOCK = "AUTO_CONFIRMBILL_TASK_LOCK";//自动关联任务锁
	String FI_EVENT_DATA_VERSION = "fiEventDataVersion";
	String VOUCHER_VERSION = "voucherVersion";
	String GL_VOUCHER_ID = "glVoucherId";
	String GL_VOUCHER_TYPE = "glVoucherType";
	String GL_VOUCHER_NO = "glVoucherNo";

	String ZH_CN = "zh_CN";
	String EN_US = "en_US";

	String ZH_TW = "zh_TW";
	Integer BATCH_QUERY_SIZE = 500;

	String ENTERCOUNTRY = "entercountry"; // 是否入境

	String CASHDIRECTLINK = "cashDirectLink"; // 是否直联

	String ACCTQUALITYCATEGORY = "acctQualityCategory"; //账户性质细分

	// 空白支票
	String CMP_CHECKSTOCK_BLANKCHEQUE = "cmp_checkstock_blankcheque";
	String TRADETYPE_CODE = "tradetype_code";
	String CURRENCY_CODE = "currency_code";
	String PAYBANKNAME = "payBankName";
	String CHECKAPPLYBILLTYPE = "20";
	//2024-04-08 适配河北建工场景 添加
	String INTERNALTRANS_EVENT_SOURCE = "cmp_internaltransferprotocol";//内转协议事件源
	String INTERNALTRANS_EVENT_TYPE_01 = "afterInternalTransferToFundPayme";//内转协议推资金付款单后
	String INTERNALTRANS_YMS_SAMEACCOUNT = "cmp.internaltransfer.sameaccount";//内转内转协议是否校验表头表体银行账户相同，true 允许相同；false 或者 没有 不允许相同
	String IS_AUTO = "isauto";//是否自动
	String INTERNALTRANS_ENTERPRISE_BANK_ACCOUNT = "enterprisebankaccount";//内转协议银行账号
	String STWB = "stwb.settlebench.SettleBench";
	String STWBCHANGE = "stwb.settlementchange.Settlementchange";
	String STWBCHANGE_NEW = "yonbip-fi-ctmstwb.batchchange.ctmstwb_batchchange";
	String STWBCHANGE_NEW_DETAIL = "yonbip-fi-ctmstwb.batchchange.ctmstwb_batchchangedetail";


	String CMP_CHECKREF = "cmp_checkRef";
	String ENTITYNAME = "_entityName";
	String CONDITION = "condition";
	String CHECKBILLSTATUS = "checkBillStatus";
	String RECEIPTTYPE = "receipttype";
	String RECEIPTTYPEB = "receipttypeb";


	String ASSOCIATION_TASK = "associationTask";//自动关联任务
	String VIRTUAL = "virtual";
	String CUSTNO = "custNo";
	String INTO_ACCENTITY_NAME = "intoAccentity_name";
	String ACCOUNTTYPE = "accountType";
	String BANKPROJECTCODE = "CTMCMP001";//业财对账 银行对账方案编码
	String CASHPROJECTCODE = "CTMCMP002";//业财对账 现金对账方案编码
	String ACCOUNTLIST = "accountList";

	//Rpc domain
	// 资金结算
	String STWB_DOMAIN = "stwb";
	// 结算中心
	String STCT_DOMAIN = "ctm-stct";
	// 商业汇票
	String DRFT_DOMAIN = "drft";
	// 资金计划
	String CSPL_DOMAIN = "cspl";
	// 账户管理
	String BAM_DOMAIN = "yonbip-fi-ctmbam";
	// 财资公共
	String TMSP_DOMAIN = "yonbip-fi-ctmtmsp";

	String QUERYELECSTATEMENT = ElectronicStatementConfirm.ENTITY_NAME + "queryElecStatement";//银行电子对账单查询锁
	String STATEMENTFILEDOWMLOAD = ElectronicStatementConfirm.ENTITY_NAME + "statementFileDownload";//银行电子对账单文件下载锁
	String QUERYBILLINFO = BankBillCheck.ENTITY_NAME + "queryBillInfo";//银行电子对账单查询锁
	String CHECKRESULTQUERY = BankBillCheck.ENTITY_NAME + "checkResultQuery";//银企对账确认状态查询锁

	String AUTOBILL_TASK = "autoBillTask";
	String TASK_BALANCE_SUPPLEMENT_LOCK = "TASK_BALANCE_SUPPLEMENT_LOCK";
	String AUTOBILL_AND_PUBLISH_TASK = "autobill_and_publish_task";

	// 批量同名账户划转
	String BATCH_TRANSFER_ACCOUNT = "cmp_batchtransferaccount";

	String BATCH_TRANSFER_ACCOUNT_FIRM_ID = "CM.cmp_batchtransferaccount";

	String BILL_DATE = "billDate";

	//查询实时余额和余额弥补用一个锁，防止并发问题
	String QUERYREALBALANCE_AND_TASK_BALANCE_SUPPLEMENT_COMBINE_LOCK = QUERYREALBALANCEKEY+"AND"+TASK_BALANCE_SUPPLEMENT_LOCK;
	//资金付款单的单据类型id
	String PAYMENTBILLTYPEID = "2553141119111680";
	//资金收款单的单据类型id
	String COLLECTIONBILLTYPEID = "2571640684663808";

	String V5_VERSION = "V5";
	String R6_VERSION = "R6";
	//档案采集使用
	String REQUEST_TYPE = "requestType";
	String QUERY_NUM_FLAG = "queryNumFlag";
	String DOC_DATA = "datas";
	String TOTAL = "total";

    String IMPORT_FLAG = "import";
    Short EXCHANGE_RATE_OPS_MULTIPLY = (short) 1;
    Short EXCHANGE_RATE_OPS_DIVIDE = (short) 2;
    String JOURNAL_BILL_SERVICE_CODE = "CTMCSH0010";

    String JOURNAL_BILL_TRADE_TYPE_BANK = "cmp_journalbill_bank";
    String JOURNAL_BILL_TRADE_TYPE_CASH = "cmp_journalbill_cash";

    String CLASSIFIER = "classifier";
    String FILE_ID = "fileId";
	String SUCCESS_SUM_FIELD = "settlesuccessSum"; //结算成功金额字段名
	String COL_TRANSIT_AMOUNT_FIELD = "oritransitamount"; //在途金额字段名
	String PAY_TRANSIT_AMOUNT_FIELD = "oriTransitAmount"; //在途金额字段名
	String REMAIN_AMOUNT_FIELD = "noriremainamount"; //待结算余额字段名
}


