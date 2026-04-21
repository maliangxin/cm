package com.yonyoucloud.fi.cmp.batchtransferaccount.rule.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.batchtransferaccount.utils.BatchTransferAccountUtil;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetBatchTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.enums.YesOrNoEnum;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.transferaccount.util.AssertUtil;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import org.apache.commons.collections4.CollectionUtils;
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
@Component
public class BatchtransferaccountDeleteRule extends AbstractCommonRule {
    @Autowired
    private TransTypeQueryService transTypeQueryService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    private CmpBudgetBatchTransferAccountManagerService btaCmpBudgetManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        BatchTransferAccount batchTransferAccount = (BatchTransferAccount) bills.get(0);
        BatchTransferAccount dbBatchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, batchTransferAccount.getId());
        AssertUtil.isNotNull(dbBatchTransferAccount, () -> new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800FA","单据不存在，请刷新后重试！") /* "单据不存在，请刷新后重试！" */)); /*单据不存在*/
        // 交易类型
        BdTransType bdTransType = transTypeQueryService.findById(batchTransferAccount.getTradeType());
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
        String tradeTypeCode = jsonObject.getString("batchtransferType_ext");
        // 释放支票
        releaseNote(batchTransferAccount, dbBatchTransferAccount, tradeTypeCode);
        // 删除预算
        deleteBudget(dbBatchTransferAccount);
        return new RuleExecuteResult();
    }

    /**
     * 释放支票
     * @param batchTransferAccount
     * @param dbBatchTransferAccount
     * @param tradeTypeCode
     */
    private void releaseNote(BatchTransferAccount batchTransferAccount, BatchTransferAccount dbBatchTransferAccount, String tradeTypeCode) throws Exception {
        if (!"EC".equals(batchTransferAccount.get("tradeType_code")) && !"tqxj".equals(tradeTypeCode)) {
            return;
        }
        List<BatchTransferAccount_b> billbs = dbBatchTransferAccount.BatchTransferAccount_b();
        for (BatchTransferAccount_b batchTransferAccountB : billbs) {
            if (batchTransferAccountB.getNoteId() == null) {
                continue;
            }
            CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, batchTransferAccountB.getNoteId());
            checkStock.setOccupy(YesOrNoEnum.NO.getValue());
            checkStock.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
        }
    }

    /**
     * 更新预算占用状态
     *
     * @param dbBatchTransferAccount
     * @throws Exception
     */
    private void deleteBudget(BatchTransferAccount dbBatchTransferAccount) throws Exception {
        // 预算
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT)) {
            return;
        }
        List<BatchTransferAccount_b> batchTransferAccountBs = dbBatchTransferAccount.BatchTransferAccount_b();
        if (CollectionUtils.isEmpty(batchTransferAccountBs)) {
            return;
        }

        ResultBudget resultBudget = btaCmpBudgetManagerService.deleteOccupyBudget(dbBatchTransferAccount, batchTransferAccountBs, BatchTransferAccountUtil.SAVE_DELETE);
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
