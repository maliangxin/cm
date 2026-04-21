package com.yonyoucloud.fi.cmp.bankearlywarning.service;

import java.util.Map;

/**
 * <h1>BankEarlyWarningService</h1>
 *
 * @author yp
 * @version 1.0
 * @since 2022-10-09 10:46
 */
public interface BankEarlyWarningService {

    Map<String, Object> bankSlipUnDealWarningTask(String accentityStr, Integer distribute, Integer publishDistribute, String logId, String tenant);
}
