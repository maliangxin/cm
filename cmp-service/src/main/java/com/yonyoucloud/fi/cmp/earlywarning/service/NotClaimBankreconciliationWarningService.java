package com.yonyoucloud.fi.cmp.earlywarning.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.Map;

public interface NotClaimBankreconciliationWarningService {

    /**
     * 未认领流水预警
     * @param body
     * @return
     */
    Map<String, Object> notClaimBankreconciliationWarning(CtmJSONObject body,String logId);
}


