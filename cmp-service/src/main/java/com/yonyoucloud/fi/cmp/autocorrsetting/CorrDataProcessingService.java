package com.yonyoucloud.fi.cmp.autocorrsetting;

import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理需要关联的数据
 */
public interface CorrDataProcessingService {
    /**
     * 返回自动关联数据
     * 数据格式： [{"bankReconciliation" : bankReconciliation,"fundPayment": fundPayment,"fundCollection": fundCollection,"isAuto": isAuto,"isPayment": isPayment}......]
     * @return
     */
    List<CorrDataEntity> autoAssociatedData(List<BankReconciliation> bankReconciliationList) throws Exception;

    /**
     * 返回手动关联数据
     * 数据格式： [{"bankReconciliation" : bankReconciliation,"fundPayment": fundPayment,"fundCollection": fundCollection,"isAuto": isAuto,"isPayment": isPayment}......]
     * @param isClaim
     * @param billType
     * @param bid
     * @param busid
     * @param pubts
     * @param pubts1
     * @return
     */
    List<CorrDataEntity> manualAssociatedData(String isClaim, String billType, Long bid, Long busid, Date pubts, Date pubts1, Short dcFlag) throws Exception;

    /**
     * 处理资金结算单数据
     * @param map
     * @return
     */
    List<CorrDataEntity> setAssociatedData(CtmJSONObject map) throws Exception;

    /**
     * 使用API接口数据生成关联数据，只用于银行对账单
     * @return
     */
    HashMap<String,List<CorrDataEntity>> apiAssociatedData(BankReconciliation bankReconciliation, CtmJSONObject busrelations) throws Exception;

    void buildCorrDataEntiry(CorrDataEntity entity,
                                    boolean auto, String billType, Long bankReconciliationId, Long busiId, String accentity,
                                    String code, String billNum, String dept, Long mainId, String project,
                                    Date vouchdate, BigDecimal oriSum ,String smartCheckNo);

    /**
     * 初始化自动关联设置；会存在租户初始化数据为空的情况，需要添加初始化数据
     * @param tenant 租户信息
     * @return
     */
    CtmJSONObject initAutoCorrSetting(Tenant tenant) throws Exception;

    /**
     * 查询符合和银行流水自动关联的货币兑换数据，并组装成关联关系记录数据
     * @param bankReconciliationList 银行流水
     * @return 关联关系记录数据
     */
    List<CorrDataEntity> autoCorrCurrencyExchangeData(List<BankReconciliation> bankReconciliationList, Map<String, Object> paramMap) throws Exception;
}
