package com.yonyoucloud.fi.cmp.currencyapply.service;

import java.util.Date;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2023/8/25 16:14
 */

public interface CurrencyApplyService {

    /**
     * 更新外币兑换申请交割状态
     * @param currencyApplyId 外币兑换申请ID
     * @param deliveryStatus 交割状态
     * @param fixtureDate 成交时间
     */
    void updateDeliveryStatus(Long currencyApplyId, short deliveryStatus, Date fixtureDate) throws Exception;
}
