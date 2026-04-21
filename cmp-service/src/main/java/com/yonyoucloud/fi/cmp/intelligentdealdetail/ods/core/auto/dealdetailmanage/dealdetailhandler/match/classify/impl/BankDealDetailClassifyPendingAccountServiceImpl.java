package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.classify.impl;

import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.BankIdentifyTypeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.classify.IBankDealDetailClassifyService;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BankDealDetailClassifyPendingAccountServiceImpl implements IBankDealDetailClassifyService {

    @Autowired
    BankIdentifyService bankIdentifyService;

    @Override
    public Map<String, List<BankReconciliation>> classifyList(List<BankReconciliation> bankReconciliationList, BankIdentifyTypeEnum bankIdentifyType) {
        Map<String, List<BankReconciliation>> resultMap = new HashMap<>();
        try {
            List<BankReconciliation> errorList = new ArrayList<>();
            List<BankReconciliation> continueList = new ArrayList<>();
            List<BankReconciliation> endList = new ArrayList<>();
            //todo 是否阻断是需要都辨识匹配规则的，根据规则设置来进行分类
            BankreconciliationIdentifyType bankreconciliationIdentifyType = bankIdentifyService.queryIdentifyTypeByType(bankIdentifyType.getValue());
            boolean stoptag = bankreconciliationIdentifyType.getStoptag();
            for (BankReconciliation bankReconciliation : bankReconciliationList) {
                if (BooleanUtils.isTrue(bankReconciliation.getIsadvanceaccounts())) {
                    if (stoptag) {
                        endList.add(bankReconciliation);
                    } else {
                        continueList.add(bankReconciliation);
                    }
                } else if (bankReconciliation.isNeedRollback()) {
                    errorList.add(bankReconciliation);
                } else {
                    continueList.add(bankReconciliation);
                }
            }
            resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus(), endList);
            resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus(), errorList);
            resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), continueList);
        } catch (Exception e) {

        }
        return resultMap;
    }
}
