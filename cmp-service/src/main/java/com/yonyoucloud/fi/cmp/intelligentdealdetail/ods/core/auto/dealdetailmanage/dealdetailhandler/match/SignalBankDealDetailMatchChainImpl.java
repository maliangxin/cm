package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author guoyangy
 * @Date 2024/6/18 16:32
 * @Description 流水处理过滤器链
 * @Version 1.0
 */
@Slf4j
@Service
public class SignalBankDealDetailMatchChainImpl extends BankDealDetailMatchChainImpl{
    public static SignalBankDealDetailMatchChainImpl get(){
        return new SignalBankDealDetailMatchChainImpl();
    }

    @Override
    public List<BankDealDetailWrapper> getExecutorProcessDealDetail(BankDealDetailContext context, List<BankReconciliation> processList) {
        return null;
    }
}