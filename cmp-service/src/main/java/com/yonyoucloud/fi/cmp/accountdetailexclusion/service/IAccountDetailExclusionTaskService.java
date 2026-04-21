package com.yonyoucloud.fi.cmp.accountdetailexclusion.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * @author JPK
 * @Description:账户收支明细剔除调度任务
 * @date 2023/11/14 11:07
 */
public interface IAccountDetailExclusionTaskService {
    /**
     * 账户收支明细剔除自动剔除调度任务
     *
     * @return 执行结果
     */
    CtmJSONObject automaticExclusion(CtmJSONObject params) throws Exception;
}
