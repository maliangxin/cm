package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao;
import com.yonyou.iuap.yms.param.SQLParameter;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.flowhandlesetting.Flowhandlesetting;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSFailModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailOperLogModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.model.DealDetailRuleExecRecord;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * @Author guoyangy
 * @Date 2024/7/23 17:23
 * @Description 流水接入Dao层
 * @Version 1.0
 */
public interface IBankDealDetailAccessDao {

    /**
     * 按concatInfo和processstauts=2查询流水总数量
     * */
    Long getBankReconciliationTotalCountByProcessstatus(String processstatus);
    List<Long> getBankReconciliationIdByProcessstatus(String processstatus);

    /**
     * 按照concatinfo+processstatus=2 分页查询流水
     * */
    List<BankReconciliation> getBankReconciliationByPage(String concatInfo,int pageSize) throws Exception;
    List<BankReconciliation> getBankReconciliationByPage(Short processstatus,List<Long> bankReconciliationIds) throws Exception;

    /**
     * @desc 根据流水唯一编码批量查询ODS流水
     * */
    List<BankDealDetailODSModel> batchQueryODSByContentSign(Set<String> contentSignatureList);
    /**
     * @desc 根据流水主键查询ODS流水
     * */
    List<BankDealDetailODSModel> queryODSByMainid(Long mainid);
    List<BankDealDetailODSModel> batchQueryODSByMainid(List<Long> mainids);
    /**
     * @desc 流水入库
     * @param dealDetailOperLogModel 入操作日志库
     * @param odsDealDetails         入ods库
     * @param exceptionDealDetails   入异常库
     * */
    void dealDetailInsert(BankDealDetailOperLogModel dealDetailOperLogModel, List<BankDealDetailODSModel> odsDealDetails, List<BankDealDetailODSFailModel> exceptionDealDetails);
    void dealDetailInsertAndUpdate(List<BankDealDetailODSModel> odsDealDetails, List<BankDealDetailODSModel> updateDealDetails,int processstatus,String traceId,String requestSeqNo);
        /**
         * 批量修改【流水表】流水状态
         * */
    void batchUpdateDealDetailProcessstatusToInitByIdsWithoutPubts(int processstatus, List<BankReconciliation> updateBankReconciliationList);
    /**
     * 批量修改ods状态改成处理中
     * */
    void batchUpdateODSprocessstatus(List<BankReconciliation> bankReconciliations,int odsprocessstatus);
    void batchUpdateODSprocessstatus(Map<String, List<BankReconciliation>> resultMap );
    /**
     * 银行流水批量更新
     * @param bankReconciliationList 银行对账单批量更新
     * */
    void batchUpdateBankReconciliation(List<BankReconciliation> bankReconciliationList);
    /**
     * 批量修改ods流水状态
     * */
    void batchUpdateOdsProcessstatusByIds(int processstatus,List<Object> odsList);

    void batchUpdateOdsProcessstatusByIds(String sql, List<SQLParameter> parameter);
    /**
     * 批量修改流水状态
     * */
    void batchUpdateDealDetailProcessstatusByIds(int processstatus,List<BankReconciliation> updateBankReconciliationList);

    /**
     * 交易明细批量入库
     * @param bankReconciliationList ods库里记录的银企联响应数据
     * */
    void batchConvertAndSaveBankDealDetail(List<BankReconciliation> bankReconciliationList);

    /**
     * 银行流水批量入库
     * @param bankReconciliationList 银行对账单批量入库
     * */
    void batchSaveBankReconciliation(List<BankReconciliation> bankReconciliationList);

    /**
     * 更新ods表和流水表的processstatus状态
     * */
    void updateProcessstatusOnlyPreHandle(Short processStatus, int odsProcessStatus, List<BankReconciliation> bankReconciliationList);

    /**
     * 更新流式和ods状态
     * */
    public void updateProcessstatus(List<SQLParameter> odsSqlParameters,List<SQLParameter> bankreconciliationSqlParameters);

    /**
     * 辨识匹配执行完成或者流程执行完成，流水入库或更新
     * */
    void doHandleBankReconciliationToDB(List<BankReconciliation> saveBankReconciliationList, List<BankReconciliation> updateBankReconciliationList, List<BankReconciliation> saveOrUpdateBankReconciliationList, List<DealDetailRuleExecRecord> dealDetailRuleExecRecords, List<SQLParameter> sqlParameters, String opertype,String currentRuleCode);
    void insertRuleExecRecords(List<DealDetailRuleExecRecord> dealDetailRuleExecRecords);
    void updateRuleExecRecords(List<DealDetailRuleExecRecord> dealDetailRuleExecRecords);
    /**
     * 根据traceid+requestseqno批量查询processstatus=0未开始的ods数据
     * */
    List<BankDealDetailODSModel> batchQueryODSByTranceidAndRequestseqnoAndodsstatus(String traceId,String requestNo,int odsstatus);
    /**
     * 根据traceid+requestseqno批量查询ods数据
     * */
    List<BankDealDetailODSModel> batchQueryODSByTranceidAndRequestseqno(String traceId,String requestNo);

    List<Long> batchQueryBankReconciliationByIdsAndProcessstatus(List<Long> ids,Short processstatus);

    /**
     * 根据流水id查询流水信息
     * */
    List<BankReconciliation> batchQueryBankReconciliationByIds(List<Long> ids);
    List<BankreconciliationIdentifyType> getBankreconciliationIdentifyTypeListByTenantId();

    /**
     * 根据流水主键id和processstatus批量获取流水
     * */
    List<BankReconciliation> getBankReconciliationByIdsAndprocessstatus(List<Long> ids,Short processstatus) throws Exception;

    void updateProcessAfterExcepton(List<BankReconciliation> saveBankReconciliationList,
                                    List<BankReconciliation>updateBankReconciliationList,
                                    List<BankReconciliation> needTODBList,
                                    BankDealDetailContext context,
                                    BankDealDetailException exception,
                                    BankreconciliationIdentifyType currentRule);

    /**
     * 根据主键查询流水
     * */
    List<BankReconciliation> getBankReconciliationById(Long id) throws Exception;

    List<DealDetailRuleExecRecord> queryDealDetailRuleExecRecordByMainid(List<Object> bankReconciliationIds);

    /**
     * 根据concat_info或uniqueno查询流水
     */
    List<BankReconciliation> queryExistBankReconciliations(String paramName, Set<String> conditionList) throws Exception;

    /**
     * 根据关联目标单据主键查询关联明细
     * */
    List<BankReconciliationbusrelation_b> queryBankReconciliationBusByBillIdAndBankReconciliationId(Long bankReconciliationId,Long billId);

    /**
     * 更新关联明细
     * */
    void updateBankReconciliationBusRelationStatus(Short relationStatus,Short relationType,Long bankReeconciliationId);
    /**
     * 根据关联目标单据主键查询关联明细
     * */
    List<BankReconciliationbusrelation_b> queryBankReconciliationBusByBillId(List<Long> billIdList);

    /**
     * 查询流水处理规则
     * */
    List<Flowhandlesetting> queryFlowhandlesetting(String flow_type,String enable,String object,String association_mode);

    void deleteOdsByTraceIdAndRequestSeqNo(String traceId, String requestSeqNo);

}