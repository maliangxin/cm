package com.yonyoucloud.fi.cmp.receivemargin.service;

import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import org.imeta.orm.base.BizObject;

import java.util.List;
import java.util.Map;

public interface ReceiveMarginService {

    Map<String, Object> latestReturnDateWarning(String tenantId, String warnDays, String accentity);
    String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception;
    List<BizObject> queryBizObjsWarpParentInfo(List<String> ids) throws Exception;
    void changeSettleFlagAfterAudit(ReceiveMargin receiveMargin) throws Exception;
    void changeSettleFlagAfterUnAudit(ReceiveMargin receiveMargin) throws Exception;
    void budgetAfterSettleStatusChange(ReceiveMargin receiveMargin,boolean checkResult) throws Exception;
}
