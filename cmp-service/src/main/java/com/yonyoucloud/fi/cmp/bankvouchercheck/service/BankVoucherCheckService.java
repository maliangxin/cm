package com.yonyoucloud.fi.cmp.bankvouchercheck.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountInfoQueryVO;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountInfoVO;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankVoucherInfoQueryVO;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.reconciliation.ReconciliationMatchRecord;

import java.util.List;
import java.util.Set;

/**
 * @description: 银企对账工作台相关接口
 * 需求文档地址 https://docs.yonyoucloud.com/l/85D31fcd154B
 * @author: wanxbo@yonyou.com
 * @date: 2025/2/13 11:22
 */

public interface BankVoucherCheckService {

    /**
     * 根据对账数据源获取用户所有的对账方案关联对账组织
     * @param reconciliationDataSource 对账数据源 1凭证；2日记账
     * @return 对账组织id合集
     * @throws Exception
     */
    List<String> getReconciliationSchemeAccentityList(Short reconciliationDataSource) throws Exception;

    /**
     * 根据对账数据源获取用户所有的对账方案关联对账组织,对账组织名称
     * @param reconciliationDataSource 对账数据源 1凭证；2日记账
     * @return 对账组织id，对账组织名称合集
     * @throws Exception
     */
    List<CtmJSONObject> getAccentityListInfo(Short reconciliationDataSource) throws Exception;

    /**
     * 根据对账概览关键信息，查询对账方案关联的银行账户+币种+对账组织 维度信息
     * @param bankAccountInfoQueryVO 查询入参
     * @return 银行账户+币种+对账组织 维度信息合集
     * @throws Exception
     */
    List<BankAccountInfoVO> queryBankAccountInfo(BankAccountInfoQueryVO bankAccountInfoQueryVO) throws Exception;

    /**
     * 根据对账概览关键信息，查询未勾对凭证信息
     * @param bankVoucherInfoQueryVO 查询入参
     * @return 凭证信息合集
     * @throws Exception
     */
    List<Journal> getVoucherByBankAccountInfo(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception;

    /**
     * 根据对账概览关键信息，查询未勾对银行日记账信息
     * @param bankVoucherInfoQueryVO 查询入参
     * @return 凭证信息合集
     * @throws Exception
     */
    List<Journal> getJournalByBankAccountInfo(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception;

    /**
     * 根据对账概览关键信息，查询未勾对银行流水信息
     * @param bankVoucherInfoQueryVO 查询入参
     * @return 银行流水信息合集
     * @throws Exception
     */
    List<BankReconciliation> getBankReconciliationByBankAccountInfo(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception;

    /**
     * 根据对账概览关键信息，查询余额调节表状态
     * @param bankVoucherInfoQueryVO 查询入参
     * @return 余额调节表状态 0未生成；1余额已平；2余额未平
     * @throws Exception
     */
    BalanceAdjustResult getBalanceStatus(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception;

    /**
     * 快速对账-点击开始对账后台功能
     * @param param
     * reconciliationSeqNo:快速对账批次号-用来记录对账进度,
     * checkEndDate:对账截止日期,
     * accentityList:对账组织,
     * bankaccountList:银行账户,
     * banktypeList:银行类别,
     * currencyList:币种,
     * reconciliationSchemeList:对账方案,
     * tranDate:交易日期开始时间,
     * businessDate:业务日期开始时间*
     * @return 对账结果
     * @throws Exception
     */
    CtmJSONObject quickReconciliation(CtmJSONObject param) throws Exception;

    /**
     * 自动对账-点击银企对账工作台step2，开始对账后的后台功能
     * @param param 自动对账参数
     * reconciliationSeqNo:快速对账批次号-用来记录对账进度,
     * data：需要对账的银行账户+币种+对账方案信息
     * @return 对账结果
     * @throws Exception
     */
    CtmJSONObject autoReconciliation(CtmJSONObject param) throws Exception;

    /**
     * 根据reconciliationSeqNo查询进度条数据：-通义灵码辅助实现
     * @param reconciliationSeqNo 前端对账批次号
     * @return 对账进度条数据
     */
    CtmJSONObject getReconciliationProgress(String reconciliationSeqNo);

    /**
     * 根据对账概览关键信息，查询已勾对关系记录
     * @param bankVoucherInfoQueryVO 筛选条件
     * @return 勾对关系记录
     * @throws Exception
     */
    List<ReconciliationMatchRecord> queryReconciliationMatchRecord(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception;
}
