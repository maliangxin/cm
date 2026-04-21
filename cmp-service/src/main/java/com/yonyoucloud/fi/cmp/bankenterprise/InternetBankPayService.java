package com.yonyoucloud.fi.cmp.bankenterprise;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * @ClassName InternetBankPayService
 * @Desc 网银支付服务
 * @Author tongyd
 * @Date 2019/9/11
 * @Version 1.0
 */
public interface InternetBankPayService {
    String BATCH_PAY_PRE_ORDER = "11B10P";
    String PRE_ORDER_TRANSACTION_CONFIRM = "50C10";
    String BATCH_PAY_DETAIL_STATUS_QUERY = "40B10";
    String UPDATE_BANK_ENTERPRISE_CUSTOM_NO = "01a21a004";
    /*
     * @Author tongyd
     * @Description 批量支付预下单
     * @Date 2019/9/11
     * @Param [param]
     * @return java.lang.String
     **/
    String batchPayPreOrder(CtmJSONObject param) throws Exception;

    /*
     * @Author tongyd
     * @Description 预下单交易确认
     * @Date 2019/9/11
     * @Param [param]
     * @return java.lang.String
     **/
    String preOrderTransactionConfirm(CtmJSONObject param) throws Exception;

    /*
     * @Author tongyd
     * @Description 批量支付明细状态查询
     * @Date 2019/9/11
     * @Param [param]
     * @return java.lang.String
     **/
    String batchPayDetailStatusQuery(CtmJSONObject param) throws Exception;

    void analysisPayStatusQueryRespData(CtmJSONObject responseBody) throws Exception;

    CtmJSONObject buildPayStatusQueryMsg(CtmJSONObject param);
}
