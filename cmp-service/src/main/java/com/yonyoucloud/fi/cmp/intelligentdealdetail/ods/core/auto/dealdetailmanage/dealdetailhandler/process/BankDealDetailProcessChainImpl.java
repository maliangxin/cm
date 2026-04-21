package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.process;

import com.google.common.collect.ImmutableMap;
import com.yonyou.iuap.yms.cache.YMSRedisTemplate;
import com.yonyou.iuap.yms.param.SQLParameter;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.model.DealDetailRuleExecRecord;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * @Author guoyangy
 * @Date 2024/6/18 16:32
 * @Description 流水处理过滤器链
 * @Version 1.0
 */
@Slf4j
@Service
public class BankDealDetailProcessChainImpl extends DefaultBankDealDetailChain{

    private List<String> codes = null;

    public void setCodes(List<String> codes) {
        this.codes = codes;
    }

    public static BankDealDetailProcessChainImpl get(){
        return new BankDealDetailProcessChainImpl();
    }
    /**
     * @param index 流程处理定义从哪个流程开始执行，从1开始<p>
     *              1:单据关联<p>
     *              2:凭据关联<p>
     *              3:生单<p>
     *              4:发布认领<p>
     * */
    @Override
    public List<BankreconciliationIdentifyType> loadStreamIdentifyMatchRule(Integer index) {
        if(null == index){
            return null;
        }
        List<BankreconciliationIdentifyType> typeList = new ArrayList<>();
        for(int i = (index-1); i< RuleCodeConst.ALLPROCESSRULE.size(); i++){
            BankreconciliationIdentifyType rule = DealDetailUtils.createRule(RuleCodeConst.ALLPROCESSRULE.get(i),RuleCodeConst.RULENAMELIST.get(i),i);
            typeList.add(rule);
        }
        log.error("加载流程处理规则,执行顺序{}",typeList.stream().map(BankreconciliationIdentifyType::getCode).collect(Collectors.toList()));
        return typeList;
    }

    @Override
    public List<BankreconciliationIdentifyType> loadStreamIdentifyMatchRuleByCode(Integer index) {
        if(null == index){
            return null;
        }
        List<BankreconciliationIdentifyType> typeList = new ArrayList<>();
        for(int i = (index-1); i< RuleCodeConst.ALLPROCESSRULE.size(); i++){
            BankreconciliationIdentifyType rule = DealDetailUtils.createRule(RuleCodeConst.ALLPROCESSRULE.get(i),RuleCodeConst.RULENAMELIST.get(i),i);
            if (codes != null && codes.contains(rule.getCode())){
                typeList.add(rule);
            }else {
                typeList.add(rule);
            }
        }
        log.error("加载流程处理规则,执行顺序{}",typeList.stream().map(BankreconciliationIdentifyType::getCode).collect(Collectors.toList()));
        return typeList;
    }


    @Override
    public List<BankReconciliation> handleBankReconciliationToDB(BankDealDetailContext context, List<BankReconciliation> saveOrUpdateBankReconciliationList) {
        //step1:区分更新流水、新增流水
        List<BankReconciliation> saveBankReconciliationList = new ArrayList<>();
        List<BankReconciliation> updateBankReconciliationList = new ArrayList<>();
        this.prepareBankReconciliationSaveOrUpdate(context,saveBankReconciliationList,updateBankReconciliationList,saveOrUpdateBankReconciliationList);
        if(saveBankReconciliationList.size()+updateBankReconciliationList.size() !=saveOrUpdateBankReconciliationList.size()){
            log.error("终态流水更新或新增流水库时，流水分类异常,流水总数量:{},新增{},更新:{}",saveOrUpdateBankReconciliationList.size(),saveBankReconciliationList.size(),updateBankReconciliationList.size());
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400693", "终态流水更新或新增流水库时，流水分类异常") /* "终态流水更新或新增流水库时，流水分类异常" */);
        }
        //step2:sql更新参数准备
        Map<String,List<? extends Object>> sqlParamMap = this.bulidODSSQLParameterAndprocessingModelList(context);
        List<DealDetailRuleExecRecord> dealDetailRuleExecRecords = (List<DealDetailRuleExecRecord>)sqlParamMap.get(DealDetailEnumConst.RULEPARAM);
        List<SQLParameter> sqlParameters = (List<SQLParameter>)sqlParamMap.get(DealDetailEnumConst.ODSPARAM);
        //step3:执行更新
        IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
        String currentRuleCode = context.getCurrentRule() != null ? context.getCurrentRule().getCode() : null;
        bankDealDetailAccessDao.doHandleBankReconciliationToDB(saveBankReconciliationList,updateBankReconciliationList,saveOrUpdateBankReconciliationList,dealDetailRuleExecRecords,sqlParameters,"processDealDetailResultDaoImpl",currentRuleCode);
//        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
//        List<String> ids = saveOrUpdateBankReconciliationList.stream().map(bankReconciliation -> bankReconciliation.getId().toString()).collect(Collectors.toList());
//        redisTemplate.delete(ids);
        return null;
    }
    @Override
    public void setBankReconciliationExecuteStatusAndProcessStatus(BankreconciliationIdentifyType rule, String executeStatus, BankReconciliation bankReconciliation) {
        //从流程处理出来，走手工处理
        if(null == rule){
           //rule = DealDetailUtils.createRule(RuleCodeConst.ALLPROCESSRULE.size()-1);
            bankReconciliation.put(DealDetailEnumConst.EXECUTESTATUS,DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus());
            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_SUCC.getStatus().shortValue());
           return;
        }
        String ruleCode = rule.getCode();
        if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus().equals(executeStatus)){
            bankReconciliation.put(DealDetailEnumConst.EXECUTESTATUS,DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus());
            if(RuleCodeConst.SYSTEM021.equals(ruleCode)){
                bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_FAIL.getStatus().shortValue());
            }else if(RuleCodeConst.SYSTEM022.equals(ruleCode)){
                bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_FAIL.getStatus().shortValue());
            }else if(RuleCodeConst.SYSTEM023.equals(ruleCode)){
                bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus().shortValue());
            }
        }
        if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus().equals(executeStatus)){
            bankReconciliation.put(DealDetailEnumConst.EXECUTESTATUS,DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus());
            if(RuleCodeConst.SYSTEM021.equals(ruleCode)){
                bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_PENDING.getStatus().shortValue());
            }else if(RuleCodeConst.SYSTEM022.equals(ruleCode)){
                bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_PENDING.getStatus().shortValue());
            }else if(RuleCodeConst.SYSTEM023.equals(ruleCode)){
                bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_PENDING.getStatus().shortValue());
            }
        }
        if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus().equals(executeStatus)){
            bankReconciliation.put(DealDetailEnumConst.EXECUTESTATUS,DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus());
            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus().shortValue());
        }
    }
    @Override
    public void prepareBankReconciliationSaveOrUpdate(BankDealDetailContext context, List<BankReconciliation> saveBankReconciliationList, List<BankReconciliation> updateBankReconciliationList, List<BankReconciliation> saveOrUpdateBankReconciliationList) {
        updateBankReconciliationList.addAll(saveOrUpdateBankReconciliationList);
    }
    @Override
    public <T> Map<String, List<?>> bulidODSSQLParameterAndprocessingModelList(BankDealDetailContext context) {
        List<BankDealDetailWrapper> bankDealDetailWrappers = context.getWrappers();
        List<SQLParameter> sqlParameters = new ArrayList<>();
        List<DealDetailRuleExecRecord> dealDetailRuleExecRecords = new ArrayList<>();
        if(!CollectionUtils.isEmpty(bankDealDetailWrappers)){
            for(BankDealDetailWrapper bankDealDetailWrapper : bankDealDetailWrappers){
                BankReconciliation bankReconciliation = bankDealDetailWrapper.getBankReconciliation();
                String processstatus = bankReconciliation.getProcessstatus()+"";
                String pushDownMark = bankReconciliation.get("pushDownMark");
                if(String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_PENDING.getStatus()).equals(processstatus)||
                        String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_FAIL.getStatus()).equals(processstatus)||
                        String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_PENDING.getStatus()).equals(processstatus)||
                        String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_FAIL.getStatus()).equals(processstatus)||
                        String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_PENDING.getStatus()).equals(processstatus)||
                        String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus()).equals(processstatus)||

                        String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_FAIL.getStatus()).equals(processstatus)||
                        String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_SUCC.getStatus()).equals(processstatus)||
                        String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus()).equals(processstatus)){
                    /**
                     * step1:拼接更新ods表状态sql参数
                     * */
                    SQLParameter sqlParameter = new SQLParameter();
                    if(String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_SUCC.getStatus()).equals(processstatus)||
                            String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus()).equals(processstatus)
                    ){
                        sqlParameter.addParam(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_PROCESSSUCC.getProcessstatus());
                    }
                    if(String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_PENDING.getStatus()).equals(processstatus)||
                            String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_PENDING.getStatus()).equals(processstatus)||
                            String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_PENDING.getStatus()).equals(processstatus)
                    ){

                        sqlParameter.addParam(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_SUSPEND.getProcessstatus());
                    }
                    if(String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_FAIL.getStatus()).equals(processstatus)||
                            String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_FAIL.getStatus()).equals(processstatus)||
                            String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus()).equals(processstatus)||
                            String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_FAIL.getStatus()).equals(processstatus)){

                        sqlParameter.addParam(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_PROCESSFAIL.getProcessstatus());
                    }
                    sqlParameter.addParam(bankDealDetailWrapper.getOdsId());
                    sqlParameters.add(sqlParameter);
                    /**
                     * step2:拼接插入rule过程表的数据
                     * */
                    DealDetailRuleExecRecord dealDetailRuleExecRecord = DealDetailUtils.getRuleExecRecord(bankDealDetailWrapper,null,context.getCurrentRule());
                    dealDetailRuleExecRecords.add(dealDetailRuleExecRecord);
                }
            }
        }
        return ImmutableMap.of(DealDetailEnumConst.RULEPARAM,dealDetailRuleExecRecords,DealDetailEnumConst.ODSPARAM,sqlParameters);
    }


    public BankDealDetailProcessChainImpl code(List<String> codes) {
        this.codes = codes;
        return this;
    }
}
