package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business;

import com.google.common.collect.ImmutableMap;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreceipt.service.TaskBankReceiptService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.cmpentity.ReceiptassociationStatus;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.functional.IDealDetailCallBack;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultStreamBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author guoyangy
 * @Date 2024/6/18 15:33
 * @Description 银行交易回单规则
 * @Version 1.0
 */
@Service(RuleCodeConst.SYSTEM001)
@Slf4j
public class BankReceiptAssociationMatchHandler extends DefaultStreamBatchHandler {

    @Autowired
    private TaskBankReceiptService taskBankReceiptService;
    @Autowired
    private BankreconciliationService bankreconciliationService;

    private static final String BANKRECEIPT_MAPPER = "com.yonyoucloud.fi.cmp.bankReceipt.rule.BankReceiptMapper";
    private static final String BANKRECONCILIATION_MAPPER = "com.yonyoucloud.fi.cmp.bankreconciliation.rule.BankReconciliationMapper";

    @Override
    public Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) {

        try {
            if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect())){
                return ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), getBankReconciliationList(context));
            }
            context.getParamsConfigs().put("ruleCode","system001");
            context.setLogName(RuleLogEnum.RuleLogProcess.BANK_RECEIPT_LOG_NAME.getDesc());
            List<BankReconciliation> bankReconciliationList = this.getBankReconciliationList(context);
            context.setLogName(RuleLogEnum.RuleLogProcess.BANK_RECEIPT_ASSOCIATION_NAME.getDesc());
            context.setOperationName(RuleLogEnum.RuleLogProcess.BANK_RECEIPT_ASSOCIATION_START.getDesc());
            return this.updateBankReceiptData(bankReconciliationList, context);
        } catch (Exception e) {
            log.error("智能流水执行辨识异常：调用回单关联规则失败，方法：streamHandler():", e);
        }
        context.getParamsConfigs().remove("ruleCode");

        return ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), getBankReconciliationList(context));
    }

    /**
     * 获取银行回单关联数据
     * 无论关联成功或失败，都不阻塞流程
     * 回单关联时需要判断银行流水的交易日期是否为当天，若为当天则不进行辨识，否则存在空跑的情况
     *
     * @return
     * @throws Exception
     */
    private Map<String, List<BankReconciliation>> updateBankReceiptData(List<BankReconciliation> bankReconciliationList, BankDealDetailContext context) {
        Map<String, List<BankReconciliation>> resultMap = new HashMap<>();
        try {
            if (CollectionUtils.isEmpty(bankReconciliationList)) {
                return resultMap;
            }
            List<String> enterpriseBankAccountList = new ArrayList<>();
            for (BankReconciliation bankReconciliation : bankReconciliationList) {
                if (!enterpriseBankAccountList.contains(bankReconciliation.getBankaccount()) && DateUtils.dateCompare(bankReconciliation.getTran_date(), new Date()) < 0) {
                    enterpriseBankAccountList.add(bankReconciliation.getBankaccount());
                }
            }
            String startDate = DateUtils.dateFormat(bankReconciliationList.stream().min(Comparator.comparing(BankReconciliation::getTran_date)).get().getTran_date(), DateUtils.DATE_PATTERN);
            String endDate = DateUtils.dateFormat(bankReconciliationList.stream().max(Comparator.comparing(BankReconciliation::getTran_date)).get().getTran_date(), DateUtils.DATE_PATTERN);
            if (!CollectionUtils.isEmpty(enterpriseBankAccountList)) {
                CommonRequestDataVo commonQueryData = new CommonRequestDataVo();
                commonQueryData.setStartDate(startDate);
                commonQueryData.setEndDate(endDate);
                commonQueryData.setEnterpriseBankAccountList(enterpriseBankAccountList);
                commonQueryData.setYtenantId(InvocationInfoProxy.getTenantid());
                if (!CollectionUtils.isEmpty(bankReconciliationList)) {
                    List<Long> bankReconciliationIds = new ArrayList<>();
                    for (BankReconciliation bankReconciliation : bankReconciliationList) {
                        Long id = bankReconciliation.getId();
                        bankReconciliationIds.add(id);
                    }
                    //借助claimIds字段暂存下流水id，sql中按照流水id更新
                    commonQueryData.setClaimIds(bankReconciliationIds);
                    //设置日志
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLogForList(bankReconciliationList, RuleLogEnum.RuleLogProcess.BANK_RECEIPT_ASSOCIATION_NAME.getDesc(), RuleLogEnum.RuleLogProcess.BANK_RECEIPT_ASSOCIATION_EXECUTION_STEP_ONE.getDesc(), context);
                    //使用callback，callback里更新回单；修改入参，最后回调时一起修改流水
                    Map<BankReconciliation, BankElectronicReceipt> bankReconciliationBankElectronicReceiptMap = taskBankReceiptService.matchBankReconciliationAndBankreceipt(bankReconciliationList);
                    bankReconciliationBankElectronicReceiptMap.forEach((bankReconciliation, bankElectronicReceipt) -> {
                        IDealDetailCallBack iDealDetailCallBack = ()->{
                            MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
                            sendReceiptRelatedEvent(bankReconciliation, bankElectronicReceipt);
                        };
                        DealDetailUtils.addCallBackToBankReconciliation(bankReconciliation,iDealDetailCallBack);
                    });



                    // 查库后，更新内存中的银行流水数据，往下传递
                    handleContextBankReconciliationList(bankReconciliationList, context);
                }

//             DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM001_01YO1.getCode(),bankReconciliationList);
            }
        } catch (Exception e) {
            log.error("银行交易回单关联异常：{}", e);
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM001_01SO1.getCode(), bankReconciliationList);
        } finally {
            resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), bankReconciliationList);
        }
        return resultMap;
    }

    private void sendReceiptRelatedEvent(BankReconciliation bankReconciliation, BankElectronicReceipt matchedBankElectronicReceipt) throws Exception {
        //凭证关联银行电子回单功能；电子回单下载过文件且银行对账单和总账凭证已勾对，要发送关联事件
        if (matchedBankElectronicReceipt!= null && matchedBankElectronicReceipt.getIsdown() && bankReconciliation.getOther_checkflag()) {
            bankreconciliationService.handleBankReceiptCorrEvent(matchedBankElectronicReceipt, bankReconciliation);
        }
        String fileId = matchedBankElectronicReceipt.getExtendss();
        Long bankreconcilionId = bankReconciliation.getId();
        LogUtil.saveBankelereceiptSendFileEventlogById(bankreconcilionId.toString(), fileId, 100);
        bankreconciliationService.sendEventOfFileid(bankreconcilionId, fileId);
    }

    /**
     * 更新上下文中银行对账单数据
     *
     * @param bankReconciliationList 上下文中银行对账单数据
     * @throws Exception
     */
    private void handleContextBankReconciliationList(List<BankReconciliation> bankReconciliationList, BankDealDetailContext context) throws Exception {
        //receiptassociation非空并且为自动回单关联
        List<BankReconciliation> modifiedReconciliationList = bankReconciliationList.stream().filter(k -> k.getReceiptassociation() != null).filter(t -> t.getReceiptassociation() == ReceiptassociationStatus.AutomaticAssociated.getValue())
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(modifiedReconciliationList)) {
            log.error("handleContextBankReconciliationList 交易回单匹配成功的流水数据为空!");
            return;
        }

        Short reassociation;
        for (BankReconciliation paramReconciliation : bankReconciliationList) {
            reassociation = paramReconciliation.getReceiptassociation();
            if (reassociation == ReceiptassociationStatus.AutomaticAssociated.getValue()) {
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(paramReconciliation, RuleLogEnum.RuleLogProcess.BANK_RECEIPT_ASSOCIATION_NAME.getDesc(), RuleLogEnum.RuleLogProcess.BANK_RECEIPT_ASSOCIATION_EXECUTION_STEP_TWO.getDesc(), context);
            } else {
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(paramReconciliation, RuleLogEnum.RuleLogProcess.BANK_RECEIPT_ASSOCIATION_NAME.getDesc(), RuleLogEnum.RuleLogProcess.BANK_RECEIPT_ASSOCIATION_EXECUTION_STEP_FOUR.getDesc(), context);
            }
        }
    }
}