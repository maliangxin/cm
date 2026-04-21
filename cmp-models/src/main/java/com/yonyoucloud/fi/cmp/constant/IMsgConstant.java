package com.yonyoucloud.fi.cmp.constant;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

/**
 * 业务日志-多语-常量
 */
public interface IMsgConstant {

    String MENU_BAR_NAME = "全球司库";  //该资源不需要多语翻译，因为数据库多语，需要直接赋值//@notranslate
    String MENU_BAR_NAMEEXT2 = "Treasury Services";
    String MENU_BAR_NAMEEXT3 = "全球司庫"; //该资源不需要多语翻译，因为数据库多语，需要直接赋值//@notranslate

    //菜单名称
    String BANK_JOURNAL = "UID:P_CM-BE_17FE975C0418010F"/* "银行日记账" */;
    String BANK_ACCOUNT_SETTING = "UID:P_TMSP-FE_1B5EDD380430006D"/* "账户直连状态" */;
    String CURRENCY_EXCHANGE = "P_YS_PF_GZTSYS_0000096387"/*外币兑换工作台*/;
    String BANK_INITDATA = "P_YS_PF_GZTSYS_0000013353"/*"银行账户期初" */;
    String CASH_INITDATA = "P_YS_PF_GZTSYS_0000013416"/*"现金账户期初" */;
    String PAY_APPLICATION = "P_YS_FI_YYFI-UI_0001145851"/*"付款申请" */;
    String RECEIVE_BILL = "P_YS_PF_GZTSYS_0000013238"/*收款工作台*/;
    String RECONCILIATE = "P_YS_PF_GZTSYS_0000013167"/*"银行对账" */;
    String DAY_SETTLE = "P_YS_PF_GZTSYS_0000013390"/*"日结" */;
    String PAYMENT_BILL = "P_YS_PF_GZTSYS_0000013450"/*"付款工作台" */;
    String TRANSFER_ACCOUNT = "P_YS_PF_GZTSYS_0000096384"/*"转账工作台" */;
    String SALARY_PAY = "P_YS_PF_GZTSYS_0001106612"/*薪资支付工作台*/;
    String BANKRECONCILIATION = "P_YS_PF_GZTSYS_0000013132"/*银行对账单*/;
    String BILLCLAIMCARD = "P_YS_CTM_CM-FE_1432909795583590415"/*对账单认领*/;
    String ACCHISBAL = "P_YS_CTM_STCT-UI_0001317402"/*账户历史余额*/;
    String INWARD_REMITTANCE = MessageUtils.getMessage("P_YS_CTM_CM-BE_1655516670693736456") /* "汇入汇款" */;
    String FUND_PAYMENT = "UID:P_CM-BE_21B2A42005400437"/* "资金付款单" */;
    String FUND_COLLECTION = "P_YS_CTM_CM-UI_0001555910"/* "资金收款单" */;
    //按钮操作名称
    String ADD = "UID:P_CM-BE_17FE975C04180044"/*新增*/;
    String UPDATE = "UID:P_CM-BE_1ED638E805700008"/*更新*/;
    String DELETE = "UID:P_CM-BE_17FE975C04180045"/*删除*/;
    String ROLLBACK = "UID:P_CM-BE_1ED631B805000006"/*回滚*/;
    String SETTLE = "P_YS_PF_PROCENTER_0000027268"/*结算*/;
    String UNSETTLE = "P_YS_PF_PROCENTER_0000027034"/*取消结算*/;
    String OPEN_BANK_ACCOUNT_SETTING = "P_YS_FI_YYFI-UI_0001145902"/* "启用银企联" */;
    String CLOSE_BANK_ACCOUNT_SETTING = "P_YS_FI_YYFI-UI_0001145892"/* "停用银企联" */;
    String OPEN_ELECTRONIC_BILL = "P_YS_FI_YYFI-UI_0001145901"/* "启用电票服务" */;
    String CLOSE_ELECTRONIC_BILL = "P_YS_FI_YYFI-UI_0001145891"/* "停用电票服务" */;
    String UPDATE_CUSTOM_NO = "UID:P_CM-UI_18108E6604B819CE"/* "更新客户号" */;
    String ACCOUNT_SYNCHRONOUS = "P_YS_CTM_CM-UI_0001290533"/*"账户同步" */;
    String OPEN = "P_YS_FED_UCFBASEDOC_0000025132"/*"打开" */;
    String CLOSE = "P_YS_FED_EXAMP_0000020183"/*"关闭" */;
    String AUTO_CHECK = "P_YS_SD_SDOC_0000165499"/* "自动勾对" */;
    String HAND_CHECK = "P_YS_FI_YYFI-UI_0000161413"/*"手动勾对" */;
    String CANCEL_CHECK = "P_YS_SD_SDOC_0000165000"/*取消勾对*/;
    String ONE_SIDE_CHECK = "P_YS_FI_YYFI-UI_0000161153"/*"单边对账" */;
    String AUTO_BILL = "P_YS_FI_YYFI-UI_0000161641"/*"自动生单" */;
    String CLOSING = "ficloud.001446"/*"结账" */;
    String CANCELCLOSING = "P_YS_FI_AP_0000072104"/*取消结账*/;
    String BANK_PREORDER = "P_YS_PF_PROCENTER_0000027169"/*网银预下单*/;
    String CANCEL_PREORDER = "P_YS_PF_PROCENTER_0000027169"/*取消预下单*/;
    String BANK_QUERY = "UID:P_CM-UI_1C4DB4F4042001FF"/*查询支付状态*/;
    String BANK_PAY = "P_YS_PF_PROCENTER_0000027500"/*网银支付*/;
    String OFFLINE_PAY = "P_YS_PF_GZTSYS_0000012014"/*线下支付*/;
    String CANCEL_OFFLINE_PAY = "P_YS_PF_PROCENTER_0000027244"/*取消线下支付*/;
    String CMDFUNDPAYMENT = "P_YS_CTM_CM-UI_1452769827452092416"/*下推资金付款单*/;
    String CMDFUNDCOLLECTION = "P_YS_CTM_CM-UI_1452769827452092419"/*下推资金收款单*/;
    String CMDPUBLISH = "YS_YC_PORTAL-FE_6304695"/*发布*/;
    String CMDPUBLISHDISPATCH = MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933125") /* "分配业务对接人" *//*分配业务对接人*/;
    String CMDNOPUBLISH = "YS_YC_PORTAL-FE_2571106601"/*取消发布*/;

    String CANCLE_REPEAT = "取消疑重";//@notranslate
    String CONFIRM_REPEAT = "确认重复";//@notranslate
    String CONFIRM_NORMAL = "确认正常";//@notranslate

    String ARAP_OAP = "P_YS_FI_ETL_0001077747"/*应付事项*/;
    String FIARAP_OAP_PUSH_CTMCMP_PAY_APPLY_BILL = "P_YS_CTM_CM-BE_1479669491150880772"/* "应付事项推付款申请" */;

    String ST_PURCHASE_ORDER ="P_YS_PF_GZTSYS_0000012585" /* "采购订单" */;

    String PO_SUBCONTRACT_ORDER = MessageUtils.getMessage("P_YS_PF_METADATA_0001039706") /* "委外订单" */;

    String UPU_PURCHASE_PLAN_LINE_PUSH_PAY_APPLY_BILL = "P_YS_CTM_CM-BE_1483318186127917066" /* "采购订单计划明细行推付款申请" */;

    String PUSH_PAY_APPLICATION_BILL_SPLIT_AMOUNT = MessageUtils.getMessage("P_YS_CTM_CM-BE_1516018641192091667") /* "推付款申请的单据根据总金额分单" */;

    String OUTSOURCING_ORDER_PRODUCT_LINE_PUSH_PAY_APPLY_BILL = MessageUtils.getMessage("P_YS_CTM_CM-BE_1502701708539068436") /* "委外订单产品明细行推付款申请" */;

    String OUTSOURCING_ORDER_PUSH_PAY_APPLY_BILL = MessageUtils.getMessage("P_YS_CTM_CM-BE_1565763347557646426") /* "委外订单整单推付款申请" */;

    String UPU_PURCHASE_PUSH_PAY_APPLY_BILL = MessageUtils.getMessage("P_YS_FI_CM_1516030409388326919") /* "采购订单推付款申请" */;

    String YCCONTRACTLIST_PUSH_PAY_APPLY_BILL = MessageUtils.getMessage("P_YS_FI_CM_1516030409388326916") /* "采购合同推付款申请" */;

    String YCCONTRACTLIST_ORDER = MessageUtils.getMessage("P_YS_PF_GZTSYS_0000012789") /* "采购合同" */;

    String COOPERATE_FUND_COLLECTION = MessageUtils.getMessage("P_YS_CTM_CM-BE_1572948965274419226") /* "协同资金收款单" */;
    String AUDIT = MessageUtils.getMessage("YS_PF_MSPLAT_audit") /* "审核" */;

    String QUERY_ACCHISBAL = "P_YS_FI_CM_1420973557473607680"/*历史余额拉取*/;
    String QUERY_ACCREALTIMEBAL = "P_YS_CTM_STCT-UI_0001317409"/*账户实时余额*/;
    //相关日志key名称
    String BILL_DATA = "billData";/*单据信息*/
    String BANK_PLACEORDER_REQUEST ="bank_placeorder_request";/*预下单请求报文*/
    String BANK_PLACEORDER_RESPONSE ="bank_placeorder_response";/*预下单应答报文*/
    String CONFIRM_PLACEORDER_REQUEST ="confirm_placeorder_request";/*支付确认请求报文*/
    String CONFIRM_PLACEORDER_RESPONSE ="confirm_placeorder_response";/*支付确认应答报文*/
    String ACCBAL_REQUEST = "accbal_request"/*账户余额请求报文*/;
    String ACCBAL_RESPONSE = "accbal_response"/*账户余额应答报文*/;
    String YQL_REQUEST = "yql_request"/*银企联请求报文*/;
    String YQL_RESPONSE = "yql_response"/*银企联应答报文*/;
    String YQL_URL = "yql_url"/*银企联请求地址*/;
    String CURRENCY_EXCHANGE_RATE_QUERY = MessageUtils.getMessage("P_YS_CTM_CM-BE_1628845129937190916") /* "外币兑换询价" */;
    String CURRENCY_EXCHANGE_SUBMIT = MessageUtils.getMessage("P_YS_CTM_CM-BE_1633086916949704713") /* "即期结售汇交易提交" */;
    String CURRENCY_EXCHANGE_DELIVERY = MessageUtils.getMessage("P_YS_CTM_CM-BE_1633086916949704716") /* "即期结售汇交割" */;
    String CURRENCY_EXCHANGE_RESULT_QUERY = MessageUtils.getMessage("P_YS_CTM_CM-BE_1633086916949704719") /* "结售汇交易结果查询" */;
    String FIARAP_OAP_INIT_PUSH_CTMCMP_PAY_APPLY_BILL = MessageUtils.getMessage("P_YS_CTM_CM-BE_1633196756466401295") /* "应付事项期初推付款申请" */;
    String ARAP_OAP_INIT = MessageUtils.getMessage("P_YS_PF_GZTSYS_0000013438")/*应付事项期初*/;
    String INWARD_REMITTANCE_LIST_QUERY = MessageUtils.getMessage("P_YS_CTM_CM-BE_1666635147045240842") /* "汇入汇款待确认业务列表查询" */;
    String INWARD_REMITTANCE_SUBMIT =  MessageUtils.getMessage("P_YS_CTM_CM-BE_1666635147045240839") /* "汇入汇款确认提交" */;
    String INWARD_REMITTANCE_RESULT_QUERY = MessageUtils.getMessage("P_YS_CTM_CM-BE_1666635147045240854") /* "汇入汇款确认交易结果查询" */;
    String INWARD_REMITTANCE_DETAIL_QUERY = MessageUtils.getMessage("P_YS_CTM_CM-BE_1666635147045240860") /* "汇入汇款业务明细查询" */;

    String THIRD_PARTY_TRANSFER_ACCOUNT = MessageUtils.getMessage("P_YS_CTM_CM-BE_1680695624434450441") /* "第三方推转账工作台" */;

    String CMP_ACCRUALSWITHHOLDINGQUERY = "银行账户预提";//@notranslate
    String CMP_UNACCRUALSWITHHOLDINGQUERY = "银行账户反预提";//@notranslate
    String CMP_INTERESTRATESETTING = "银行账户利率设置";//@notranslate
    String CMP_WITHHOLDINGRELESETTING = "预提规则设置";//@notranslate

    String CMP_AUTOCONFIG_UPDATE = "现金基础参数修改";//@notranslate


    String CMP_ENABLE = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932E9EE0408000D", "启用") /* "启用" */;
    String CMP_UNENABLE = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932E9EE0408000E", "停用") /* "停用" */;
    String CMP_INTERNAL_TRANSFER_PROTOCOL = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932E9EE0408000F", "内转协议") /* "内转协议" */;
    String INTERNAL_TRANSFER_PROTOCOL_CONVERT_FUNDPAYMENT = "UID:P_CM-BE_1932E9EE0408000C" /* "内转协议单生成资金付款单" */;

    String FUND_PAYMENT_POSTING = "资金付款单过账";//@notranslate

    String FUND_COLLECTION_POSTING = "资金收款单过账";//@notranslate

    String FUND_COLLECTION_PUSH_DATA_SETTLES = "资金收款单传待结算";//@notranslate
    String FUND_PAYMENT_PUSH_DATA_SETTLES = "资金付款单传待结算";//@notranslate
    String FUND_PAYMENT_RELATION_SETTLES = "资金付款单关联结算";//@notranslate
    String FUND_COLLECTION_RELATION_SETTLES = "资金收款单关联结算";//@notranslate

    String FUND_COLLECTION_DELETE_DATA_SETTLES = "资金收款单删除待结算";//@notranslate
    String FUND_PAYMENT_DELETE_DATA_SETTLES = "资金付款单删除待结算";//@notranslate

    String SETTLE_CANCEL_VALIDATE_FUND_PAYMENT= "结算单取消结算校验资金付款单是否允许删除";//@notranslate

    String SETTLE_CANCEL_VALIDATE_FUND_COLLECTION= "结算单取消结算校验资金收款单是否允许删除";//@notranslate

    String BANK_ELEC_RECEIPT = MessageUtils.getMessage("P_YS_FI_CM_0000026186") /* "银行电子回单" */;
    String BANK_ELEC_RECEIPT_FILE_DOWN =MessageUtils.getMessage("UID:P_CM-UI_18108E6604B81986") /* "回单下载" */;
    String CMP_ELECTRONIC_STATEMENT_QUERY =MessageUtils.getMessage("UID:P_CM-UI_18E6CAEA04680196") /* "对账单获取" */;
    String CMP_ELECTRONIC_STATEMENT_DOWN =MessageUtils.getMessage("UID:P_CM-UI_18E6CAEA04680192") /* "对账单文件下载" */;

    String FUND_PAYMENT_EMPLOY_AND_RELEASE_FUND_PLAN = "资金付款单实占或释放资金计划";//@notranslate
    String FUND_PAYMENT_PRE_EMPLOY_AND_RELEASE_FUND_PLAN = "资金付款单预占或释放资金计划";//@notranslate
    String FUND_COLLECTION_EMPLOY_AND_RELEASE_FUND_PLAN = "资金收款单实占或释放资金计划";//@notranslate
    String FUND_COLLECTION_PRE_EMPLOY_AND_RELEASE_FUND_PLAN = "资金收款单预占或释放资金计划";//@notranslate

    String CHECKSTOCK_OPERATION = "操作重空凭证";//@notranslate
}
