package com.yonyoucloud.fi.cmp.transferaccount.rule.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("taAfterUnSubmitRule")
public class TaAfterUnSubmitRule extends AbstractCommonRule {
    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;
    @Autowired
    private AutoConfigService autoConfigService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizObject.getId(), null);
            releaseBudget(transferAccount);
            Boolean pushSettlement = autoConfigService.getCheckFundTransfer();
            //当不传结算且结算状态为已结算补单的时候，单据审批通过后要给结算日期、结算成功金额赋值，结算日期取单据日期，结算成功金额取转账金额；
            SettleStatus settlestatus = transferAccount.getSettlestatus();
            if (!pushSettlement && settlestatus != null && settlestatus.getValue() == SettleStatus.SettledRep.getValue()) {
                TransferAccount update = new TransferAccount();
                update.setId(transferAccount.getId());
                update.setSettledate(null);
                update.setSettleSuccessAmount(null);
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(TransferAccount.ENTITY_NAME, update);

            }
            TransferAccount transferAccountnew = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, transferAccount.getId());
            bizObject.setPubts(transferAccountnew.getPubts());
        }
        return result;
    }

    /**
     * 是否占预算为实占成功时，删除实占；
     * 是否占预算为预占成功时，删除预占；
     *
     * @param transferAccount
     * @throws Exception
     */
    private void releaseBudget(TransferAccount transferAccount) throws Exception {
        Short budgeted = transferAccount.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return;
        }
        if (OccupyBudget.PreSuccess.getValue() == budgeted) {//是否占预算为预占成功时，删除预占；
            boolean releaseBudget = cmpBudgetTransferAccountManagerService.releaseBudget(transferAccount);
            if (releaseBudget) {
                cmpBudgetTransferAccountManagerService.updateOccupyBudget(transferAccount, OccupyBudget.UnOccupy.getValue());
            }
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            boolean releaseImplement = cmpBudgetTransferAccountManagerService.releaseImplement(transferAccount);
            if (releaseImplement) {
                cmpBudgetTransferAccountManagerService.updateOccupyBudget(transferAccount, OccupyBudget.UnOccupy.getValue());
            }
        }
    }
}
