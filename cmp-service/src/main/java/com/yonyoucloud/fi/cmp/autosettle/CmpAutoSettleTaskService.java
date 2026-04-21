package com.yonyoucloud.fi.cmp.autosettle;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * @description: 自动结算调度任务接口
 * @author: wanxbo@yonyou.com
 * @date: 2022/12/9 10:35
 */

public interface CmpAutoSettleTaskService {

    /**
     * 付款工作台自动结算接口
     * @param params
     * @return
     */
    CtmJSONObject payBillAutoSettle(CtmJSONObject params);

    /**
     * 收款单自动结算接口
     * @param params
     * @return
     */
    CtmJSONObject receiveBillAutoSettle(CtmJSONObject params);
}
