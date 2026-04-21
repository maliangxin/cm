package com.yonyoucloud.fi.cmp.batchtransferaccount.rule.business;

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
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * @author xuxbo
 * @date 2025/6/5 13:55
 */
@Slf4j
@Component
public class BatchtransferaccountAfterSaveRule extends AbstractCommonRule {

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    private CmpBudgetBatchTransferAccountManagerService btaCmpBudgetManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        String billnum = billContext.getBillnum();
        if (bills != null && bills.size() > 0) {
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101195"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801FC","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            BatchTransferAccount batchTransferAccount = (BatchTransferAccount) bills.get(0);
            if (batchTransferAccount.getEntityStatus().equals(EntityStatus.Update) || batchTransferAccount.getEntityStatus().equals(EntityStatus.Insert)) {
                saveBudget(batchTransferAccount);
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 更新预算占用状态
     * @param batchTransferAccount
     * @throws Exception
     */
    private void saveBudget(BatchTransferAccount batchTransferAccount) throws Exception {
        // 预算
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT)) {
            return;
        }
        BatchTransferAccount dbBatchTransferAccount = MetaDaoHelper.findByIdPartition(BatchTransferAccount.ENTITY_NAME, batchTransferAccount.getId(), 2);
        List<BatchTransferAccount_b> batchTransferAccountBs = dbBatchTransferAccount.BatchTransferAccount_b();
        if (CollectionUtils.isEmpty(batchTransferAccountBs)) {
            return;
        }

        ResultBudget resultBudget = btaCmpBudgetManagerService.saveOccupyBudget(dbBatchTransferAccount, batchTransferAccountBs, BatchTransferAccountUtil.SAVE_SAVE);
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
