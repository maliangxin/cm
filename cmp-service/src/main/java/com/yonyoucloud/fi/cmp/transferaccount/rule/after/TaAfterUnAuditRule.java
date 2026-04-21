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
@Component("taAfterUnAuditRule")
public class TaAfterUnAuditRule extends AbstractCommonRule {

    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            TransferAccount account = (TransferAccount) bizObject;
            TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizObject.getId(), null);
            if (ObjectUtils.isEmpty(transferAccount)) {
                return result;
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 是否占预算为实占成功时，删除实占；
     * 是否占预算为预占成功时，删除预占；
     *
     * @param transferAccount
     * @throws Exception
     */
    private boolean releaseBudget(TransferAccount transferAccount) throws Exception {
        Short budgeted = transferAccount.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return false;
        } else if (OccupyBudget.PreSuccess.getValue() == budgeted) {//是否占预算为预占成功时，删除预占；
            return cmpBudgetTransferAccountManagerService.releaseBudget(transferAccount);
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            return cmpBudgetTransferAccountManagerService.releaseImplement(transferAccount);
        }
        return false;
    }

    /**
     * 如果是预占就跳过，如果是实占，删除实占，重新预占
     *
     * @param transferAccount
     * @throws Exception
     */
    private Short budgetAfterUnAudit(TransferAccount transferAccount) throws Exception {
        Short budgeted = transferAccount.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return null;
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            boolean releaseImplement = cmpBudgetTransferAccountManagerService.releaseImplement(transferAccount);
            if (releaseImplement) {
                //重新预占
                log.error("重新预占.....");
                //且结算状态应置为待结算、并清空结算成功时间
                transferAccount.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                boolean budget = cmpBudgetTransferAccountManagerService.budget(transferAccount);
                if (budget) {//可能是没有匹配上规则，也可能是没有配置规则
                    return OccupyBudget.PreSuccess.getValue();
                } else {
                    return OccupyBudget.UnOccupy.getValue();
                }
            } else {
                log.error("释放实占失败,releaseImplement:{}", releaseImplement);
            }
        }
        return null;
    }
}
