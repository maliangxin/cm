package com.yonyoucloud.fi.cmp.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author: liaojbo
 * @Date: 2025年09月28日 16:25
 * @Description:组装常见错误信息
 */
public class ErrorMsgUtil {
    public static @NotNull String getCurrencyCodeWrongMsg(String currencyCode) {
        return String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400393", "根据币种编码【%s】未获取到币种，请检查【币种】节点【币种简称】设置是否正确（如人民币需为CNY，美元需为USD）!") /* "根据币种编码【%s】未获取到币种，请检查【币种】节点【币种简称】设置是否正确（如人民币需为CNY，美元需为USD）!" */, currencyCode);
    }

}
