package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankautocheckconfig.BankAutoCheckConfig;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.reconciliate.vo.ReconciliationInfoVO;
import com.yonyoucloud.fi.cmp.reconciliation.ReconciliationMatchRecord;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2022/9/29 10:49
 */

public interface BankBillSmartCheckService {

    /**
     * 智能对账，自动对账任务
     * @param param 调度任务配置的入参
     * @return
     */
    boolean smartCheck(CtmJSONObject param) throws Exception;

    /**
     * 根据财资统一对账码（凭证/日记账为银行对账编码字段；银行对账单为smartcheckno字段）
     *
     * @param sourceType 勾对类型：1财资统一对账码；2银行对账编码
     * @param journals 存储勾兑成功的日记账
     * @param banks 存储勾兑成功的银行对账单
     * @param bankReconciliationList 未勾兑的银行对账单集合
     * @param journalList 未勾兑的日记账集合
     * @param settingId 银行对账设置ID
     * @param reconciliationdatasource 数据来源 1凭证；2日记账
     * @return 勾对结果统计,勾对关系记录
     *
     */
    List<ReconciliationInfoVO> handleBySmartCheckNo(Integer sourceType, List<Journal> journals, List<BankReconciliation> banks, List<BankReconciliation> bankReconciliationList,
                                      List<Journal> journalList, Long settingId, Integer reconciliationdatasource );

    /**
     * 处理同一个勾对号下的对账关系记录
     * @param journalList 凭证或银行日记账集合
     * @param bankList 银行流水集合
     * @param reconciliationBasisType 对账依据
     * @param bankAutoCheckConfig 自动对账设置
     * @param bankreconciliationscheme 对账方案
     * @param reconciliationdatasource 对账数据源 1凭证；2银行日记账
     * @param checkno 勾对号
     * @return 勾对关系记录
     */
    ReconciliationInfoVO handleReconciliationInfoVO(List<Journal> journalList, List<BankReconciliation> bankList, Short reconciliationBasisType, BankAutoCheckConfig bankAutoCheckConfig,
                                                    String bankreconciliationscheme, Integer reconciliationdatasource, String checkno, Date checkDate);

    /**
     * 调用总账list2接口，获取凭证列表
     * @param filterArgs 查询入参
     * @return 分页数据
     */
    Pager reqVoucheList2(String filterArgs) throws Exception;

    /**
     * 自动对账逻辑
     * @param bankReconciliationList 待匹配的银行流水号
     * @param journalList 待匹配的日记账/凭证数据
     * @param settingId 银行对账设置ID
     * @param reconciliationdatasource 数据源 1凭证，2银行日记账
     * @param bankAutoCheckConfig 自动对账设置
     * @return 返回对账匹配结果统计
     * @throws Exception
     */
    CtmJSONObject handleAutoCheckAutomaticRules(List<BankReconciliation> bankReconciliationList, List<Journal> journalList,Long settingId,Integer reconciliationdatasource,BankAutoCheckConfig bankAutoCheckConfig) throws Exception;

    /**
     * 按关键要素匹配对账，具体实现
     * @param journals 已勾对的银行日记账或者凭证
     * @param banks 已勾对的银行流水
     * @param bankReconciliationList 待匹配的银行流水
     * @param journalList 待匹配的日记账/凭证数据
     * @param settingId 银行对账设置ID
     * @param reconciliationdatasource 数据源 1凭证，2银行日记账
     * @param bankAutoCheckConfig 自动对账设置
     * @return
     * @throws Exception
     */
    List<ReconciliationInfoVO> handleByAutomaticRulesCheckFactor(List<Journal> journals,List<BankReconciliation> banks,List<BankReconciliation> bankReconciliationList,
                                                                        List<Journal> journalList,Long settingId,Integer reconciliationdatasource,BankAutoCheckConfig bankAutoCheckConfig) throws  Exception;

    /**
     * 现金调用凭证勾对/取消勾对接口
     * @param journals
     * @param seqNo
     * @throws Exception
     */
    void batchUpdateCheckFlag(List<Journal> journals,String seqNo) throws Exception;

    /**
     * 处理凭证或者日记账跟银行流水的勾对/取消勾对
     * @param journals
     * @param banks
     * @param reconciliationdatasource
     * @throws Exception
     */
    void handleJournalAndBankCheck( List<Journal> journals,List<BankReconciliation> banks,Integer reconciliationdatasource) throws Exception;

    /**
     * 保存勾对关系记录；将勾对关系记录到表cmp_reconciliation_match_record中
     * @param reconciliationInfoVOList 凭证和银行日记账的勾对关系
     * @throws Exception
     */
    void saveReconciliationMathRecord(List<ReconciliationInfoVO> reconciliationInfoVOList) throws Exception;

    /**
     * 根据勾对号查询已勾对关系记录
     * @param checknoList 勾对号集合
     * @return 勾对关系记录
     * @throws Exception
     */
    List<ReconciliationMatchRecord> queryReconciliationMatchRecord(List<String> checknoList) throws Exception;

    /**
     * 删除勾对关系记录
     * @param recordList 勾对关系记录
     * @throws Exception
     */
    void deleteReconciliationMatchRecord(List<ReconciliationMatchRecord> recordList) throws Exception;

    /**
     * 封存勾对关系记录
     * @param recordList 勾对关系记录
     * @param sealFlag 是否封存
     * @throws Exception
     */
    void sealReconciliationMatchRecord(List<ReconciliationMatchRecord> recordList,boolean sealFlag) throws Exception;

    /**
     * 根据datasource和对应的数据idList查询勾对记录信息
     * datasource 1凭证;2银行日记账;3银行流水*
     * @param param 查询参数 {"dataSource":1, "idList":[1,2]}
     * @return 查询信息集合 {"data":recordInfoList}
     * @throws Exception
     */
    List<Map<String, Object>> queryReconciliationRecordInfo(CtmJSONObject param) throws Exception;
}
