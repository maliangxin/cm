package com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service;

import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;

import java.util.List;
import java.util.Map;


/**
 * @Author maliangn
 * @Date 2024/6/29
 * @Description 银行对账单相关公共逻辑
 * @Version 1.0
 */
public interface IBankReconciliationCommonService {
    /**
     * 执行提前入账辨识
     * @param bankReconciliationList
     * @throws Exception
     */
    List<BankReconciliation>  executeIdentificationAdvanceEnterAccount(List<BankReconciliation> bankReconciliationList, String ruleType, Map<String,String> ruleCodes, BankDealDetailContext context) throws Exception;


    /**
     * 插入银行交易流水 发布处理记录子表信息 *
     * @param bankReconciliation
     * @param action
     * @param returnReason
     * @throws Exception
     */
    void insertBankreconciliationDetailNew(BankReconciliation bankReconciliation, String action, String returnReason,Long claimid) throws Exception;

    /**
     * 插入银行交易流水 发布处理记录子表信息,处理手工发布到用户，角色，部门，人员
     * @param bankReconciliation
     * @param params
     * @throws Exception
     */
    void handlePublishToOthers(BankReconciliation bankReconciliation,Short publishedType,Map<String, Object> params) throws Exception;

    /**
     * 更新发布处理记录子表信息*
     * @param bankReconciliation
     * @throws Exception
     */
    void updateBankreconciliationDetail(BankReconciliation bankReconciliation) throws Exception;

    /**
     * 相关性规则执行*
     * @param list
     * @param ruleType
     * @param ruleCode
     * @throws Exception
     */
//    void commonExecuteRule(List<BankReconciliation> list, String ruleType, String ruleCode) throws Exception;

    /**
     * 重新查询银行对账单数据*
     * @param list
     * @return
     * @throws Exception
     */
    List<BankReconciliation> getRuleAfterBankData(List<BankReconciliation> list) throws Exception;


    /**
     * 获取匹配辨识规则编码
     */
    Map<String,String> getRuleCodes(String ruleType) throws Exception;

    /**
     * 获取匹配辨识规则编码
     */
    Map<Short,Map<Integer, BankreconciliationIdentifySetting>> getRuleByOrder(String ruleType) throws Exception;
}
