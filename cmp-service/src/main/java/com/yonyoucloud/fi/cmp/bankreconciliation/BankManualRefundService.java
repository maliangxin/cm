package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.Map;

/**
 * @description: 银行对账单手工退票接口
 * @author: wanxbo@yonyou.com
 * @date: 2023/10/26 20:41
 */

public interface BankManualRefundService {

    /**
     * 手工退票接口
     * @param paramMap
     * @return
     * @throws Exception
     */
    JsonNode manualRefund(Map<String,Object> paramMap) throws Exception;

    /**
     * 现金运维工具 - 清空银行流水退票状态
     * @param params id或者银行交易流水号
     * @throws Exception
     */
    void clearRefundStatusById(CtmJSONObject params) throws Exception;
}
