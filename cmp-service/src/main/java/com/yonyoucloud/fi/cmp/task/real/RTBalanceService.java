package com.yonyoucloud.fi.cmp.task.real;

import java.util.Map;

/**
 * Real time balance 实时余额定时任务
 * @author Administrator
 *
 */
public interface RTBalanceService {

    //账户实时余额查询
    String QUERY_ACCOUNT_BALANCE = "40T20";
    //调度任务接口
    //String dispatchUrl = "/warning/warning/async/updateTaskLog";

    /**
     * 账户实时余额数据拉取-定时任务
     *
     * @return
     * @throws Exception
     */
    Map<String,Object> queryAccountBalanceTask(Map<String,Object> paramMap) throws Exception;
}

