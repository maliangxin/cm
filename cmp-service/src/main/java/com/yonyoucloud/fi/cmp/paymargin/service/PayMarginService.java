package com.yonyoucloud.fi.cmp.paymargin.service;

import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import org.imeta.orm.base.BizObject;

import java.util.List;
import java.util.Map;

public interface PayMarginService {

    Map<String, Object> expectedRetrievalDateWarning(String tenantId,String warnDays,String accentity);
    String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception;
    List<BizObject> queryBizObjsWarpParentInfo(List<String> ids) throws Exception;
    void changeSettleFlagAfterAudit(PayMargin payMargin) throws Exception;
    void changeSettleFlagAfterUnAudit(PayMargin payMargin) throws Exception;
    void budgetAfterSettleStatusChange(PayMargin payMargin, boolean checkResult) throws Exception;
}
