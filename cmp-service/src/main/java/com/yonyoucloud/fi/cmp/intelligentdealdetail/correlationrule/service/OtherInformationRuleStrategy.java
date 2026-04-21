package com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.service;

import com.yonyou.iuap.ruleengine.dto.relevant.RuleExtParamDto;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.iuap.ruleengine.enums.RelevantDataTypeEnum;
import com.yonyou.iuap.ruleengine.enums.RelevantRuleFormEnum;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleBusiLog;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleInfo;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.impl.CommonRuleStrategy;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleCheckLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleModuleLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 生单 相关性规则辨识*
 *
 * @author xuxbo
 * @date 2024/6/27 20:46
 */
@Slf4j
@Component
public class OtherInformationRuleStrategy extends CommonRuleStrategy {

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BankIdentifyService bankIdentifyService;

    @Override
    public void executeRule(List<BankReconciliation> list, String ruleType, String ruleCode, String ruleId, BankDealDetailContext context,Boolean continueOrNot) throws Exception {
//        Map<Integer, TargetRuleInfoDto> executeRuleMap = super.loadRule(ruleType);
        Map<Integer, TargetRuleInfoDto> executeRuleMap = bankIdentifyService.loadRuleBySettingId(ruleId);
        if (ObjectUtils.isEmpty(executeRuleMap)) {
            log.error("其他信息辨识规则中,配置的相关性规则查询为空！！");
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM010_10YO3.getCode(), list.get(0), ruleCode);
            return;
        }
        Map<Long, Map<String, Object>> sourcesMap = super.querySourcesValue(list, executeRuleMap);//提前查询所有对账单的source对应的值
        BankReconciliation bankReconciliation = list.get(0);
        //业务日志初始化及赋值
        CmpRuleCheckLog cmpRuleCheckLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleBusiLog(bankReconciliation, context, context.getLogName());
        // 命中标识
        boolean hitFlag = false;
        // 获取并排序 keys
        List<Integer> sortedKeys = new ArrayList<>(executeRuleMap.keySet());
        Collections.sort(sortedKeys);  // 默认升序
        for (Integer orderKey : sortedKeys) {
            TargetRuleInfoDto ruleInfoDto = executeRuleMap.get(orderKey);


            List<RuleItemDto> targets = ruleInfoDto.getTargets();
            RelevantRuleFormEnum ruleForm = ruleInfoDto.getRuleForm();
            if (ruleForm == RelevantRuleFormEnum.SCRIPT){
                //单独执行脚本
                RuleExtParamDto ruleExtParamDto = new RuleExtParamDto();// 执行规则参数
                ruleExtParamDto.setRuleId(ruleInfoDto.getId());
                List<RuleItemDto> sources = ruleInfoDto.getSources();
                super.dealSourcesValue(sourcesMap, bankReconciliation, sources);
                ruleExtParamDto.setSources(sources);
                //业务日志初始化及赋值
                CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(),ruleCode);
                HashMap<String, Object> ruleTargets = cmpRuleModuleLog.getModuleName_rule_info().getTargets();
                for (RuleItemDto target : targets) {
                    ruleExtParamDto.setTarget(target);
                    Object assign = relevantRuleExtService.assign(ruleExtParamDto);
                    if (assign != null){
                        Map<String, Object> map_result = sourcesMap.get(bankReconciliation.getId());
                        String[] assignsKey = target.getCode().split("\\.");
                        String key = assignsKey[assignsKey.length - 1];
                        if (target.getCode().contains("characterDef")) {//处理特征
                            //特质预置数据
                            map_result.put("characterDef."+key,assign);
                            map_result.put("characterDef_"+key,assign);
                            this.checkCharacterDef(bankReconciliation, key, assign);
                        }
                        //结果重填map集合，以防止数据变化还拿之前的规则
                        map_result.put(key,assign);
                        bankReconciliation.set(target.getCode(), assign);
                        ruleTargets.put(target.getCode(), assign);
                        ruleTargets.put("result", "Hit");
                    }else {
                        ruleTargets.put("result", "notHit");
                        CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog,RuleLogEnum.RuleLogProcess.OTHER_INFORMATION_TWO.getDesc()+ruleInfoDto.getCode(),null);
                    }
                }
                continue;
            }


            RuleExtParamDto ruleExtParamDto = new RuleExtParamDto();// 执行规则参数
            ruleExtParamDto.setRuleId(ruleInfoDto.getId());
            List<RuleItemDto> sources = ruleInfoDto.getSources();
            super.dealSourcesValue(sourcesMap, bankReconciliation, sources);
            ruleExtParamDto.setSources(sources);
            CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, RuleLogEnum.RuleLogProcess.OTHER_INFORMATION_NAME.getDesc(), ruleCode);
            HashMap<String, Object> ruleTargets = cmpRuleModuleLog.getModuleName_rule_info().getTargets();
            CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OTHER_INFORMATION_START.getDesc() + ruleInfoDto.getCode(), null);
            Map<String, Object> assign = relevantRuleExecService.assign(ruleExtParamDto);
            if (assign != null) {
                Map<String, Object> map_result = sourcesMap.get(bankReconciliation.getId());
                for (Map.Entry<String, Object> assignEntry : assign.entrySet()) {
                    String[] assignsKey = assignEntry.getKey().split("\\.");
                    String key = assignsKey[assignsKey.length - 1];
                    if (assignEntry.getKey().contains("characterDef")) {//处理特征
                        //特质预置数据
                        map_result.put("characterDef."+key,assignEntry.getValue());
                        map_result.put("characterDef_"+key,assignEntry.getValue());
                        this.checkCharacterDef(bankReconciliation, key, assignEntry.getValue());
                    }
                    map_result.put(key,assignEntry.getValue());
                    bankReconciliation.set(key, assignEntry.getValue());
                    //结果重填map集合，以防止数据变化还拿之前的规则
                    map_result.put(key,assignEntry.getValue());
                    ruleTargets.put(key, assignEntry.getValue());
                }
                ruleTargets.put("result", "Hit");
                hitFlag = true;
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OTHER_INFORMATION_ONE.getDesc() + ruleInfoDto.getCode(), null);
                if (continueOrNot != null  && continueOrNot){
                    continue;
                }
                break;
            } else {
                ruleTargets.put("result", "notHit");
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OTHER_INFORMATION_TWO.getDesc() + ruleInfoDto.getCode(), null);
            }

        }
        //没有匹配上相关性规则 或者 授权使用组织是否为空不进行发布  这两种情况是没有命中的情况 可以认为没有匹配上发布辨识规则
        if (!hitFlag) {
            bankReconciliation.set("OtherInformationbreak","0");
            log.error("其他信息辨识规则中,配置的相关性规则未匹配成功！");
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM010_10YO2.getCode(),bankReconciliation,ruleCode);
        } else {
            bankReconciliation.set("OtherInformationbreak","1");
        }
    }

}
