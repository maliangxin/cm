package com.yonyoucloud.fi.cmp.checkStock.service;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;

/**
 *  重空凭证工作台 *
 */
public interface CheckStockService {


    /**
     * 领用
     * @param rows
     * @return
     */
    CtmJSONArray checkStockGetUsed(CtmJSONArray rows,String custNo) throws Exception;

    /**
     * 取消领用
     * @param rows
     * @return
     */
    CtmJSONArray checkStockCancelUsed(CtmJSONArray rows) throws Exception;

    /**
     * 兑付
     * @param rows
     * @return
     */
    CtmJSONArray checkStockGetCash(CtmJSONArray rows, CtmJSONObject obj) throws Exception;

    /**
     * 取消兑付
     * @param rows
     * @return
     */
    CtmJSONArray checkStockCancelCash(CtmJSONArray rows) throws Exception;

    CheckStock getCheckStockById(Long id) throws Exception;

    void migrationStockToStatus() throws Exception;

    void checkAccAuthority (String accentity, String custNo,String checkBookNo) throws Exception;
}
