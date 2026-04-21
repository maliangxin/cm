package com.yonyoucloud.fi.cmp.openapi.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.vo.Result;

/**
 * @description: 银行交易回单 openapi相关服务接口
 * @author: wanxbo@yonyou.com
 * @date: 2023/2/23 14:58
 */

public interface BankElectronicOpenApiService {
    /**
     * 根据银行对账单ID或者对账单ID获取交易回单下载地址
     * @param param
     * @return
     * @throws Exception
     */
    Result<Object> queryDownloadUrl(CtmJSONObject param) throws Exception;

    /**
     * 银行对账单查询
     * @param param
     * @return
     * @throws Exception
     */
    CtmJSONObject querylist(CtmJSONObject param) throws Exception;
}
