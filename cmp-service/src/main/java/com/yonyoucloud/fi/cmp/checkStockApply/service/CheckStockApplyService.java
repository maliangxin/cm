package com.yonyoucloud.fi.cmp.checkStockApply.service;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * @author yp
 */
public interface CheckStockApplyService {


    /**
     * 支票申请审批
     * @param rows
     * @return
     */
    CtmJSONArray insertCheckStockService(CtmJSONArray rows) throws Exception;

    /**
     * 支票申请弃审
     * @param rows
     * @return
     */
    CtmJSONArray abandonCheckStock(CtmJSONArray rows) throws Exception;

    /**
     * 支票预占
     * @param rows
     * @return
     */
    void checkStockOccupy(CtmJSONObject param) throws Exception;

}
