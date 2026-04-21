package com.yonyoucloud.fi.cmp.openapi.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * @author qihaoc
 * @Description: 日记账对外OpenAPI接口
 * @date 2021/06/07
 */
public interface OpenApiJournalService {
    /**
     * 根据参数查询日记账，可查询现金日记账和银行日记账
     * @param param
     * @return
     * @throws Exception
     */
    CtmJSONObject queryJournalByParam(CtmJSONObject param) throws Exception;
}
