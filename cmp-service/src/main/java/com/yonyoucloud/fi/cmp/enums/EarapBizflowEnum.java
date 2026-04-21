package com.yonyoucloud.fi.cmp.enums;

import lombok.Getter;

/**
 * 银行流水处理生成应收应付单据枚举类
 * @author guoxh
 * com.yonyoucloud.fi.cmp.cmpentity.EventType
 */
@Getter
public enum EarapBizflowEnum {
    //付款单
    EARP_PAYMENT("137","payment","cmp_bankreconciliationToPayment","EAP","yonbip-fi-earapbill"),
    //收款单
    EARP_COLLECTION("87","collection","cmp_bankreconciliationToCollection","EAR","yonbip-fi-earapbill"),
    //付款退款单
    EARP_REFUND("85","apRefund","cmp_bankreconciliationToRefund","EAP","yonbip-fi-earapbill");
    private String code;
    private String billNum;
    private String ruleCode;
    private String subId;
    private String domain;

    EarapBizflowEnum(String code, String billNum, String ruleCode, String subId, String domain) {
        this.code = code;
        this.billNum = billNum;
        this.ruleCode = ruleCode;
        this.subId = subId;
        this.domain = domain;
    }
}

