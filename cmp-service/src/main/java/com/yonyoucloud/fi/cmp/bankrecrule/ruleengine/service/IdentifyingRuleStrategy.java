package com.yonyoucloud.fi.cmp.bankrecrule.ruleengine.service;

import com.yonyou.iuap.ruleengine.dto.relevant.RuleExtParamDto;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleBusiLog;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleInfo;
import com.yonyoucloud.fi.cmp.bankrecrule.ruleengine.imp.RuleStrategy;
import com.yonyoucloud.fi.cmp.constant.IParamConstant;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * desc:辨识规则
 * author:wangqiangac
 * date:2023/5/25 11:11
 */
@Component
public class IdentifyingRuleStrategy extends RuleStrategy {
    /**
     * 辨识执行规则
     *
     * @param list
     * @throws Exception
     */
    @Override
    public void executeRule(List<BankReconciliation> list , String rulePrefix) throws Exception {
        Map<Integer, TargetRuleInfoDto> executeRuleMap = super.loadRule(rulePrefix);
        Map<Long, Map<String, Object>> sourcesMap = super.querySourcesValue(list, executeRuleMap);//提前查询所有对账单的source对应的值
        for (BankReconciliation bankReconciliation : list) {
            //业务日志初始化及赋值
            CmpRuleBusiLog cmpRuleBusiLog = new CmpRuleBusiLog();
            cmpRuleBusiLog.setId(bankReconciliation.get(CmpRuleBusiLog.idField).toString());
            Map<String, Object> sourceReconciliation = sourcesMap.get(bankReconciliation.getId());//获取根据相关性规则获取到的银行对账单，包含规则source的value
            cmpRuleBusiLog.setCode((String) sourceReconciliation.get(CmpRuleBusiLog.codeField));
            cmpRuleBusiLog.setName(bankReconciliation.get(CmpRuleBusiLog.nameField));
            List<CmpRuleInfo> ruleInfos = cmpRuleBusiLog.getRuleInfos();
            boolean hitOrNot = false;
            for (Map.Entry<Integer, TargetRuleInfoDto> entry : executeRuleMap.entrySet()) {
                TargetRuleInfoDto ruleInfoDto = entry.getValue();
                RuleExtParamDto ruleExtParamDto = new RuleExtParamDto();// 执行规则参数
                ruleExtParamDto.setRuleId(ruleInfoDto.getId());
                List<RuleItemDto> sources = ruleInfoDto.getSources();
//                    List<RuleItemDto> targets = ruleInfoDto.getTargets();
                super.dealSourcesValue(sourcesMap, bankReconciliation, sources);
                ruleExtParamDto.setSources(sources);
                Map<String, Object> assign = relevantRuleExecService.assign(ruleExtParamDto);

                CmpRuleInfo cmpRuleInfo = new CmpRuleInfo();
                cmpRuleInfo.setRuleCode(ruleInfoDto.getCode());
                cmpRuleInfo.setRuleType(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400385", "规则配置:") /* "规则配置:" */+ruleInfoDto.getId());
                HashMap<String, Object> ruleSources = cmpRuleInfo.getSources();
                sources.stream().forEach(e -> {
                    ruleSources.put(e.getCode(), e.getValue());
                });
                HashMap<String, Object> ruleTargets = cmpRuleInfo.getTargets();
                ruleInfos.add(cmpRuleInfo);
                if (assign != null) {
                    for (Map.Entry<String, Object> assignEntry : assign.entrySet()) {
                        String[] assignsKey = assignEntry.getKey().split("\\.");
                        String key = assignsKey[assignsKey.length - 1];
                        if(assignEntry.getKey().contains("characterDef")){//处理特征
                            this.checkCharacterDef(bankReconciliation,key, assignEntry.getValue());
                        }
                        bankReconciliation.set(key, assignEntry.getValue());
                        ruleTargets.put(key, assignEntry.getValue());
                        bankReconciliation.set("rulemark","Hit");
                        hitOrNot = true;
                    }
                } else {
                    if (hitOrNot){
                        break;
                    }
                    bankReconciliation.set("rulemark","notHit");
                    ruleTargets.put("result", "notHit");
                }
            }
            //super.updateBankReconciliation(bankReconciliation);
            if(AppContext.getEnvConfig(IParamConstant.BUSINESS_LOG_SWITCH,"1").equals("1")){
                super.sendBusinessLog(cmpRuleBusiLog);
            }
        }
    }
}
