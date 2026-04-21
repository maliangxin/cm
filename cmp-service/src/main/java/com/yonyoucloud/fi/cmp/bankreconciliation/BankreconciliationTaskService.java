package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * @author qihaoc
 * @Description: 银行对账单调度任务综合接口
 * @date 2023/10/14 11:07
 */
public interface BankreconciliationTaskService {
    /**
     * 银行对账单自动发布调度任务
     *
     * @return 执行结果
     */
    CtmJSONObject automaticPublic(CtmJSONObject params) throws Exception;
}
