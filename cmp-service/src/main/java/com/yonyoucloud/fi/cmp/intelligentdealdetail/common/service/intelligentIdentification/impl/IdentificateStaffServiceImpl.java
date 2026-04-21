package com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.intelligentIdentification.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationPullService;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IntelligentIdentificationService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.intelligentIdentification.AbstractIntelligentIdentificationService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleCheckLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleModuleLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.BankMatchAndProcessUtils;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyBO;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;

@Service
public class IdentificateStaffServiceImpl extends AbstractIntelligentIdentificationService implements IntelligentIdentificationService {

    @Resource
    private BankreconciliationPullService bankreconciliationPullService;

    private static final Cache<String, BillSmartClassifyBO> periodCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(60))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();

    @Override
    public short getTypeValue() {
        return OppositeType.Employee.getValue();
    }

    @Override
    public List<BankReconciliation> excuteIdentificate(List<BankReconciliation> bankReconciliationList, BankDealDetailContext context) throws Exception {
        List<BankReconciliation> noMatchList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
//            if (bankReconciliation.getOppositetype() != null) {
//                continue;
//            }
            String accentity = bankReconciliation.getAccentity();
            String toaccountno = bankReconciliation.getTo_acct_no();
            String toaccountname = bankReconciliation.getTo_acct_name();
            String currency = bankReconciliation.getCurrency();
            short flag = bankReconciliation.getDc_flag().getValue();
            String key = String.format("%s_%s_%s_%s_%s", accentity, toaccountno, toaccountname, currency, flag);

            BillSmartClassifyBO classifyBO = null;
            context.setLogName(RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc());
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(), RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_STAFF_UNIT.getDesc(), context);
            BillSmartClassifyBO cacheClassifyBO = periodCache.getIfPresent(key);
            if (cacheClassifyBO != null) {
                classifyBO = cacheClassifyBO;
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(), RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_STAFF_RESULT.getDesc(), context);
            } else {
                Map<String, Object> employee = bankreconciliationPullService.getInnerEmployee(toaccountno, accentity);
                if (ValueUtils.isEmpty(employee)) {
                    employee = bankreconciliationPullService.getInnerEmployeeByName(toaccountname, accentity);
                }
                if (ValueUtils.isNotEmpty(employee)) {
                    classifyBO = new BillSmartClassifyBO(OppositeType.Employee.getValue(), employee.get("id") != null ? employee.get("id").toString() : null,
                            employee.get("name") != null ? employee.get("name").toString() : null, employee.get("staffBankAccount") != null ? employee.get("staffBankAccount").toString() : null);
                    //放入缓存
                    periodCache.put(key, classifyBO);
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(), RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_STAFF_RESULT.getDesc(), context);
                }
            }
            if (classifyBO == null) {
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(), RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_STAFF_RESULT_NO.getDesc(), context);
            }
            setOppoSiteType(bankReconciliation, classifyBO);
        }
        return noMatchList;
    }

    @Override
    public List<BankReconciliation> excuteIdentificateForCheck(List<BankReconciliation> bankReconciliationList, BankreconciliationIdentifySetting bankreconciliationIdentifySetting, BankIdentifyService bankIdentifyService, BankDealDetailContext context) throws Exception {
        List<BankReconciliation> noMatchList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            if (BankMatchAndProcessUtils.identifiedInformation.equals(bankReconciliation.getString(BankMatchAndProcessUtils.identifiedInformation))) {
                continue;
            }

            TargetRuleInfoDto ruleInfoDto = new TargetRuleInfoDto();
            List<RuleItemDto> sources = new ArrayList<>();
            ruleInfoDto.setSources(sources);
            ruleInfoDto.setCode(bankreconciliationIdentifySetting.getCode());
            CmpRuleCheckLog cmpRuleCheckLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleBusiLog(bankReconciliation,context,context.getLogName());
            CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(),bankreconciliationIdentifySetting.getCode());

            String accentity = bankReconciliation.getAccentity();
            String toaccountno = bankReconciliation.getTo_acct_no();
            String toaccountname = bankReconciliation.getTo_acct_name();
            String customername = bankReconciliation.getTo_acct_name();
            short flag = bankReconciliation.getDc_flag().getValue();
            String key = String.format("%s_%s_%s_%s", accentity, toaccountno, toaccountname, flag);

            BillSmartClassifyBO classifyBO = null;
            context.setLogName(RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc());
            //CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_STAFF_UNIT.getDesc(), RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_STAFF_UNIT.getDesc(), context);
            BillSmartClassifyBO cacheClassifyBO = periodCache.getIfPresent(key);
            if (cacheClassifyBO != null) {
                classifyBO = cacheClassifyBO;
                //CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_STAFF_RESULT.getDesc(),context);
                //CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(), bankreconciliationIdentifySetting.getCode());
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_STAFF_RESULT.getDesc() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400802", "缓存中获取配置") /* "缓存中获取配置" */, null);
            } else {
                toaccountno = null;
                toaccountname = null;
                customername = null;
                boolean employeenamemark = false;
                if (bankreconciliationIdentifySetting != null) {
                    Map<String, BankreconciliationIdentifySetting_b> bankreconciliationIdentifySettingBMap = bankIdentifyService.loadBankreconciliationIdentifySetting_bById(bankreconciliationIdentifySetting.getId().toString());
                    if (bankreconciliationIdentifySettingBMap != null) {
                        Set<String> strings = bankreconciliationIdentifySettingBMap.keySet();
                        for (String orderKey : strings) {
                            if ("to_acct_no".equals(orderKey)) {
                                toaccountno = bankReconciliation.getTo_acct_no();
                            }
                            if ("to_acct_name".equals(orderKey) && !"name".equals(bankreconciliationIdentifySettingBMap.get(orderKey).getMatchfield())) {
                                toaccountname = bankReconciliation.getTo_acct_name();
                            }
                            if ("to_acct_name".equals(orderKey) && "name".equals(bankreconciliationIdentifySettingBMap.get(orderKey).getMatchfield())) {
                                customername = bankReconciliation.getTo_acct_name();
                                employeenamemark = true;
                            }
                        }
                    }
                    if (StringUtils.isNotEmpty(toaccountno)) {
                        Map<String, Object> employee = bankreconciliationPullService.getInnerEmployeeForCheck(toaccountno, accentity);
                        classifyBO = createResult(bankReconciliation, employee, key, context);
                    }
                    if (StringUtils.isNotEmpty(toaccountname) && !employeenamemark) {
                        Map<String, Object> employee = bankreconciliationPullService.getInnerEmployeeByNameForCheck(toaccountname, accentity);
                        classifyBO = createResult(bankReconciliation, employee, key, context);
                    }
                    if (StringUtils.isNotEmpty(toaccountname) && employeenamemark) {
                        Map<String, Object> employee = bankreconciliationPullService.getInnerEmployeeByAccountNameForCheck(customername, accentity);
                        classifyBO = createResult(bankReconciliation, employee, key, context);
                    }

                }
            }
            //设置辨识匹配成功标识
            bankReconciliation.put(BankMatchAndProcessUtils.identifiedInformation,BankMatchAndProcessUtils.identifiedInformation);
            if (classifyBO == null) {
                //移除辨识匹配标识
                bankReconciliation.remove(BankMatchAndProcessUtils.identifiedInformation);
                noMatchList.add(bankReconciliation);
               // CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(), bankreconciliationIdentifySetting.getCode());
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_STAFF_RESULT_NO.getDesc() + bankreconciliationIdentifySetting.getCode(), null);
            }else {
               // CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(), bankreconciliationIdentifySetting.getCode());
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_STAFF_RESULT.getDesc() + bankreconciliationIdentifySetting.getCode(), null);
            }
            setOppoSiteType(bankReconciliation, classifyBO);
        }


        return noMatchList;
    }


    private BillSmartClassifyBO createResult(BankReconciliation bankReconciliation, Map<String, Object> employee, String key, BankDealDetailContext context) {
        BillSmartClassifyBO classifyBO = null;
        if (ValueUtils.isNotEmpty(employee)) {
            classifyBO = new BillSmartClassifyBO(OppositeType.Employee.getValue(), employee.get("id") != null ? employee.get("id").toString() : null,
                    employee.get("name") != null ? employee.get("name").toString() : null, employee.get("staffBankAccount") != null ? employee.get("staffBankAccount").toString() : null);
            //放入缓存
            periodCache.put(key, classifyBO);
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(), RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_STAFF_RESULT.getDesc(), context);
        }
        return classifyBO;
    }

}
