package com.yonyoucloud.fi.cmp.merchant;


import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * 客户和供应商操作
 * @author miaowb
 *
 */
public interface MerchantService {

    /**
     * 同步客商档案
     * @param params
     * @return
     */
    String synMerchant(CtmJSONObject params) throws Exception;

    /**
     * 通过name获取国家信息
     * @param params
     * @return
     */
    CtmJSONObject getCountryByName(CtmJSONObject params) throws Exception;


    /**
     * 检查同步客户还是供应商
     * @param params
     * @return
     */
    CtmJSONObject checkMerchant(CtmJSONObject params) throws Exception;

    
}
