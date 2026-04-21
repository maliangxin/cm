package com.yonyoucloud.fi.cmp.https.utils;

import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;

/**
 * 银企联请求 结果解析通用方法
 */
public class HttpServiceInforamtionUtils {

    /**
     * 根据银企联状态码 判断当前交易是否成功
     * @param tradeCode 当前交易码
     * @param service_resp_code 银企联返回状态码
     * @return
     */
    public static Boolean httpSuccessByRespCode(String tradeCode,String service_resp_code){
        if(tradeCode.equals(ITransCodeConstant.QUERY_ACCOUNT_BALANCE)|| //实时余额
                tradeCode.equals(ITransCodeConstant.QUERY_HIS_ACCOUNT_BALANCE)||//历史余额
                tradeCode.equals(ITransCodeConstant.DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL)||//回单下载
                tradeCode.equals(ITransCodeConstant.DOWNLOAD_ELECTRONIC_STATEMENT_CONFIRM_FILE)){//对账单下载
            return service_resp_code.equals("000000");
        }else if(tradeCode.equals(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL)||//交易明细
                tradeCode.equals(ITransCodeConstant.QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL)){//账户电子回单查询

            //return service_resp_code.equals("000000") || service_resp_code.equals("350019");
            //350019	查询交易明细无记录 认为报错，抛出错误信息
            return service_resp_code.equals("000000");
        }else
            return true;
    }

}
