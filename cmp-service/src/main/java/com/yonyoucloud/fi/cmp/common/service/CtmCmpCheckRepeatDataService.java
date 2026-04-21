package com.yonyoucloud.fi.cmp.common.service;

import com.yonyou.yonbip.iuap.xport.importing.data.ImportBatchData;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import org.imeta.orm.base.BizObject;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 银企联下载数据验重
 * @author liuwtr
 * @version V1.0
 * @date 2023/12/13 16:59
 * @Copyright yonyou
 *
 */
public interface CtmCmpCheckRepeatDataService {

    /**
     *
     * @param downloadData 银企联下载数据
     * @param checkRepeatDataBillType 验重单据类型
     * @return
     */
    <T extends BizObject> List<T> checkRepeatData(List<T> downloadData, Short checkRepeatDataBillType) throws Exception;

    /**
     *
     * @param downloadData 银企联下载数据账户交易明细
     * @return
     */
    Map<String, List<BankDealDetail>> checkBankDealDetailRepeat(List<BankDealDetail> downloadData) throws Exception;


    String formatConctaInfoBankReconciliation(BankReconciliation bankReconciliation);

    String formatConctaInfoBankDealDetail(BankDealDetail bankDealDetail);

    void fillBankReconciliationConcatInfo(BankReconciliation bankRecord);

    void fillBankDealDetailConcatInfo(BankDealDetail bankDealDetail);

    /**
     * 4要素验重，更新返回流水状态、增加不存在流水进布隆过滤器
     * @param bankReconciliations 获取的流水集合
     * @param enterpriseInfo 查询流水的对象
     */
    void deal4FactorsBankDealDetail(List<BankReconciliation> bankReconciliations,Map<String, Object> enterpriseInfo) throws Exception;

    /**
     * 银行流水处理疑重判定
     * @param bankReconciliation 银行流水对象
     */
    void checkRepeatInfo(BankReconciliation bankReconciliation);

    /**
     * 检测当前批次流水是否有8要素重复
     * @param importBatchData 本批次数据
     */
    boolean checkCurrentBatchIsRepeate(ImportBatchData importBatchData);

}
