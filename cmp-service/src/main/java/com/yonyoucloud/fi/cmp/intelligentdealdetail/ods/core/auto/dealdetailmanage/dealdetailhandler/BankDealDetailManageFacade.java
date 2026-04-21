package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common.CmpCheckRuleCommonProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailThreadLocalUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author guoyangy
 * @Date 2024/7/18 15:24
 * @Description 流水辨识匹配门面模式
 * @Version 1.0
 */
@Service
@Slf4j
public class BankDealDetailManageFacade{
    @Resource
    private IDealDetailProcessing dealDetailProcessing;
    @Resource
    private IBankDealDetailAccessDao bankDealDetailAccessDao;
    public void bankDealDetailManageAccess(List<BankDealDetailWrapper> bankDealDetailWrappers, List<BankReconciliation> updateBankReconciliationList, DefaultBankDealDetailChain bankDealDetailChain,String traceId,String requestSeqNo){
        try{
            if(CollectionUtils.isEmpty(bankDealDetailWrappers)){
                log.error("【流水辨识匹配及流程处理完成】,流水集合空");
                return;
            }
            int originCount = bankDealDetailWrappers.size();
            String checkMark = CmpCheckRuleCommonProcessor.loadStreamIdentifyMatchRule();
            /**
             * step1:去掉状态为"已完成"流水
             * */
            if ("0".equals(checkMark)){
                DealDetailUtils.removeFinishDealDetail(bankDealDetailWrappers);
            }
            log.info("【流程管理器】原始数据{},去掉[已完成]流水后剩余{}条",originCount,bankDealDetailWrappers.size());
            if(CollectionUtils.isEmpty(bankDealDetailWrappers)){
                log.error("【流程管理器】去掉[已完成]流水后待处理流水为空，结束");
                return;
            }
            extractedForHistpull(bankDealDetailWrappers, updateBankReconciliationList, bankDealDetailChain, traceId, requestSeqNo);
        }finally {
            DealDetailThreadLocalUtils.release();
        }
    }

    private void extractedForHistpull(List<BankDealDetailWrapper> bankDealDetailWrappers, List<BankReconciliation> updateBankReconciliationList, DefaultBankDealDetailChain bankDealDetailChain, String traceId, String requestSeqNo) {
        /**
         * step2:将流水状态改为“开始辨识”checksmartflowswitch
         * */
        if(!CollectionUtils.isEmpty(updateBankReconciliationList)){
            bankDealDetailAccessDao.batchUpdateDealDetailProcessstatusToInitByIdsWithoutPubts(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_START.getStatus(), updateBankReconciliationList);
            List<BankReconciliation> bankReconciliationList = new ArrayList<>();
            for(BankDealDetailWrapper wrapper: bankDealDetailWrappers){
                bankReconciliationList.add(wrapper.getBankReconciliation());
            }
            DealDetailUtils.getBankReconciliationListAfterUpdateProcessstatus(bankReconciliationList);
        }
        /**
         * step3:匹配辨识管理器入口
         * */
        dealDetailProcessing.dealDetailProcesing(bankDealDetailWrappers, updateBankReconciliationList, bankDealDetailChain, traceId, requestSeqNo);
        log.info("【流水管理器处理完成】");
    }

    public void bankDealDetailManageAccessByProcess(List<BankDealDetailWrapper> bankDealDetailWrappers, List<BankReconciliation> updateBankReconciliationList, DefaultBankDealDetailChain bankDealDetailChain,Integer index,String traceId,String requestSeqNo){
        try{
            if(CollectionUtils.isEmpty(bankDealDetailWrappers)){
                log.info("【流水辨识匹配及流程处理完成】,流水集合空");
                return;
            }
            int originCount = bankDealDetailWrappers.size();
            /**
             * step1:去掉状态为"已完成"流水
             * */
            DealDetailUtils.removeFinishDealDetail(bankDealDetailWrappers);
            log.error("【流程管理器】原始数据{},去掉[已完成]流水后剩余{}条",originCount,bankDealDetailWrappers.size());
            if(CollectionUtils.isEmpty(bankDealDetailWrappers)){
                log.info("【流程管理器】去掉[已完成]流水后待处理流水为空，结束");
                return;
            }
            /**
             * step2:匹配辨识管理器入口
             * */
            dealDetailProcessing.dealDetailProcesing(bankDealDetailWrappers,updateBankReconciliationList,bankDealDetailChain,index,traceId,requestSeqNo);
            log.info("【流水管理器处理完成】");
        }finally {
            DealDetailThreadLocalUtils.release();
        }
    }
}