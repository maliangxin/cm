package com.yonyoucloud.fi.cmp.batchtransferaccount.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.batchtransferaccount.utils.BatchTransferAccountUtil;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetBatchTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author xuxbo
 * @date 2025/6/5 16:12
 */
@Component
@Slf4j
public class BatchtransferaccountBeforeSubmitRule extends AbstractCommonRule {

    @Autowired
    private CmCommonService cmCommonService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    private CmpBudgetBatchTransferAccountManagerService btaCmpBudgetManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            BatchTransferAccount batchTransferAccount = MetaDaoHelper.findByIdPartition(BatchTransferAccount.ENTITY_NAME, bizObject.getId(), 2);
            List<BatchTransferAccount_b> batchTransferAccount_bs = batchTransferAccount.BatchTransferAccount_b();
            if (StringUtils.isEmpty(batchTransferAccount.getTradeType())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100518"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180437", "交易类型不能为空！") /* "交易类型不能为空！" */);
            }

            BigDecimal totalAmount = BigDecimal.ZERO;
            for (BatchTransferAccount_b batchTransferAccountB : batchTransferAccount_bs) {
                BigDecimal oriSum = batchTransferAccountB.getOriSum();
                if (oriSum != null) {
                    totalAmount = totalAmount.add(oriSum);
                }
            }
            if (totalAmount.compareTo(batchTransferAccount.getTransferSumAmount()) != 0) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004CC", "批量同名账户划转单金额与转账明细汇总金额不相等") /* "批量同名账户划转单金额与转账明细汇总金额不相等" */);
            }

            short verifystate = Short.parseShort(bizObject.get("verifystate").toString());
            if (verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100308"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00033", "单据已提交，不能进行重复提交!") /* "单据已提交，不能进行重复提交!" */);
            }
            if (verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100308"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00033", "单据已提交，不能进行重复提交!") /* "单据已提交，不能进行重复提交!" */);
            }

            // 走电子影像
            checkShareImgSubmit(bizObject, billContext, paramMap);
            String action = BatchTransferAccountUtil.SUBMIT_SUBMIT;
            if (null == batchTransferAccount.getIsWfControlled() || !batchTransferAccount.getIsWfControlled()) {
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule("audit", billContext, paramMap);
                result.setCancel(true);
                // 未启动审批流，单据直接审批通过
                action = BatchTransferAccountUtil.SUBMIT_APPROVE_PASS;
            }
            // 占用预算
            submitBudget(batchTransferAccount, action);
        }

        return result;
    }

    /**
     * 走电子影像
     * @param bizObject
     * @param billContext
     * @throws Exception
     */
    private void checkShareImgSubmit(BizObject bizObject, BillContext billContext, Map<String, Object> map) throws Exception {
        Map<String, Object> autoConfigMap = cmCommonService.queryAutoConfigByAccentity(bizObject.get(ICmpConstant.ACCENTITY)) ;
        if(!Objects.isNull(autoConfigMap) && null != autoConfigMap.get("isShareVideo") && (Boolean) autoConfigMap.get("isShareVideo")){
            //走影像
            BillBiz.executeRule("shareSubmit", billContext, map);
        }
    }

    /**
     * 更新预算占用状态
     *
     * @param batchTransferAccount
     * @throws Exception
     */
    private void submitBudget(BatchTransferAccount batchTransferAccount, String action) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT)) {
            return;
        }
        BatchTransferAccount dbBatchTransferAccount = MetaDaoHelper.findByIdPartition(BatchTransferAccount.ENTITY_NAME, batchTransferAccount.getId(), 2);
        List<BatchTransferAccount_b> batchTransferAccountBs = dbBatchTransferAccount.BatchTransferAccount_b();

        ResultBudget resultBudget = btaCmpBudgetManagerService.submitOccupyBudget(dbBatchTransferAccount, batchTransferAccountBs, action);
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
