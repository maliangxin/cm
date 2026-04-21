package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.ODSBasicInfoObject;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IStremBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import org.imeta.core.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * @Author guoyangy
 * @Date 2024/6/18 13:49
 * @Description todo
 * @Version 1.0
 */
@Service
public abstract class DefaultStreamBatchHandler extends ODSBasicInfoObject implements IStremBatchHandler {
    /**
     * 规则处理主流程
     * */
    @Override
    public abstract Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) throws Exception;
    /**
     * 规则幂等校验
     * */
    public List<BankReconciliation> checkIdempotent(List<BankReconciliation> bankReconciliations,Map<String,List<BankReconciliation>> resultMap){
        return bankReconciliations;
    }
    @Override
    public void batchUpdateProcessstatus(BankDealDetailContext context) {
        BankreconciliationIdentifyType bankreconciliationIdentifyType = context.getCurrentRule();
        if(null != bankreconciliationIdentifyType){
            String ruleCode = bankreconciliationIdentifyType.getCode();
            List<BankReconciliation> bankReconciliationList = this.getBankReconciliationList(context);
            Short processStatus = null;
            if(RuleCodeConst.SYSTEM021.equals(ruleCode)){
                //关联
                processStatus = DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_START.getStatus();
            }
            if(RuleCodeConst.SYSTEM022.equals(ruleCode)){
                //凭据关联
                processStatus = DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_START.getStatus();
            }
            if(RuleCodeConst.SYSTEM023.equals(ruleCode)){
                //生单
                processStatus = DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_START.getStatus();
            }
            int odsStatus = DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_PROCESS_START.getProcessstatus();
            if(null!=processStatus){
                IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
                bankDealDetailAccessDao.updateProcessstatusOnlyPreHandle(processStatus,odsStatus,bankReconciliationList);
                DealDetailUtils.getBankReconciliationListAfterUpdateProcessstatus(bankReconciliationList);
            }
        }
    }

    public void addBankReconciliationToMap(List<BankReconciliation> bankReconciliations,String businessCode,
                                           String key,Map<String, List<BankReconciliation>> resultMap){
        for(BankReconciliation bankReconciliation:bankReconciliations){
            this.addBankReconciliationToMap(bankReconciliation,businessCode,key,resultMap);
        }
    }
    public void addBankReconciliationToMap(BankReconciliation bankReconciliation,String businessCode,
                                           String key,Map<String, List<BankReconciliation>> resultMap){
        if(null == bankReconciliation){
            return;
        }
        if(StringUtils.isEmpty(key)){
            return;
        }
        if(CollectionUtils.isEmpty(resultMap)){
            return;
        }
        if(!DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus().equals(key) &&
            !DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus().equals(key) &&
            !DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus().equals(key) &&
            !DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus().equals(key)){
            return;
        }
        List<BankReconciliation> endList = resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus());
        List<BankReconciliation> continueList = resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus());
        List<BankReconciliation> pendingList = resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus());
        List<BankReconciliation> errorList = resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus());
        if(!StringUtils.isEmpty(businessCode)){
            DealDetailUtils.appendBusiCode(businessCode,bankReconciliation,null);
        }
        //流水添加到已完结集合
        if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus().equals(key)){
            if(null!=endList&&!endList.contains(bankReconciliation)){
                endList.add(bankReconciliation);
            }
            if(null!=continueList&&continueList.contains(bankReconciliation)){
                continueList.remove(bankReconciliation);
            }
            if(null!=pendingList&&pendingList.contains(bankReconciliation)){
                pendingList.remove(bankReconciliation);
            }
            if(null!=errorList&&errorList.contains(bankReconciliation)){
                errorList.remove(bankReconciliation);
            }
        }
        //流水添加到continue集合
        if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus().equals(key)){
            if(null!=continueList&&!continueList.contains(bankReconciliation)){
                continueList.add(bankReconciliation);
            }
            if(null!=endList&&endList.contains(bankReconciliation)){
                endList.remove(bankReconciliation);
            }
            if(null!=pendingList&&pendingList.contains(bankReconciliation)){
                pendingList.remove(bankReconciliation);
            }
            if(null!=errorList&&errorList.contains(bankReconciliation)){
                errorList.remove(bankReconciliation);
            }
        }
        //流水添加到挂起集合
        if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus().equals(key)){
            if(null!=pendingList&&!pendingList.contains(bankReconciliation)){
                pendingList.add(bankReconciliation);
            }
            if(null!=continueList&&continueList.contains(bankReconciliation)){
                continueList.remove(bankReconciliation);
            }
            if(null!=endList&&endList.contains(bankReconciliation)){
                endList.remove(bankReconciliation);
            }
            if(null!=errorList&&errorList.contains(bankReconciliation)){
                errorList.remove(bankReconciliation);
            }
        }
        //流水添加到异常集合
        if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus().equals(key)){
            if(null!=errorList&&!errorList.contains(bankReconciliation)){
                errorList.add(bankReconciliation);
            }
            if(null!=continueList&&continueList.contains(bankReconciliation)){
                continueList.remove(bankReconciliation);
            }
            if(null!=pendingList&&pendingList.contains(bankReconciliation)){
                pendingList.remove(bankReconciliation);
            }
            if(null!=endList&&endList.contains(bankReconciliation)){
                endList.remove(bankReconciliation);
            }
        }
    }

    /**
     * 准备给调度器反参
     * <p>DealDetailEnumConst.ExecuteStatusEnum#EXECUTE_STATUS_SUCCESS_END 执行成功不走下一个规则，这笔流水在调度器中已结束，状态为已完结，不执行后面的生单、发布等逻辑
     * <p>DealDetailEnumConst.ExecuteStatusEnum#EXECUTE_STATUS_SUCCESS_PENDING 流水挂起且不走下一个规则，这笔流水在调度器中已结束，直接入库，状态为挂起，不执行后面生单、发布
     * <p>DealDetailEnumConst.ExecuteStatusEnum#EXECUTE_STATUS_SUCCESS_CONTINUE 流水走下一个规则，这笔流水在调度器中未结束，继续执行后续生单、发布业务
     * */
    public Map<String, List<BankReconciliation>> prepareResult() {
        Map<String, List<BankReconciliation>> resultMap = new HashMap<>();
        // 匹配上自动阻止
        resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus(), new ArrayList<>());
        // 人工介入
        resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(), new ArrayList<>());
        // 匹配不上下一个流程
        resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), new ArrayList<>());
        // 异常
        resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus(), new ArrayList<>());
        return resultMap;
    }
}