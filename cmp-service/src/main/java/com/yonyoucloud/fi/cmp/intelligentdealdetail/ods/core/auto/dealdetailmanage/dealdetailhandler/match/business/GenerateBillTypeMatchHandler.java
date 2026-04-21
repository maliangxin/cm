package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business;

import com.google.common.collect.ImmutableMap;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationDetail;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OprType;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IBankReconciliationCommonService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.service.GenerateBillTypeRuleStrategy;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultStreamBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleCheckLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common.CheckRuleCommonUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.OdsCommonUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yonyou.bpm.rest.utils.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author guoyangy
 * @Date 2024/6/18 15:33
 * @Description 生单类型辨识规则
 * @Version 1.0
 */
@Service(RuleCodeConst.SYSTEM007)
@Slf4j
public class GenerateBillTypeMatchHandler extends DefaultStreamBatchHandler {

    @Resource
    GenerateBillTypeRuleStrategy generateBillTypeRuleStrategy;
    @Autowired
    IBankReconciliationCommonService bankReconciliationCommonService;

    /**
     *
     * @param context
     * @param chain
     * @return
     */
    @Override
    public Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) {
        long s0 = System.currentTimeMillis();
        if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect())){
            return ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), getBankReconciliationList(context));
        }
        List<BankReconciliation> bankReconciliationList = this.getBankReconciliationList(context);
        List<BankReconciliation> pendingList = new ArrayList<>();
        bankReconciliationList = CheckRuleCommonUtils.processRuleForReturn(bankReconciliationList,pendingList);
        Map<String, List<BankReconciliation>> resultMap = new HashMap<>();
        if(CollectionUtils.isEmpty(bankReconciliationList)){
            return resultMap;
        }
        context.setLogName(RuleLogEnum.RuleLogProcess.GENERATE_BILL_module_NAME.getDesc());
        //执行相关性规则
        executePushBill(bankReconciliationList,context);
        // 拼接规则链需要的返回值，根据生单类型，进行判断
        List<BankReconciliation> endList = new ArrayList<>();
        List<BankReconciliation> continueList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            if (bankReconciliation.getGenertbilltype() != null) {
                endList.add(bankReconciliation);
            } else {
                continueList.add(bankReconciliation);
            }
        }
        if (CollectionUtils.isNotEmpty(endList)){
            resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), continueList);
        }
        if (CollectionUtils.isNotEmpty(endList)){
            resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus(), endList);
        }
        if (CollectionUtils.isNotEmpty(resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus())) && CollectionUtils.isNotEmpty(pendingList)){
            resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus()).addAll(pendingList);
        }else if (CollectionUtils.isNotEmpty(pendingList)){
            resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(), pendingList);
        }
        log.error("【生单类型辨识】一个批次=======================执行完成,包含{}条流水明细,匹配银行回单共耗时{}s",  org.springframework.util.CollectionUtils.isEmpty(bankReconciliationList) ? "0" : bankReconciliationList.size(), (System.currentTimeMillis() - s0) / 1000.0);

        return resultMap;
    }


    /**
     * 根据推送数据，匹配推送规则，将数据推送到事件中心
     * @param pushData
     */
    private void executePushBill(List<BankReconciliation> pushData,BankDealDetailContext context) {
        try {
            /*
                TODO 不为提前入账且是未关联，才需要进行生单辨识
                 getIsadvanceaccounts()和getAssociationstatus(),刚拉取的银行流水，没有值
             */
            List<BankReconciliation> needIdentificationList = new ArrayList<>();
            for(BankReconciliation bankReconciliation : pushData){
                if(canGenertBill(bankReconciliation,context)){
                    needIdentificationList.add(bankReconciliation);
                }else {
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_module_NAME.getDesc(),RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_IDENTIFY_FOUR.getDesc(),context);
                }
            }
            //赋值对接人  24年8.31 注释掉此功能  原有海康的逻辑 不需要了
//            setCounterpartByDetail(needIdentificationList);
            //调用生单规则
            Map<String,String> ruleCodes = new HashMap<>();
            try {
                ruleCodes = bankReconciliationCommonService.getRuleCodes(RuleCodeConst.SYSTEM007);
                if (ObjectUtils.isEmpty(ruleCodes) || ruleCodes.size() < 1 ) {
                    return ;
                }
            } catch (Exception e) {
                log.error("SYSTEM007：生单类型辨识规则设置查询异常",e);
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM007_07S02.getCode(),needIdentificationList);
            }
            if (ObjectUtils.isEmpty(ruleCodes) || ruleCodes.size() < 1 ) {
                log.error("SYSTEM007：未查询到可用的生单类型辨识匹配规则");
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM007_07YO1.getCode(),needIdentificationList);
            }
            for (Map.Entry<String, String> entry : ruleCodes.entrySet()) {
                //需要根据rulecode 对应到相应的相关性规则的前缀
                boolean shouldBreak = false; // 添加一个标志位用于判断是否需要跳出循环
                for (BankReconciliation bankReconciliation : needIdentificationList) {
                    if (bankReconciliation.get("break") != null && "1".equals(bankReconciliation.get("break"))) { // break
                        shouldBreak = true; // 设置标志位为true，表示需要跳出循环
                        break; // 跳出内层循环
                    }
                    if (bankReconciliation.get(OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK) != null && bankReconciliation.get(OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK).equals(OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK_VALUE_TRUE)) { // break
                        shouldBreak = true; // 设置标志位为true，表示需要跳出循环
                        break; // 跳出内层循环
                    }
                    List<BankReconciliation> newlist = new ArrayList<BankReconciliation>();
                    newlist.add(bankReconciliation);
                    try {
                        // 分批处理银行对账单的数据 进行规则辨识
                        context.setLogName(RuleLogEnum.RuleLogProcess.GENERATE_BILL_module_NAME.getDesc());
                        context.setOperationName(RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_START.getDesc());
                        context.setResultSuccessLog(RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_ONE.getDesc());
                        context.setResultFailLog(RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_TWO.getDesc());
                        generateBillTypeRuleStrategy.executeRule(newlist,null, entry.getKey(),entry.getValue(),context,null);
                        bankReconciliation.setNeedRollback(false);
                    } catch (Exception e) {
                        log.error("生单类型辨识中相关性规则执行异常",e);
                        DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM007_07S01.getCode(),bankReconciliation,entry.getKey());
                        //异常处理逻辑
                        bankReconciliation.setNeedRollback(true);
                    }
                }
                if (shouldBreak) break; // 根据标志位决定是否跳出外层循环
//                iBankrecRuleEngineService.executeRuleEngine(needIdentificationList, CmpRuleEngineTypeConstant.cmp_generate, true);
            }
            //todo 查询流水辨识匹配规则设置内容判断是否生单(715不做)
            boolean pushbill = true;
            if (pushbill) {
                needIdentificationList.stream().forEach(bankReconciliation -> {
                    // 相关性规则赋值使用的map，此处生单类型可能为String数据类型，生单入库之前强转下格式
                    String genera = bankReconciliation.get(OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK) !=null ? bankReconciliation.get(OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK).toString() : null;
                    Short generateBillType = bankReconciliation.getGenertbilltype();
                    if(OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK_VALUE_TRUE.equals(genera)){
                        //生单辨识上了但是没有赋值的数据进行校验
                        if (generateBillType == null){
                            bankReconciliation.set(OdsCommonUtils.GENERATE_BILL_TYPE_BREAK_MARK,null);
                            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.GENERATE_BILL_module_NAME.getDesc(),RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_THREE.getDesc(),context);
                        }
                    }
                    bankReconciliation.setGenertbilltype(generateBillType);
//                    bankReconciliation.setEntityStatus(EntityStatus.Insert);
                });
                //推送现金管理生成资金收付款单(生单逻辑改到GenerateBillTypeProcessHandler处理)
//                businessGenerateFundNewService.bankreconciliationGenerateDoc(pushData, false);
            }
        } catch (Exception e) {
            log.error("智能流水执行辨识异常：生单类型辨识异常："+e);
            log.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00033", "调用自动生单规则失败，请检查以cmp_generate开头的自动生单规则是否存在或者是否启用！") /* "调用自动生单规则失败，请检查以cmp_generate开头的自动生单规则是否存在或者是否启用！" */);
        }
    }

    /**
     * 赋值对接人
     * @param allPushData
     * @throws Exception
     */
    private void setCounterpartByDetail(List<BankReconciliation> allPushData) throws Exception {
        if (CollectionUtils.isNotEmpty(allPushData)) {
            List<Object> idList = allPushData.stream().map(recordBill -> recordBill.getId()).collect(Collectors.toList());
            List<BankReconciliationDetail> bankReconciliationDetails = getBankReconciliationDetails(idList);
            HashMap<Object, List<BankReconciliationDetail>> detailMap = new HashMap<>();
            for (BankReconciliationDetail brVo : bankReconciliationDetails) {
                if (detailMap.containsKey(brVo.getMainid())) {
                    detailMap.get(brVo.getMainid()).add(brVo);
                } else {
                    List<BankReconciliationDetail> list = new ArrayList<>();
                    list.add(brVo);
                    detailMap.put(brVo.getMainid(), list);
                }
            }
            for (int i = 0; i < allPushData.size(); i++) {
                // 封装id集合
                BankReconciliation bankReconciliation = allPushData.get(i);
                Object id = bankReconciliation.getId();
                List<BankReconciliationDetail> bankReconciliationDetail = detailMap.get(id);
                // 设置业务人员财务人员日期
                setAssignBankData(bankReconciliation, bankReconciliationDetail);
            }
        }

    }

    /**
     *
     * @param ids
     * @return
     * @throws Exception
     */
    private List<BankReconciliationDetail> getBankReconciliationDetails(List<Object> ids) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("id,mainid,autheduser,returndate,return_reason,oprtype,group,operator,oprdate,pubts");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("mainid").in(ids));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(BankReconciliationDetail.ENTITY_NAME, querySchema, null);
    }

    /**
     *
     * @param bankReconciliation
     * @param details
     */
    private void setAssignBankData(BankReconciliation bankReconciliation, List<BankReconciliationDetail> details) {
        if (CollectionUtils.isNotEmpty(details)) {
            String counterpart = "";
            String busscounterpart = "";
            Date busiDate = null;
            for (int i = 0; i < details.size(); i++) {
                BankReconciliationDetail detail = details.get(i);
                String oprtype = detail.getOprtype();
                String autheduser = detail.getAutheduser();
                if (busiDate == null) {
                    busiDate = detail.getOprdate();
                } else {
                    if (detail.getOprdate() != null && DateUtils.dateCompare(detail.getOprdate(), busiDate) > 0) {
                        busiDate = detail.getOprdate();
                    }
                }
                //分派
                if (OprType.AutoFinance.getValue().equals(oprtype) || OprType.ManualFinance.getValue().equals(oprtype)) {
                    if (StringUtils.isNotBlank(autheduser)) {
                        if (StringUtils.isBlank(counterpart)) {
                            counterpart = autheduser;
                        } else {
                            counterpart = counterpart + "," + autheduser;
                        }
                    }
                } else {
                    if (StringUtils.isNotBlank(autheduser)) {
                        if (StringUtils.isBlank(busscounterpart)) {
                            busscounterpart = autheduser;
                        } else {
                            busscounterpart = busscounterpart + "," + autheduser;
                        }
                    }
                }
            }
            if (StringUtils.isNotBlank(counterpart)) {
                bankReconciliation.setCounterpart(counterpart);
            }
            if (StringUtils.isNotBlank(busscounterpart)) {
                bankReconciliation.setBusscounterpart(busscounterpart);
            }
            if (busiDate != null) {
                bankReconciliation.put("distributebusdate", busiDate);
            }
        }
    }
    /**
     * 校验对账单状态，是否可以生单
     * @param bankReconciliation
     * @return
     */
    private boolean canGenertBill(BankReconciliation bankReconciliation,BankDealDetailContext context){
        //提前入账为空或为false && 关联状态为未关联 && 是否发布为未发布 && 是否勾兑为未勾兑 && 是否总账勾兑为未勾兑
        //记录日志
//        if (BooleanUtils.isTrue(bankReconciliation.getIsadvanceaccounts())) {
//            log.error("SYSTEM007：当前单据不满足生单类型辨识条件- 提前入账为是");
//            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM007_07YO4.getCode(),bankReconciliation);
//        }
        if (bankReconciliation.getAssociationstatus() != null && AssociationStatus.Associated.getValue() == bankReconciliation.getAssociationstatus()) {
            log.error(String.format("SYSTEM007：当前单据[%s]-[%s]不满足生单类型辨识条件- 关联状态为已关联",bankReconciliation.getBank_seq_no()));
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM007_07YO5.getCode(),bankReconciliation,bankReconciliation.getId());
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_module_NAME.getDesc(),DealDetailBusinessCodeEnum.SYSTEM007_07YO5.getDesc(),context);

        }
        if (BooleanUtils.isTrue(bankReconciliation.getIspublish())) {
            log.error(String.format("SYSTEM007：当前单据[%s]-[%s]不满足生单类型辨识条件- 发布状态为已发布",bankReconciliation.getBank_seq_no()));
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM007_07YO6.getCode(),bankReconciliation,bankReconciliation.getId());
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_module_NAME.getDesc(),DealDetailBusinessCodeEnum.SYSTEM007_07YO6.getDesc(),context);

        }
        if (BooleanUtils.isTrue(bankReconciliation.getCheckflag())) {
            log.error(String.format("SYSTEM007：当前单据[%s]-[%s]不满足生单类型辨识条件- 日记账勾对状态为已勾对",bankReconciliation.getBank_seq_no(),bankReconciliation.getId()));
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM007_07YO7.getCode(),bankReconciliation);
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_module_NAME.getDesc(),DealDetailBusinessCodeEnum.SYSTEM007_07YO7.getDesc(),context);

        }
        if (BooleanUtils.isTrue(bankReconciliation.getOther_checkflag())) {
            log.error(String.format("SYSTEM007：当前单据[%s]-[%s]不满足生单类型辨识条件- 总账勾对状态为已勾对",bankReconciliation.getBank_seq_no(),bankReconciliation.getId()));
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM007_07YO8.getCode(),bankReconciliation);
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_module_NAME.getDesc(),DealDetailBusinessCodeEnum.SYSTEM007_07YO8.getDesc(),context);

        }
        return (bankReconciliation.getAssociationstatus() == null || AssociationStatus.NoAssociated.getValue() == bankReconciliation.getAssociationstatus())
                &&
                (bankReconciliation.getIspublish() == null || BooleanUtils.isFalse(bankReconciliation.getIspublish()))
                &&
                (bankReconciliation.getCheckflag() == null || BooleanUtils.isFalse(bankReconciliation.getCheckflag()))
                &&
                (bankReconciliation.getOther_checkflag() == null || BooleanUtils.isFalse(bankReconciliation.getOther_checkflag()))
                &&
                (bankReconciliation.getAutoassociation() == null || BooleanUtils.isTrue(bankReconciliation.getAutoassociation()));

    }
}
