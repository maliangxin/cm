package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * 银行对账单自动生单
 * 新建service 避免侵入原BankAutoPushBillService
 */
public interface BankAutoPushBillNewService {
    /**
     * 银行对账单 自动推单资金调度等接口
     *
     * @param params 自动推单生单参数
     * @return 任务执行结果
     */
    CtmJSONObject autoPush(CtmJSONObject params);

}
