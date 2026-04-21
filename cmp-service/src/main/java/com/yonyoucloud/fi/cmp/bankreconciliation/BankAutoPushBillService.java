package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyoucloud.fi.cmp.vo.AutoTaskCommonVO;

import java.util.Date;

/**
 * @description: 银行对账单，自动下推资金调度等推单功能
 * @author: wanxbo@yonyou.com
 * @date: 2022/7/7 13:42
 */

public interface BankAutoPushBillService {

    /**
     * 银行对账单 自动推单资金调度等接口
     * @param params 自动推单生单参数
     * @return 任务执行结果
     */
    void autoPush(Date startDate,Date endDate,JsonNode params) throws Exception;

    /**
     * 银行对账单，自动生单后自动确认
     * @param params 参数
     * @return 任务执行结果
     */
    void autoConfirmBill(AutoTaskCommonVO params) throws Exception;

    /**
     * 自动生单确认接口
     * @param params 请求参数
     */
    JsonNode confirmBill(JsonNode params) throws Exception;

    /**
     * 自动生单拒绝接口
     * @param params 请求参数
     */
    JsonNode refuseBill(JsonNode params) throws Exception;
}
