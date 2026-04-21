package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultBankDealDetailChain;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * @Author guoyangy
 * @Date 2024/6/18 9:26
 * @Description 去重后流水处理入口类
 * @Version 1.0
 */
public interface IDealDetailProcessing {
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = RuntimeException.class)
    void dealDetailProcesing(List<BankDealDetailWrapper> bankDealDetailWrappers, List<BankReconciliation> updateBankReconciliationList, DefaultBankDealDetailChain bankDealDetailChain,String traceId,String requestSeqNo);
    /**
     *流水辨识匹配、流程处理入口方法<p>
     * 1)构建流水上下文BankDealDetailContext<p>
     * 2)加载规则;<p>
     * 3)规则加载到上下文<p>
     * 4) 通过执行规则器执行具体规则<p>
     * @param bankDealDetailWrappers
     * @param updateBankReconciliationList 流水库已存在的流水
     * */
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = RuntimeException.class)
    void dealDetailProcesing(List<BankDealDetailWrapper> bankDealDetailWrappers,List<BankReconciliation> updateBankReconciliationList,DefaultBankDealDetailChain bankDealDetailChain,Integer index,String traceId,String requestSeqNo);
}