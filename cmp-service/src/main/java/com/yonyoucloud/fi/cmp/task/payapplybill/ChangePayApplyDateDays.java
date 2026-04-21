package com.yonyoucloud.fi.cmp.task.payapplybill;

import java.util.Map;

/**
 * <h1>定时任务：每天更新付款申请单距离期望付款日期天数</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021-01-07 15:13
 */
public interface ChangePayApplyDateDays {
    /**
     * 每天更新付款申请单距离期望付款日期天数-定时任务
     *
     * @return
     * @throws Exception
     */
    Map<String, Object> updateDistanceProposePaymentDateDaysTask(Map<String, Object> paramMap) throws Exception;
}
