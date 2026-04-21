package com.yonyoucloud.fi.cmp.https.utils;

import java.util.HashMap;

/**
 * 银企联相关交易错误码及错误信息
 * @author maliangn
 * @date 2023-04-24
 */
public enum TransErrorInfo {

    //交易成功
    SUCCESS("0000", "银企联云交易成功"),
    //网络异常，可能是网络不通
    ERROR_NET_ERROR("0001","银企联云交互异常，请检查银企联连接配置[银企联server地址]是否能正常访问"),
    //响应码错误，非200
    ERROR_STATUS_ERROR("0002",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418011A","调用畅捷支付响应错误：【") /* "调用畅捷支付响应错误：【" */ ),
    //签名验签失败
    ERROR_SIGN_ERROR("0003",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C0418011B","畅捷支付返回报文验签失败") /* "畅捷支付返回报文验签失败" */),
    //签名为空，银企联存在此种情况
    ERROR_SIGN_EMPTY("0004","银企联云返回报文签名信息为空，请联系银企联支持人员"),
    //返回信息原文，签名为空
    ERROR_SIGN_DATA_EMPTY("0005","银企联云返回报文信息为空，请联系银企联支持人员");

    private String errorCode;
    private String errorMessage;

    private TransErrorInfo(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private static HashMap<String, TransErrorInfo> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<String, TransErrorInfo>();
        TransErrorInfo[] items = TransErrorInfo.values();
        for (TransErrorInfo item : items) {
            map.put(item.getErrorCode(), item);
        }
    }


}
