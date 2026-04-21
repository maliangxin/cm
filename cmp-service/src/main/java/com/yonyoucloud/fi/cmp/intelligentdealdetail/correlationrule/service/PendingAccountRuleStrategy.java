package com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.service;

import com.yonyou.iuap.ruleengine.dto.relevant.RuleExtParamDto;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationDetail;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleBusiLog;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleInfo;
import com.yonyoucloud.fi.cmp.cmpentity.OprType;
import com.yonyoucloud.fi.cmp.cmpentity.PublishStatus;
import com.yonyoucloud.fi.cmp.enums.PublishedType;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.impl.CommonRuleStrategy;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleCheckLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleModuleLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.SendBizMessageUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

/**
 * 自动发布 相关性规则辨识*
 *
 * @author xuxbo
 * @date 2024/6/27 20:46
 */
@Slf4j
@Component
public class PendingAccountRuleStrategy extends CommonRuleStrategy {

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BankIdentifyService bankIdentifyService;

    @Override
    public void executeRule(List<BankReconciliation> list, String ruleType, String ruleCode, String ruleId, BankDealDetailContext context, Boolean continueOrNot) throws Exception {
//        Map<Integer, TargetRuleInfoDto> executeRuleMap = super.loadRule(ruleType);
        Map<Integer, TargetRuleInfoDto> executeRuleMap = bankIdentifyService.loadRuleBySettingId(ruleId);

        if (ObjectUtils.isEmpty(executeRuleMap)) {
            log.error("挂账辨识规则中,配置的相关性规则查询为空！！{}");
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM008_08YO3.getCode(),list.get(0),ruleCode);
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
            RuleExtParamDto ruleExtParamDto = new RuleExtParamDto();// 执行规则参数
            ruleExtParamDto.setRuleId(ruleInfoDto.getId());
            List<RuleItemDto> sources = ruleInfoDto.getSources();
            super.dealSourcesValue(sourcesMap, bankReconciliation, sources);
            ruleExtParamDto.setSources(sources);
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
                hitFlag = true;
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog,context.getResultSuccessLog()+ruleInfoDto.getCode(),null);
                break;
            } else {
                ruleTargets.put("result", "notHit");
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog,context.getResultFailLog()+ruleInfoDto.getCode(),null);
            }

//            if (!ruleTargets.get("result").equals("notHit")) {
//                hitFlag = true;
//            } else {
//                hitFlag = false;
//            }
        }
        // 给郭扬处理返回值
        if (!hitFlag) {
            bankReconciliation.set("executeStatusEnum","3");
            bankReconciliation.set("pendingAccountBreak","0");
            bankReconciliation.set("pendingAccountSuccess","0");
            log.error("挂账辨识规则中,配置的相关性规则未匹配成功！");
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM009_09YO2.getCode(),bankReconciliation,ruleCode);
        } else {
            bankReconciliation.set("executeStatusEnum","3");
            bankReconciliation.set("pendingAccountBreak","1");
            bankReconciliation.set("pendingAccountSuccess","1");
            //重新对 自动提交 生单类型 赋值
            Short isautosubmit = bankReconciliation.getIsautosubmit();
            bankReconciliation.setIsautosubmit(isautosubmit);
            Short genertbilltype = bankReconciliation.getGenertbilltype();
            bankReconciliation.setGenertbilltype(genertbilltype);
        }
    }

}
