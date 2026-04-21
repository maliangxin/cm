package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.model.DealDetailRuleExecRecord;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author guoyangy
 * @Date 2024/6/18 16:32
 * @Description 流水处理过滤器链
 * @Version 1.0
 */
@Slf4j
public class OnlyUsePublishBankDealDetailMatchChainImpl extends BankDealDetailMatchChainImpl{
    public static OnlyUsePublishBankDealDetailMatchChainImpl get(){
        return new OnlyUsePublishBankDealDetailMatchChainImpl();
    }

    @Override
    public List<BankDealDetailWrapper> getExecutorProcessDealDetail(BankDealDetailContext context, List<BankReconciliation> processList) {
        return null;
    }

    @Override
    public List<BankreconciliationIdentifyType> loadStreamIdentifyMatchRule(Integer index) {
        List<BankreconciliationIdentifyType> bankreconciliationIdentifyTypes = new ArrayList<>();
        BankreconciliationIdentifyType rule = DealDetailUtils.createRule(RuleCodeConst.SYSTEM008,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400453", "辨识并发布认领中心") /* "辨识并发布认领中心" */,index);
        bankreconciliationIdentifyTypes.add(rule);
        return bankreconciliationIdentifyTypes;
    }
    @Override
    public List<BankReconciliation> handleBankReconciliationToDB(BankDealDetailContext context, List<BankReconciliation> saveOrUpdateBankReconciliationList) {
        //step1:区分更新流水、新增流水
        List<BankReconciliation> saveBankReconciliationList = new ArrayList<>();
        List<BankReconciliation> updateBankReconciliationList = new ArrayList<>();
        this.prepareBankReconciliationSaveOrUpdate(context,saveBankReconciliationList,updateBankReconciliationList,saveOrUpdateBankReconciliationList);
        if(saveBankReconciliationList.size()+updateBankReconciliationList.size() !=saveOrUpdateBankReconciliationList.size()){
            log.error("终态流水更新或新增流水库时，流水分类异常,流水总数量:{},新增{},更新:{}",saveOrUpdateBankReconciliationList.size(),saveBankReconciliationList.size(),updateBankReconciliationList.size());
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400452", "终态流水更新或新增流水库时，流水分类异常") /* "终态流水更新或新增流水库时，流水分类异常" */);
        }
        //step2:sql更新参数准备
        Map<String,List<? extends Object>> sqlParamMap = this.bulidODSSQLParameterAndprocessingModelList(context);
        List<DealDetailRuleExecRecord> dealDetailRuleExecRecords = (List<DealDetailRuleExecRecord>)sqlParamMap.get(DealDetailEnumConst.RULEPARAM);
        //step3:执行更新
        IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
        String currentRuleCode = context.getCurrentRule() != null ? context.getCurrentRule().getCode() : null;
        bankDealDetailAccessDao.doHandleBankReconciliationToDB(saveBankReconciliationList,updateBankReconciliationList,saveOrUpdateBankReconciliationList,dealDetailRuleExecRecords,null,"matchDealDetailResultDaoImpl",currentRuleCode);
        return saveBankReconciliationList;
    }

    @Override
    public void setBankReconciliationExecuteStatusAndProcessStatus(BankreconciliationIdentifyType rule, String executeStatus, BankReconciliation bankReconciliation) {
        if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus().equals(executeStatus)){
            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_ERROR.getStatus().shortValue());
            bankReconciliation.put(DealDetailEnumConst.EXECUTESTATUS,DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus());
        }
        if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus().equals(executeStatus) && rule!=null&&(rule.getCode().equals(RuleCodeConst.SYSTEM008))){
            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus().shortValue());
        }else{
            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_SUCC.getStatus().shortValue());
        }
        bankReconciliation.put(DealDetailEnumConst.EXECUTESTATUS,DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus());
    }
}
