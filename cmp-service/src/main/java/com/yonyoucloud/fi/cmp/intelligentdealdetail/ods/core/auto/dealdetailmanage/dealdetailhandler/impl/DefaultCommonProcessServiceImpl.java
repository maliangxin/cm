package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.DefaultCommonProcessService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IDealDetailProcessing;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.BankDealDetailMatchChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.process.BankDealDetailProcessChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

@Service
public class DefaultCommonProcessServiceImpl implements DefaultCommonProcessService {

    @Resource
    private IDealDetailProcessing dealDetailProcessing;


    /**
     * 智能处理,只处理新增的流水
     * @param BankReconciliations
     * @param codes
     */
    @Override
    public void intelligentFlowIdentification(List<BankReconciliation> BankReconciliations,List<String> codes){
        //若是指定规则，新增的话则需要指定已完成，这是因为新增的话需要先落库，只有SAVE_DIRECT_FINISH才能落库，SAVE_DIRECT状态是需要有本方信息辨识的
        if (CollectionUtils.isNotEmpty(BankReconciliations)){
            BankReconciliations.stream().forEach(
                bankReconciliation -> {
                    bankReconciliation.put(DealDetailEnumConst.SAVE_DIRECT, DealDetailEnumConst.SAVE_AND_UPDATE_NOT_FINISH);
                }
            );
        }
        //不使用IDSID进行校验
        List<BankDealDetailWrapper> bankDealDetailWrappers = DealDetailUtils.convertBankReconciliationToWrapper(BankReconciliations,false);
        if(!org.springframework.util.CollectionUtils.isEmpty(bankDealDetailWrappers)){
            dealDetailProcessing.dealDetailProcesing(bankDealDetailWrappers, BankReconciliations, BankDealDetailMatchChainImpl.get().code(codes), 1, DealDetailUtils.getTraceId(), UUID.randomUUID().toString());
        }
    }
}
