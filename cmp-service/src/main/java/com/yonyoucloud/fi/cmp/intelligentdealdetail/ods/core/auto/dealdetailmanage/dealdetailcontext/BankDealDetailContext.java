package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleCheckLog;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author guoyangy
 * @Date 2024/6/18 9:25
 * @Description 流水上下文
 * @Version 1.0
 */
@Data
public class BankDealDetailContext {
    private String traceId;
    private String requestSeqNo;
    //待处理流水包装类
    private List<BankDealDetailWrapper> wrappers;
    private Integer totalCountCurrentBatch;
    //已处理流水，已入库
    private List<BankDealDetailWrapper> processedDealDetails = new ArrayList<>();
    //记录下已存在的流水，后面做更新操作
    private List<BankReconciliation> updateBankReconciliationList;
    //当前批次流水需要执行的规则列表
    private List<BankreconciliationIdentifyType> bankDealDetailIdentifyMatchRules;
    //执行的当前规则
    private BankreconciliationIdentifyType currentRule;
    //记录上一个规则
    private BankreconciliationIdentifyType preRule;
    private String currentRuleUsedTime;
    //执行当前规则流水入参数量
    private Integer bankReconciliationCountInCurrentRule;
    private Map<String, CmpRuleCheckLog> cmpOnlyBankRuleCheckLogs = new HashMap<>();
    private String logName;
    private String operationName;
    private String resultSuccessLog;
    private String resultFailLog;
    private String saveDirect;
    private DefaultBankDealDetailChain bankDealDetailChain;
    private Map<String, String> paramsConfigs = new HashMap<>();


    public BankDealDetailContext() {
    }

    public BankDealDetailContext(List<BankDealDetailWrapper> wrappers, List<BankReconciliation> updateBankReconciliationList, String traceId, String requestSeqNo) {
        this.wrappers = wrappers;
        this.totalCountCurrentBatch = this.wrappers.size();
        this.updateBankReconciliationList = updateBankReconciliationList;
        this.traceId = traceId;
        this.requestSeqNo = requestSeqNo;
        String saveDirect = wrappers.get(0).getBankReconciliation().get(DealDetailEnumConst.SAVE_DIRECT) != null ? wrappers.get(0).getBankReconciliation().get(DealDetailEnumConst.SAVE_DIRECT).toString() : null;
        if (DealDetailEnumConst.SAVE_DIRECT.equals(saveDirect)) {
            this.saveDirect = DealDetailEnumConst.SAVE_DIRECT;
            //this.updateBankReconciliationList = null;
        }
        //若是指定规则，新增的话则需要指定已完成，这是因为新增的话需要先落库，只有SAVE_DIRECT_FINISH才能落库，SAVE_DIRECT状态是需要有本方信息辨识的
        if (DealDetailEnumConst.SAVE_AND_UPDATE_NOT_FINISH.equals(saveDirect)) {
            this.saveDirect = DealDetailEnumConst.SAVE_AND_UPDATE_NOT_FINISH;
        }
    }
}