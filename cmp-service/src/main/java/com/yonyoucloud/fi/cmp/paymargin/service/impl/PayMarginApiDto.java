package com.yonyoucloud.fi.cmp.paymargin.service.impl;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenApi
 */
@Getter
@Setter
public class PayMarginApiDto {

    // 基本信息
    private String accentity_code;//	资金组织
    private String code;//	单据编号
    private String vouchdate;//	单据日期
    private String tradetype_code;//	交易类型
    private String marginbusinessno;//	保证金原始业务号
    private String margintype_code;//	保证金类型
    private String currency_code;//	币种
    private String project_code;//	项目
    private String dept_code;//	部门
    private String expectedretrievaldate;//	预计取回日期
    private String exchangeratetype_code;//	汇率类型
    private String srcbillno;//来源业务单据编号
    private String description;//	备注

    // 取出取回信息
    private Short oppositetype;//	对方类型
    private BigDecimal marginamount;//	保证金金额
    private Short settleflag;//	是否结算
    private Short paymenttype;// 收付类型
    private String settlemode_code;//	结算方式
    private Short settlestatus;//	结算状态
    private Short paymentsettlemode;//	付款结算方式
    private String enterprisebankaccount_code;//	本方银行账户
    private String targetId;//	对方档案ID
    private String targetbankaccount;//	对方银行信息ID
    private String oppositename;//	对方名称
    private String oppositebankaccountname;//	对方银行账户名称
    private String oppositebankaccount;//	对方银行账号
    private String oppositebankNumber_code;//	对方开户网点
    private String oppositebankType_code;//	对方银行类别

    // 轧差相关
    private String netIdentificateCode;//	轧差识别码
    private Short netSettleCount;//	轧差结算总笔数
    private BigDecimal afterNetAmt;//	轧差后金额
    private Short afterNetDir;//	轧差后收付方向

    // 转换信息
    private Short conversionmarginflag;//	转换保证金
    private BigDecimal conversionamount;//	转换金额
    private String newmarginbusinessno;//	新保证金原始业务号
    private String newmargintype_code;//	新保证金类型
    private String newproject_code;//	新项目
    private String newdept_code;//	新部门
    private String newexpectedretrievaldate;//	新预计取回日期

    // 特征
    private Map<String, Object> characterDef = new HashMap<>();

    // 本方关联信息
    private Short ourassociationstatus;  // 本方关联状态
    private String ourcheckno;           // 本方勾兑号
    private String ourbankbillid;        // 本方银行对账单id
    private String ourbillclaimid;       // 本方认领单id

    // 对方关联信息
    private Short oppassociationstatus;  // 对方关联状态
    private String oppcheckno;           // 对方勾兑号
    private String oppbankbillid;        // 对方银行对账单id
    private String oppbillclaimid;       // 对方认领单id


}
