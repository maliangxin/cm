package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.classify.impl;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.BankIdentifyTypeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.classify.IBankDealDetailClassifyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BankDealDetailClassifyPublishServiceImpl implements IBankDealDetailClassifyService {


    @Autowired
    BankIdentifyService bankIdentifyService;

    @Override
    public Map<String, List<BankReconciliation>> classifyList(List<BankReconciliation> bankReconciliationList , BankIdentifyTypeEnum bankIdentifyType) {
        Map<String, List<BankReconciliation>> resultMap = new HashMap<>();
        try {
            List<BankReconciliation> errorList = new ArrayList<>();
            List<BankReconciliation> continueList = new ArrayList<>();
            List<BankReconciliation> endList = new ArrayList<>();
            List<BankReconciliation> nohandlerList = new ArrayList<>();
            //todo 是否阻断是需要都辨识匹配规则的，根据规则设置来进行分类
            BankreconciliationIdentifyType bankreconciliationIdentifyType = bankIdentifyService.queryIdentifyTypeByType(bankIdentifyType.getValue());
            boolean stoptag =bankreconciliationIdentifyType.getStoptag();
            for (BankReconciliation bankReconciliation : bankReconciliationList) {
                if(ObjectUtils.isNotEmpty(bankReconciliation.getIspublish()) && BooleanUtils.isTrue(bankReconciliation.getIspublish())){
                    endList.add(bankReconciliation);
                }else if(bankReconciliation.isNeedRollback()){
                    errorList.add(bankReconciliation);
                }else {
                    continueList.add(bankReconciliation);
                }
            }
            if (CollectionUtils.isNotEmpty(endList)){
                resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus(),endList);
            }
            if (CollectionUtils.isNotEmpty(errorList)){
                resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus(),errorList);
            }
            if (CollectionUtils.isNotEmpty(continueList)){
                resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(),continueList);
            }
        } catch (Exception e){
            log.error("发布辨识规则 返回数据分类处理错误！",e);
        }
        return resultMap;
    }
}
