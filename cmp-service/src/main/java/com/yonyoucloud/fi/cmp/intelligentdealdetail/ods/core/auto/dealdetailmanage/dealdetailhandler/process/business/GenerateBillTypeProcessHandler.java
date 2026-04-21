package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.process.business;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill.BusinessGenerateFundNewService;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultStreamBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.process.BankDealDetailProcessChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author guoyangy
 * @Date 2024/7/3 13:54
 * @Description 生单
 * @Version 1.0
 */
@Slf4j
@Service(RuleCodeConst.SYSTEM023)
public class GenerateBillTypeProcessHandler extends DefaultStreamBatchHandler {

    //20250416修改，生单方法使用已有的类
    @Autowired
    private BusinessGenerateFundNewService businessGenerateFundNewService;
    @Override
    public Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) {
        long s0 = System.currentTimeMillis();
        context.setLogName(RuleLogEnum.RuleLogProcess.BANK_RECEIPT_LOG_PROCESS.getDesc());
        context.setLogName(RuleLogEnum.RuleLogProcess.BANK_RECEIPT_LOG_PROCESS_IDENTIFY_ONE.getDesc());
        context.setBankDealDetailChain(new BankDealDetailProcessChainImpl());
        List<BankReconciliation> bankReconciliationList = this.getBankReconciliationList(context);
        Map<String, List<BankReconciliation>> result = new HashMap<>();
        List<BankReconciliation> trueList = new ArrayList<>();
        List<BankReconciliation> pedingList = new ArrayList<>();
        List<BankReconciliation> continueList = new ArrayList<>();
        List<BankReconciliation> pendingAccountList = new ArrayList<>();
        List<BankReconciliation> errorList = new ArrayList<>();
        List<BankReconciliation> pendingAccount_TrueList = new ArrayList<>();
        List<BankReconciliation> pendingAccount_FalseList = new ArrayList<>();

        List<BankReconciliation> generateBillTypeList = new ArrayList<>();
        List<BankReconciliation> generateBillType_TrueList = new ArrayList<>();
        List<BankReconciliation> generateBillType_FalseList = new ArrayList<>();
        context.setLogName(RuleLogEnum.RuleLogProcess.GENERATE_BILL_PROCESS_NAME.getDesc());
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            if (bankReconciliation.getGenertbilltype() != null) {
                //判断是否可以生单
                if(canGenertBill(bankReconciliation)){
                    //识别出哪些是挂账辨识生单的
                    if (bankReconciliation.getIsadvanceaccounts() != null && BooleanUtils.isTrue(bankReconciliation.getIsadvanceaccounts())) {
                        pendingAccountList.add(bankReconciliation); //挂账辨识生单
                    } else {
                        generateBillTypeList.add(bankReconciliation); // 生单类型辨识生单
                    }
                } else {
                    continueList.add(bankReconciliation);
                }
            } else {
                continueList.add(bankReconciliation);
            }
        }
        if (pendingAccountList.size() > 0) {
            for (BankReconciliation bankReconciliation : pendingAccountList) {
                if (bankReconciliation.getIsautosubmit() == null || bankReconciliation.getIsautosubmit() == (short)0) {
                    pendingAccount_FalseList.add(bankReconciliation);
                } else {
                    pendingAccount_TrueList.add(bankReconciliation);
                }
            }
        }

        if (generateBillTypeList.size() > 0) {
            for (BankReconciliation bankReconciliation : generateBillTypeList) {
                if (bankReconciliation.getIsautosubmit() == null || bankReconciliation.getIsautosubmit() == (short)0) {
                    generateBillType_FalseList.add(bankReconciliation);
                } else {
                    generateBillType_TrueList.add(bankReconciliation);
                }
            }
        }

        try {
            Map<String, Boolean> accurateAutoassociateMap = getAccurateAutoassociateMap(bankReconciliationList);
            try {
                if (pendingAccount_TrueList.size() > 0) {

                    for (BankReconciliation bankReconciliation : pendingAccount_TrueList) {
                        try {
                            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_PROCESS_NAME.getDesc(),RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_IDENTIFY_ONE.getDesc(),context);
                            businessGenerateFundNewService.bankreconciliationGenerateDoc(Collections.singletonList(bankReconciliation), true);
                            setPushDownMark(bankReconciliation,accurateAutoassociateMap,pedingList,context,RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_IDENTIFY_NAME.getDesc());
                        }catch (Exception e){
                            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus());
                            removePushDownMark(bankReconciliation);
                            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_PROCESS_NAME.getDesc(),RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_IDENTIFY_TWO.getDesc()+e.getMessage(),context);
                            extracted(bankReconciliation, e, errorList,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400671", "system023: 挂账辨识规则生单，执行生单逻辑异常！") /* "system023: 挂账辨识规则生单，执行生单逻辑异常！" */);
                        }
                    }
                }
                if (pendingAccount_FalseList.size() > 0) {
                    for (BankReconciliation bankReconciliation : pendingAccount_FalseList) {
                        try {
                            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.GENERATE_BILL_PROCESS_NAME.getDesc(),RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_IDENTIFY_ONE.getDesc(),context);
                            businessGenerateFundNewService.bankreconciliationGenerateDoc(Collections.singletonList(bankReconciliation), false);
                            setPushDownMark(bankReconciliation,accurateAutoassociateMap,pedingList,context,RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_IDENTIFY_NAME.getDesc());
                        }catch (Exception e){
                            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus());
                            removePushDownMark(bankReconciliation);
                            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_PROCESS_NAME.getDesc(),RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_IDENTIFY_TWO.getDesc()+e.getMessage(),context);
                            extracted(bankReconciliation, e, errorList, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400671", "system023: 挂账辨识规则生单，执行生单逻辑异常！") /* "system023: 挂账辨识规则生单，执行生单逻辑异常！" */);
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(),"system023: 挂账辨识规则生单，执行生单逻辑异常！");
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM009_09S03.getCode(),pendingAccountList);
            }
            // todo 资金收付生单的异常，可以收集到，收付的单据处理异常之后，这里收集不到异常，流水的处理状态可能不对
            // 收付的异常必须要收集
            if (generateBillType_TrueList.size() > 0) {
                for (BankReconciliation bankReconciliation : generateBillType_TrueList) {
                    try {
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_PROCESS_NAME.getDesc(),RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_ONE.getDesc(),context);
                        businessGenerateFundNewService.bankreconciliationGenerateDoc(Collections.singletonList(bankReconciliation), true);
                        setPushDownMark(bankReconciliation,accurateAutoassociateMap,pedingList,context,RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_NAME.getDesc());
                    }catch (Exception e){
                        bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus());
                        removePushDownMark(bankReconciliation);
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_PROCESS_NAME.getDesc(),RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_TWO.getDesc()+e.getMessage(),context);
                        extracted(bankReconciliation, e, errorList,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400672", "system023: 生单类型辨识规则生单，执行生单逻辑异常！") /* "system023: 生单类型辨识规则生单，执行生单逻辑异常！" */);
                    }
                }
            }
            if (generateBillType_FalseList.size() > 0) {
                for (BankReconciliation bankReconciliation : generateBillType_FalseList) {
                    try {
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_PROCESS_NAME.getDesc(),RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_ONE.getDesc(),context);
                        businessGenerateFundNewService.bankreconciliationGenerateDoc(Collections.singletonList(bankReconciliation), false);
                        setPushDownMark(bankReconciliation,accurateAutoassociateMap,pedingList,context,RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_NAME.getDesc());
                    }catch (Exception e){
                        bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus());
                        removePushDownMark(bankReconciliation);
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,RuleLogEnum.RuleLogProcess.GENERATE_BILL_PROCESS_NAME.getDesc(),RuleLogEnum.RuleLogProcess.BANK_SERIALIZATION_ERROR_MESSAGE.getDesc()+e.getMessage(),context);
                        extracted(bankReconciliation, e, errorList,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400672", "system023: 生单类型辨识规则生单，执行生单逻辑异常！") /* "system023: 生单类型辨识规则生单，执行生单逻辑异常！" */);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), continueList);
            result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus(), pendingAccountList);
            result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus(), generateBillTypeList);
            return result;
        }
        //8.31 与郭老板沟通： 不管是挂账生单 还是 生单类型生单  最后都应该是end EXECUTE_STATUS_SUCCESS_END("1","阻断性规则且满足阻断条件，下一步执行具体流程处理"),
        List<BankReconciliation> endList = new ArrayList<>();
        endList.addAll(pendingAccountList);
        endList.addAll(generateBillTypeList);
        //生单异常的数据需要从成功和继续中移除掉
        endList.removeAll(errorList);
        continueList.removeAll(errorList);
        endList.removeAll(pedingList);
        continueList.removeAll(pedingList);
        result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), continueList);
        result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus(), endList);
        result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(), pedingList);
        result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus(), errorList);
        log.error("【调用生单具体操作】一个批次=======================执行完成,包含{}条流水明细,匹配银行回单共耗时{}s,其中未匹配上的有{}条流水明细，匹配并生单的有{}条流水明细，异常的有{}条流水明细！",  org.springframework.util.CollectionUtils.isEmpty(bankReconciliationList) ? "0" : bankReconciliationList.size(), (System.currentTimeMillis() - s0) / 1000.0,continueList.size(),endList.size(),errorList.size());
        return result;
    }

    private static void extracted(BankReconciliation bankReconciliation, Exception e, List<BankReconciliation> errorList, String message) {
        errorList.add(bankReconciliation);
        log.error(e.getMessage(),message);
        log.error("生单异常堆栈信息==================================================",e);
        bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus());
        DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM007_07S03.getCode(), bankReconciliation);
    }

    /**
     * 校验对账单状态，是否可以生单
     * @param bankReconciliation
     * @return
     */
    private boolean canGenertBill(BankReconciliation bankReconciliation){
        // 关联状态为未关联 && 是否发布为未发布 && 是否勾兑为未勾兑 && 是否总账勾兑为未勾兑 && 金额大于0 && 统一对账码是否解析生成，是解析生成则不能生单
        return (
                (bankReconciliation.getAssociationstatus() == null || AssociationStatus.NoAssociated.getValue() == bankReconciliation.getAssociationstatus())
                &&
                (bankReconciliation.getIspublish() == null || BooleanUtils.isFalse(bankReconciliation.getIspublish()))
                &&
                (bankReconciliation.getCheckflag() == null || BooleanUtils.isFalse(bankReconciliation.getCheckflag()))
                &&
                (bankReconciliation.getOther_checkflag() == null || BooleanUtils.isFalse(bankReconciliation.getOther_checkflag()))
                &&
                (bankReconciliation.getTran_amt() !=null && bankReconciliation.getTran_amt().compareTo(BigDecimal.ZERO) > 0)
                &&
                !bankReconciliation.getIsparsesmartcheckno()
        );

    }




    private Map<String,Boolean> getAccurateAutoassociateMap(List<BankReconciliation> bankReconciliations) throws Exception {
        List<String> accentityList = bankReconciliations.stream().map(s -> s.getAccentity()).collect(Collectors.toList());
        HashSet<String> accentitySet = new HashSet<>();
        for (String bankId:accentityList) {
            accentitySet.add(bankId);
        }
        //获取现金参数自动配置信息表标识字段
        QuerySchema querySchema = QuerySchema.create().addSelect("accentity,autogenerateconfirm");
        QueryConditionGroup condition = QueryConditionGroup.and(
                //会计主体参数赋值
                QueryCondition.name("accentity").in(accentitySet)
        );
        querySchema.addCondition(condition);
        List<AutoConfig> autoConfigList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema, null);
        Map<String, Boolean> retMap = autoConfigList.stream().collect(Collectors.toMap(AutoConfig::getAccentity, AutoConfig::getAutogenerateconfirm));
        return retMap;
    }

    private void setPushDownMark(BankReconciliation bankReconciliation,Map<String,Boolean> accurateAutoassociateMap,List<BankReconciliation> pedingList,BankDealDetailContext context,String name){
        Boolean b = accurateAutoassociateMap.get(bankReconciliation.getAccentity());
        if  ("true".equals(bankReconciliation.get("pushDownMark"))) {
            String pushDownErrorMessage = bankReconciliation.getString("pushDownErrorMessage");
            removePushDownMark(bankReconciliation);
            if (StringUtils.isEmpty(pushDownErrorMessage)){
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,name,RuleLogEnum.RuleLogProcess.GENERATE_BILL_TYPE_PROCESS_IDENTIFY_THREE.getDesc(),context);
            }else {
                pedingList.add(bankReconciliation);
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation,name,pushDownErrorMessage,context);
            }

        }
//        if ("true".equals(bankReconciliation.get("pushDownMark")) && (b != null && !b)) {
//            pedingList.add(bankReconciliation);
//            bankReconciliation.remove("pushDownMark");
//        }
    }

    private void removePushDownMark(BankReconciliation bankReconciliation){
        bankReconciliation.remove("pushDownMark");
        bankReconciliation.remove("pushDownErrorMessage");
    }
}