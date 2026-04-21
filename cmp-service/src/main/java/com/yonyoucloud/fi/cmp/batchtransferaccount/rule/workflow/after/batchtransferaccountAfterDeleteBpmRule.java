package com.yonyoucloud.fi.cmp.batchtransferaccount.rule.workflow.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.batchtransferaccount.utils.BatchTransferAccountUtil;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetBatchTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.util.BillAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author hanll
 * @Date 2025/12/5-19:59
 */
@Component("batchtransferaccountAfterDeleteBpmRule")
@Slf4j
public class batchtransferaccountAfterDeleteBpmRule extends AbstractCommonRule {

    @Autowired
    private CmpBudgetBatchTransferAccountManagerService btaCmpBudgetManagerService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BizObject bizObject = getBills(billContext, paramMap).get(0);
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT)) {
            return new RuleExecuteResult();
        }
        BatchTransferAccount currentBill = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, bizObject.get("id"), 3);
        String deleteReason = billContext.getDeleteReason();
        switch (deleteReason){
            // 退回制单
            case "REJECTTOSTART":
            // 终止
            case "ACTIVITI_DELETED":
                String billAction = deleteReason.equals("ACTIVITI_DELETED") ? BatchTransferAccountUtil.AUDIT_SAVE_SUBMIT_TERMINATE : BatchTransferAccountUtil.AUDIT_SAVE_SUBMIT_APPROVE_UNPASS;
                unauditOccupyBudget(currentBill, billAction);
                break;
            // 撤回退回动作
            case "WITHDRAWREJECTTOSTART":
                unauditOccupyBudget(currentBill, BatchTransferAccountUtil.AUDIT_SAVE_SUBMIT_APPROVE_UNSUBMIT);
                break;
            default:
                log.error("withdraw........");
        }
        return new RuleExecuteResult();
    }

    /**
     * 更新预算占用状态
     *
     * @param batchTransferAccount
     * @throws Exception
     */
    private void unauditOccupyBudget(BatchTransferAccount batchTransferAccount, String action) throws Exception {
        List<BatchTransferAccount_b> batchTransferAccountBs = batchTransferAccount.BatchTransferAccount_b();
        ResultBudget resultBudget = btaCmpBudgetManagerService.unauditOccupyBudget(batchTransferAccount, batchTransferAccountBs, action);
        if (resultBudget.isSuccess()) {
            if (resultBudget.getIds() != null && !resultBudget.getIds().isEmpty()) {
                batchTransferAccountBs.stream().forEach(item -> {
                    if (resultBudget.getIds().contains(item.getId().toString())) {
                        item.setIsOccupyBudget(resultBudget.getBudgeted());
                        item.setEntityStatus(EntityStatus.Update);
                    }
                });
            } else {
                batchTransferAccountBs.stream().forEach(item -> {
                    item.setIsOccupyBudget(resultBudget.getBudgeted());
                    item.setEntityStatus(EntityStatus.Update);
                });
            }
            MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, batchTransferAccountBs);
        }

    }
}
