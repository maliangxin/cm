package com.yonyoucloud.fi.cmp.marginworkbench.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;


public interface MarginWorkbenchService {


    /**
     * 支付保证金台账传保证金工作台 保存接口
     * @param params
     *    ICmpConstant.PAYMARGIN   支付保证金vo
     *    ICmpConstant.DBMARGINAMOUNT//没有可不传
     *    ICmpConstant.DBNATMARGINAMOUNT//没有可不传
     *    ICmpConstant.DBCONVERSIONAMOUNT//没有可不传
     *    ICmpConstant.DBNATCONVERSIONAMOUNT//没有可不传
     * @return  保证金工作台id
     * @throws Exception
     */
    String payMarginWorkbenchSave(CtmJSONObject params) throws Exception;

    /**
     * 支付保证金台账传保证金工作台 更新接口
     * @param params
     *       ICmpConstant.MARGINBUSINESSNO
     *       ICmpConstant.SRC_ITEM
     *       ICmpConstant.ACTION
     *       ICmpConstant.TRADETYPE
     *       ICmpConstant.MARGINAMOUNT
     *       ICmpConstant.NATMARGINAMOUNT
     *       ICmpConstant.CONVERSIONAMOUNT//没有可不传
     *       ICmpConstant.NATCONVERSIONAMOUNT//没有可不传
     *       ICmpConstant.SETTLEFLAG
     * @throws Exception
     */
    void payMarginWorkbenchUpdate(CtmJSONObject params) throws Exception;

    /**
     * 收到保证金台账传保证金工作台 保存接口
     * @param params
     * @return
     * @throws Exception
     */
    String recMarginWorkbenchSave(CtmJSONObject params) throws Exception;

    /**
     * 收到保证金台账传保证金工作台 更新接口
     * @param params
     * @throws Exception
     */
    void recMarginWorkbenchUpdate(CtmJSONObject params) throws Exception;

    /**
     * 删除收到保证金虚拟户
     * @param params
     * @throws Exception
     */
    void delRecMarginWorkbench(CtmJSONObject params) throws Exception;

    /**
     * 删除支付保证金虚拟户
     * @param params
     * @throws Exception
     */
    void delPayMarginWorkbench(CtmJSONObject params) throws Exception;

}
