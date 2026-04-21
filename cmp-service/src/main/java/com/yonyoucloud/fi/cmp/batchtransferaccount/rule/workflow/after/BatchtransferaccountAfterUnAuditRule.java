package com.yonyoucloud.fi.cmp.batchtransferaccount.rule.workflow.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.batchtransferaccount.service.BatchtransferaccountService;
import com.yonyoucloud.fi.cmp.batchtransferaccount.utils.BatchTransferAccountUtil;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetBatchTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * @author xuxbo
 * @date 2025/6/5 16:12
 */
@Component
@Slf4j
public class BatchtransferaccountAfterUnAuditRule extends AbstractCommonRule {

    @Autowired
    private BatchtransferaccountService batchtransferaccountService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    private CmpBudgetBatchTransferAccountManagerService btaCmpBudgetManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bizobject : bills) {
            BatchTransferAccount batchTransferAccount = (BatchTransferAccount)bizobject;
            batchtransferaccountService.revokeBatchTransferAccount(batchTransferAccount.getId());
            // 释放预算
            BatchTransferAccount currentBill = MetaDaoHelper.findByIdPartition(BatchTransferAccount.ENTITY_NAME, batchTransferAccount.getId());
            unauditBudget(currentBill);
        }
        return new RuleExecuteResult();
    }

    /**
     * 更新预算占用状态
     * @param batchTransferAccount
     * @throws Exception
     */
    private void unauditBudget(BatchTransferAccount batchTransferAccount) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT)) {
            return;
        }
        List<BatchTransferAccount_b> batchTransferAccountBs = batchTransferAccount.BatchTransferAccount_b();
        ResultBudget resultBudget = btaCmpBudgetManagerService.unauditOccupyBudget(batchTransferAccount, batchTransferAccountBs, BatchTransferAccountUtil.AUDIT_SAVE_SUBMIT_APPROVE_UNSUBMIT);
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
