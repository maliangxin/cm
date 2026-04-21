package com.yonyoucloud.fi.cmp.accounthistorybalance;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.Map;

public interface AccountBalanceCheckService {
    /**
     * 银行历史余额检查
     * @param paramMap
     * @return
     * @throws Exception
     */
    CtmJSONObject balanceCheck(Map<String, Object> paramMap)  throws Exception;
}
