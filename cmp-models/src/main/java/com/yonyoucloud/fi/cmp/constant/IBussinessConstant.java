package com.yonyoucloud.fi.cmp.constant;

/**
 * @Author: mal
 * @Description: 本类用于常用业务字段常量
 * @Date: Created in  2022/7/14 17:09
 * @Version v1.0
 */
public interface IBussinessConstant {

    /** 币种金额 */
    String ORI_SUM = "oriSum";//原币金额
    String ORI_SUM_L = "orisum";//原币金额
    String NAT_SUM = "natSum";//本币金额
    String LOCAL_SUM = "localsum";//本币金额
    String ORISETTLED_SUM = "oriSumsettled";//原币实占金额
    String NATSETTLED_SUM = "natSumsettled";//本币实占金额
    String CURRENCY = "currency";//币种
    String NATCURRENCY = "natCurrency";//本币币种
    String PRICEDIGIT = "priceDigit"; //单价精度
    String CURRENCY_PRICEDIGIT = "currency_priceDigit";//币种单价精度
    String MONEYDIGIT = "moneyDigit";//金额精度
    String CURRENCY_MONEYDIGIT = "currency_moneyDigit";//币种金额精度
    //借
    String DEBIT = "debit";
    //贷
    String CREDIT = "credit";

    /** 基础档案 */
    String SUPPLIER = "supplier"; // 供应商

    /** 基础档案 */
    String CUSTOMER = "customer"; // 客户


    String EMPLOYEE = "employee";//员工

    String OTHERNAME= "othername";//对方名称

    String INNERUNIT = "innerunit";//内部单位

    String FUNDBUSINOBJ = "fundbusinobj"; //资金业务对象
    /** 财务相关  */
    String ACCENTITY = "accentity";

    String ACCENTITY_CODE = "accentity_code";

    /** 登账日期 */
    String DZDATE = "dzdate";

    String BANK_ACCOUNT = "bankaccount";

    String PUBTS = "pubts";

    String VOUCHERSTATUS = "voucherstatus";

    String PAY_BANK_ACCOUNT = "payBankAccount";

    String PAY_CASH_ACCOUNT= "payCashAccount";

    String REC_BANK_ACCOUNT = "recbankaccount";

    String REC_CASH_ACCOUNT = "reccashaccount";

    String PAY_VIRTUAL_ACCOUNT = "payVirtualAccount";

    String COLL_VIRTUAL_ACCOUNT = "collVirtualAccount";
    //回单下载标识
    String EXTENDSS = "extendss";
    /** YMS网银支付UKey校验开关键名 */
    String PAY_BANK_CHECK_UKEY = "payBankCheckUKey";
}
