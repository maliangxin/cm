package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.autorefundcheckrule.AutoRefundCheckRule;

/**
 * @description: 退票辨识规则
 * @author: wanxbo@yonyou.com
 * @date: 2023/2/6 15:07
 */

public interface RefundAutoCheckRuleService {

    /**
     * 根据条件查询自动对账方案设置
     * @param params
     * @return 设置方案
     */
    AutoRefundCheckRule queryRuleInfo(CtmJSONObject params) throws Exception;

    /**
     *
     * @param params
     */
    String updateRuleInfo(CtmJSONObject params) throws Exception;
}
