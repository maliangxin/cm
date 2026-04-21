package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business;


import com.google.common.collect.ImmutableMap;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.BankIdentifyTypeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IBankReconciliationCommonService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.impl.CommonRuleStrategy;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.service.AutoPublishRuleStrategy;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultStreamBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.classify.impl.BankDealDetailClassifyPublishServiceImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common.CheckRuleCommonUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.OdsCommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
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
 * @Description 发布对象辨识规则
 * @Version 1.0
 */
@Slf4j
@Service(RuleCodeConst.SYSTEM008)
public class PublishObjMatchHandler extends DefaultStreamBatchHandler {
    @Resource
    AutoPublishRuleStrategy autoPublishRuleStrategy;

    @Autowired
    BankDealDetailClassifyPublishServiceImpl bankDealDetailClassifyHandler;
    @Autowired
    BankIdentifyService bankIdentifyService;
    @Resource
    IBankReconciliationCommonService bankReconciliationCommonService;

    @Override
    public Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) {
        long s0 = System.currentTimeMillis();
        Map<String, List<BankReconciliation>> resultMap = new HashMap<>();
        try {
            if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect())) {
                return ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), getBankReconciliationList(context));
            }
            //发布对象辨识 待实现 上游一次最多传过来50条数据 所以可以不用线程池
            //1.拿到银行对账单数据 （context里）2.识别相关性规则  BankReconciliation需要赋值id 后续需要用到
            Map<String, List<BankReconciliation>> result = new HashMap<String, List<BankReconciliation>>();
            List<BankReconciliation> data = this.getBankReconciliationList(context);
            List<BankReconciliation> pendingList = new ArrayList<>();
            data = CheckRuleCommonUtils.processRuleForReturn(data,pendingList);
            //查询发布辨识规则 中启用的规则，获取到规则编码集合 调用小波提供的接口（小波）
            Map<String, String> ruleCodes = new HashMap<>();
            try {
                ruleCodes = bankReconciliationCommonService.getRuleCodes(RuleCodeConst.SYSTEM008);
            } catch (Exception e) {
                log.error("SYSTEM008：发布对象辨识规则设置查询异常", e);
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM008_08S02.getCode(), data);
            }
            if (ObjectUtils.isEmpty(ruleCodes) || ruleCodes.size() < 1) {
                result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), data);
                log.error("SYSTEM008：未查询到可用的发布辨识匹配规则");
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM008_08YO1.getCode(), data);
                return result;
            }
            //前缀确定（东方）
            List<BankReconciliation> bankReconciliationlist = getIdentificationTaskList(data, CommonRuleStrategy.CMP_PUBLISHRULE_PREFIX, ruleCodes, context,resultMap);

            resultMap = bankDealDetailClassifyHandler.classifyList(bankReconciliationlist, BankIdentifyTypeEnum.Publish);
            if (CollectionUtils.isNotEmpty(resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus()))){
                resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus()).addAll(pendingList);
            }else {
                resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(), pendingList);
            }
            log.error("【发布辨识】一个批次=======================执行完成,包含{}条流水明细,匹配银行回单共耗时{}s",  org.springframework.util.CollectionUtils.isEmpty(data) ? "0" : data.size(), (System.currentTimeMillis() - s0) / 1000.0);
        } catch (Exception e) {
            log.error("智能流水执行辨识异常：SYSTEM008：发布对象辨识规则执行异常", e);
        }
        return resultMap;
    }


    /**
     * 银行对账单获取任务队列集合
     *
     * @param list 银行对账单数组
     * @return
     */
    private List<BankReconciliation> getIdentificationTaskList(List<BankReconciliation> list, String ruleType, Map<String, String> ruleCodes, BankDealDetailContext context,Map<String, List<BankReconciliation>> resultMap) {

        List<BankReconciliation> identificationTaskList = new ArrayList<>();
        //ruleType 是相关性规则的前缀
        for (Map.Entry<String, String> entry : ruleCodes.entrySet()) {
            //需要根据rulecode 对应到相应的相关性规则的前缀，然后再进行相关性规则的执行
            if (CollectionUtils.isEmpty(list)){
                return identificationTaskList;
            }
            List<BankReconciliation> continueList = new ArrayList<>();
            for (BankReconciliation bankReconciliation : list) {
                boolean shouldBreak = this.canForPush(bankReconciliation, context);
                if (!shouldBreak){
                    continueList.add(bankReconciliation);
                    continue;
                }
                List<BankReconciliation> newlist = new ArrayList<BankReconciliation>();
                newlist.add(bankReconciliation);
                try {
                    // 分批处理银行对账单的数据 进行规则辨识
                    context.setLogName(RuleLogEnum.RuleLogProcess.PUBLISH_OBJ_NAME.getDesc());
                    context.setOperationName(RuleLogEnum.RuleLogProcess.PUBLISH_OBJ_START.getDesc());
                    context.setResultSuccessLog(RuleLogEnum.RuleLogProcess.PUBLISH_OBJ_ONE.getDesc());
                    context.setResultFailLog(RuleLogEnum.RuleLogProcess.PUBLISH_OBJ_TWO.getDesc());
                    autoPublishRuleStrategy.executeRule(newlist, ruleType, entry.getKey(), entry.getValue(), context, null);
                    if (!OdsCommonUtils.PUSH.equals(newlist.get(0).get("break"))){
                        //未匹配上的
                        continueList.addAll(newlist);
                    }else {
                        //匹配上的
                        identificationTaskList.add(bankReconciliation);
                    }
                    bankReconciliation.setNeedRollback(false);
                } catch (Exception e) {
                    log.error("发布对象辨识中相关性规则执行异常", e);
                    DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM008_08SO1.getCode(), bankReconciliation, entry.getKey());
                    //异常处理逻辑
                    bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_FAIL.getStatus());
                    bankReconciliation.setNeedRollback(true);
                }
            }

            if (resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus()) == null){
                List<BankReconciliation> listNew = new ArrayList<>();
                listNew.addAll(continueList);
                resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), listNew);
            }else{
                resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus()).addAll(continueList);
            }

        }
        return identificationTaskList;

    }


    /**
     * 返回false标识需要此银行流水需要跳过发布，返回true则可以继续
     * @param bankReconciliation
     * @param context
     * @return
     */
    private boolean canForPush(BankReconciliation bankReconciliation,BankDealDetailContext context){

        //关联已经匹配上
        if (bankReconciliation.get("break") != null && "1".equals(bankReconciliation.get("break"))) { // break
            String format = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2282D8D005D0000D", "当前单据[%s]-[%s]不满足发布类型辨识条件- 关联状态为已关联") /* "当前单据[%s]-[%s]不满足发布类型辨识条件- 关联状态为已关联" */, bankReconciliation.getBank_seq_no());
            log.error(format);
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_module_NAME.getDesc(),format,context);
            return false;
        }

        //提前入账为空或为false && 关联状态为未关联 && 是否发布为未发布 && 是否勾兑为未勾兑 && 是否总账勾兑为未勾兑
        //若是提前入账，则不需要判断关联关系以及完结状态
        if (BooleanUtils.isTrue(bankReconciliation.getIsadvanceaccounts())) {
            String format = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2282D8D005D0000F", "当前单据[%s]-[%s]为提前入账，则可以匹配发布类型辨识条件！") /* "当前单据[%s]-[%s]为提前入账，则可以匹配发布类型辨识条件！" */, bankReconciliation.getBank_seq_no());
            log.error(format);
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.PUBLISH_OBJ_NAME.getDesc(),format,context);
            return true;
        }

        //判断是否已经生单
        if (bankReconciliation.get(OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK) != null && OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK_VALUE_TRUE.equals(bankReconciliation.get(OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK))) { // break
            String format = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2282D8D005D0000E", "当前单据[%s]-[%s]不满足发布类型辨识条件- 单据已经生单！") /* "当前单据[%s]-[%s]不满足发布类型辨识条件- 单据已经生单！" */, bankReconciliation.getBank_seq_no());
            log.error(format);
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_module_NAME.getDesc(),format,context);
            return false;
        }
        return true;
    }


}
