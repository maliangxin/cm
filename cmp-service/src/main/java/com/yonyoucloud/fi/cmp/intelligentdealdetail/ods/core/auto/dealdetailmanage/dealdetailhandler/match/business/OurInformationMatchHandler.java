package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business;

import com.google.common.collect.ImmutableMap;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.enums.OrgConfirmBillEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IBankReconciliationCommonService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultStreamBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author guoyangy
 * @Date 2024/6/18 15:33
 * @Description 本方信息匹配规则
 * @Version 1.0
 */
@Service(RuleCodeConst.SYSTEM003)
@Slf4j
public class OurInformationMatchHandler extends DefaultStreamBatchHandler {
    @Autowired
    IBankReconciliationCommonService bankReconciliationCommonService;
    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) {
        long s0 = System.currentTimeMillis();
        //step1:构建反参
        Map<String, List<BankReconciliation>> resultMap = this.prepareResult();
        try {
            //step2:获取待处理流水
            List<BankReconciliation> bankReconciliationList = this.getBankReconciliationList(context);
            //step3:幂等校验，过滤掉已确认流水
            List<BankReconciliation> waitConfirmBankReconciliationList = this.checkIdempotent(bankReconciliationList, resultMap);
            if (CollectionUtils.isEmpty(waitConfirmBankReconciliationList)) {
                return resultMap;
            }
            context.setLogName(RuleLogEnum.RuleLogProcess.OUR_INFORMATION_NAME.getDesc());
            context.setOperationName(RuleLogEnum.RuleLogProcess.OUR_INFORMATION_START.getDesc());
            //step4:待确认流水执行本方信息辨识逻辑
            this.executeIdentificationBankAccountOfOrg(waitConfirmBankReconciliationList, resultMap, context);
            //银企联拉取的则直接辨识退出
            if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect())) {
                context.setSaveDirect(DealDetailEnumConst.SAVE_DIRECT_FINISH);
                return ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus(), waitConfirmBankReconciliationList);
            }
            log.error("【本方信息匹配】一个批次=======================执行完成,包含{}条流水明细,匹配银行回单共耗时{}s",  CollectionUtils.isEmpty(bankReconciliationList) ? "0" : bankReconciliationList.size(), (System.currentTimeMillis() - s0) / 1000.0);

        } catch (Exception e) {
            log.error("智能流水执行辨识异常：本方信息辨识失败,构建反参异常", e);
        }

        return resultMap;
    }

    @Override
    public List<BankReconciliation> checkIdempotent(List<BankReconciliation> bankReconciliationList, Map<String, List<BankReconciliation>> resultMap) {
        /**
         * 筛除掉"已确认"流水
         * */
        List<BankReconciliation> confirmedBankReconciliationList = new ArrayList<>();
        try {
            //筛除掉"已确认"流水
            bankReconciliationList = bankReconciliationList.stream().filter(b -> {
                if (ConfirmStatusEnum.Confirmed.getIndex().equals(b.getConfirmstatus()) || ConfirmStatusEnum.RelationConfirmed.getIndex().equals(b.getConfirmstatus())) {
                    confirmedBankReconciliationList.add(b);
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
            //已确认过的流水
            if (!CollectionUtils.isEmpty(confirmedBankReconciliationList)) {
                this.addBankReconciliationToMap(confirmedBankReconciliationList, DealDetailBusinessCodeEnum.SYSTEM003_03Y01.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap);
            }
        } catch (Exception e) {
            log.error("本方信息辨识失败,筛除掉已确认流水异常", e);
            this.addBankReconciliationToMap(bankReconciliationList, DealDetailBusinessCodeEnum.SYSTEM003_03Y02.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap);
        }
        return bankReconciliationList;
    }

    /**
     * 检查组织的银行账户
     * 授权使用组织&所属组织&确认状态赋值逻辑
     * 所属组织 = 账户的所属组织
     * 1，授权使用组织有值
     * （1）授权使用组织在银行账户适用范围内，导入成功，授权使用组织 = 录入的授权使用组织，确认状态 = 已确认
     * （2）授权使用组织不在银行账户适用范围内，导入失败，给提示信息
     * 2，授权使用组织无值
     * （1）银行账户适用范围只有一条数据，导入成功，授权使用组织 = 适用范围内的组织，确认状态 = 已确认
     * （2）银行账户使用范围有多条数据，导入成功，授权使用组织 = null，确认状态 = 未确认
     *
     * @param bankReconciliationList 银行对账单业务对象
     * @throws Exception 异常
     */
    public void executeIdentificationBankAccountOfOrg(List<BankReconciliation> bankReconciliationList, Map<String, List<BankReconciliation>> resultMap, BankDealDetailContext context) {
        /**
         * step1:根据流水上银行账号查询企业银行账号信息
         * */
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVoWithRanges = null;
        try {
            EnterpriseParams params = new EnterpriseParams();
            List<String> bankaccountIds = new ArrayList<>();
            //账户授权使用组织有值，则不需要进行本方信息辨识
            bankReconciliationList.stream().forEach(e -> {
                if (StringUtils.isEmpty(e.getAccentity())) {
                    bankaccountIds.add(e.getBankaccount());
                }
            });
            if (CollectionUtils.isNotEmpty(bankaccountIds)){
                params.setIdList(bankaccountIds);
                enterpriseBankAcctVoWithRanges = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeByCondition(params);
            }
        } catch (Exception e) {
            log.error("本方信息辨识失败,查询企业银行账户信息水异常", e);
            this.addBankReconciliationToMap(bankReconciliationList, DealDetailBusinessCodeEnum.SYSTEM003_03S01.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap);
            return;
        }
        /**
         * step2:针对待确认流水，匹配企业银行账户信息
         * */
        for (BankReconciliation bizObject : bankReconciliationList) {
            // 账户所属组织
            String orgid = bizObject.getOrgid();
            // 获取授权使用组织编码
            // 获取银行账户
            String bankaccount = bizObject.getBankaccount();
            boolean remark = false;
            // 查询企业银行账户信息的授权使用组织范围
            try {
                if (enterpriseBankAcctVoWithRanges == null || enterpriseBankAcctVoWithRanges.isEmpty()){
                    //CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bizObject, context.getLogName(), "本方信息不为空不需要辨识！", context);
                    this.addBankReconciliationToMap(bizObject, DealDetailBusinessCodeEnum.SYSTEM003_03Y03.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap);
                }
                for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange : enterpriseBankAcctVoWithRanges) {
                    if (enterpriseBankAcctVoWithRange.getId().equals(bankaccount)) {
                        List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange != null ? enterpriseBankAcctVoWithRange.getAccountApplyRange() : Collections.emptyList();
                        // 获取范围内的组织ID列表
                        List<String> rangeOrgIds = orgRangeVOS.stream().map(OrgRangeVO::getRangeOrgId).collect(Collectors.toList());
                        // 如果有一个组织则赋上默认值
                        remark = true;
                        if (rangeOrgIds.size() == 1) {
                            // 设置为待确认
                            bizObject.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
                            bizObject.setAccentity(rangeOrgIds.get(0));
                            bizObject.setConfirmbill(OrgConfirmBillEnum.CMP_BANKRECONCILIATION.getIndex());
                            this.addBankReconciliationToMap(bizObject, null, DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap);
                            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bizObject, context.getLogName(), RuleLogEnum.RuleLogProcess.OUR_INFORMATION_ONE.getDesc(), context);
                        } else {
                            // 设置为待确认
                            bizObject.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
                            bizObject.setConfirmbill(null);
                            this.addBankReconciliationToMap(bizObject, DealDetailBusinessCodeEnum.SYSTEM003_03Y03.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap);
                            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bizObject, context.getLogName(), DealDetailBusinessCodeEnum.SYSTEM003_03Y03.getDesc(), context);
                        }
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bizObject, context.getLogName(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getDesc(), context);
                    }
                }
                if (!remark){
                    this.addBankReconciliationToMap(bizObject, DealDetailBusinessCodeEnum.SYSTEM003_03Y02.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus(), resultMap);
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bizObject, context.getLogName(), DealDetailBusinessCodeEnum.SYSTEM003_03S01.getDesc(), context);
                    log.error(String.format("流水[%s]本方信息辨识失败:%s"),bizObject.getBank_seq_no(),"多使用组织未查询到对应的企业银行账号！");
                }
            } catch (Exception e) {
                log.error("流水本方信息辨识失败_{}", bizObject.getId(), e);
                this.addBankReconciliationToMap(bizObject, DealDetailBusinessCodeEnum.SYSTEM003_03S02.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap);
            }
        }
    }
}