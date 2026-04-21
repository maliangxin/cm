package com.yonyoucloud.fi.cmp.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务编码常量
 * 粗粒度权限控制
 */
public class IServicecodeConstant {

    public static final Map<String, String> SERVICECODE_MAP;

    static {
        SERVICECODE_MAP = new HashMap<>();
        // 基础服务编码
        SERVICECODE_MAP.put("ficmp0003", "收款工作台");//@notranslate
        SERVICECODE_MAP.put("ficmp0004", "现金日记账");//@notranslate
        SERVICECODE_MAP.put("ficmp0006", "银行对账单");//@notranslate
        SERVICECODE_MAP.put("ficmp0007", "余额调节表");//@notranslate
        SERVICECODE_MAP.put("ficmp0008", "银行账户期初");//@notranslate
        SERVICECODE_MAP.put("ficmp0009", "付款工作台");//@notranslate
        SERVICECODE_MAP.put("ficmp0011", "账户直联开通设置");//@notranslate
        SERVICECODE_MAP.put("ficmp0013", "日结");//@notranslate
        SERVICECODE_MAP.put("ficmp0014", "银行对账");//@notranslate
        SERVICECODE_MAP.put("ficmp0016", "现金账户期初");//@notranslate
        SERVICECODE_MAP.put("ficmp0017", "账户实时余额");//@notranslate
        SERVICECODE_MAP.put("ficmp0018", "银行账户交易明细");//@notranslate
        SERVICECODE_MAP.put("ficmp0021", "银行日记账");//@notranslate
        SERVICECODE_MAP.put("ficmp0022", "银行未达");//@notranslate
        SERVICECODE_MAP.put("ficmp0024", "资金收款");//@notranslate
        SERVICECODE_MAP.put("ficmp0025", "现金汇兑损益");//@notranslate
        SERVICECODE_MAP.put("ficmp0026", "资金付款");//@notranslate
        SERVICECODE_MAP.put("ficmp0028", "现金盘点");//@notranslate
        SERVICECODE_MAP.put("ficmp0031", "银行账户历史余额");//@notranslate
        SERVICECODE_MAP.put("ficmp0032", "重空凭证入库");//@notranslate
        SERVICECODE_MAP.put("ficmp0033", "重空凭证工作台");//@notranslate
        SERVICECODE_MAP.put("ficmp0034", "我的认领");//@notranslate
        SERVICECODE_MAP.put("ficmp0036", "自动关联确认");//@notranslate
        SERVICECODE_MAP.put("ficmp0039", "银行对账单解冻申请");//@notranslate
        SERVICECODE_MAP.put("ficmp0040", "外币兑换工作台");//@notranslate
        SERVICECODE_MAP.put("ficmp0042", "银行交易回单");//@notranslate
        SERVICECODE_MAP.put("ficmp0050", "现金参数");//@notranslate
        SERVICECODE_MAP.put("ficmp0051", "银行对账方案设置");//@notranslate
        SERVICECODE_MAP.put("ficmp0052", "转账工作台");//@notranslate

        // 扩展服务编码
        SERVICECODE_MAP.put("cmp_payapplicationbilllist", "付款申请工作台");//@notranslate
        SERVICECODE_MAP.put("cmp_salarypaylist", "薪资支付工作台");//@notranslate
        SERVICECODE_MAP.put("cmp_inwardremittancelist", "汇入汇款");//@notranslate
        SERVICECODE_MAP.put("cmp_autoorderrule_list", "自动生单规则");//@notranslate
        SERVICECODE_MAP.put("cmp_BankReconciliation_checkRefund", "自动关联确认");//@notranslate
        SERVICECODE_MAP.put("cmp_denominationSettinglist", "货币面额配置");//@notranslate
        SERVICECODE_MAP.put("cmp_openingOutstandinglist", "期初未达项");//@notranslate
        SERVICECODE_MAP.put("cmp_virtualflowruleconfigcard", "银行资金池虚拟对账单设置");//@notranslate
        SERVICECODE_MAP.put("stwb_bankaccountsettinglist", "直联银行设置");//@notranslate
        SERVICECODE_MAP.put("cmp_autorefundcheckrule", "退票辨识规则");//@notranslate
        SERVICECODE_MAP.put("cmp_withholdingrulesetting", "预提规则设置");//@notranslate
        SERVICECODE_MAP.put("cmp_interestratesetting", "银行账户利率设置");//@notranslate
        SERVICECODE_MAP.put("cmp_accrualsWithholdingquery", "银行账户预提记录");//@notranslate
        SERVICECODE_MAP.put("cmp_depositinterestWithholdinglist", "银行账户预提");//@notranslate
        SERVICECODE_MAP.put("cmp_checkInventorylist", "支票盘点");//@notranslate
        SERVICECODE_MAP.put("cmp_margintypelist", "保证金类型");//@notranslate

        // 其他服务编码
        SERVICECODE_MAP.put("cmp_paymargin", "支付保证金");//@notranslate
        SERVICECODE_MAP.put("cmp_receivemargin", "收到保证金");//@notranslate
        SERVICECODE_MAP.put("cmp_internaltransferprotocollist", "收到保证金");//@notranslate
        SERVICECODE_MAP.put("cmp_accountdetailexclusionlist", "账户收支明细剔除");//@notranslate
        SERVICECODE_MAP.put("cmp_foreignpayment", "外币支付");//@notranslate
        SERVICECODE_MAP.put("cmp_bankreconciliationidentifytypelist", "银行对账识别类型");//@notranslate
        SERVICECODE_MAP.put("cmp_currencyapply", "货币兑换申请");//@notranslate
        SERVICECODE_MAP.put("cmp_electronicstatementconfirm", "电子对账单直连确认");//@notranslate
        SERVICECODE_MAP.put("cmp_bankbillchecklist", "银行票据检查");//@notranslate
        SERVICECODE_MAP.put("doubtfulhandling", "疑重处理");//@notranslate
        SERVICECODE_MAP.put("cmp_billclaimcenterlist", "到账认领中心");//@notranslate
        SERVICECODE_MAP.put("cmp_bankvourhcercheck_workbench", "银账对账工作台 ，凭证对账");//@notranslate
        SERVICECODE_MAP.put("cmp_bankjournalcheck_workbench", "银企对账工作台，银行日记账对账");//@notranslate
        SERVICECODE_MAP.put("cmp_batchtransferaccount", "同名账户批量划转");//@notranslate
    }

    public static final String RECEIVEBILL = "ficmp0003";// 收款工作台
    public static final String CASHJOURNAL = "ficmp0004";// 现金日记账
    public static final String CMPBANKRECONCILIATION = "ficmp0006";//银行对账单
    public static final String BALANCEADJUSTRESULT = "ficmp0007";//余额调节表
    public static final String BANKINITDATA = "ficmp0008";//银行账户期初
    public static final String PAYMENTBILL = "ficmp0009";// 付款工作台
    public static final String BANKACCOUNTSETTING = "ficmp0011";//银企联账号
    public static final String DAYSETTLE = "ficmp0013";//日结
    public static final String BANKRECONCILIATION = "ficmp0014";//银行对账
    public static final String CASHINITDATA = "ficmp0016";//现金账户期初
    public static final String RETIBALIST = "ficmp0017";//账户实时余额
    public static final String DLLIST = "ficmp0018";//银行账户交易明细
    public static final String BANKJOURNAL = "ficmp0021";//银行日记账
    public static final String BANKRECONCILIATIONWD = "ficmp0022";//银行未达
    public static final String FUNDCOLLECTION = "ficmp0024";//资金收款单
    public static final String EXCHANGEGAINLOSS = "ficmp0025";//现金汇兑损益
    public static final String FUNDPAYMENT = "ficmp0026";//资金付款单
    public static final String INVENTORY = "ficmp0028";//现金盘点
    public static final String ACCHISBAL = "ficmp0031";//银行账户历史余额
    public static final String CHECKSTOCKAPPLY = "ficmp0032";//支票入库
    public static final String CHECKTABLE = "ficmp0033";//支票工作台
    public static final String BILLCLAIMCARD = "ficmp0034";//认领单 我的认领
    public static final String BANKRECONCILIATION_ISSURE = "ficmp0036";//自动关联确认
    public static final String CURRENCYEXCHANGE = "ficmp0040";//外币兑换工作台
    public static final String BANKRECEIPTMATCH = "ficmp0042";//银行交易回单
    public static final String AUTOCONFIG = "ficmp0050";//现金参数
    public static final String BANKRECONCILIATIONSETTING = "ficmp0051";//银行对账方案设置
    public static final String TRANSFERACCOUNT = "ficmp0052";//转账工作台

    public static final String PAYAPPLICATIONBILL = "cmp_payapplicationbilllist";//付款申请工作台
    public static final String SALARYPAY = "cmp_salarypaylist";//薪资支付工作台
    public static final String INWARD_REMITTANCE = "cmp_inwardremittancelist";// 汇入汇款
    public static final String AUTOORDERRULE = "cmp_autoorderrule_list";//自动生单规则
    public static final String BANKRECONCILIATION_CHECKREFUND = "cmp_BankReconciliation_checkRefund";//自动关联确认
    public static final String DENOMINATIONSETTING = "cmp_denominationSettinglist";//货币面额配置
    public static final String OPENINGOUTSTANDING = "cmp_openingOutstandinglist";//期初未达项
    public static final String VIRTUALFLOWRULECONFIGCARD = "cmp_virtualflowruleconfigcard";//银行资金池虚拟对账单设置
    public static final String STWB_BANKACCOUNTSETTING = "stwb_bankaccountsettinglist";//直联银行设置
    public static final String AUTOREFUNDCHECKRULE = "cmp_autorefundcheckrule";//退票辨识规则
    public static final String WITHHOLDINGRULESETTING = "cmp_withholdingrulesetting";//预提规则设置
    public static final String INTERESTRATESETTING = "cmp_interestratesetting";//银行账户利率设置
    public static final String CMP_ACCRUALSWITHHOLDINGQUERY = "cmp_accrualsWithholdingquery";//银行账户预提记录
    public static final String CMP_DEPOSITINTERESTWITHHOLDINGLIST = "cmp_depositinterestWithholdinglist";//银行账户预提
    public static final String CHECKINVENTORY = "cmp_checkInventorylist";//支票盘点
    public static final String CMP_MARGINTYPELIST = "cmp_margintypelist";//保证金类型

    public static final String PAYMARGIN = "cmp_paymargin";//支付保证金
    public static final String RECEIVEMARGIN = "cmp_receivemargin";//收到保证金
    public static final String INTERNAL_TRANSFER_PROTOCOL_SERVICE_CODE = "cmp_internaltransferprotocollist";//收到保证金
    public static final String ACCOUNT_DETAIL_EXCLUSIONLIST_SERVICE_CODE = "cmp_accountdetailexclusionlist";//账户收支明细剔除

    public static final String FOREIGNPAYMENT = "cmp_foreignpayment";

    public static final String BANK_RECONCILIATION_IDENTIFY_TYPE = "cmp_bankreconciliationidentifytypelist";

    public static final String CURRENCYAPPLY = "cmp_currencyapply";//货币兑换申请

    public static final String CMP_ELECTRONICSTATEMENTCONFIRM = "cmp_electronicstatementconfirm";//电子对账单直连确认

    public static final String BANK_BILL_CHECK = "cmp_bankbillchecklist";

    public static final String doubtfulhandling = "doubtfulhandling"; //疑重处理

    public static final String CMP_BILLCLAIMCENTERLIST = "cmp_billclaimcenterlist";//到账认领中心

    public static final String CMP_BANKVOUCHERCHECK_WORKBENCH = "cmp_bankvourhcercheck_workbench";//银账对账工作台 ，凭证对账
    public static final String CMP_BANKJOURANLCHECK_WORKBENCH = "cmp_bankjournalcheck_workbench";//银企对账工作台，银行日记账对账

    // 同名账户批量划转
    public static final String BATCH_TRANSFERACCOUNT = "cmp_batchtransferaccount";
    //账户定期余额维护
    public static final String CMP_REGULARBALANCELIST="cmp_regularbalancelist";

}
