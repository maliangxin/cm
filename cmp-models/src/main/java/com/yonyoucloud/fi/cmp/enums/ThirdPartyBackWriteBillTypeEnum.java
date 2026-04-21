package com.yonyoucloud.fi.cmp.enums;


import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

/**
 * <h1>回写第三方平台单据类型枚举</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023/3/14 16:14
 */
public enum ThirdPartyBackWriteBillTypeEnum {

    FundCollection(1, MessageUtils.getMessage("P_YS_CTM_CM-UI_0001555910") /* "资金收款单" */),
    FundPayment(2, MessageUtils.getMessage("P_YS_CTM_CM-UI_0001555889") /* "资金付款单" */),
    FundTransfer(3, MessageUtils.getMessage("P_YS_FI_AAI-BE_1672566093131546641") /* "资金转账单" */),
    DiscrepancyConfirmation (4, MessageUtils.getMessage("P_YS_FI_AAI-BE_1672566093131546932") /* "差异确认单" */);

    private int value;
    private String name;

    ThirdPartyBackWriteBillTypeEnum(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setCode(int value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
