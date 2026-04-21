package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog;


import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.cloud.utils.StringUtils;
import com.yonyou.iuap.log.model.BusinessArrayObject;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.iuap.log.util.BusiObjectBuildUtil;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleBusiLog;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleInfo;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.process.BankDealDetailProcessChainImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;


@Slf4j
@Component
public class CmpCheckAndProcessRuleLogProcessor {


    /**
     * 创建流程日志实体类，并将其添加到上下文中，然后将引用返回，方便后续对其做相关的业务操作操作。
     * CmpRuleCheckLog 一个银行流水只有一个对象，其多条日志则使用CmpRuleModuleLog对象承接
     * @param bankReconciliation
     * @param context
     * @param logName 属于那个规则辨识
     * @return
     */
    public static CmpRuleCheckLog buildCmpRuleBusiLog(BankReconciliation bankReconciliation,BankDealDetailContext context, String logName) {
        if (context == null){
            return new CmpRuleCheckLog();
        }
        //一个银行流水对应的一个CmpRuleCheckLog
        Map<String,CmpRuleCheckLog> cmpOnlyBankRuleCheckLogs = context.getCmpOnlyBankRuleCheckLogs();
        //当前银行流水没有执行过
        if (cmpOnlyBankRuleCheckLogs.get(bankReconciliation.getBank_seq_no()) == null){
            //当前银行流水没有执行过
            CmpRuleCheckLog cmpRuleBusiLog = new CmpRuleCheckLog();
            cmpRuleBusiLog.setId(bankReconciliation.get(CmpRuleBusiLog.idField).toString());
            cmpRuleBusiLog.setCode(bankReconciliation.getBank_seq_no());
            cmpRuleBusiLog.setName(bankReconciliation.get(CmpRuleBusiLog.nameField));
            cmpRuleBusiLog.setLogName(RuleLogEnum.RuleLogProcess.BANK_RECEIPT_LOG_NAME.getDesc());
            cmpOnlyBankRuleCheckLogs.put(bankReconciliation.getBank_seq_no(),cmpRuleBusiLog);
            return cmpRuleBusiLog;
        }else {
            //当前银行流水有执行过
            CmpRuleCheckLog cmpRuleCheckLog = cmpOnlyBankRuleCheckLogs.get(bankReconciliation.getBank_seq_no());
            //判断当前的模块是否执行过
            if (cmpRuleCheckLog != null){
                //执行过则不再重新创建日志对象
                return cmpRuleCheckLog;
            }else {
                CmpRuleCheckLog cmpRuleBusiLog = new CmpRuleCheckLog();
                cmpRuleBusiLog.setId(bankReconciliation.get(CmpRuleBusiLog.idField).toString());
                cmpRuleBusiLog.setCode(bankReconciliation.getBank_seq_no());
                cmpRuleBusiLog.setName(bankReconciliation.get(CmpRuleBusiLog.nameField));
                cmpRuleBusiLog.setLogName(RuleLogEnum.RuleLogProcess.BANK_RECEIPT_LOG_NAME.getDesc());
                //一个辨识模块只有一个
                return cmpRuleCheckLog;
            }
        }
    }

    /**
     * 承接个规则的多条日志，包括规则便是日志以及非规则日志
     * 其中辨识时，必须ruleInfoDto不能为null，sources不能为null==>CmpRuleCheckLog{moduleName{CmpRuleModuleLog{}}}
     * result_log进行承接{CmpRuleCheckLog：result_log ：{moduleName：{CmpRuleModuleLog+code:{}}}}     {moduleName：{}，moduleName：{}}
     * @param cmpRuleCheckLog
     * @param ruleInfoDto
     * @param sources
     * @param moduleName
     *
     * @return
     */
    public static CmpRuleModuleLog  buildCmpRuleInfoAndReturnRuleTargets(CmpRuleCheckLog cmpRuleCheckLog, TargetRuleInfoDto ruleInfoDto,List<RuleItemDto> sources,String moduleName,String ruleCode){
        Map<String, List<Map<String,CmpRuleModuleLog>>> moduleNameRuleInfoMap = cmpRuleCheckLog.getModuleName_rule_infos();
        List<Map<String,CmpRuleModuleLog>> cmpRuleModuleLogs = moduleNameRuleInfoMap.get(moduleName);
        if (cmpRuleModuleLogs == null){
            cmpRuleModuleLogs = new ArrayList<>();
        }
        Map<String,CmpRuleModuleLog> moduleNameRuleInfo = new HashMap<>();
        CmpRuleModuleLog cmpRuleModuleLog = new CmpRuleModuleLog();
        cmpRuleModuleLog.setModuleName(moduleName);
        CmpRuleInfo cmpRuleInfo = cmpRuleModuleLog.getModuleName_rule_info();
        if (ruleInfoDto != null && ruleCode != null){
            cmpRuleInfo.setRuleCode(ruleInfoDto.getCode());
            HashMap<String, Object> ruleSources = cmpRuleInfo.getSources();
            sources.stream().forEach(e -> {
                ruleSources.put(e.getCode(), e.getValue());
            });
            moduleNameRuleInfo.put(RuleLogEnum.RuleLogProcess.BANK_RECEIPT_MODULE.getDesc()+"-("+ruleCode+"——"+ruleInfoDto.getCode()+")",cmpRuleModuleLog);//@notranslate
        }else {
            moduleNameRuleInfo.put(RuleLogEnum.RuleLogProcess.BANK_RECEIPT_MODULE.getDesc()+"-(00000000——00000000)",cmpRuleModuleLog);//@notranslate
        }
        cmpRuleModuleLogs.add(moduleNameRuleInfo);
        //此处不使用module，使用规则名称，因为一个规则可能会执行多次
        moduleNameRuleInfoMap.put(moduleName, cmpRuleModuleLogs);
        return cmpRuleModuleLog;
    }

    /**
     * 承接个规则的多条日志，包括规则便是日志以及非规则日志
     * 其中辨识时，必须ruleInfoDto不能为null，sources不能为null==>CmpRuleCheckLog{moduleName{CmpRuleModuleLog{}}}
     * result_log进行承接{CmpRuleCheckLog：result_log ：{moduleName：{CmpRuleModuleLog+code:{}}}}     {moduleName：{}，moduleName：{}}
     * @param cmpRuleCheckLog
     * @param moduleName
     *
     * @return
     */
    public static CmpRuleModuleLog  buildCmpRuleInfoAndReturnRuleForPayTargets(CmpRuleCheckLog cmpRuleCheckLog,String moduleName,CmpRuleInfo cmpRuleInfo){
        Map<String, List<Map<String,CmpRuleModuleLog>>> moduleNameRuleInfoMap = cmpRuleCheckLog.getModuleName_rule_infos();
        List<Map<String,CmpRuleModuleLog>> cmpRuleModuleLogs = moduleNameRuleInfoMap.get(moduleName);
        if (cmpRuleModuleLogs == null){
            cmpRuleModuleLogs = new ArrayList<>();
        }
        Map<String,CmpRuleModuleLog> moduleNameRuleInfo = new HashMap<>();
        CmpRuleModuleLog cmpRuleModuleLog = new CmpRuleModuleLog();
        cmpRuleModuleLog.setModuleName(moduleName);
        if (cmpRuleInfo != null){
            cmpRuleModuleLog.setModuleName_rule_info(cmpRuleInfo);
            moduleNameRuleInfo.put(RuleLogEnum.RuleLogProcess.BANK_RECEIPT_MODULE.getDesc()+"-("+cmpRuleInfo.getRuleCode()+"——"+cmpRuleInfo.getRuleCode()+")",cmpRuleModuleLog);//@notranslate
        }else {
            moduleNameRuleInfo.put(RuleLogEnum.RuleLogProcess.BANK_RECEIPT_MODULE.getDesc()+"-(00000000——00000000)",cmpRuleModuleLog);//@notranslate
        }
            cmpRuleModuleLogs.add(moduleNameRuleInfo);
        //此处不使用module，使用规则名称，因为一个规则可能会执行多次
        moduleNameRuleInfoMap.put(moduleName, cmpRuleModuleLogs);
        return cmpRuleModuleLog;
    }

    /**
     *
     * @param cmpRuleModuleLog
     * @param logRuleName 规则日志操作，承接的是rule_steps字段所执行的日志，即同一个规则下打印多条日志
     * @param logModuleStep 非规则内的日志，若不新建，则回取最后一次
     */
    public static void executeRuleStepLog(CmpRuleModuleLog cmpRuleModuleLog,String logRuleName,String logModuleStep)  {
        if (StringUtils.isNotBlank(logRuleName)){
            Map<Integer, String> moduleNameRuleSteps = cmpRuleModuleLog.getModuleName_rule_steps();
            int size = moduleNameRuleSteps.size();
            moduleNameRuleSteps.put(++size,  logRuleName);
        }
        if (StringUtils.isNotBlank(logModuleStep)){
            cmpRuleModuleLog.setModuleName_steps(logModuleStep);
        }
    }


    /**
     * 没有规则的打印日志
     * @param bankReconciliation
     * @param moduleName
     * @param logModuleStep
     * @param context
     */
    public static void executeNoRuleLog(BankReconciliation bankReconciliation,String moduleName, String logModuleStep, BankDealDetailContext context){
        if (context == null){
            return;
        }
        CmpRuleCheckLog cmpRuleCheckLog = buildCmpRuleBusiLog(bankReconciliation, context,context.getLogName());
        CmpRuleModuleLog cmpRuleModuleLog = buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, null, null, moduleName,null);
        executeRuleStepLog(cmpRuleModuleLog,null,logModuleStep);
    }


    public static void executeNoRuleLogForList(List<BankReconciliation> bankReconciliationList,String moduleName, String logModuleStep, BankDealDetailContext context){
        if (context == null){
            return;
        }
        if (CollectionUtils.isNotEmpty(bankReconciliationList)){
            bankReconciliationList.stream().forEach(bankReconciliation -> {
                CmpRuleCheckLog cmpRuleCheckLog = buildCmpRuleBusiLog(bankReconciliation, context,context.getLogName());
                CmpRuleModuleLog cmpRuleModuleLog = buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, null, null, moduleName,null);
                executeRuleStepLog(cmpRuleModuleLog,null,logModuleStep);
            });
        }
    }

    private static void sendBusinessLog(List<CmpRuleCheckLog> cmpRuleCheckLogList, IBusinessLogService businessLogService,String logName, String operation) {
        String serviceCode = IServicecodeConstant.CMPBANKRECONCILIATION;
        String typeCode = "";
        String typeName = logName;
        // 操作分类
        String operationName = operation;
        List<Object> listRuleBusinessLog = new ArrayList<>();
        listRuleBusinessLog.addAll(cmpRuleCheckLogList);
        // 构建业务日志实体
        BusinessArrayObject businessArrayObject = BusiObjectBuildUtil.buildArrayObjectByField(CmpRuleCheckLog.idField, CmpRuleCheckLog.codeField,
                CmpRuleCheckLog.nameField, serviceCode, null, typeCode, typeName, listRuleBusinessLog);
        businessArrayObject.setOperationName(operationName);
        // 保存业务日志
        businessLogService.saveBusinessLog(businessArrayObject);
    }

    public static void executeRuleLogLast(BankDealDetailContext context, CTMCMPBusinessLogService ctmcmpBusinessLogService,IBusinessLogService businessLogService)  {
        if (context == null){
            return;
        }
        //最后处理业务日志
        try {
            Map<String,CmpRuleCheckLog> cmpOnlyBankRuleCheckLogs = context.getCmpOnlyBankRuleCheckLogs();
            if (cmpOnlyBankRuleCheckLogs != null && cmpOnlyBankRuleCheckLogs.size()>0) {
                cmpOnlyBankRuleCheckLogs.entrySet().stream().forEach(mapEntry -> {
                List<CmpRuleCheckLog> cmpRuleCheckLogsList = new ArrayList<>();
                    CmpRuleCheckLog cmpRuleCheckLog = mapEntry.getValue();
                    cmpRuleCheckLogsList.add(cmpRuleCheckLog);
                    if (cmpRuleCheckLog != null) {
                        if (context.getBankDealDetailChain() != null && context.getBankDealDetailChain() instanceof BankDealDetailProcessChainImpl) {
                            sendBusinessLog(cmpRuleCheckLogsList, businessLogService, RuleLogEnum.RuleLogProcess.BANK_RECEIPT_LOG_NAME.getDesc(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400769", "流水自动辨识流程处理") /* "流水自动辨识流程处理" */);
                        } else {
                            sendBusinessLog(cmpRuleCheckLogsList, businessLogService, RuleLogEnum.RuleLogProcess.BANK_RECEIPT_LOG_NAME.getDesc(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540076A", "流水自动辨识规则") /* "流水自动辨识规则" */);
                        }
                    }
                    //发送后移除
                    context.setLogName("");
                    context.setOperationName("");
                    context.setCmpOnlyBankRuleCheckLogs(new HashMap<>());
                    context.setBankDealDetailChain(null);
                    context.setResultSuccessLog(null);
                    context.setResultFailLog(null);
                });
            }
        }catch (Exception e){
            log.error("保存业务日志异常",e);
        }
    }

}
