package com.yonyoucloud.fi.cmp.earlywarning.service;

import java.util.Map;

/**
 * @author qihaoc
 * @Description:未认领预警
 * @date 2023/6/2 16:25
 */
public interface UnclaimedWarningService {
    /**
     * 未认领预警
     * @param accentity
     * @param checkRange 单据日期范围（前X日）
     * @param timeOuts 超时天数
     * @param logId
     * @param tenantId
     * @return
     */
    Map<String, Object> unclaimedWarning(String accentity, Integer checkRange, Integer timeOuts, String logId, String tenantId);
    /**
     * 未导入预警
     *
     * @param bankType            银行类别
     * @param currency            币种
     * @param checkRange          是否非直联
     * @param checkDate           检查前几日
     * @param cotainFreezeAccount
     * @param tenantId
     * @return
     */
    Map<String, Object> notImportWarning(String accentity, String bankType, String currency, String checkRange, Integer checkDate, String cotainFreezeAccount, String logId, String tenantId);

}
