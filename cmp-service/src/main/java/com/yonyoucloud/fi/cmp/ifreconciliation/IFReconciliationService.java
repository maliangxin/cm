/**
 * Copyright (c) 2019 ucsmy.com, All rights reserved.
 */
package com.yonyoucloud.fi.cmp.ifreconciliation;


import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.evarc.detail.api.v1.IReconciliationDetailQueryApi;

import java.util.Map;

/**
 * @Description: 业财对账接口
 * @Author: wsl
 * @Created Date: 2019年12月31日
 * @Version:
 */
public interface IFReconciliationService extends IReconciliationDetailQueryApi {
    CtmJSONObject getReconciliationDataDetail(CtmJSONObject params);

    CtmJSONObject getReconciliationDataAll(CtmJSONObject params);

    String getQuerydetailSql(CtmJSONObject params) throws Exception;

    /**
     * 获取对账账户信息
     * @param busiQuery
     * @param projectName
     * @param accentity
     * @return
     * @throws Exception
     */
    Map<String, Object> getAccount(String busiQuery, String projectName, String accentity) throws Exception;
}
