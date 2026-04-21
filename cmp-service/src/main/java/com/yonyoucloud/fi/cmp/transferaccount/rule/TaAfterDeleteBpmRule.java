package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component("taAfterDeleteBpmRule")
@Slf4j
public class TaAfterDeleteBpmRule extends AbstractCommonRule {

    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BizObject bizObject = getBills(billContext, map).get(0);
        TransferAccount currentBill = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizObject.get("id"), 3);
        String deleteReason = billContext.getDeleteReason();
        Long transferId = bizObject.get("id");
        TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, transferId);

        if (currentBill.getCheckid() != null) {
            // 驳回到制单，已入库
            switch(deleteReason){
                case "REJECTTOSTART":
                    break;
                case "WITHDRAWREJECTTOSTART":
                    break;
                default:
            }
        }
        switch (deleteReason){
            case "REJECTTOSTART":
            case "ACTIVITI_DELETED":
                transferAccount.put("deleteReason", deleteReason);
                boolean success = cmpBudgetTransferAccountManagerService.releaseBudget(transferAccount);
                if (success) {
                    cmpBudgetTransferAccountManagerService.updateOccupyBudget(transferAccount, OccupyBudget.UnOccupy.getValue());
                }
                break;
            case "WITHDRAWREJECTTOSTART":
                //撤回
                log.error("WITHDRAWREJECTTOSTART........");
                boolean successWithdraw = cmpBudgetTransferAccountManagerService.budget(transferAccount);
                if (successWithdraw) {
                    cmpBudgetTransferAccountManagerService.updateOccupyBudget(transferAccount, OccupyBudget.PreSuccess.getValue());
                }
                break;
                default:
                    log.error("withdraw........");
        }
        return new RuleExecuteResult();
    }
}
