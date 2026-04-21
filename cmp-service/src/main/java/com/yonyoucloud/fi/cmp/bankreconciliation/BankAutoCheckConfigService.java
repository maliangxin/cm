package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankautocheckconfig.BankAutoCheckConfig;


/**
 * @description: 银行对账，自动对账方案设置业务接口
 * @author: wanxbo@yonyou.com
 * @date: 2022/11/10 19:01
 */

public interface BankAutoCheckConfigService {

    /**
     * 根据条件查询自动对账方案设置
     * @param params
     * @return 设置方案
     */
    BankAutoCheckConfig queryConfigInfo(CtmJSONObject params) throws Exception;

    /**
     *
     * @param params
     */
    String updateConfigInfo(CtmJSONObject params) throws Exception;
}
