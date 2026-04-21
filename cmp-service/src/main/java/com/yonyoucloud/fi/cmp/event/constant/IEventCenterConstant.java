package com.yonyoucloud.fi.cmp.event.constant;


/**
 * 事件中心所使用常量，方便统计现金管理所使用事件中心的事件
 *
 * @author maliangn
 */
public interface IEventCenterConstant {

    /**
     * 事件源编码 银行账户交易明细
     */
    String CMP_BANKDEALDETAIL= "cmp_bankdealdetail";
    /**
     * 事件类型，银行账户交易明细推单
     */
    String CMP_BANKDEALDETAIL_PULL = "cmp_bankdealdetail_pull";

    /**
     * 事件源编码 银行对账单
     */
     String CMP_BANKRECONCILIATION = "cmp_bankreconciliation";

     // 总账回单关联事件
    String CMP_BANKELECTRONICRECEIPTURL_GL = "cmp_BankElectronicReceiptUrl_gl";

    // 事件类型，银行对账单退票发布
    String CMP_BANKRECONCILIATION_REFUND = "cmp_bankreconciliation_refund";

    // 事件类型，银行对账单直联拉取
    String CMP_BANKRECONCILIATIONDIRECTPULL = "cmp_bankreconciliationdirectpull";

    // 事件类型编码 - 电子回单文件发送 *
    String CTM_CMP_BANKELECTRONICRECEIPTURL = "ctm_cmp_BankElectronicReceiptUrl";

    // 流水关联事件类型编码
    String EVENT_TYPE_CODE_BANKRECONCILIATION = "cmp_bankreconciliation_corrbill";

    // 对账单到资金调度，事件类型
    String CMP_TO_FDTR_EVENT_TYPE = "cmp_bankreconciliation_to_fdtr";

    /**
     * 事件源编码 保证金存入支取单
     */
    String MARGIN_ACCOUNT = "margin_account";
    /**
     * 事件类型，保证金存入支取单-结算结果通知
     */
    String MARGIN_ACCOUNT_SETTLE = "margin_account_settle";


    /**
     * 事件源编码 支付保证金
     */
    String PAYMARGIN_SAVE = "paymargin_save";
    /**
     * 事件类型，支付保证金保存发送事件
     */
    String PAYMARGIN_SAVE_SEND = "paymargin_save_send";


    /**
     * 事件源编码 付款申请工作台
     */
    String CMP_PAY_APPLY_BILL = "CMP_PAY_APPLY_BILL";
    /**
     * 事件类型编码 付款申请到采购
     */
    String PayApplyBillToUpu = "PayApplyBillToUpu";




    /**
     * 事件源编码 业财对账
     */
    String RECONC_RECEIVE = "reconc-receive";
    // 事件类型编码 业财对账返回
    String RECONC_SUMMARY_RETURN = "reconc-summary-return";


}
