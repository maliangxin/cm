package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler;

import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;

import java.util.List;
import java.util.Map;

public interface IBankDealDetailChain {
    /**
     * 执行辨识匹配主流程处理
     *
     * @param context 待处理流水上下文
     * @param chain   责任链模式
     */
    void handle(BankDealDetailContext context, IBankDealDetailChain chain);

    /**
     * 规则均执行完成后，针对未处理流水做最后处理
     *
     * @param context 流水上下文
     */
    void flush(BankDealDetailContext context);

    /**
     * 辨识匹配规则执行完成后对返回结果做处理
     *
     * @param context   上下文
     * @param resultMap 规则执行后反参 key {@link DealDetailEnumConst.ExecuteStatusEnum}
     * @param rule      当前执行的具体规则
     */
    void processResult(BankDealDetailContext context, Map<String, List<BankReconciliation>> resultMap, BankreconciliationIdentifyType rule);

    /**
     * 辨识匹配规则执行完成后对返回结果做处理
     *
     * @param context     上下文
     * @param resultMap   规则执行后反参 key {@link DealDetailEnumConst.ExecuteStatusEnum}
     * @param rule        当前执行的具体规则
     * @param isErrorRule
     * @param e
     */
    void processResult(BankDealDetailContext context, Map<String, List<BankReconciliation>> resultMap, BankreconciliationIdentifyType rule, boolean isErrorRule, Exception e);

    /**
     * 辨识匹配规则执行完成后构建流水执行流程处理
     *
     * @param context     辨识匹配规则上下文
     * @param processList 流程处理器将要处理流水集合
     */
    default List<BankDealDetailWrapper> getExecutorProcessDealDetail(BankDealDetailContext context, List<BankReconciliation> processList) {
        return null;
    }

    /**
     * 某个辨识匹配规则执行完设置流水的processsatus和executestatus
     *
     * @param rule               当前规则
     * @param executeStatus      规则执行后执行结果状态值
     * @param bankReconciliation 流水
     */
    void setBankReconciliationExecuteStatusAndProcessStatus(BankreconciliationIdentifyType rule, String executeStatus, BankReconciliation bankReconciliation);

    List<BankReconciliation> handleBankReconciliationToDB(BankDealDetailContext context, List<BankReconciliation> addOrUpdateReconciliations);

    /**
     * 待处理流水分类，分成更新和新增两类集合
     *
     * @param context
     * @param saveBankReconciliationList         待新增流水
     * @param updateBankReconciliationList       待更新流水
     * @param saveOrUpdateBankReconciliationList 全量流水
     */
    void prepareBankReconciliationSaveOrUpdate(BankDealDetailContext context, List<BankReconciliation> saveBankReconciliationList, List<BankReconciliation> updateBankReconciliationList, List<BankReconciliation> saveOrUpdateBankReconciliationList);

    /**
     * 流水持久化处理前拼接ods更新参数SQLParameter和构建执行过程实体
     *
     * @param context
     * @return map <p>
     * <p>key</p>
     * {@link com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst#RULEPARAM}<p>
     * {@link com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst#ODSPARAM}
     */
    <T> Map<String, List<? extends Object>> bulidODSSQLParameterAndprocessingModelList(BankDealDetailContext context);

    /**
     * 加载辨识匹配规则大类<p>
     * 先读缓存在读db,规则基于组户级缓存，需要维护缓存刷新机制<p>
     */
    List<BankreconciliationIdentifyType> loadStreamIdentifyMatchRule(Integer index);

    /**
     * 加载辨识匹配规则大类<p>
     * 指定加载哪些辨识规则，指定后，其他的规则不再执行，即规则可配置
     */
    List<BankreconciliationIdentifyType> loadStreamIdentifyMatchRuleByCode(Integer index);
}