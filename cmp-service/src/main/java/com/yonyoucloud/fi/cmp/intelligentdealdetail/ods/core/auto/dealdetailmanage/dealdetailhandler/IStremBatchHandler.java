package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler;

import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
/**
 * @Author guoyangy
 * @Date 2024/6/18 13:35
 * @Description 流水批处理规则
 * @Version 1.0
 */
public interface IStremBatchHandler {
    Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) throws Exception;
    default boolean preHandler(BankDealDetailContext context, BankreconciliationIdentifyType rule){
        //step1:如果待处理流水为空，则不进行规则调用
        if(CollectionUtils.isEmpty(context.getWrappers())){
            return false;
        }
        //step2:当前执行的规则编码记录到上下文
        context.setCurrentRule(rule);
        context.setBankReconciliationCountInCurrentRule(context.getWrappers().size());
        //setp3:如果是流程处理，将流水processstatus状态改为"某个流程开始处理"
        this.batchUpdateProcessstatus(context);
        return true;
    }
    /**
     * 流水流程处理，修改状态为开始处理
     * */
    void batchUpdateProcessstatus(BankDealDetailContext context);
    default void postHandler(BankDealDetailContext context){
       //step1:清理上下文
       context.setPreRule(context.getCurrentRule());
       context.setCurrentRule(null);
       context.setCurrentRuleUsedTime(null);
       context.setBankReconciliationCountInCurrentRule(null);
       //step2:移动"已终结"流水到processedDealDetails集合
       DealDetailUtils.removeDealDetailFromContext(context);
    }
}