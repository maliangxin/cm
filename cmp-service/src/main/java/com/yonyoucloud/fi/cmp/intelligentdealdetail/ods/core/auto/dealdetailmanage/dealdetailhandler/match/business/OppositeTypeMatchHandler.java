package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business;

import com.google.common.collect.ImmutableMap;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.intelligentIdentification.IntelligentIdentificationManager;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultStreamBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyService;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author guoyangy
 * @Date 2024/6/18 15:33
 * @Description 对方信息辨识
 * @Version 1.0
 */
@Service(RuleCodeConst.SYSTEM004)
@Slf4j
public class OppositeTypeMatchHandler extends DefaultStreamBatchHandler {

    @Autowired
    BillSmartClassifyService billSmartClassifyService;

    @Autowired
    IntelligentIdentificationManager intelligentIdentificationManager;


    @Override
    public Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) {
        if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect())){
            return ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), getBankReconciliationList(context));
        }
        Map<String, List<BankReconciliation>> resultMap = new HashMap<>();
        resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(),new ArrayList<>());
        context.setLogName(RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc());
        List<BankReconciliation> bankReconciliationList = this.getBankReconciliationList(context);
        //授权使用组织未确认，不能执行对方信息辨识
        List<BankReconciliation> identifyList = bankReconciliationList.stream().filter(b->{
            String confirmStatus = b.getConfirmstatus();
            if(!StringUtils.isEmpty(confirmStatus) && (ConfirmStatusEnum.Confirmed.getIndex().equals(confirmStatus) || ConfirmStatusEnum.RelationConfirmed.getIndex().equals(confirmStatus))){
                return true;
            }
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(b, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_ONE.getDesc(),context);
            this.addBankReconciliationToMap(b, DealDetailBusinessCodeEnum.SYSTEM004_04Y01.getCode(),DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(),resultMap);
            return false;
        }).collect(Collectors.toList());
        try {
            //todo 对方信息辨识
            if(!CollectionUtils.isEmpty(identifyList)){
                intelligentIdentificationManager.excuteIdentificate(identifyList,context);
            }
            //todo 三方信息辨识
        } catch (Exception e) {
            log.error("智能流水执行辨识异常：对方信息辨识错误"+e);
        } finally {
            resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus()).addAll(identifyList);
        }

        return resultMap;
    }
}
