package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core;

import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.queue.OdsConsumerArrayBlockingQueue;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common.CmpCheckRuleCommonProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
/**
 * @Author guoyangy
 * @Date 2024/7/23 15:58
 * @Description 流水处理基础类
 * @Version 1.0
 */
@Slf4j
public class ODSBasicInfoObject {
    @Autowired
    @Qualifier("busiBaseDAO")
    protected IYmsJdbcApi ymsJdbcApi;
    public void setYmsJdbcApi(IYmsJdbcApi ymsJdbcApi) {
        this.ymsJdbcApi = ymsJdbcApi;
    }
    @Resource
    protected OdsConsumerArrayBlockingQueue odsConsumerArrayBlockingQueue;
    @Resource(name="odsConsumerExecutorService")
    protected ExecutorService odsConsumerExecutorService;

    //只有状态为非完结的数据才会被返回
    public List<BankReconciliation> getBankReconciliationList(BankDealDetailContext context) {
        List<BankReconciliation> bankReconciliationList = new ArrayList<>();
        context.getWrappers().stream().forEach(e -> {
            bankReconciliationList.add(e.getBankReconciliation());
        });
        return bankReconciliationList;
    }

}