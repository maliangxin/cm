package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl;

import com.google.common.collect.ImmutableMap;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.ODSBasicInfoObject;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailRuleResult;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.BankDealDetailManageFacade;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IDealDetailProcessing;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IStremBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.process.BankDealDetailProcessChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.model.DealDetailRuleExecRecord;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailExceptionCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author guoyangy
 * @Date 2024/7/3 12:05
 * @Description 流水处理责任链模式
 * @Version 1.0
 */
@Slf4j
public abstract class DefaultBankDealDetailChain extends ODSBasicInfoObject implements IBankDealDetailChain {
    private int cursor;
    //管理器需要执行的具体规则
    private List<BankreconciliationIdentifyType> bankDealDetailIdentifyMatchRules;
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    public IBusinessLogService businessLogService;
    public void setCtmcmpBusinessLogService(CTMCMPBusinessLogService ctmcmpBusinessLogService,IBusinessLogService businessLogService) {
        this.ctmcmpBusinessLogService = ctmcmpBusinessLogService;
        this.businessLogService = businessLogService;
    }

    public void setDentifyMatchHandlerList(List<BankreconciliationIdentifyType> bankDealDetailIdentifyMatchRules) {
        this.bankDealDetailIdentifyMatchRules = bankDealDetailIdentifyMatchRules;
    }

    @Override
    public void handle(BankDealDetailContext context, IBankDealDetailChain chain){
        //step1:规则非空校验
        if (CollectionUtils.isEmpty(bankDealDetailIdentifyMatchRules)) {
            log.error("管理器中设置的执行规则为空，结束流水辨识匹配流程处理");
            return;
        }
        //step2:逐个执行每个规则并处理结果
        if (cursor < bankDealDetailIdentifyMatchRules.size()) {
            BankreconciliationIdentifyType rule = bankDealDetailIdentifyMatchRules.get(cursor++);
            String ruleName = RuleCodeConst.getRuleName(rule.getCode());
            //1.根据规则编码找到处理改规则的具体实现类
            IStremBatchHandler handler = AppContext.getBean(rule.getCode(), IStremBatchHandler.class);
            //2.判断当前规则是否需要执行并将当前执行的规则设置到上下文
            if (!handler.preHandler(context, rule)) {
                log.error("规则{}不需要执行", rule.getCode());
                chain.handle(context, chain);
                return;
            }
            //3.执行具体规则类  todo 这里只处理辨识匹配的场景
            Map<String, List<BankReconciliation>> resultMap = null;
            try {
                log.error("开始执行{}", ruleName);
                resultMap = this.invokeRule(context, handler, chain, rule);
                //4.处理返回结果
                this.processResult(context, resultMap, rule);
            } catch (Exception e) {
                log.error("{}的反参流水数据处理异常", ruleName, e);
                BankDealDetailException bankDealDetailException = new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_CALL_RULE_ERROR.getErrCode(), e.getMessage());
                this.processAfterException(bankDealDetailException, context, resultMap);
                //todo 接口没有显式抛异常，但是还是抛出异常
                //最后处理业务日志
                try {
                    //智能流水异常后，日志处理
                    List<BankDealDetailWrapper> wrappers = context.getWrappers();
                    for (BankDealDetailWrapper wrapper : wrappers){
                        BankReconciliation bankReconciliation = wrapper.getBankReconciliation();
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, context.getLogName(),e.getMessage(),context);
                    }
                    CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                    CmpCheckAndProcessRuleLogProcessor.executeRuleLogLast(context,ctmcmpBusinessLogService,businessLogService);
                }catch (Exception ex){
                    log.error("保存业务日志异常",ex);
                }
                throw bankDealDetailException;
            }
            //5.规则执行后处理 todo 这里只处理生单的场景
            // 这里把异常吃掉了，说明生单场景下，异常了，不影响匹配的执行结果
            try {
                handler.postHandler(context);
            } catch (Exception e) {
                log.error("{}规则后处理异常", ruleName, e);
            }
            if (DealDetailEnumConst.SAVE_DIRECT_FINISH.equals(context.getSaveDirect())){
                return;
            }
            //6.执行下一个规则
            if (!CollectionUtils.isEmpty(context.getWrappers())) {
                chain.handle(context, chain);
            }
        }
    }

    private void processAfterException(Exception e, BankDealDetailContext context, Map<String, List<BankReconciliation>> resultMap) throws BankDealDetailException {
        if (e instanceof BankDealDetailException) {
            BankDealDetailException exception = (BankDealDetailException) e;

//            if(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_TRANSACTION_SUBMIT_ERROR.getErrCode() != errorCode){
//                return;
//            }
            List<BankReconciliation> needTODBList = new ArrayList<>();
            List<BankDealDetailWrapper> bankDealDetailWrapperList = context.getWrappers();
            for (BankDealDetailWrapper bankDealDetailWrapper : bankDealDetailWrapperList) {
                needTODBList.add(bankDealDetailWrapper.getBankReconciliation());
            }
            if (CollectionUtils.isEmpty(needTODBList)) {
                return;
            }
            BankreconciliationIdentifyType currentRule = context.getCurrentRule();
            currentRule = currentRule == null ? context.getPreRule() : currentRule;
            if (null == currentRule) {
                log.error("流水更新或新增回滚了，修改流水状态或ods状态时发现规则rule空，无法修改");
                return;
            }
            List<BankReconciliation> saveBankReconciliationList = new ArrayList<>();
            List<BankReconciliation> updateBankReconciliationList = new ArrayList<>();
            this.prepareBankReconciliationSaveOrUpdate(context, saveBankReconciliationList, updateBankReconciliationList, needTODBList);

            IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
            int errorCode = exception.getErrCode();
            resultMap = ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus(), needTODBList);
            //判断是否需要执行过程处理
            if (errorCode == BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_CALL_RULE_ERROR.getErrCode()) {
                this.setRuleListAndProcessStatusOneByOneBankReconciliation(context, resultMap, true, exception, currentRule);
            }
            BankreconciliationIdentifyType finalCurrentRule = currentRule;
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            //事务中更新状态
                            bankDealDetailAccessDao.updateProcessAfterExcepton(saveBankReconciliationList, updateBankReconciliationList, needTODBList, context, exception, finalCurrentRule);
                        }
                    }
                });
            } else {
                //事务中更新状态
                bankDealDetailAccessDao.updateProcessAfterExcepton(saveBankReconciliationList, updateBankReconciliationList, needTODBList, context, exception, finalCurrentRule);
            }
        }
    }

    private Map<String, List<BankReconciliation>> invokeRule(BankDealDetailContext context, IStremBatchHandler handler, IBankDealDetailChain chain, BankreconciliationIdentifyType rule) {
        String ruleName = RuleCodeConst.getRuleName(rule.getCode());
        //执行完规则统一反参
        Map<String, List<BankReconciliation>> resultMap = null;
        try {
            Long start = System.currentTimeMillis();
            resultMap = handler.streamHandler(context, chain);
            Long usedTime = System.currentTimeMillis() - start;
            context.setCurrentRuleUsedTime(usedTime + "");
            // todo
            log.error("{}执行完成,耗时{}ms", ruleName, usedTime);
        } catch (Exception e) {
            log.error("规则执行失败,当前规则{},失败原因:", ruleName, e);
            resultMap = this.processWhenError(context, e);
            this.processResult(context, resultMap, rule, true, e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400381", "规则执行失败,当前规则") /* "规则执行失败,当前规则" */ + ruleName);
        }
        return resultMap;
    }

    @Override
    public void flush(BankDealDetailContext context) {
        Map<String, List<BankReconciliation>> resultMap = null;
        try {
            //step1: 流程走完，将流水入库或更新
            List<BankDealDetailWrapper> bankDealDetailWrappers = context.getWrappers();

            if (CollectionUtils.isEmpty(bankDealDetailWrappers)) {
                //最后处理业务日志
                try {
                    CmpCheckAndProcessRuleLogProcessor.executeRuleLogLast(context,ctmcmpBusinessLogService,businessLogService);
                }catch (Exception e){
                    log.error("保存业务日志异常",e);
                }
                return;
            }
            List<BankReconciliation> bankReconciliations = this.getBankReconciliationList(context);
            BankreconciliationIdentifyType preRule = context.getPreRule();
            if (null != preRule) {
                if (RuleCodeConst.ALLMATCHRULE.contains(preRule.getCode())) {
                    bankReconciliations.forEach(b -> {
                        b.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_SUCC.getStatus());
                    });
                } else {
                    bankReconciliations.forEach(b -> {
                        b.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_SUCC.getStatus());
                    });
                }
            }
            resultMap = ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus(), bankReconciliations);
            this.processResult(context, resultMap, null, false, null);
            //最后处理业务日志
           try {
               CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
               CmpCheckAndProcessRuleLogProcessor.executeRuleLogLast(context,ctmcmpBusinessLogService,businessLogService);
           }catch (Exception e){
               log.error("保存业务日志异常",e);
           }
        } catch (Exception e) {
            log.error("flush异常", e);
            this.processAfterException(e, context, resultMap);
            throw e;
        }
        //step2:走后置逻辑，将处理后的流水从上下文移除
        DealDetailUtils.removeDealDetailFromContext(context);
    }

    @Override
    public void processResult(BankDealDetailContext context, Map<String, List<BankReconciliation>> resultMap, BankreconciliationIdentifyType rule) {
        this.processResult(context, resultMap, rule, false, null);
    }

    @Override
    public void processResult(BankDealDetailContext context, Map<String, List<BankReconciliation>> resultMap, BankreconciliationIdentifyType rule, boolean isErrorRule, Exception e) {
        log.error("开始处理{}规则的反参", (rule != null ? RuleCodeConst.getRuleName(rule.getCode()) : ""));
        long start = System.currentTimeMillis();
        //step1:参数非空校验
        if (null == resultMap || resultMap.size() == 0) {
            log.error("{}规则的反参为空，不在处理，本规则结束", (rule != null ? RuleCodeConst.getRuleName(rule.getCode()) : ""));
            return;
        }
        Iterator<Map.Entry<String, List<BankReconciliation>>> iterator = resultMap.entrySet().iterator();
        while (iterator.hasNext())  {
            Map.Entry<String, List<BankReconciliation>> entry = iterator.next();
            if (entry.getValue()  == null || entry.getValue().isEmpty())  {
                iterator.remove();
            }
        }
        if (null == resultMap || resultMap.size() == 0) {
            log.error("{}规则的反参为空，不在处理，本规则结束", (rule != null ? RuleCodeConst.getRuleName(rule.getCode()) : ""));
            return;
        }
        //直走辨识，即只走辨识，但不做数据库操作
        if (DealDetailEnumConst.SAVE_AND_UPDATE_NOT_FINISH.equals(context.getSaveDirect())){
            return;
        }
        //step2:处理每条流水的处理过程rulelist及processstatus
        this.setRuleListAndProcessStatusOneByOneBankReconciliation(context, resultMap, isErrorRule, e, rule);
        //step3:获取需要处理流水，入库或者更新
        List<BankReconciliation> addOrUpdateReconciliations = this.checkReturnCountAndGetNeedHandleBankReconciliationList(context, resultMap, rule);
        //step4:流水更新或落库、修改流水在ods状态、保存流水过程
        if (!CollectionUtils.isEmpty(addOrUpdateReconciliations)) {
            this.handleBankReconciliationToDB(context, addOrUpdateReconciliations);
        }
        if (DealDetailEnumConst.SAVE_DIRECT_FINISH.equals(context.getSaveDirect()) || DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect())){
            return;
        }
        log.error(rule != null ? RuleCodeConst.getRuleName(rule.getCode()) + "规则的反参处理完成,耗时{}ms" : "规则的反参流水数据处理完成,耗时{}", System.currentTimeMillis() - start);
        //step5:如果存在流程处理，则执行流程处理逻辑
        this.invokeProcess(context, resultMap, rule);
    }

    private void setRuleListAndProcessStatusOneByOneBankReconciliation(BankDealDetailContext context, Map<String, List<BankReconciliation>> resultMap, boolean isErrorRule, Exception e, BankreconciliationIdentifyType rule) {
        List<BankDealDetailWrapper> bankDealDetailWrappers = context.getWrappers();
        if (CollectionUtils.isEmpty(bankDealDetailWrappers)) {
            return;
        }
//        if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect())){
//            List<BankReconciliation> bankReconciliationList = resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus());
//            if (!CollectionUtils.isEmpty(bankReconciliationList)){
//                bankReconciliationList.stream().forEach(p->{
//                    p.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus());
//                });
//            }
//            return;
//        }
        if (null != resultMap && resultMap.size() > 0) {
            Iterator<String> executeStatusIterator = resultMap.keySet().iterator();
            Map<Long, BankDealDetailWrapper> bankDealDetailWrapperMap = bankDealDetailWrappers.stream().collect(Collectors.toMap(BankDealDetailWrapper::getBankReconciliationId, Function.identity(), (key1, key2) -> key2));
            while (executeStatusIterator.hasNext()) {
                String executeStatus = executeStatusIterator.next();
                List<BankReconciliation> bankReconciliations = resultMap.get(executeStatus);
                if (CollectionUtils.isEmpty(bankReconciliations)) {
                    continue;
                }
                //如果executestatus=4从第1笔流水中取出错误信息
                if (DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus().equals(executeStatus)) {
                    e = new BankDealDetailException(bankReconciliations.get(0).get(DealDetailEnumConst.EXECUTERESULTDESC) + "");
                    isErrorRule = true;
                }
                for (BankReconciliation bankReconciliation : bankReconciliations) {
                    //设置流水的processsatus
                    this.setBankReconciliationExecuteStatusAndProcessStatus(rule, executeStatus, bankReconciliation);
                    BankDealDetailWrapper bankDealDetailWrapper = bankDealDetailWrapperMap.get(bankReconciliation.getId() instanceof String ? Long.parseLong(bankReconciliation.getId()) : bankReconciliation.getId());
                    String ruleDesc = bankDealDetailWrapper.getBankReconciliation().get(DealDetailEnumConst.EXECUTERESULTDESC);
                    List<BankDealDetailRuleResult> bankDealDetailRuleResults = bankDealDetailWrapper.getRuleList();
                    if (null == bankDealDetailRuleResults) {
                        bankDealDetailRuleResults = new ArrayList<>();
                        bankDealDetailWrapper.setRuleList(bankDealDetailRuleResults);
                    }
                    DealDetailRuleExecRecord ruleExecRecord = bankDealDetailWrapper.getDealDetailRuleExecRecord();
                    if (null == ruleExecRecord) {
                        ruleExecRecord = new DealDetailRuleExecRecord();
                        ruleExecRecord.setTraceid(context.getTraceId());
                        ruleExecRecord.setRequestseqno(context.getRequestSeqNo());
                        ruleExecRecord.setOsdid(bankReconciliation.get(DealDetailEnumConst.ODSID) + "");
                        bankDealDetailWrapper.setDealDetailRuleExecRecord(ruleExecRecord);
                        List<BankreconciliationIdentifyType> bankreconciliationIdentifyTypes = context.getBankDealDetailIdentifyMatchRules();
                        if (!CollectionUtils.isEmpty(bankreconciliationIdentifyTypes)) {
                            List<String> ruleCodes = bankreconciliationIdentifyTypes.stream().map(BankreconciliationIdentifyType::getCode).collect(Collectors.toList());
                            ruleExecRecord.setFullrules(ruleCodes.toString());
                        }
                        bankDealDetailWrapper.setDealDetailRuleExecRecord(ruleExecRecord);
                    }
                    BankreconciliationIdentifyType bankreconciliationIdentifyType = context.getCurrentRule();
                    if (bankreconciliationIdentifyType != null) {
                        BankDealDetailRuleResult bankDealDetailRuleResult = new BankDealDetailRuleResult();
                        bankDealDetailRuleResult.setBusinessCode(ruleDesc);
                        bankDealDetailRuleResult.setRuleName(bankreconciliationIdentifyType.getCode());
                        bankDealDetailRuleResult.setRuleorder(bankreconciliationIdentifyType.getExcuteorder());
                        bankDealDetailRuleResult.setUsedTime(context.getCurrentRuleUsedTime());
                        if (isErrorRule) {
                            bankDealDetailRuleResult.setFailrule(context.getCurrentRule().getCode());
                            bankDealDetailRuleResult.setBusinessCode(StringUtils.isEmpty(ruleDesc) ? e.getMessage() : ruleDesc);
                        }
                        bankDealDetailRuleResult.setExecuteStatus(executeStatus);
                        bankDealDetailRuleResults.add(bankDealDetailRuleResult);
                    }
                }
            }
        }
        return;
    }

    /**
     * 流水执行完当前规则，校验规则返回map集合流水数量是否正确
     */
    private List<BankReconciliation> checkReturnCountAndGetNeedHandleBankReconciliationList(BankDealDetailContext context, Map<String, List<BankReconciliation>> resultMap, BankreconciliationIdentifyType rule) {
        List<BankReconciliation> needTODBList = new ArrayList<>();
        Integer returnTotal = this.countNeedToDBDealDetail(needTODBList, resultMap,context);
        //校验，执行规则前传入的流水数量和返回的流水数量是否一致
        if (null != context.getBankReconciliationCountInCurrentRule() && !context.getBankReconciliationCountInCurrentRule().equals(returnTotal)) {
            log.error("当前规则{}执行完，返回流水数量与传入流水数量不一致,返回数量:{}传入数量:{}", RuleCodeConst.getRuleName(rule.getCode()), returnTotal, context.getWrappers().size());
            throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_BUSINESSRETURN_COUNT_ERROR.getErrCode(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540037F", "当前规则") /* "当前规则" */ + rule.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400380", "执行完，返回流水数量与传入流水数量不一致,返回数量:") /* "执行完，返回流水数量与传入流水数量不一致,返回数量:" */ + returnTotal + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540037E", "传入数量:") /* "传入数量:" */ + context.getWrappers().size());
        }
        return needTODBList;
    }

    public Integer countNeedToDBDealDetail(List<BankReconciliation> needTODBList, Map<String, List<BankReconciliation>> resultMap,BankDealDetailContext context) {
        if (null == resultMap || resultMap.size() == 0) {
            return null;
        }
        Integer returnTotal = 0;
        if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect()) || DealDetailEnumConst.SAVE_DIRECT_FINISH.equals(context.getSaveDirect())){
            List<BankReconciliation> nextList = resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus());
            List<BankReconciliation> processList = resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus());
            if (!CollectionUtils.isEmpty(processList)) {
                needTODBList.addAll(processList);
                returnTotal += processList.size();
            }
            if (!CollectionUtils.isEmpty(nextList)) {
                returnTotal += nextList.size();
            }
            return returnTotal;
        }
        //入库+执行具体流程
        List<BankReconciliation> processList = resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus());
        //人工介入;入库+终止
        List<BankReconciliation> pendingList = resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus());
        //异常;入库+终止
        List<BankReconciliation> errorList = resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus());
        List<BankReconciliation> nextList = resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus());

        if (!CollectionUtils.isEmpty(errorList)) {
            returnTotal += errorList.size();
            needTODBList.addAll(errorList);
        }
        if (!CollectionUtils.isEmpty(pendingList)) {
            returnTotal += pendingList.size();
            needTODBList.addAll(pendingList);
        }
        if (!CollectionUtils.isEmpty(processList)) {
            returnTotal += processList.size();
            needTODBList.addAll(processList);
        }
        if (!CollectionUtils.isEmpty(nextList)) {
            returnTotal += nextList.size();
        }
        return returnTotal;
    }

    private void invokeProcess(BankDealDetailContext context, Map<String, List<BankReconciliation>> resultMap, BankreconciliationIdentifyType rule) {
        List<BankReconciliation> processList = resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus());
        List<BankDealDetailWrapper> bankDealDetailWrappers = this.getExecutorProcessDealDetail(context, processList);
        if (!CollectionUtils.isEmpty(bankDealDetailWrappers)) {
            String ruleCode = rule != null ? rule.getCode() : null;
            callDealDetailProcess(ruleCode, bankDealDetailWrappers, context.getTraceId(), context.getRequestSeqNo());
        }
    }

    public static void callDealDetailProcess(String ruleCode, List<BankDealDetailWrapper> bankDealDetailWrappers) {
        callDealDetailProcess(ruleCode, bankDealDetailWrappers, null, null);
    }

    public static void callDealDetailProcess(String ruleCode, List<BankDealDetailWrapper> bankDealDetailWrappers, String traceId, String requestSeqNo) {
        Integer index = null;
        if (StringUtils.isEmpty(ruleCode) || RuleCodeConst.SYSTEM005.equals(ruleCode)) {
            //流程处理从单据关联开始执行
            index = 1;
        } else if (RuleCodeConst.SYSTEM007.equals(ruleCode)||RuleCodeConst.SYSTEM008.equals(ruleCode)) {
            //流程处理从生单流程开始执行，依次执行生单、挂账生单、发布认领 ; SYSTEM008 发布辨识+已发布 = 阻断性规则
            index = 3;
        }
        else {
            log.error("规则{}不是阻断性规则,不能跳转到流程处理", ruleCode);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400382", "规则") /* "规则" */ + ruleCode + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400383", "不是阻断性规则,不能跳转到流程处理") /* "不是阻断性规则,不能跳转到流程处理" */);
        }
        if (index != null) {
            log.error(RuleCodeConst.getRuleName(ruleCode) + "为阻断性规则，开始执行流水对应的流程处理");
            //去掉processstatus=25的已执行关联或生单的流水
            List<BankDealDetailWrapper> bankDealDetailWrappersRemoveFinish = bankDealDetailWrappers.stream().filter(wrapper -> {
                if (DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus().shortValue() == wrapper.getBankReconciliation().getProcessstatus()) {
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
            callDealDetailProcess(index, bankDealDetailWrappersRemoveFinish, traceId, requestSeqNo);
        }
    }

    public static void callDealDetailProcess(Integer index, List<BankDealDetailWrapper> bankDealDetailWrappers) {
        callDealDetailProcess(index, bankDealDetailWrappers, null, null);
    }

    public static void callDealDetailProcess(Integer index, List<BankDealDetailWrapper> bankDealDetailWrappers, String traceId, String requestSeqNo) {
        if (index != null) {
            IDealDetailProcessing bankDealDetailProcess = AppContext.getBean(IDealDetailProcessing.class);
            if (!CollectionUtils.isEmpty(bankDealDetailWrappers)) {
                //走了深copy,移除executeStatus不影响context中流水的executeStatus
                for (BankDealDetailWrapper wrapper : bankDealDetailWrappers) {
                    DealDetailUtils.removeExecuteStatusFromBankReconciliation(wrapper.getBankReconciliation());
                }
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == TransactionSynchronization.STATUS_COMMITTED) {
                                log.error("流水辨识匹配规则事务提交成功");
                                CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
                                ctmThreadPoolExecutor.getThreadPoolExecutor().submit(()->{
                                    //流水处理事务提交成功，执行流水对应的流程处理
                                    bankDealDetailProcess.dealDetailProcesing(bankDealDetailWrappers, null, BankDealDetailProcessChainImpl.get().code(null), index, traceId, requestSeqNo);
                                });
                            }
                        }
                    });
                } else {
                    bankDealDetailProcess.dealDetailProcesing(bankDealDetailWrappers, null, BankDealDetailProcessChainImpl.get().code(null), index, traceId, requestSeqNo);
                }
            }
        }
    }

    /**
     * 当执行具体规则异常时，处理器捕获了异常，将该批流水标记为处理失败
     */
    private Map<String, List<BankReconciliation>> processWhenError(BankDealDetailContext context, Exception e) {
        List<BankReconciliation> bankReconciliations = this.getBankReconciliationList(context);

        if (!CollectionUtils.isEmpty(bankReconciliations)) {
            String businessCode = null;
            if (e instanceof BankDealDetailException) {
                BankDealDetailException exception = (BankDealDetailException) e;
                DealDetailBusinessCodeEnum businessCodeEnum = exception.getDealDetailBusinessCodeEnum();
                if (null != businessCodeEnum) {
                    businessCode = businessCodeEnum.getCode();
                }
            }
            for (BankReconciliation bankReconciliation : bankReconciliations) {
                if (!StringUtils.isEmpty(businessCode)) {
                    DealDetailUtils.appendBusiCode(businessCode, bankReconciliation);
                } else {
                    bankReconciliation.put(DealDetailEnumConst.EXECUTERESULTDESC, e.getMessage());
                }
            }
            return ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus(), bankReconciliations);
        }
        return null;
    }
}
