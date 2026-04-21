package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.impl;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.IBankDealDetailBusiOper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.repetition.RepetitionResultVo;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.repetition.manager.BankReconciliationRepetitionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author guoyangy
 * @Date 2024/6/28 11:58
 * @Description todo
 * @Version 1.0
 */
@Service
@Slf4j
public class BankDealDetailBusiOperImpl implements IBankDealDetailBusiOper {


    @Autowired
    BankReconciliationRepetitionManager bankReconciliationRepetitionManager;

    @Override
    public Map<String, List<BankReconciliation>> executeDedulication(List<BankReconciliation> bankReconciliationList) {

        if(CollectionUtils.isEmpty(bankReconciliationList)){
            return null;
        }
        RepetitionResultVo result = bankReconciliationRepetitionManager.check(bankReconciliationList);
        List<BankReconciliation> repeapList = result.getRepetitionList();
        List<BankReconciliation> addList = result.getInsertList();
        List<BankReconciliation> updateList = result.getUpdateList();
        List<BankReconciliation> rollbackList = result.getRollbackList();
        int returnTotalCount = 0;
        Map<String, List<BankReconciliation>> map = new HashMap<>();
        if(!CollectionUtils.isEmpty(repeapList)){
            map.put(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_REPEAT.getKey(),repeapList);
            returnTotalCount+=repeapList.size();
        }
        if(!CollectionUtils.isEmpty(addList)){
            map.put(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_ADD.getKey(),addList);
            returnTotalCount+=addList.size();
        }
        if(!CollectionUtils.isEmpty(updateList)){
            map.put(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_UPDATE.getKey(),updateList);
            returnTotalCount+=updateList.size();
        }
        if(!CollectionUtils.isEmpty(rollbackList)){
            map.put(DealDetailEnumConst.Deduplication_KEYEnum.Deduplication_KEY_ROLLBACK.getKey(),rollbackList);
            returnTotalCount+=rollbackList.size();
        }
        if(returnTotalCount!=bankReconciliationList.size()){
            log.error("【业务去重】步骤二:验重逻辑返回数据条数不对,传入{}条，返回{}条",bankReconciliationList.size(),returnTotalCount);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006A4", "验重逻辑返回数据条数不对传入") /* "验重逻辑返回数据条数不对传入" */+bankReconciliationList.size()+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006A5", "条，返回") /* "条，返回" */+returnTotalCount+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006A6", "条") /* "条" */);
        }
        return map;
    }

}
