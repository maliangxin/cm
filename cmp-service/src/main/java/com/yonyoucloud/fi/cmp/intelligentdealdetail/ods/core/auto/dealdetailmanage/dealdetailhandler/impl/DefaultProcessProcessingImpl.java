package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl;

import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.ODSBasicInfoObject;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.BankDealDetailManageFacade;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IDealDetailProcessing;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.BankDealDetailMatchChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common.CmpCheckRuleCommonProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailThreadLocalUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @Author guoyangy
 * @Date 2024/7/3 11:50
 * @Description todo
 * @Version 1.0
 */
@Service
@Slf4j
public class DefaultProcessProcessingImpl implements IDealDetailProcessing {

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    public IBusinessLogService businessLogService;
    @Resource
    private IBankDealDetailAccessDao bankDealDetailAccessDao;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void dealDetailProcesing(List<BankDealDetailWrapper> bankDealDetailWrappers, List<BankReconciliation> updateBankReconciliationList, DefaultBankDealDetailChain bankDealDetailChain, String traceId, String requestSeqNo) {
        this.dealDetailProcesing(bankDealDetailWrappers, updateBankReconciliationList, bankDealDetailChain, null, traceId, requestSeqNo);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public void dealDetailProcesing(List<BankDealDetailWrapper> bankDealDetailWrappers, List<BankReconciliation> updateBankReconciliationList, DefaultBankDealDetailChain bankDealDetailChain, Integer index, String traceId, String requestSeqNo) {
        if (CollectionUtils.isEmpty(bankDealDetailWrappers)) {
            log.error("进入辨识匹配规则管理器待流水为空，结束");
            return;
        }
        log.error(index == null ? "【辨识匹配规则管理器】开始进入流水辨识匹配规则管理器" : "【流程处理器】开始进入流水流程处理流程");
        //step1:构建上下文
        BankDealDetailContext context = new BankDealDetailContext(bankDealDetailWrappers, updateBankReconciliationList, traceId, requestSeqNo);
        //step2:加载规则
        List<BankreconciliationIdentifyType> bankDealDetailIdentifyMatchRules = bankDealDetailChain.loadStreamIdentifyMatchRuleByCode(index);
        if (CollectionUtils.isEmpty(bankDealDetailIdentifyMatchRules)) {
            log.error("【流程管理器执行完成】辨识匹配规则为空，流水管理器不在往下执行，流水流程终止");
            // throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004C6", "【流程管理器执行完成】辨识匹配规则为空，流水管理器不在往下执行，流水流程终止") /* "【流程管理器执行完成】辨识匹配规则为空，流水管理器不在往下执行，流水流程终止" */);
        } else {
            //step3:将规则加载到上下文
            context.setBankDealDetailIdentifyMatchRules(bankDealDetailIdentifyMatchRules);
            //step4:通过执行规则器执行具体规则
            bankDealDetailChain.setDentifyMatchHandlerList(bankDealDetailIdentifyMatchRules);
            bankDealDetailChain.setCtmcmpBusinessLogService(ctmcmpBusinessLogService, businessLogService);
            //step5:执行辨识匹配、流程处理动作，此处异常之后，打印的日志无法再输出
            bankDealDetailChain.handle(context, bankDealDetailChain);
            //若是被标识的上下文，则直接处理结束即可
            if (DealDetailEnumConst.SAVE_DIRECT_FINISH.equals(context.getSaveDirect())) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        LocalDateTime now = LocalDateTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        log.error("【银企联拉取落库后的执行逻辑】一个批次=======================开始执行,执行时间为{}", now.format(formatter));
                        ODSBasicInfoObject odsBasicInfoObject = new ODSBasicInfoObject();
                        List<BankReconciliation> bankReconciliations = odsBasicInfoObject.getBankReconciliationList(context);
                        bankReconciliations.stream().forEach(p -> {
                            p.put(DealDetailEnumConst.SAVE_DIRECT, null);
                        });
                        context.setSaveDirect(null);
                        List<BankDealDetailWrapper> bankDealDetailWrappers = DealDetailUtils.convertBankReconciliationToWrapper(bankReconciliations, true);
                        BankDealDetailManageFacade bankDealDetailManageFacade = AppContext.getBean(BankDealDetailManageFacade.class);
                        bankDealDetailManageFacade.bankDealDetailManageAccess(bankDealDetailWrappers, bankReconciliations, BankDealDetailMatchChainImpl.get().code(null), context.getTraceId(), "from context");
                    }
                });
                return;
            }
            //step6:所有流程处理完，处理最后数据
            context.setBankDealDetailChain(bankDealDetailChain);
            bankDealDetailChain.flush(context);
            log.error(index == null ? "辨识匹配规则管理器执行完成" : "流程处理器执行完成");
        }

    }
}
