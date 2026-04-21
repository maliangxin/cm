package com.yonyoucloud.fi.cmp.bankreconciliation.service.count;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.HashMap;

public interface IBillCountService {

    /**
     * 银行流水认领、认领中心、我的认领统计区查询
     * @param params
     * @return
     * @throws Exception
     */
    HashMap<String, Object> getCount(CtmJSONObject params) throws Exception;

}
