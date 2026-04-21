package com.yonyoucloud.fi.cmp.constant;

/**
 * billnum的集中类
 * @author maliang
 * @version V1.0
 * @date 2021/6/22 10:03
 * @Copyright yonyou
 */
public interface IBillNumConstant {

    //账户定期余额
    String CMP_REGULARBALANCE = "cmp_regularbalance";
    String CMP_REGULARBALANCELIST = "cmp_regularbalancelist";
    //手工日结
    String CMP_SETTLEMENTLIST = "cmp_settlementlist";
    // 现金参数
    String CMP_AUTOCONFIG = "cmp_autoconfig";

    // 收款工作台
    String RECEIVE_BILL  = "cmp_receivebill";
    String RECEIVE_BILL_LIST  = "cmp_receivebilllist";
    String RECEIVE_BILL_UPDATE  = "cmp_receivebill_update";//结算变更

    //付款工作台
    String PAYMENT  = "cmp_payment";

    String PAYBILL  = "cmp_paybill";

    String PAYMENTLIST  = "cmp_paymentlist";
    /**
     * 付款工作台支付变更
     */
    String PAYMENT_UPDATE  = "cmp_payment_update";
    /**
     * 付款工作台结算变更
     */
    String PAYMENT_SETTLE  = "cmp_payment_settlement";

    //付款申请工作台
    String PAYAPPLICATIONBILLLIST = "cmp_payapplicationbilllist";
    String PAYAPPLICATIONBILL = "cmp_payapplicationbill";
    String PAYAPPLICATIONBILL_B = "payApplicationBill_b";

    //转账工作台
    String TRANSFERACCOUNTLIST = "cm_transfer_account_list";
    String TRANSFERACCOUNT = "cm_transfer_account";

    // 薪资支付工作台
    String SALARYPAYLIST = "cmp_salarypaylist";
    String SALARYPAY = "cmp_salarypay";
    String SALARYSETTLE = "cmp_salarysettle";
    /**
     * 薪资支付工作台支付变更
     */
    String SALARYPAY_UPDATE = "cmp_salaryupdate";
    /**
     * 薪资支付工作台结算变更
     */
    String SALARYPAY_SETTLE = "cmp_salarysettle";

    // 外币兑换工作台
    String CURRENCYEXCHANGELIST = "cmp_currencyexchangelist";
    String CURRENCYEXCHANGE = "cmp_currencyexchange";
    //外币兑换申请
    String CURRENCYAPPLY = "cmp_currencyapply";

    // 银行对账单
    String  BANKRECONCILIATIONLIST = "cmp_bankreconciliationlist";
    String  BANKRECONCILIATION = "cmp_bankreconciliation";

    //银行电子对账单
    String  ELECTRONICSTATEMENTCONFIRMLIST = "cmp_electronicstatementconfirmlist";

    // 余额调节
    String BALANCEADJUSTRESULTLIST = "cmp_balanceadjustresultlist";
    String BALANCEADJUSTRESULT = "cmp_balanceadjustresult";
    String BALANCEADJUSTRESULT_BANK="cmp_balanceadjustbankreconciliation";
    String BALANCEADJUSTRESULT_JOURAL="cmp_balanceadjustjournal";

    String FUND_TABLE_NAME = "stwb_settleapply_fund_assistant";

    // 资金收款单
    String FUND_COLLECTION = "cmp_fundcollection";
    String FUND_COLLECTION_B = "cmp_fundcollection_b";
    /*String FUND_COLLECTION_TABLE_B = "stwb_settleapplydetail_fund_assistant";*/
    String FUND_COLLECTIONLIST = "cmp_fundcollectionlist";
    //资金付款单
    String FUND_PAYMENT = "cmp_fundpayment";
    String FUND_PAYMENT_B = "cmp_fundpayment_b";
    /*String FUND_PAYMENT_TABLE_B = "stwb_settleapplydetail_fund_assistant";*/
    String FUND_PAYMENTLIST = "cmp_fundpaymentlist";
    // 重空凭证入库
    String CMP_CHECKSTOCKAPPLY = "cmp_checkStockApply";
    //资金费用
    String  FUNDEXPENSE ="fundexpense";



    //账户历史余额同步
    String ACCOUNTHISTORYBALANCEQUERY = "cmp_gethisba";
    //汇兑损益
    String EXCHANG_EGAIN_LOSS = "cmp_exchangegainloss";
    String EXCHANG_EGAIN_LOSS_LIST = "cmp_exchangegainlosslist";

    //货币面额设置
    String DENOMINATION_SETTING_LIST = "cmp_denominationSettinglist";

    // 自动关联规则设置
    String AUTO_CORRSETTING = "cmp_autocorrsetting";
    String CMP_AUTOCORR_LIST = "cmp_autocorr_list";
    //自动生单规则  借&贷
    String AUTO_ORDER_RULE_DEBIT = "cmp_autoorderruleone";
    String AUTO_ORDER_RULE_CREDIT = "cmp_autoorderruletwo";
    String CMP_AUTOORDERRULE_LIST = "cmp_autoorderrule_list";
    //银企联账户
    String BANKACCOUNTSETTINGLIST = "cmp_bankaccountsettinglist";

    /**
     * 汇入汇款
     */
    String INWARD_REMITTANCE_SUBMIT = "cmp_inwardremittancesubmit";
    String INWARD_REMITTANCE_LIST = "cmp_inwardremittancelist";
    String INWARD_REMITTANCE = "cmp_inwardremittance";

    /**
     * 银行资金池虚拟流水单
     */
    String VIRTUALFLOWRULECONFIGCARD_LIST = "cmp_virtualflowruleconfigcardlist";
    String VIRTUALFLOWRULECONFIGCARD = "cmp_virtualflowruleconfigcard";

    //预提
    String CMP_DEPOSITINTERESTWITHHOLDINGLIST = "cmp_depositinterestWithholdinglist";
    String CMP_ACCRUALSWITHHOLDINGQUERY = "cmp_accrualsWithholdingquery";
    String CMP_ACCRUALSWITHHOLDINGQUERYLIST = "cmp_accrualsWithholdingquerylist";
    //预提tableName
    String CMP_ACCRUALS_WITHHOLDING = "cmp_accruals_withholding";

    //银行账户利率设置
    String INTERESTRATESETTINGLIST = "cmp_interestratesettinglist";
    /**
     * 支票盘点*
     */

    String CHECKINVENTORY = "cmp_checkInventory";
    String CHECKINVENTORY_LIST = "cmp_checkInventorylist";
    String CMP_MARGINTYPELIST = "cmp_margintypelist";

    String CMP_MARGINTYPE = "cmp_margintype";

    /**
     * 支付保证金台账管理*
     */

    String CMP_PAYMARGIN = "cmp_paymargin";
    String CMP_PAYMARGINLIST = "cmp_paymarginlist";


    /**
     * 收到保证金台账管理
     * */
    String CMP_RECEIVEMARGIN = "cmp_receivemargin";
    String CMP_RECEIVEMARGINLIST = "cmp_receivemarginlist";



    /**
     * 支付保证金工作台
     */
    String CMP_PAYMARGINWORKBENCHLIST = "cmp_paymarginworkbenchlist";

    /**
     * 收到保证金工作台
     */
    String CMP_RECMARGINWORKBENCHLIST = "cmp_recmarginworkbenchlist";

    String CMP_INTERNALTRANSFERPROTOCOL = "cmp_internaltransferprotocol";


    /**
     * 到账认领中心
     */
    String CMP_BILLCLAIMCENTER_LIST = "cmp_billclaimcenterlist";//到账认领中心
    String CMP_MYBILLCLAIM_LIST = "cmp_mybillclaimlist";//我的认领列表

    String CMP_BILLCLAIM_CARD = "cmp_billclaimcard";//我的认领卡片

    String CMP_BILLCLAIMCENTER = "cmp_billclaimcenter";//到账认领卡片

    //退票确认列表
    String CMP_BANKRECONCILIATION_CHECKREFUND = "cmp_BankReconciliation_checkRefund";

    //关联确认
    String CMP_BANKRECONCILIATION_ISSURE_LIST = "cmp_BankReconciliation_isSure_list";

    //生单确认
    String CMP_AUTO_PUSH_BILL_CONFIRM_LIST = "cmp_auto_push_bill_confirm_list";


    //账户收支明细剔除
    String CMP_ACCOUNT_DETAIL_EXCLUSIONLIST = "cmp_accountdetailexclusionlist";
    String CMP_ACCOUNT_DETAIL_EXCLUSION = "cmp_accountdetailexclusion";

    /**
     * 外汇付款*
     */
    String CMP_FOREIGNPAYMENT = "cmp_foreignpayment";
    String CMP_FOREIGNPAYMENTLIST = "cmp_foreignpaymentlist";


    //银企对账设置
    String CMP_BANKRECONCILIATIONSETTING = "cmp_bankreconciliationsetting";
    //期初未达项
    String CMP_BANKRECONCILIATIONWDLIST = "cmp_bankreconciliationwdlist";//对账单期初未达
    String CMP_BANKJOURNALWDLIST = "cmp_bankjournalwdlist";//日记账期初未达
    String CMP_OPENINGOUTSTANDING_CARD = "cmp_openingOutstanding_card";//期初余额设置
    String CMP_RETIBALIST= "cmp_retibalist";//实时余额列表
    String CMP_HISBALIST = "cmp_hisbalist";//历史余额列表
    String CMP_HISBA = "cmp_hisba";//历史余额卡片
    String CMP_DLLIST = "cmp_dllist";//交易明细列表
    String CMP_MERCHANT = "cmp_merchant";//交易明细列表查询参照过滤
    String CMP_BANKELECTRONICRECEIPTLIST = "cmp_bankelectronicreceiptlist";//银行电子回单
    String CMP_BANKELECTRONICRECEIPT = "cmp_bankelectronicreceipt";//银行电子回单

    String BANKRECONCILIATIONREPEATLIST = "cmp_bankreconciliation_repeat_list";// 疑重列表
    String CMP_BANKVOURHCERCHECK_QUICKRECONCILIATION = "cmp_bankvourhcercheck_quickreconciliation";// 银企对账工作台-快速对账弹框
    String CMP_BANKVOURHCERCHECK_INFOLIST = "cmp_bankvourhcercheck_infolist";// 银账对账工作台 对账概览
    String CMP_BANKJOURNALCHECK_INFOLIST = "cmp_bankjournalcheck_infolist";// 银账对账工作台 对账概览

    // 流水自动辨识匹配规则
    String CMP_BANKRECONCILIATIONIDENTIFYSETTING = "cmp_bankreconciliationidentifysetting";
    String CMP_BANKRECONCILIATIONIDENTIFYSETTINGLIST = "cmp_bankreconciliationidentifysettinglist";


    String CMP_BATCHTRANSFERACCOUNT = "cmp_batchtransferaccount";
    String CMP_BATCHTRANSFERACCOUNTLIST = "cmp_batchtransferaccountlist";
    String CMP_BATCHTRANSFERACCOUNT_TABLE = "stwb_settleapply_ta_assistant";

    String STWB_SETTLEAPPLY_FUND_ASSISTANT = "stwb_settleapply_fund_assistant";

    String JOURNAL_BILL = "cmp_journalbill";
    String JOURNAL_BILL_B = "cmp_journalbill_b";

}
