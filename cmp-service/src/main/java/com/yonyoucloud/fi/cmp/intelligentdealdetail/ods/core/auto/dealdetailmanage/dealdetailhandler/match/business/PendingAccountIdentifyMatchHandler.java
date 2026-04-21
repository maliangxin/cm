package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business;

import com.google.common.collect.ImmutableMap;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankrecrule.ruleengine.imp.RuleStrategy;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IBankReconciliationCommonService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultStreamBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author guoyangy
 * @Date 2024/6/18 15:33
 * @Description 挂账辨识匹配规则
 * @Version 1.0
 */
@Service(RuleCodeConst.SYSTEM009)
@Slf4j
public class PendingAccountIdentifyMatchHandler extends DefaultStreamBatchHandler {

    @Autowired
    IBankReconciliationCommonService bankReconciliationCommonService;
    @Autowired
    BankIdentifyService bankIdentifyService;
    @Override
    public Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) {
        long s0 = System.currentTimeMillis();
        if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect())){
            return ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), getBankReconciliationList(context));
        }
        Map<String, List<BankReconciliation>> result = new HashMap<String, List<BankReconciliation>>();
        List<BankReconciliation> bankReconciliationList = this.getBankReconciliationList(context);
        try {
            //todo 查询发布辨识规则 中启用的规则，获取到规则集合 调用小波提供的接口（小波）
            Map<String,String> ruleCodes = new HashMap<>();
            try {
                ruleCodes = bankReconciliationCommonService.getRuleCodes(RuleCodeConst.SYSTEM009);
            } catch (Exception e) {
                log.error("SYSTEM009：挂账辨识规则设置查询异常",e);
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM009_09S02.getCode(),bankReconciliationList);
            }
            if (ObjectUtils.isEmpty(ruleCodes) || ruleCodes.size() < 1 ) {
                result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(),bankReconciliationList);
                log.error("SYSTEM009：未查询到可用的挂账辨识匹配规则");
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM009_09YO1.getCode(),bankReconciliationList);
                return result;
            }
            context.setLogName(RuleLogEnum.RuleLogProcess.PENDING_ACCOUNT_IDENTIFY_NAME.getDesc());
            context.setOperationName(RuleLogEnum.RuleLogProcess.PENDING_ACCOUNT_IDENTIFY_START.getDesc());
            // 前缀待确定（东方）
            List<BankReconciliation> bankReconciliationlist = bankReconciliationCommonService.executeIdentificationAdvanceEnterAccount(bankReconciliationList, RuleStrategy.CMP_EARLY_RECORD_PREFIX,ruleCodes,context);
            // 创建两个列表，一个用于存放executeStatusEnum为1的元素，另一个用于存放executeStatusEnum为3的元素
            List<BankReconciliation> status4List = new ArrayList<>();
            List<BankReconciliation> status3List = new ArrayList<>();
            List<BankReconciliation> successList = new ArrayList<>();
            List<BankReconciliation> nohandlerList = new ArrayList<>();
            // 遍历原始列表，根据executeStatusEnum的值将元素分配到对应的列表中
            for (BankReconciliation reconciliation : bankReconciliationlist) {
                if (ObjectUtils.isNotEmpty(reconciliation.get("executeStatusEnum")) && reconciliation.get("executeStatusEnum").equals("4")) {
                    status4List.add(reconciliation);
                } else if (ObjectUtils.isNotEmpty(reconciliation.get("executeStatusEnum")) && reconciliation.get("executeStatusEnum").equals("3")) {
                    status3List.add(reconciliation);
                } else if (ObjectUtils.isNotEmpty(reconciliation.get("pendingAccountSuccess")) && reconciliation.get("pendingAccountSuccess").equals("1")){
                    successList.add(reconciliation);
                } else {
                    nohandlerList.add(reconciliation);
                }
                // 注意：如果executeStatusEnum可能有其他值且需要处理，可以在这里添加额外的条件分支
            }
            status3List.addAll(nohandlerList);
            result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus(),status4List);
            result.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(),status3List);
            log.error("【挂账辨识】一个批次=======================执行完成,包含{}条流水明细,匹配银行回单共耗时{}s",  org.springframework.util.CollectionUtils.isEmpty(bankReconciliationList) ? "0" : bankReconciliationList.size(), (System.currentTimeMillis() - s0) / 1000.0);

        }catch (Exception e){
            log.error("智能流水执行辨识异常：挂账辨识匹配规则异常:{}",e);
        }
        return result;
    }
}
