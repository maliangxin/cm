package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business;


import com.google.common.collect.ImmutableMap;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IBankReconciliationCommonService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.impl.CommonRuleStrategy;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.service.OtherInformationRuleStrategy;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultStreamBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author guoyangy
 * @Date 2024/6/18 15:33
 * @Description 其它匹配规则
 * @Version 1.0
 */
@Slf4j
@Service(RuleCodeConst.SYSTEM010)
public class OtherInformationMatchHandler extends DefaultStreamBatchHandler {

    @Autowired
    IBankReconciliationCommonService bankReconciliationCommonService;

    @Resource
    OtherInformationRuleStrategy otherInformationRuleStrategy;

    @Override
    public Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) {
        long s0 = System.currentTimeMillis();
        if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect())) {
            return ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), getBankReconciliationList(context));
        }
        Map<String, List<BankReconciliation>> result = new HashMap<String, List<BankReconciliation>>();
        try {
            List<BankReconciliation> data = this.getBankReconciliationList(context);
            //查询其他辨识规则 中启用的规则，获取到规则编码集合 调用小波提供的接口（小波）
            Map<String, String> ruleCodes = new HashMap<>();
            try {
                ruleCodes = bankReconciliationCommonService.getRuleCodes(RuleCodeConst.SYSTEM010);
            } catch (Exception e) {
                log.error("SYSTEM010：其他信息辨识规则设置查询异常", e);
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM010_10S02.getCode(), data);
            }
            if (ObjectUtils.isEmpty(ruleCodes) || ruleCodes.size() < 1) {
                result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), data);
                log.error("SYSTEM010：未查询到可用的其他信息辨识匹配规则");
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM010_10YO1.getCode(), data);
                return result;
            }
            context.setLogName(RuleLogEnum.RuleLogProcess.OTHER_INFORMATION_NAME.getDesc());
            context.setOperationName(RuleLogEnum.RuleLogProcess.OTHER_INFORMATION_START.getDesc());
            List<BankReconciliation> bankReconciliationlist = getIdentificationTaskList(data, CommonRuleStrategy.CMP_PUBLISHRULE_PREFIX, ruleCodes, context);
            result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), bankReconciliationlist);
            log.error("【其他信息辨识】一个批次=======================执行完成,包含{}条流水明细,匹配银行回单共耗时{}s",  org.springframework.util.CollectionUtils.isEmpty(data) ? "0" : data.size(), (System.currentTimeMillis() - s0) / 1000.0);

        } catch (Exception e) {
            log.error("智能流水执行辨识异常：其他信息辨识中相关性规则执行异常", e);
        }
        return result;

//        return ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), getBankReconciliationList(context));
    }


    private List<BankReconciliation> getIdentificationTaskList(List<BankReconciliation> list, String ruleType, Map<String, String> ruleCodes, BankDealDetailContext context) {
        //ruleType 是相关性规则的前缀
        for (Map.Entry<String, String> entry : ruleCodes.entrySet()) {
            //需要根据rulecode 对应到相应的相关性规则的前缀
            boolean shouldBreak = false; // 添加一个标志位用于判断是否需要跳出循环
            for (BankReconciliation bankReconciliation : list) {
                if (bankReconciliation.get("OtherInformationbreak") != null && bankReconciliation.get("OtherInformationbreak").equals("1")) { // break
                    shouldBreak = true; // 设置标志位为true，表示需要跳出循环
                    break; // 跳出内层循环
                }
                List<BankReconciliation> newlist = new ArrayList<BankReconciliation>();
                newlist.add(bankReconciliation);
                try {
                    // 分批处理银行对账单的数据 进行规则辨识
                    context.setLogName(RuleLogEnum.RuleLogProcess.OTHER_INFORMATION_NAME.getDesc());
                    context.setOperationName(RuleLogEnum.RuleLogProcess.OTHER_INFORMATION_START.getDesc());
                    context.setResultSuccessLog(RuleLogEnum.RuleLogProcess.OTHER_INFORMATION_ONE.getDesc());
                    context.setResultFailLog(RuleLogEnum.RuleLogProcess.OTHER_INFORMATION_TWO.getDesc());
                    otherInformationRuleStrategy.executeRule(newlist, ruleType, entry.getKey(), entry.getValue(), context, true);
                    bankReconciliation.setNeedRollback(false);
                } catch (Exception e) {
                    log.error("其他信息辨识中相关性规则执行异常", e);
                    DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM010_10S01.getCode(), bankReconciliation, entry.getKey());
                    //异常处理逻辑
                    bankReconciliation.setNeedRollback(true);
                }
            }
            if (shouldBreak) break; // 根据标志位决定是否跳出外层循环
        }
        return list;
    }
}
