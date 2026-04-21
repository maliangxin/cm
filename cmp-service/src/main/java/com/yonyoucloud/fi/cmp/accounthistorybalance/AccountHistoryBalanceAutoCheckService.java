package com.yonyoucloud.fi.cmp.accounthistorybalance;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.Map;

/**
 * @ClassName AccountHistoryBalanceService
 * @Description 账户历史余额接口
 * @Author wnagyao
 * @Date 2021/01/21
 * @Version 1.0
 **/
public interface AccountHistoryBalanceAutoCheckService {

    CtmJSONObject checkAccountBalance(Map<String, Object> paramMap, String logId)  throws Exception;
}
