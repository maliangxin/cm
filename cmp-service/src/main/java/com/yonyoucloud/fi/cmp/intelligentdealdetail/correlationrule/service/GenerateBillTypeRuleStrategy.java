package com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.service;

import com.yonyou.iuap.ruleengine.dto.relevant.RuleExtParamDto;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.iuap.ruleengine.enums.RelevantDataTypeEnum;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.impl.CommonRuleStrategy;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleCheckLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleModuleLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.OdsCommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
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
public class GenerateBillTypeRuleStrategy extends CommonRuleStrategy {

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BankIdentifyService bankIdentifyService;

    @Override
    public void executeRule(List<BankReconciliation> list, String ruleType, String ruleCode, String ruleId, BankDealDetailContext context, Boolean continueOrNot) throws Exception {
//        Map<Integer, TargetRuleInfoDto> executeRuleMap = super.loadRule(ruleType);
        Map<Integer, TargetRuleInfoDto> executeRuleMap = bankIdentifyService.loadRuleBySettingId(ruleId);
        if (ObjectUtils.isEmpty(executeRuleMap)) {
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLogForList(list,context.getLogName(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400782", "生单类型辨识规则中,配置的相关性规则查询为空！！") /* "生单类型辨识规则中,配置的相关性规则查询为空！！" */,context);
            log.error("生单类型辨识规则中,配置的相关性规则查询为空！！");
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM007_07YO3.getCode(),list.get(0),ruleCode);
            return;
        }
        Map<Long, Map<String, Object>> sourcesMap = super.querySourcesValue(list, executeRuleMap);//提前查询所有对账单的source对应的值
        BankReconciliation bankReconciliation = list.get(0);
        CmpRuleCheckLog cmpRuleCheckLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleBusiLog(bankReconciliation, context, context.getLogName());
        // 命中标识
        boolean hitFlag = false;
        // 获取并排序 keys
        List<Integer> sortedKeys = new ArrayList<>(executeRuleMap.keySet());
        Collections.sort(sortedKeys);  // 默认升序
        for (Integer orderKey : sortedKeys) {
            TargetRuleInfoDto ruleInfoDto = executeRuleMap.get(orderKey);
            RuleExtParamDto ruleExtParamDto = new RuleExtParamDto();// 执行规则参数
            ruleExtParamDto.setRuleId(ruleInfoDto.getId());
            List<RuleItemDto> sources = ruleInfoDto.getSources();
            super.dealSourcesValue(sourcesMap, bankReconciliation, sources);
            ruleExtParamDto.setSources(sources);
            //业务日志初始化及赋值
            CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(),ruleCode);
            HashMap<String, Object> ruleTargets = cmpRuleModuleLog.getModuleName_rule_info().getTargets();
            CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog,context.getOperationName()+ruleInfoDto.getCode(),null);
            Map<String, Object> assign = relevantRuleExecService.assign(ruleExtParamDto);


            if (assign != null) {
                for (Map.Entry<String, Object> assignEntry : assign.entrySet()) {
                    String[] assignsKey = assignEntry.getKey().split("\\.");
                    String key = assignsKey[assignsKey.length - 1];
                    if (assignEntry.getKey().contains("characterDef")) {//处理特征
                        this.checkCharacterDef(bankReconciliation, key, assignEntry.getValue());
                    }
                    bankReconciliation.set(key, assignEntry.getValue());
                    ruleTargets.put(key, assignEntry.getValue());
                }
                ruleTargets.put("result", "Hit");
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog,context.getResultSuccessLog()+ruleInfoDto.getCode(),null);
                hitFlag = true;
                //生单已经匹配上了，后面则不需要执行发布
                break;
            } else {
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog,context.getResultFailLog()+ruleInfoDto.getCode(),null);
                ruleTargets.put("result", "notHit");
            }

        }
        //没有匹配上相关性规则
        if (!hitFlag) {
            bankReconciliation.set(OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK,OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK_VALUE_FALSE);
            log.error("生单类型辨识规则中,配置的相关性规则未匹配成功！");
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM007_07YO2.getCode(),bankReconciliation,ruleCode);
        } else {
            bankReconciliation.set(OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK,OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK_VALUE_TRUE);
        }
    }

}
