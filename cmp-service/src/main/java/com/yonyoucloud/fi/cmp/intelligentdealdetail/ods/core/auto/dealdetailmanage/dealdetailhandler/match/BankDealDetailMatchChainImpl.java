package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match;

import com.google.common.collect.ImmutableMap;
import com.yonyou.iuap.yms.param.SQLParameter;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.process.BankDealDetailProcessChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.model.DealDetailRuleExecRecord;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
/**
 * @Author guoyangy
 * @Date 2024/6/18 16:32
 * @Description 流水处理过滤器链
 * @Version 1.0
 */
@Slf4j
@Service
public class BankDealDetailMatchChainImpl extends DefaultBankDealDetailChain{

    private List<String> codes = null;

    public void setCodes(List<String> codes) {
        this.codes = codes;
    }
    public static BankDealDetailMatchChainImpl get(){
        return new BankDealDetailMatchChainImpl();
    }


    @Override
    public List<BankreconciliationIdentifyType> loadStreamIdentifyMatchRule(Integer index) {
        try{
            //step1:查询规则大类
            IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
            List<BankreconciliationIdentifyType> bankreconciliationIdentifyTypes = bankDealDetailAccessDao.getBankreconciliationIdentifyTypeListByTenantId();
            if(CollectionUtils.isEmpty(bankreconciliationIdentifyTypes)){
                return null;
            }
            //step2:排序
            Collections.sort(bankreconciliationIdentifyTypes, new Comparator<BankreconciliationIdentifyType>() {
                @Override
                public int compare(BankreconciliationIdentifyType o1, BankreconciliationIdentifyType o2) {
                    return o1.getExcuteorder() - o2.getExcuteorder();
                }
            });
            log.error("加载辨识匹配规则,执行顺序{}",bankreconciliationIdentifyTypes.stream().map(BankreconciliationIdentifyType::getCode).collect(Collectors.toList()));
            //step3:移除system001回单关联规则，该规则单独执行
//            Iterator<BankreconciliationIdentifyType> iterator= bankreconciliationIdentifyTypes.iterator();
//            while(iterator.hasNext()){
//                BankreconciliationIdentifyType type = iterator.next();
//                if(RuleCodeConst.SYSTEM001.equals(type.getCode())){
//                    bankreconciliationIdentifyTypes.remove(type);
//                    break;
//                }
//            }
            return bankreconciliationIdentifyTypes;
        }catch (Exception e){
            log.error("加载辨识匹配规则异常",e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540077B", "加载辨识匹配规则异常") /* "加载辨识匹配规则异常" */);
        }
    }


    @Override
    public List<BankreconciliationIdentifyType> loadStreamIdentifyMatchRuleByCode(Integer index) {
        try{
            //step1:查询规则大类
            IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
            List<BankreconciliationIdentifyType> bankreconciliationIdentifyTypes = bankDealDetailAccessDao.getBankreconciliationIdentifyTypeListByTenantId();
            if(CollectionUtils.isEmpty(bankreconciliationIdentifyTypes)){
                return null;
            }
            //step2:排序
            Collections.sort(bankreconciliationIdentifyTypes, new Comparator<BankreconciliationIdentifyType>() {
                @Override
                public int compare(BankreconciliationIdentifyType o1, BankreconciliationIdentifyType o2) {
                    return o1.getExcuteorder() - o2.getExcuteorder();
                }
            });
            log.error("加载辨识匹配规则,执行顺序{}",bankreconciliationIdentifyTypes.stream().map(BankreconciliationIdentifyType::getCode).collect(Collectors.toList()));
            //step3:移除system001回单关联规则，该规则单独执行
            if (codes != null){
                Iterator<BankreconciliationIdentifyType> iterator= bankreconciliationIdentifyTypes.iterator();
                List<BankreconciliationIdentifyType> bankreconciliationIdentifyTypesByCodeList = new ArrayList<>();
                while(iterator.hasNext()){
                    BankreconciliationIdentifyType type = iterator.next();
                    if (codes.contains(type.getCode())){
                        bankreconciliationIdentifyTypesByCodeList.add( type);
                    }
                }
                return bankreconciliationIdentifyTypesByCodeList;
            }
            return bankreconciliationIdentifyTypes;
        }catch (Exception e){
            log.error("加载辨识匹配规则异常",e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540077B", "加载辨识匹配规则异常") /* "加载辨识匹配规则异常" */);
        }
    }



    @Override
    public List<BankDealDetailWrapper> getExecutorProcessDealDetail(BankDealDetailContext context, List<BankReconciliation> processList ) {
        List<BankDealDetailWrapper> contextWrappers = context.getWrappers();
        List<BankDealDetailWrapper> bankDealDetailWrappers = new ArrayList<>();
        try{
            if (!CollectionUtils.isEmpty(processList)) {
                for(BankReconciliation bankReconciliation:processList){
                    Long bankReconciliationId = bankReconciliation.getId();
                    for (BankDealDetailWrapper wrapper : contextWrappers) {
                        //深拷贝
                        try {
                            BankDealDetailWrapper cloneWrapper =  SerializationUtils.clone(wrapper);
                            if(wrapper.getBankReconciliationId().equals(bankReconciliationId)){
                                bankDealDetailWrappers.add(cloneWrapper);
                                break;
                            }
                        }catch (Exception e){
                            log.error(String.format("流水[%s]包装类深拷贝异常=========:[%s]",bankReconciliation.getBank_seq_no(),e));
                        }
                    }
                }
            }
        }catch (Exception e){
            log.error("流水包装类深拷贝异常",e);
        }
        return bankDealDetailWrappers;
    }
    @Override
    public List<BankReconciliation> handleBankReconciliationToDB(BankDealDetailContext context, List<BankReconciliation> saveOrUpdateBankReconciliationList) {
        //step1:区分更新流水、新增流水
        List<BankReconciliation> saveBankReconciliationList = new ArrayList<>();
        List<BankReconciliation> updateBankReconciliationList = new ArrayList<>();
        this.prepareBankReconciliationSaveOrUpdate(context,saveBankReconciliationList,updateBankReconciliationList,saveOrUpdateBankReconciliationList);
        if(saveBankReconciliationList.size()+updateBankReconciliationList.size() !=saveOrUpdateBankReconciliationList.size()){
            log.error("终态流水更新或新增流水库时，流水分类异常,流水总数量:{},新增{},更新:{}",saveOrUpdateBankReconciliationList.size(),saveBankReconciliationList.size(),updateBankReconciliationList.size());
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540077C", "终态流水更新或新增流水库时，流水分类异常") /* "终态流水更新或新增流水库时，流水分类异常" */);
        }

        //step2:sql更新参数准备
        Map<String,List<? extends Object>> sqlParamMap = this.bulidODSSQLParameterAndprocessingModelList(context);
        List<DealDetailRuleExecRecord> dealDetailRuleExecRecords = (List<DealDetailRuleExecRecord>)sqlParamMap.get(DealDetailEnumConst.RULEPARAM);
        List<SQLParameter> sqlParameters = (List<SQLParameter>)sqlParamMap.get(DealDetailEnumConst.ODSPARAM);
        //step3:执行更新
        IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
        String currentRuleCode = context.getCurrentRule() != null ? context.getCurrentRule().getCode() : null;
        if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect()) || DealDetailEnumConst.SAVE_DIRECT_FINISH.equals(context.getSaveDirect())){
            bankDealDetailAccessDao.doHandleBankReconciliationToDB(saveBankReconciliationList,updateBankReconciliationList,saveOrUpdateBankReconciliationList,dealDetailRuleExecRecords,sqlParameters,"matchDealDetailResultDaoImpl",currentRuleCode);
            return saveBankReconciliationList;
        }
        if(SignalBankDealDetailMatchChainImpl.class.getSimpleName().equals(this.getClass().getSimpleName())){
            bankDealDetailAccessDao.doHandleBankReconciliationToDB(null,null,saveOrUpdateBankReconciliationList,dealDetailRuleExecRecords,sqlParameters,"matchDealDetailResultDaoImpl",currentRuleCode);
            return null;
        }
        bankDealDetailAccessDao.doHandleBankReconciliationToDB(saveBankReconciliationList,updateBankReconciliationList,saveOrUpdateBankReconciliationList,dealDetailRuleExecRecords,sqlParameters,"matchDealDetailResultDaoImpl",currentRuleCode);
        return saveBankReconciliationList;
    }
    @Override
    public void setBankReconciliationExecuteStatusAndProcessStatus(BankreconciliationIdentifyType rule, String executeStatus, BankReconciliation bankReconciliation) {
        if (DealDetailEnumConst.SAVE_DIRECT.equals(bankReconciliation.get(DealDetailEnumConst.SAVE_DIRECT))){
            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus().shortValue());
            bankReconciliation.put(DealDetailEnumConst.EXECUTESTATUS,DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus());
            return;
        }
        if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus().equals(executeStatus)){
            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_ERROR.getStatus().shortValue());
            bankReconciliation.put(DealDetailEnumConst.EXECUTESTATUS,DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus());
        }
        if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus().equals(executeStatus)){
            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_PENDING.getStatus().shortValue());
            bankReconciliation.put(DealDetailEnumConst.EXECUTESTATUS,DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus());
        }
        if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus().equals(executeStatus)){
            if(rule!=null&&(rule.getCode().equals(RuleCodeConst.SYSTEM008)||rule.getCode().equals(RuleCodeConst.SYSTEM005)||rule.getCode().equals(RuleCodeConst.SYSTEM007))){
                //（发布对象辨识和发布认领中心）（收付单据匹配、收付单据关联）合在一起了，且已发布到认领中心的数据其返回值executeStatus=3,processstatus=25 即【DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH】
                if(rule.getCode().equals(RuleCodeConst.SYSTEM008)||rule.getCode().equals(RuleCodeConst.SYSTEM005)){
                    bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus().shortValue());
                }
                if(rule.getCode().equals(RuleCodeConst.SYSTEM007)){
                    bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_GENERATE_SUCC.getStatus().shortValue());
                }
            }else{
                bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_SUCC.getStatus().shortValue());
            }
            bankReconciliation.put(DealDetailEnumConst.EXECUTESTATUS,DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus());
        }
    }
    @Override
    public void prepareBankReconciliationSaveOrUpdate(BankDealDetailContext context,
                                                      List<BankReconciliation> saveBankReconciliationList,
                                                      List<BankReconciliation> updateBankReconciliationList,
                                                      List<BankReconciliation> saveOrUpdateBankReconciliationList) {

        List<BankReconciliation> existsBankReconciliations = context.getUpdateBankReconciliationList();
        if(CollectionUtils.isEmpty(existsBankReconciliations)){
            saveBankReconciliationList.addAll(saveOrUpdateBankReconciliationList);
        }else{
            List<Object> bankReconciliationIdList = (existsBankReconciliations.stream().map(BankReconciliation::getId).collect(Collectors.toList()));
            Iterator<BankReconciliation> iterator = saveOrUpdateBankReconciliationList.iterator();
            while(iterator.hasNext()){
                BankReconciliation bankReconciliation = iterator.next();
                if(bankReconciliationIdList.contains(bankReconciliation.getId())){
                    updateBankReconciliationList.add(bankReconciliation);
                }else{
                    //新增
                    saveBankReconciliationList.add(bankReconciliation);
                }
            }
        }
    }
    @Override
    public <T> Map<String, List<?>> bulidODSSQLParameterAndprocessingModelList(BankDealDetailContext context) {
        List<BankDealDetailWrapper> bankDealDetailWrappers = context.getWrappers();
        List<SQLParameter> sqlParameters = new ArrayList<>();
        List<DealDetailRuleExecRecord> dealDetailRuleExecRecords = new ArrayList<>();
        if(!CollectionUtils.isEmpty(bankDealDetailWrappers)){
            for(BankDealDetailWrapper bankDealDetailWrapper : bankDealDetailWrappers){
                if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect())){
                    SQLParameter sqlParameter = new SQLParameter();
                    sqlParameter.addParam(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_NO.getProcessstatus());
                    sqlParameter.addParam(bankDealDetailWrapper.getOdsId());
                    sqlParameters.add(sqlParameter);
                    /**
                     * step2:拼接插入rule过程表的数据
                     * */
                    DealDetailRuleExecRecord dealDetailRuleExecRecord = DealDetailUtils.getRuleExecRecord(bankDealDetailWrapper,null,context.getCurrentRule());
                    dealDetailRuleExecRecords.add(dealDetailRuleExecRecord);
                    continue;
                }
                BankReconciliation bankReconciliation = bankDealDetailWrapper.getBankReconciliation();
                String processstatus = bankReconciliation.getProcessstatus()+"";
                if( String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_PENDING.getStatus()).equals(processstatus)||
                        String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_ERROR.getStatus()).equals(processstatus)||
                        String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_GENERATE_SUCC.getStatus()).equals(processstatus)||
                        String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_SUCC.getStatus()).equals(processstatus)||
                        String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus()).equals(processstatus)){

                    /**
                     * step1:拼接更新ods表状态sql参数
                     * */
                    SQLParameter sqlParameter = new SQLParameter();
                    if(String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_SUCC.getStatus()).equals(processstatus)){
                        sqlParameter.addParam(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_MATCHSUCC.getProcessstatus());
                    }
                    if(String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_PENDING.getStatus()).equals(processstatus)){
                        sqlParameter.addParam(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_SUSPEND.getProcessstatus());
                    }
                    if(String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_ERROR.getStatus()).equals(processstatus)){
                        sqlParameter.addParam(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_MATCHFAIL.getProcessstatus());
                    }
                    if(String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus()).equals(processstatus)){
                        sqlParameter.addParam(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_PROCESSSUCC.getProcessstatus());
                    }
                    if(String.valueOf(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_GENERATE_SUCC.getStatus()).equals(processstatus)){
                        sqlParameter.addParam(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_MATCHSUCC.getProcessstatus());
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

    public BankDealDetailMatchChainImpl code(List<String> codes) {
        this.codes = codes;
        return this;
    }
}
