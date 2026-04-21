package com.yonyoucloud.fi.cmp.bankaccountsetting.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * @Author xuyao2
 * @Date 2023/3/23 11:26
 */
public interface AccountSynchronizationService {

    /**
     * 账号同步调度任务
     * @return 执行结果
     */
    CtmJSONObject bankaccountsync(CtmJSONObject params)throws Exception;
}
