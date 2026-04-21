package com.yonyoucloud.fi.cmp.earlywarning.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.Map;

/**
 * <h1>EarlyWarningService</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-08-15 10:46
 */
public interface EarlyWarningService {

    Map<String, Object> payApplyBillPayDateWarningTask(int beforeDays, String logId, String tenant);

    void acctbalWarningTask(int beforeDays, String logId, String tenant);

    /**
     * 余额自动检查预警
     * @return
     */
    Map<String, Object> acctbalCheckWarning(Integer checkRange, String logId, String tenant);

    /**
     * 疑似退票预警任务
     * @param param
     * @return
     * @throws Exception
     */
    Map<String, Object> suspectedRefundWarning(CtmJSONObject param, String logId, String tenantId) throws Exception;
}
