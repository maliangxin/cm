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
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;

@Service
public class IdentificateInnerUnitServiceImpl extends AbstractIntelligentIdentificationService implements IntelligentIdentificationService {

    @Resource
    private BankreconciliationPullService bankreconciliationPullService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    private static final Cache<String, BillSmartClassifyBO> periodCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(60))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();

    public short getTypeValue() {
        return OppositeType.InnerOrg.getValue();
    }

    @Override
    public List<BankReconciliation> excuteIdentificate(List<BankReconciliation> bankReconciliationList, BankDealDetailContext context) throws Exception {
        List<BankReconciliation> noMatchList = new ArrayList<>();
        //内部单位
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
            context.setLogName(RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc());
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(), RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_INNER_UNIT.getDesc(), context);
            BillSmartClassifyBO cacheClassifyBO = periodCache.getIfPresent(key);
            if (cacheClassifyBO == null) {
                Map<String, Object> bankAccount = bankreconciliationPullService.getBankAcctByAccount(toaccountno);
                if (ValueUtils.isEmpty(bankAccount)) {
                    bankAccount = bankreconciliationPullService.getBankAcctByAccountByName(toaccountname, currency);
                }
                if (ValueUtils.isNotEmpty(bankAccount)) {
                    cacheClassifyBO = new BillSmartClassifyBO(OppositeType.InnerOrg.getValue(), bankAccount.get("orgid").toString()
                            , bankAccount.get("orgname").toString(), bankAccount.get("bankacct") != null ? bankAccount.get("bankacct").toString() : null);
                    //放入缓存
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(), RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_INNER_RESULT.getDesc(), context);
                    periodCache.put(key, cacheClassifyBO);
                }
            } else {
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(), RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_INNER_RESULT.getDesc(), context);
            }
            if (cacheClassifyBO == null) {
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(), RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_INNER_RESULT_NO.getDesc(), context);
            }
            setOppoSiteType(bankReconciliation, cacheClassifyBO);
        }
        return noMatchList;
    }

    @Override
    public List<BankReconciliation> excuteIdentificateForCheck(List<BankReconciliation> bankReconciliationList, BankreconciliationIdentifySetting bankreconciliationIdentifySetting, BankIdentifyService bankIdentifyService, BankDealDetailContext context) throws Exception {
        List<BankReconciliation> noMatchList = new ArrayList<>();
        //内部单位
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
            String currency = bankReconciliation.getCurrency();
            short flag = bankReconciliation.getDc_flag().getValue();
            String key = String.format("%s_%s_%s_%s_%s", accentity, toaccountno, toaccountname, currency, flag);
            context.setLogName(RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc());
            //CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_INNER_UNIT.getDesc(), RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_INNER_UNIT.getDesc(), context);
            BillSmartClassifyBO cacheClassifyBO = periodCache.getIfPresent(key);
            if (cacheClassifyBO == null) {
                Map<String, BankreconciliationIdentifySetting_b> bankreconciliationIdentifySettingBMap = bankIdentifyService.loadBankreconciliationIdentifySetting_bById(bankreconciliationIdentifySetting.getId().toString());
                if (bankreconciliationIdentifySettingBMap != null) {
                    Set<String> strings = bankreconciliationIdentifySettingBMap.keySet();
                    for (String orderKey : strings) {
                        if ("to_acct_no".equals(orderKey)) {
                            toaccountno = bankReconciliation.getTo_acct_no();
                        }
                    }
                }
                Map<String, Object> bankAccount = bankreconciliationPullService.getBankAcctByAccount(toaccountno);
                if (ValueUtils.isNotEmpty(bankAccount)) {
                    cacheClassifyBO = new BillSmartClassifyBO(OppositeType.InnerOrg.getValue(), bankAccount.get("orgid").toString()
                            , bankAccount.get("orgname").toString(), bankAccount.get("bankacct") != null ? bankAccount.get("bankacct").toString() : null);
                    //放入缓存
                    //CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(), bankreconciliationIdentifySetting.getCode());
                    CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_INNER_RESULT.getDesc() + bankreconciliationIdentifySetting.getCode(), null);
                    periodCache.put(key, cacheClassifyBO);
                }
            } else {
               // CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(), bankreconciliationIdentifySetting.getCode());
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_INNER_RESULT.getDesc() +com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540040F", "缓存中获取配置") /* "缓存中获取配置" */, null);
            }
            //设置辨识匹配成功标识
            bankReconciliation.put(BankMatchAndProcessUtils.identifiedInformation,BankMatchAndProcessUtils.identifiedInformation);
            if (cacheClassifyBO == null) {
                //移除辨识匹配标识
                bankReconciliation.remove(BankMatchAndProcessUtils.identifiedInformation);
                noMatchList.add(bankReconciliation);
                //CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(), bankreconciliationIdentifySetting.getCode());
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_INNER_RESULT_NO.getDesc()+ bankreconciliationIdentifySetting.getCode(), null);
            }
            setOppoSiteType(bankReconciliation, cacheClassifyBO);
        }

        return noMatchList;
    }


}
