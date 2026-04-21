package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * @Author zhangxiaojun
 * @Date 2022/10/10 17:06
 */
public interface BankreconciliationAutomaticStayService {
    /**
     * 银行对账单自动冻结调度任务
     * @return 执行结果
     */
    CtmJSONObject automaticStay(CtmJSONObject params)throws Exception;
}
