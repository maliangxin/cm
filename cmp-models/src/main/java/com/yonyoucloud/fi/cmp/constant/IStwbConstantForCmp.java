package com.yonyoucloud.fi.cmp.constant;

public interface IStwbConstantForCmp {

    String ACCENTITY = "accentity";

    String DATA = "data";

    String CURRENCY = "currency";

    String BANKACCT = "bankacct";

    String BANKACCOUNT = "bankaccount";

    String BANK_ACCOUNT = "bankAccount";

    String YHT_USER_ID = "yht_userid";

    String YHT_TENANT_ID = "yht_tenantid";

    String BILL_NUM = "billnum";
    String YHT_ACCESS_TOKEN = "yht_access_token";
    String LOGID = "logId";

    String ID = "id";
    String NAME = "name";
    String CAPBIZOBJ_NAME = "fundbusinobjtypename";

    //供应商账户档案-银行网点和联行号
    String OPEN_ACCOUNT_BANK = "openaccountbank";
    String OPENACCOUNTBANK_LINENUMBER = "openaccountbank_linenumber";
    //供应商账户档案-开户行名
    String OPENACCOUNTBANK_NAME = "openaccountbank_name";
    String OPENBANK = "openBank";
    String OPENBANK_LINENUMBER = "openBank_linenumber";
    String OPENBANK_NAME = "openBank_name";
    String LINENUMBER = "linenumber";
    String JOINTLINENO = "jointLineNo";
    String CORRE_SPONDENT_CODE = "correspondentcode";
    String WDATAORIGIN = "wdataOrigin";
    String BusinessDetailsid = "businessdetailsid";
    String ExternaloutDefine1 = "externaloutdefine1";

    String QUERY_IN = "in";

    String QUERY_EQ = "eq";

    String QUERY_NEQ = "neq";

    String QUERY_EGT = "egt";

    String query_elt = "elt";

    String STCT_MODUAL_NAME = "stct";

    String STWB_MODUAL = "stwb";

    String CSPL_MODUAL_NAME = "cspl";

    String CM_MODUAL_NAME = "cm";

    String LCM_MODUAL_NAME = "lcm";

    String DRFT_MODUAL_NAME = "drft";

    String FDTR_MODUAL_NAME = "fdtr";

    String TMSP_MODUAL_NAME ="tmsp";
    //账户管理
    String BAM_MODUAL_NAME ="bam";

    String QUERY_LIKE = "like";

    String PRIMARY_IDS = "ids";

    String ROWS = "rows";

    String MSG = "msg";
    String STATUS = "status";

    String DATA_CHECK="dataCheckExt";

    String SETTLECHECKEXT =  "settleCheckExt";

    String RETURN_MESSAGE = "message";

    String RETURN_SUCCESS = "success";

    String RETURN_CODE = "code";

    String SETTLE_STATUS = "settlestatus";

    String AUDIT_STATUS = "auditstatus";

    String STWB_MODUAL_NAME = "STWB";

    String BEGIN_DATE = "begindate";

    String END_DATE = "enddate";

    String ORDER_DESC = "desc";

    String ORDER_ASC = "asc";

    String ENTERPRISE_BANK_ACCOUNT = "enterpriseBankAccount";

    // String ENTERPRISE_CASH_ACCOUNT = "enterpriseBankAccount";

    String ENABLE_STATUS = "enable";

    String ORG_ID = "orgid";

    String KEY = "key";

    String PARAM = "param";

    String VOUCHDATE = "vouchdate";

    String TENANT = "tenant";

    String TENANTID = "tenantId";

    String FAILED = "failed";

    String SELECT_TOTAL_PARAM = "*";

    String SELECT_ONE_PARAM = "1";

    String TWO_PARAM = "2";

    Short SELECT_TWO_PARAM = 2;

    String COMMONVOS = "commonVOs";

    String AUTO_BILL_FLAG = "auto";

    String SERVICECODE_SETTLEBENCH = "stwb_settlebenchlist";

    Short SETTLE = 0;

    Short CODE_ONE = 1;

    Short CODE_ZERO = 0;

    Short LIST_SIZE_ONE = 1;

    Short SETTLE_BILL = 111;

    String RMB_CH = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180431","人民币") /* "人民币" */;

    String CNY = "CNY";
    String RMB_EN = "RMB";
    String CHENK = "check";

    String EMPLOY = "employ";

    String RELEASE = "release";
    String OLDINDEXSTATUS = "oldIndexStatus";

    String OPENWSETTLESTATUS = "openwsettlestatus";

    String SETTLEMENTID = "settlementid";

    String TENANT_ID = "tenant_id";
    String INDEXSTATUS = "indexstatus";
    String REQUESTSEQNO = "requestseqno";
    String BATNO = "batno";
    String TRANSEQNO = "transeqno";
    String PUBTS = "pubts";
    String REQDATA = "reqData";
    String REQSIGNDATA = "reqSignData";
    //默认字符集
    String DEFAULTCHARSET = "UTF-8";

    String ERROR_CODE = "error-code";
    String ERROR_DESC = "error-desc";
    String DEALSUCCEED = "dealSucceed";
    String FAILUREMESSAGE = "failureMessage";

    String CORRECTSTATUS = "correctstatus";

    String DEFAULTSERVICESTATUS = "00";
    String RESPONSE_HEAD = "response_head";
    String SERVICE_STATUS = "service_status";
    String RESPONSE_BODY = "response_body";
    String BANK_RESP_CODE = "bank_resp_code";
    String BANK_RESP_DESC = "bank_resp_desc";
    String PAY_STATUS = "pay_status";
    String SERVICE_RESP_CODE = "service_resp_code";
    String SERVICE_RESP_DESC = "service_resp_desc";
    //跨行标识
    String TO_BANK_TYPE = "to_bank_type";
    String OTHERBANKFLAG = "otherBankFlag";
    //收方类型  01：对私；02：对公；03：银行内部户
    String TO_ACCT_TYPE = "to_acct_type";
    //对私
    String FORPRIVATE = "01";
    //对公
    String TOPUBLIC = "02";
    //银行内部户
    String TOBANKACCOUNT = "03";
    //收方账号
    String TO_ACCT_NO = "to_acct_no";
    //收方户名
    String TO_ACCT_NAME = "to_acct_name";
    //收方开户行联行号
    String TO_BANK_NO = "to_bank_no";
    //收方开户行号
    String TO_BRCH_NO = "to_brch_no";
    //收方开户行名
    String TO_BRCH_NAME = "to_brch_name";
    //收方地址
    String TO_ADDR = "to_addr";
    String TO_CITY_CODE = "to_city_code";
    String NULL = "";
    //默认金额精度
    String DEFALUE_MONEYDIGIT = "1000000";
    //交易金额
    String TRAN_AMT = "tran_amt";
    //用途
    String USE_DESC = "use_desc";
    //附言
    String REMARK = "remark";
    //通 知 手 机号
    String PHONE_NO = "phone_no";
    //通知内容
    String SMS_STRING = "sms_string";
    //电 子 邮 件地址
    String EMAIL = "email";
    //电 子 邮 件内容
    String MAIL_DESC = "mail_desc";
    //客 户 自 定义 回 推 数据
    String BACK_PARA = "back_para";
    //客户回推地址
    String BACK_URL = "back_url";
    //备注
    String MEMO = "memo";
    //归集类型
    String CASH_TYPE = "cash_type";


    //收方银行SWIFT码
    String TO_BANK_SWIFT = "to_bank_swift";
    //收款人账户银行代号
    String TO_BANK_CODE = "to_bank_code";
    //收方银行所在国家
    String TO_BANK_COUNTRY_CODE = "to_bank_country_code";
    //交易币种
    String CURR_CODE = "curr_code";
    //此笔款项是否为保税货物项下付款
    String TAX_FREE_GOODS_RELATED = "tax_free_goods_related";
    //银行费用
    String CHARGE_BEARER = "charge_bearer";
    //银行费用扣费账户
    String DEBIT_ACCOUNT_BANK_CHARGE = "debit_account_bank_charges";
    //收方币种
    String TO_CURR_CODE = "to_curr_code";
    //收款人类别
    String TO_BENE_TYPE = "to_benetype";
    //加急标志
    String PRIORITY = "priority";
    //付款类型
    String PAYMENT_TYPE = "paymenttype";
    //手续费/代理费 收取方式
    String CHARGE_SOPTION = "chargesoption";
    //境内外标志
    String INOUT_FLAG = "inout_flag";
    //代理行账号
    String COR_BANK_NO = "cor_bank_no";
    //代理行国家/地区
    String COR_COUNTRY_CODE = "cor_country_code";
    //代理行SWIFT CODE
    String COR_BANKSWIFT = "cor_bank_swift";
    //收款人常驻国家(地区)代码
    String TO_NATION_CODE = "to_nation_code";
    //付款性质
    String PAY_NATURE = "pay_nature";
    //汇款用途
    String PAY_USE_DESC = "pay_use_desc";
    //汇率合约
    String EXCON_TRACT = "excontract";
    //付方国家
    String BANK_COUNTRY_CODE = "bank_country_code";
    //新增附言 文档如此
    String NEW_REMARK = "remark01";
    //用户说明 文档如此
    String USER_EXPLAIN = "user_name";
    //文件信息(文件名)
    String FILE_INFO = "file_info";
    //交易编码
    String TRANS_CODE = "trans_code";
    //预计报关单日期
    String BILL_WDATE = "bill_wdate";

    String REQUEST_HEAD = "request_head";
    String REQUEST_BODY = "request_body";
    String ISSUCCESS = "isSuccess";
    String VERSION = "version";
    String REQUEST_SEQ_NO = "request_seq_no";
    //客户号
    String CUST_NO = "cust_no";
    //渠道号
    String CUST_CHNL = "cust_chnl";
    String REQUEST_DATE = "request_date";
    String REQUEST_TIME = "request_time";
    //操作员
    String OPER = "oper";
    //操作员签名
    String OPER_SIGN = "oper_sign";
    //交易码
    String TRAN_CODE = "tran_code";
    //是否验证天威签名  1 true  0 false
    String ISVERIFYSIGNATURE = "1";
    //收款方非中文户名：to_acct_fnname
    String TO_ACCT_FNNAME = "to_acct_fnname";
    //收款方非中文开户行名：to_brch_fnname
    String TO_BRCH_FNNAME = "to_brch_fnname";
    //收方非中文地址to_fnaddr
    String TO_FNADDR = "to_fnaddr";
    String SETTLEBENCHDETAILID =  "settleBenchDetailId";


    /**
     * 结算方式 银行业务
     */
    int SERVICEATTR_BANK = 0;

    /**
     * 结算方式 现金业务
     */
    int SERVICEATTR_CASH = 1;

    /**
     * 结算方式 票据业务
     */
    int SERVICEATTR_DIRT = 2;

    /**
     * 结算方式 支票
     */
    int SERVICEATTR_NOTE = 8;

    /**
     * 结算方式 信用证
     */
    int SERVICEATTR_LCM = 9;

    /**
     * 结算方式 第三方
     */
    int SERVICEATTR_VIR = 10;
    /**
     * 资金业务对象类型是核算会计 主体
     */
    String CAP_ACCENTITY ="TBOT0007";

    boolean BOOLEANFALSE = false;

    boolean BOOLEANTRUE = true;

    String FALSE = "0";

    String TRUE = "1";
    //待结算数据校验
    String BIZ_VALID = "406";
    //可疑校验
    String DOUBT = "555";
    //通过待结算数据校验
    String SUCCESS = "200";
    //系统异常
    String EXCEPTION = "500";

    //自动结算正常校验
    String VAILDBYSETTLE = "103";

    //自动结算正常校验缺资金计划
    String VAILDBYCSPL = "100";

    String DEL = "Delete";


    String ManTreatExt = "manTreatExt";
    String ScheduledTreatExt = "scheduledTreatExt";

    String SAVE = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418042C","保存") /* "保存" */;
    String AUDIT = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418042D","审核") /* "审核" */;
    String SETTLING = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418042F","结算中") /* "结算中" */;

    //日结查询常量
    String SETTLEMENT_FULL_NAME = "cmp.settlement.Settlement";
    String SETTLEMENT_QUERY_DATA = "id,settleflag";
    String SETTLEMENT_DATE = "settlementdate";
    String SETTLE_FLAG = "settleflag";
    String CTM_DOMIN = "CM.DOMAIN";

    //资金计划控制常量
    String CSPL_DOMIN = "diwork.rpc.csplDomain";
    String CSPL_STRATEGY_FULL_NAME = "cspl.strategyset.StrategySet";
    String CSPL_QUERY_DATA = "id";
    String STRATEGY_STATUS = "strategyStatus";

    //自动结算规则
    String RULE_QUERY_DATA = "*";
    String ENABLE_OUT_AGE_STATUS = "enableoutagestatus";
    String IS_HISTORY = "isHistory";
    String ORDERNUMBER = "ordernumber";
    String GLOBAL_ACCENTITY = "666666";

    //自动结算规则编码
    String RULE_ENCODE = "Auto006";
    String RULE_DONECODE = "Auto001";
    String IS_ENABLE = "isEnabled";
    String SERVICE_ATTR = "serviceAttr";

    //币种查询
    String CURRENCY_FULL_NAME = "bd.enterprise.BankAcctCurrencyVO";
    String UCF_BASE_DOC = "ucfbasedoc";

    String SETTLEBENCH = "stwb_settlebench";

    String SETTLECHANGE= "stwb_settlementchange";

    String DZDATA = "dzdate";

    //数据格式
    String BIGDECIMAL_FORMAT = "0.00";
    String COUNT_DETAILS = "1";


    String BILL_TYPE_FULL_NAME = "bd.bill.BillTypeVO";
    String TRANS_TYPE = "transtype";
    String BILL_TYPE_QUERY_DATA = "id,code";
    String DR = "dr";
    String TRANS_TYPE_FULL_NAME = "bd.bill.TransType";
    String TRANS_TYPE_QUERY_DATA = "id,billtype_id,code,name";
    String BILL_TYPE_ID = "billtype_id";
    String ORG_BP_FULL_NAME = "bd.orgBpConf.OrgBpOrgConfVO";
    String ORG_BP_QUERY_DATA = "periodid,periodid.begindate as begindate,periodid.enddate as enddate,type_code,orgid,enable";
    String TYPE_CODE = "type_code";
    /**
     * 票据校验的提示信息
     * @author wangshbv
     * @date 11:04
     */
    public interface DrftCheckMsg{
        String COUNTER_TYPE_EMPTY = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180429","所选票据为电票时对方名称不能为空！") /* "所选票据为电票时对方名称不能为空！" */;

        String COUNTER_BANKACC_EMPTY = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418042A","所选票据为电票时对方银行账号不能为空！") /* "所选票据为电票时对方银行账号不能为空！" */;

        String PARTY_BANKACCOUNT_EMPTY = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418042E","所选票据为电票时对方账户名称不能为空！") /* "所选票据为电票时对方账户名称不能为空！" */;

        String COUNTER_PARTYBANKNAME_EMPTY =com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180430","所选票据为电票时对方开户行名不能为空！") /* "所选票据为电票时对方开户行名不能为空！" */;

        String COUNTER_PARTYBANKCODE_EMPTY =com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180428","所选票据为电票时对方开户行联行号不能为空！") /* "所选票据为电票时对方开户行联行号不能为空！" */;

    }

    //国际结算
    String USD = "USD";
    String HKD = "HKD";
    String EUR = "EUR";
    String ZHONGYIN_BANK_CODE = "989";
    String MY = "MY";
    String COUNTRY_NAME_MY = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA0418042B","马来西亚") /* "马来西亚" */;
    String BANK_FLAG_LOCAL = "01";
    String BANK_FLAG_LOCAL_OTHER = "03";
    String BANK_FLAG_UNLOCAL_OTHER = "04";
    String BANK_FLAG_OUT_OTHER = "05";

    // 结算疑重规则
    String PAGE_STATE = "_status";
    String REQUEST_DATA = "requestData";
    Short RETURN_TRUE = 1;
    Short RETURN_FALSE = 0;
    String MAIN_ID_ACCENTITY = "mainid.accentity";
    String MAIN_ID_ID = "mainid.id";

    String MAIN_ID_CODE = "mainid.code";
    String MAIN_ID_STATEMENT_STATUS = "mainid.statementstatus";
    String ORIGINALCURRENCY = "originalcurrency";
    String SETTLEDETAIL_ID = "settleDetailId";
    String ORIGINALCURRENCYAMT = "originalcurrencyamt";
    String COUNTERPARTYNAME = "counterpartyname";
    String SETTLEMODE = "settlemode";
    String COUNTERPARTYBANKACC = "counterpartybankacc";
    String DESCRIPTION = "description";
    String BIZSYSSRC = "bizsyssrc";
    String SUBMITTIME = "submitTime";
    String RECEIPTTYPEB = "receipttypeb";
    String ID_AND_CODE =  "id,code";
    String PAYAMOUNT = "payAmount";
    String RECBANKNO = "recbankno";
    String BANCKUP7 = "banckup7";
    String MODIFY_DATE = "modify_date";
    String USE_NAME = "use_name";
    String YONBIP_FI_CTMRSM = "YONBIP-FI-CTMRSM";
    String ENABLEOUTAGESTATUS = "enableOutageStatus";

    // 智能配款规则
    String ORDER_NUMBER = "orderNumber";
    String RULE_NAME = "ruleName";
    String STRING_TRUE = "true";
    String STRING_FALSE = "false";
    String ENABLESTATUS = "enableStatus";
    String SETTLEMODEBUSATTR = "settleModeBusAttr";
    String ASSIGNMENTTIME = "assignmentTime";

    //调度任务加锁的key
    String AUTOSUBMITPAYINDEX = "autoSubmitPayIndex";
    String ASYNCHRONIZED = "asynchronized";

    static final int TASK_BACK_SUCCESS = 1;// 定时任务执行成功
    static final int TASK_BACK_FAILURE = 0;// 定时任务执行失败

    //预警任务
    String LOG_ID = "logId";
    String USER_ID = "userId";
    String TENANT_ID_WARN = "tenantId";
    Integer WARNING_FAIL = 0;
    Integer WARNING_SUCCESS = 1;
    String OVERTIMEDATASETTLED = "overTimeDataSettled";
    String BILLID = "billId";

    //调度任务加锁的key
    String AUTO_UPDATE_PAYINDEX = "autoUpdatePayIndex";
}

