package com.yonyoucloud.fi.cmp.task.real;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.Map;

/**
 * Real time balance 实时余额定时任务
 * @author Administrator
 *
 */
public interface RTHistoryBalanceService {
	
	//账户历史余额查询
//    String QUERY_ACCOUNT_HISTORYBALANCE = "40T21";
    //调度任务接口
    //String dispatchUrl = "/warning/warning/async/updateTaskLog";
	
	 /**
     * 账户历史余额数据拉取-定时任务
     *
     * @return
     * @throws Exception
     */
     Map<String,Object> queryAccountHistoryBalanceTask(CtmJSONObject paramMap) throws Exception;
}
