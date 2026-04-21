package com.yonyoucloud.fi.cmp.autocorrsetting;

import java.util.List;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2025/4/17 20:36
 */

public interface AsyncCorrService {
    /**
     * 异步确认对账单关联关系
     * @param corrIds
     * @param dcFlags
     * @param uid
     */
    void asyncConfirmCorrOpration(List corrIds, List dcFlags, String uid);
    /**
     * 异步拒绝对账单关联关系
     * @param corrIds
     * @param uid
     */
    void asyncRefuseCorrOpration(List corrIds,String uid);
}
