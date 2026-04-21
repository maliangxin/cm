package com.yonyoucloud.fi.cmp.fundcollection.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;

/**
 * 资金收款接口
 *
 * @author maliangn  2021-12-03
 *
 *
 */
public interface FundCollectionService {

    CtmJSONObject submit(FundCollection fundCollection) throws Exception;

    /**
     * 整单拒绝的接口
     */
    void entrustReject(CtmJSONObject jsonObject) throws Exception;

    /**
     * 子表拒绝的接口
     */
    void entrustRejectSub(CtmJSONObject jsonObject) throws Exception;

}
