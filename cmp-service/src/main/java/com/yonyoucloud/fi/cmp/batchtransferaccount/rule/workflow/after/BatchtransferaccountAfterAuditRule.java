package com.yonyoucloud.fi.cmp.batchtransferaccount.rule.workflow.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.settleapply.enums.SettleApplyDetailStateEnum;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.batchtransferaccount.service.BatchtransferaccountService;
import com.yonyoucloud.fi.cmp.batchtransferaccount.utils.BatchTransferAccountUtil;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetBatchTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.enums.YesOrNoEnum;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author xuxbo
 * @date 2025/6/5 16:12
 */
@Component
@Transactional(rollbackFor = Exception.class)
public class BatchtransferaccountAfterAuditRule extends AbstractCommonRule {

    @Autowired
    private BatchtransferaccountService batchtransferaccountService;
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
        BatchTransferAccount currentBill = MetaDaoHelper.findByIdPartition(BatchTransferAccount.ENTITY_NAME, batchTransferAccount.getId());
        //判断交易类型 非缴存现金 直接推送结算单
        // 交易类型
        BdTransType bdTransType = transTypeQueryService.findById(currentBill.getTradeType());
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
        String tradeTypeCode = (String) jsonObject.get("batchtransferType_ext");
        if (!("SC".equals(currentBill.get("tradeType_code")) || "jcxj".equals(tradeTypeCode))) {
            // 更新单据结算状态
            updateBillSettleStatus(currentBill, false);
            // 审批通过需要推结算单
            batchtransferaccountService.pushPaymentSettleFirst(currentBill,false);
            // 审核通过占用预算
            auditBudget(currentBill, BatchTransferAccountUtil.AUDIT_APPROVE_PASS);
        } else if ("SC".equals(currentBill.get("tradeType_code")) || "jcxj".equals(tradeTypeCode)){ //缴存现金
            // 审批通过需要推结算单
            // 子表数据要么全是现金柜业务  要么全都不是现金柜业务
            List<BatchTransferAccount_b> batchTransferAccount_bs = currentBill.BatchTransferAccount_b();
            if (CollectionUtils.isNotEmpty(batchTransferAccount_bs)) {
                Boolean isCashBusiness = batchTransferAccount_bs.get(0).getIsCashBusiness() == YesOrNoEnum.YES.getValue();
                // 更新单据结算状态
                updateBillSettleStatus(currentBill, isCashBusiness);
                //现金柜业务
                //非现金柜业务
                batchtransferaccountService.pushPaymentSettleFirst(currentBill, isCashBusiness);
                // 审核通过占用预算
                auditBudget(currentBill, BatchTransferAccountUtil.AUDIT_APPROVE_PASS);
            }
        }

        return new RuleExecuteResult();
    }

    /**
     * 更新单据主表和子表的结算状态
     *
     * @param currentBill
     * @param isCashBusiness
     * @throws Exception
     */
    public void updateBillSettleStatus(BatchTransferAccount currentBill, Boolean isCashBusiness) throws Exception {
        // 更新子表结算状态
        updateBtfabStatus(currentBill.BatchTransferAccount_b(), isCashBusiness);
        // 更新主表结算状态
        updateMainBillStatus(currentBill, isCashBusiness);
    }

    /**
     * 更新主表状态为待结算
     *
     * @param currentBill
     * @param isCashBusiness
     */
    public void updateMainBillStatus(BatchTransferAccount currentBill, Boolean isCashBusiness) throws Exception {
        BatchTransferAccount dbBatchTransferAccount = new BatchTransferAccount();
        dbBatchTransferAccount.setEntityStatus(EntityStatus.Update);
        // 现金柜主表更新成结算中
        if (isCashBusiness) {
            dbBatchTransferAccount.setSettlestatus(SettleApplyDetailStateEnum.HANDLING.getValue());
        } else {
            dbBatchTransferAccount.setSettlestatus(SettleApplyDetailStateEnum.WAIT_HANDLE.getValue());
        }

        dbBatchTransferAccount.setPubts(currentBill.getPubts());
        dbBatchTransferAccount.setId(currentBill.getId());
        MetaDaoHelper.update(BatchTransferAccount.ENTITY_NAME, dbBatchTransferAccount);
    }


    /**
     * 更新子表状态为结算中
     *
     * @param batchTransferAccount_bs
     * @param isCashBusiness
     * @throws Exception
     */
    public void updateBtfabStatus(List<BatchTransferAccount_b> batchTransferAccount_bs, Boolean isCashBusiness) throws Exception {
        List<BatchTransferAccount_b> dbBatchTransferAccount_bs = new ArrayList<>();
        for (BatchTransferAccount_b batchTransferAccount_b : batchTransferAccount_bs) {
            BatchTransferAccount_b dbBatchTransferAccount_b = getBatchTransferAccountB(batchTransferAccount_b, isCashBusiness);
            dbBatchTransferAccount_bs.add(dbBatchTransferAccount_b);
        }
        MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, dbBatchTransferAccount_bs);
    }

    /**
     * 获取子表
     *
     * @param batchTransferAccount_b
     * @param isCashBusiness
     * @return
     */
    private BatchTransferAccount_b getBatchTransferAccountB(BatchTransferAccount_b batchTransferAccount_b, Boolean isCashBusiness) {
        BatchTransferAccount_b dbBatchTransferAccount_b = new BatchTransferAccount_b();

        if (isCashBusiness) {
            // 结算中
            dbBatchTransferAccount_b.setRecSettleStatus(SettleApplyDetailStateEnum.HANDLING.getValue());
        } else {
            // 待结算
            dbBatchTransferAccount_b.setPaySettleStatus(SettleApplyDetailStateEnum.WAIT_HANDLE.getValue());
        }
        dbBatchTransferAccount_b.setPubts(batchTransferAccount_b.getPubts());
        dbBatchTransferAccount_b.setId(batchTransferAccount_b.getId());
        dbBatchTransferAccount_b.setEntityStatus(EntityStatus.Update);
        return dbBatchTransferAccount_b;
    }

    /**
     * 更新预算占用状态
     *
     * @param batchTransferAccount
     * @throws Exception
     */
    private void auditBudget(BatchTransferAccount batchTransferAccount, String action) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT)) {
            return;
        }
        BatchTransferAccount dbBatchTransferAccount = MetaDaoHelper.findByIdPartition(BatchTransferAccount.ENTITY_NAME, batchTransferAccount.getId());
        List<BatchTransferAccount_b> batchTransferAccountBs = dbBatchTransferAccount.BatchTransferAccount_b();

        ResultBudget resultBudget = btaCmpBudgetManagerService.auditOccupyBudget(dbBatchTransferAccount, batchTransferAccountBs, action);
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
