package com.yonyoucloud.fi.cmp.transferaccount.rule.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("taAfterSubmitRule")
public class TaAfterSubmitRule extends AbstractCommonRule {

    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizObject.getId(), null);
            if (ObjectUtils.isEmpty(transferAccount)) {
                return result;
            }
            boolean budget = cmpBudgetTransferAccountManagerService.budget(transferAccount);
            if (budget) {
                cmpBudgetTransferAccountManagerService.updateOccupyBudget(transferAccount, OccupyBudget.PreSuccess.getValue());
            }
        }
        return new RuleExecuteResult();
    }
}
