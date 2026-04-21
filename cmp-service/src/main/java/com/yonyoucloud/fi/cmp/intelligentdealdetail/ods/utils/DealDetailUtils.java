package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils;

import com.alibaba.fastjson.JSON;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.BillClaimStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ClaimCompleteType;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.flowhandlesetting.Flowhandlesetting;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.functional.IDealDetailCallBack;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.model.DealDetailRuleExecRecord;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.iuap.upc.api.IMerchantServiceV2;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialFieldKeyConstant;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialQryDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.MDC;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
/**
 * @Author guoyangy
 * @Date 2024/7/8 15:41
 * @Description todo
 * @Version 1.0
 */
@Slf4j
public class DealDetailUtils {

    public static final String SMARTCHECKNO_SPLIT_SYMBOL = "_";
    public static int getNeedUpdateProcessstatusByRule(BankDealDetailContext context) {
        BankreconciliationIdentifyType currentRule = context.getCurrentRule();
        int processstatus = 0;
        //识别规则类型
        if(currentRule==null){
            BankreconciliationIdentifyType preRule = context.getPreRule();
            if(null!=preRule){
                String preCode = preRule.getCode();
                if(RuleCodeConst.ALLMATCHRULE.contains(preCode)){
                    //辨识匹配规则，
                    processstatus = DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_ERROR.getStatus();
                }else{
                    //流程处理规则
                    processstatus = DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_FAIL.getStatus();
                }
            }
        }else{
            String ruleCode = currentRule.getCode();
            if(RuleCodeConst.ALLMATCHRULE.contains(ruleCode)){
                //辨识匹配规则，
                processstatus = DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_ERROR.getStatus();
            }else{
                //流程处理规则
                if(ruleCode.equals(RuleCodeConst.SYSTEM021)){
                    processstatus = DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_FAIL.getStatus();
                }else if(ruleCode.equals(RuleCodeConst.SYSTEM022)){
                    processstatus = DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_FAIL.getStatus();
                }else if(ruleCode.equals(RuleCodeConst.SYSTEM023)){
                    processstatus = DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus();
                }
            }
        }
        return processstatus;
    }

    /**
     * 移除执行状态
     * @param bankReconciliation
     */
    public static void removeExecuteStatusFromBankReconciliation(BankReconciliation bankReconciliation){
        if(null == bankReconciliation){
            return;
        }
        bankReconciliation.remove(DealDetailEnumConst.EXECUTESTATUS);
        bankReconciliation.remove(DealDetailEnumConst.EXECUTERESULTDESC);
        bankReconciliation.remove(DealDetailEnumConst.SAVE_DIRECT);
    }
//    public static void checkBankReconciliationId(List<BankReconciliation> processBankReconciliationList){
//        if(!CollectionUtils.isEmpty(processBankReconciliationList)){
//            for(BankReconciliation bankReconciliation : processBankReconciliationList){
//                if(bankReconciliation.get(DealDetailEnumConst.ODSID)==null){
//                    throw new BankDealDetailException(bankReconciliation.getId()+"流水对应odsid为空");
//                }
//            }
//        }
//    }
    /**
     * 根据银行对账单id找到osdid
     * */
    public static Object getOsdIdByReConciliationId(Object id, List<BankDealDetailODSModel> bankDealDetailODSModelList) {
        if(!CollectionUtils.isEmpty(bankDealDetailODSModelList)&&null!=id){
            for(BankDealDetailODSModel odsModel:bankDealDetailODSModelList){
                String osdId = odsModel.getId();
                String bankReconciliationId = odsModel.getMainid().toString();
                if(id.toString().equals(bankReconciliationId)){
                    return osdId;
                }
            }
        }
        return null;
    }
    public static void removeDealDetailFromContext(BankDealDetailContext context) {
        List<BankDealDetailWrapper> bankDealDetailWrappers = context.getWrappers();
        if(!CollectionUtils.isEmpty(bankDealDetailWrappers)){
            Iterator<BankDealDetailWrapper> iterator = bankDealDetailWrappers.iterator();
            while (iterator.hasNext()){
                BankDealDetailWrapper bankDealDetailWrapper = iterator.next();
                String executeStatus = bankDealDetailWrapper.getBankReconciliation().get(DealDetailEnumConst.EXECUTESTATUS);
                //step3:从流水中移除本规则放入的executstatus
                DealDetailUtils.removeExecuteStatusFromBankReconciliation(bankDealDetailWrapper.getBankReconciliation());
                if(!StringUtils.isEmpty(executeStatus)){
                    if(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus().equals(executeStatus)
                            ||DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus().equals(executeStatus)
                            ||DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus().equals(executeStatus)
                    ){
                        iterator.remove();
                        context.getProcessedDealDetails().add(bankDealDetailWrapper);
                    }
                }

            }
        }
    }
    public static BankreconciliationIdentifyType createRule(String code,String name,int i) {
        BankreconciliationIdentifyType rule = new BankreconciliationIdentifyType();
        rule.setCode(code);
        rule.setName(name);
        rule.setExcuteorder(i+1);
        return rule;
    }
    public static void removeFinishDealDetail(List<BankDealDetailWrapper> bankDealDetailWrappers) {
        List<Long> ids = bankDealDetailWrappers.stream().map(BankDealDetailWrapper::getBankReconciliationId).collect(Collectors.toList());
        IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
        List<Long>existsIds = bankDealDetailAccessDao.batchQueryBankReconciliationByIdsAndProcessstatus(ids,DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus());
        if(!CollectionUtils.isEmpty(existsIds)){
            Iterator<BankDealDetailWrapper> iterator = bankDealDetailWrappers.iterator();
            while(iterator.hasNext()){
                BankDealDetailWrapper bankDealDetailWrapper = iterator.next();
                if(existsIds.contains(bankDealDetailWrapper.getBankReconciliationId())){
                    iterator.remove();
                }
            }
        }
    }
    public static String getTraceId(){
        return MDC.get("traceId");
    }
    public static void removeRepeatODSBymainid(List<BankDealDetailODSModel> bankDealDetailResponseRecords, List<BankDealDetailODSModel> needHandleODSModel, List<BankDealDetailODSModel> noNeedHandleODSModel) {
        //ods根据mainid去重，重复的ods标记为无需处理
        Map<Long,List<BankDealDetailODSModel>> groupByMainidMap = bankDealDetailResponseRecords.stream().collect(Collectors.groupingBy(BankDealDetailODSModel::getMainid));
        //包含重复mainid
        if(groupByMainidMap.keySet().size() != bankDealDetailResponseRecords.size()){
            Collection<List<BankDealDetailODSModel>> collection = groupByMainidMap.values();
            Iterator<List<BankDealDetailODSModel>> iterator = collection.iterator();
            while(iterator.hasNext()){
                List<BankDealDetailODSModel> bankDealDetailODSModelList=iterator.next();
                if(bankDealDetailODSModelList.size()>0){
                    needHandleODSModel.add(bankDealDetailODSModelList.get(0));
                    bankDealDetailODSModelList.remove(0);
                    if(!CollectionUtils.isEmpty(bankDealDetailODSModelList)){
                        noNeedHandleODSModel.addAll(bankDealDetailODSModelList);
                    }
                }
            }
            if(!CollectionUtils.isEmpty(noNeedHandleODSModel)){
                //ods processstatus改为无需处理
                List<Object> ids = noNeedHandleODSModel.stream().map(BankDealDetailODSModel::getId).collect(Collectors.toList());
                IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
                bankDealDetailAccessDao.batchUpdateOdsProcessstatusByIds(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_NOTHING.getProcessstatus(),ids);
            }
        }else{
            needHandleODSModel.addAll(bankDealDetailResponseRecords);
        }
    }

    public static void getBankReconciliationListAfterUpdateProcessstatus(List<BankReconciliation> bankReconciliationList,List<Object> ids){
        if(CollectionUtils.isEmpty(ids)){
            return;
        }
        Map<Long,BankReconciliation> bankReconciliationMap = bankReconciliationList.stream().collect(Collectors.toMap(BankReconciliation::getId, Function.identity(),(key1, key2)->key1));
        List<Map<String,Object>> queryObjs = getBankReconciliationListByIds(bankReconciliationList,ids);
        if(!CollectionUtils.isEmpty(queryObjs)){
            queryObjs = queryObjs.stream().distinct().collect(Collectors.toList());
            for (Map<String, Object> data : queryObjs) {
                BankReconciliation bankReconciliation = Objectlizer.convert(data, BankReconciliation.ENTITY_NAME);
                Long id = bankReconciliation.getId();
                //Short processstatus = bankReconciliation.getProcessstatus();
                Date putbs = bankReconciliation.getPubts();
                BankReconciliation bankReconciliationOrigin = bankReconciliationMap.get(id);
                bankReconciliationOrigin.setPubts(putbs);
                //bankReconciliationOrigin.setProcessstatus(processstatus);
                //发布时，此处移除各类辨识对某些字段的影响
                if (bankReconciliationOrigin.getIspublish() == null || !bankReconciliationOrigin.getIspublish()){
                    bankReconciliationOrigin.remove("ispublish");
                    bankReconciliationOrigin.remove("amounttobeclaimed");
                    bankReconciliationOrigin.remove("claimamount");
                }
                //关联或生单时，移除各类辨识对某些字段的影响
                if (bankReconciliationOrigin.getAssociationstatus() == null || AssociationStatus.Associated.getValue() != bankReconciliationOrigin.getAssociationstatus()){
                    //关联关系
                    bankReconciliationOrigin.remove("associationstatus");
                    //待确认
                    bankReconciliationOrigin.remove("relationstatus");
                    //完结状态
                    if (bankReconciliationOrigin.getSerialdealendstate() == null || SerialdealendState.END.getValue() !=bankReconciliationOrigin.getSerialdealendstate()) {
                        bankReconciliationOrigin.remove("serialdealendstate");
                    }
                    //自动关联状态
                    bankReconciliationOrigin.remove("autodealstate");
                    //是否自动生单
                    bankReconciliationOrigin.remove("isautocreatebill");
                }else if (bankReconciliationOrigin.getAssociationstatus() != null && AssociationStatus.Associated.getValue() == bankReconciliationOrigin.getAssociationstatus() &&
                        bankReconciliationOrigin.getSerialdealendstate() != null && SerialdealendState.END.getValue() != bankReconciliationOrigin.getSerialdealendstate()){
                    //完结状态(当关联状态是已关联的则必须已完结状态才会更新完结状态)
                    bankReconciliationOrigin.remove("serialdealendstate");
                }
                if (bankReconciliationOrigin.getBillclaimstatus() == null || BillClaimStatus.ToBeClaim.getValue() != bankReconciliationOrigin.getBillclaimstatus()){
                    bankReconciliationOrigin.remove("billclaimstatus");
                }
            }
        }
    }

    public static List<Map<String,Object>> getBankReconciliationListByIds(List<BankReconciliation> bankReconciliationList,List<Object> ids){

        try{
            Long[] idLongs = new Long[ids.size()];
            for(int i=0;i<ids.size();i++){
                Object idObject = ids.get(i);
                idLongs[i] = Long.parseLong(idObject.toString());
            }
            List<Map<String,Object>> queryObjs = MetaDaoHelper.queryByIds(BankReconciliation.ENTITY_NAME,"*",idLongs);
            return queryObjs;
        }catch (Exception e){
            log.error("流水更新后需要重新查询流水Pubts并赋值，异常",e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005CE", "流水更新后需要重新查询流水Pubts并赋值，异常") /* "流水更新后需要重新查询流水Pubts并赋值，异常" */);
        }
    }
    public static void getBankReconciliationListAfterUpdateProcessstatus(List<BankReconciliation> bankReconciliationList){
        List<Object> ids = bankReconciliationList.stream().map(BankReconciliation::getId).collect(Collectors.toList());
        getBankReconciliationListAfterUpdateProcessstatus(bankReconciliationList,ids);
    }
    public static List<BankReconciliation> getBankReconciliationPubtsBeforeUpdate(List<BankReconciliation> bankReconciliationList){
        if(CollectionUtils.isEmpty(bankReconciliationList)){
            return null;
        }
        List<Object> ids = bankReconciliationList.stream().map(BankReconciliation::getId).collect(Collectors.toList());
        Map<Long,BankReconciliation> bankReconciliationMap = bankReconciliationList.stream().collect(Collectors.toMap(BankReconciliation::getId, Function.identity(),(key1, key2)->key1));
        List<Map<String,Object>> queryObjs = getBankReconciliationListByIds(bankReconciliationList,ids);
        if(!CollectionUtils.isEmpty(queryObjs)){
            queryObjs = queryObjs.stream().distinct().collect(Collectors.toList());
            for (Map<String, Object> data : queryObjs) {
                BankReconciliation bankReconciliation = Objectlizer.convert(data, BankReconciliation.ENTITY_NAME);
                Long id = bankReconciliation.getId();
                //Short processstatus = bankReconciliation.getProcessstatus();
                Date putbs = bankReconciliation.getPubts();
                BankReconciliation bankReconciliationOrigin = bankReconciliationMap.get(id);
                bankReconciliationOrigin.setPubts(putbs);
                //发布时，此处移除各类辨识对某些字段的影响
                if (bankReconciliationOrigin.getIspublish() == null || !bankReconciliationOrigin.getIspublish()){
                    bankReconciliationOrigin.remove("ispublish");
                    bankReconciliationOrigin.remove("amounttobeclaimed");
                    bankReconciliationOrigin.remove("claimamount");
                }
                //关联或生单时，移除各类辨识对某些字段的影响
                if (bankReconciliationOrigin.getAssociationstatus() == null || AssociationStatus.Associated.getValue() != bankReconciliationOrigin.getAssociationstatus()){
                    //关联关系
                    bankReconciliationOrigin.remove("associationstatus");
                    //待确认
                    bankReconciliationOrigin.remove("relationstatus");
                    //完结状态
                    if (bankReconciliationOrigin.getSerialdealendstate() == null || SerialdealendState.END.getValue() !=bankReconciliationOrigin.getSerialdealendstate()) {
                        bankReconciliationOrigin.remove("serialdealendstate");
                    }
                    //自动关联状态
                    bankReconciliationOrigin.remove("autodealstate");
                    //是否自动生单
                    bankReconciliationOrigin.remove("isautocreatebill");
                }else if (bankReconciliationOrigin.getAssociationstatus() != null && AssociationStatus.Associated.getValue() == bankReconciliationOrigin.getAssociationstatus() &&
                        bankReconciliationOrigin.getSerialdealendstate() != null && SerialdealendState.END.getValue() != bankReconciliationOrigin.getSerialdealendstate()){
                    //完结状态(当关联状态是已关联的则必须已完结状态才会更新完结状态)
                    bankReconciliationOrigin.remove("serialdealendstate");
                }
                if (bankReconciliationOrigin.getBillclaimstatus() == null || BillClaimStatus.ToBeClaim.getValue() != bankReconciliationOrigin.getBillclaimstatus()){
                    bankReconciliationOrigin.remove("billclaimstatus");
                }
                bankReconciliationMap.put(id,bankReconciliationOrigin);
            }
        }
        return bankReconciliationMap.values().stream().collect(Collectors.toList());
    }

    /**
     * 只根据生单规则将数据库的数据回写内存的数据
     * @param bankReconciliationList
     */
    public static List<BankReconciliation> getBankReconciliationPubtsBeforeUpdateForRule(List<BankReconciliation> bankReconciliationList){
        if(CollectionUtils.isEmpty(bankReconciliationList)){
            return null;
        }
        List<Object> ids = bankReconciliationList.stream().map(BankReconciliation::getId).collect(Collectors.toList());
        Map<Long,BankReconciliation> bankReconciliationMap = bankReconciliationList.stream().collect(Collectors.toMap(BankReconciliation::getId, Function.identity(),(key1, key2)->key1));
        List<Map<String,Object>> queryObjs = getBankReconciliationListByIds(bankReconciliationList,ids);
        if(!CollectionUtils.isEmpty(queryObjs)){
            queryObjs = queryObjs.stream().distinct().collect(Collectors.toList());
            for (Map<String, Object> data : queryObjs) {
                BankReconciliation bankReconciliation = Objectlizer.convert(data, BankReconciliation.ENTITY_NAME);
                Long id = bankReconciliation.getId();
                BankReconciliation bankReconciliationOrigin = bankReconciliationMap.get(id);
                //Short processstatus = bankReconciliationOrigin.getProcessstatus();
                if (bankReconciliationOrigin != null){
                    bankReconciliationOrigin = bankReconciliation;
                    //发布时，此处移除各类辨识对某些字段的影响
                    if (bankReconciliationOrigin.getIspublish() == null || !bankReconciliationOrigin.getIspublish()){
                        bankReconciliationOrigin.remove("ispublish");
                        bankReconciliationOrigin.remove("amounttobeclaimed");
                        bankReconciliationOrigin.remove("claimamount");
                    }
                    //关联或生单时，移除各类辨识对某些字段的影响
                    if (bankReconciliationOrigin.getAssociationstatus() == null || AssociationStatus.Associated.getValue() != bankReconciliationOrigin.getAssociationstatus()){
                        //关联关系
                        bankReconciliationOrigin.remove("associationstatus");
                        //待确认
                        bankReconciliationOrigin.remove("relationstatus");
                        //完结状态
                        if (bankReconciliationOrigin.getSerialdealendstate() == null || SerialdealendState.END.getValue() !=bankReconciliationOrigin.getSerialdealendstate()) {
                            bankReconciliationOrigin.remove("serialdealendstate");
                        }
                        //自动关联状态
                        bankReconciliationOrigin.remove("autodealstate");
                        //是否自动生单
                        bankReconciliationOrigin.remove("isautocreatebill");
                    }else if (bankReconciliationOrigin.getAssociationstatus() != null && AssociationStatus.Associated.getValue() == bankReconciliationOrigin.getAssociationstatus() &&
                            bankReconciliationOrigin.getSerialdealendstate() != null && SerialdealendState.END.getValue() != bankReconciliationOrigin.getSerialdealendstate()){
                        //完结状态(当关联状态是已关联的则必须已完结状态才会更新完结状态)
                        bankReconciliationOrigin.remove("serialdealendstate");
                    }
                    if (bankReconciliationOrigin.getBillclaimstatus() == null || BillClaimStatus.ToBeClaim.getValue() != bankReconciliationOrigin.getBillclaimstatus()){
                        bankReconciliationOrigin.remove("billclaimstatus");
                    }
                    bankReconciliationMap.put(id,bankReconciliationOrigin);
                }
            }
        }
        return bankReconciliationMap.values().stream().collect(Collectors.toList());
    }

    public static boolean isOpenMultiInstanceAccess() {
        String openMultiInstance = AppContext.getEnvConfig("cmp.dealdetail.open.multiinstance","false");
        //开启多实例
        return BooleanUtils.b(openMultiInstance);
    }

    public static void appendBusiCode(String code,BankReconciliation b,String... extendCodes) {
        String busiCode = b.get(DealDetailEnumConst.EXECUTERESULTDESC);
        if(extendCodes!=null&&extendCodes.length>0){
            String extendCode = extendCodes[0];
            if(extendCode.length()>5){
                extendCode = extendCode.substring(extendCode.length()-5);
            }
            code = code+"_"+extendCode+",";
        }
        if(StringUtils.isEmpty(busiCode)){
            busiCode = code+",";
            b.put(DealDetailEnumConst.EXECUTERESULTDESC,busiCode);
            return;
        }
        if(!busiCode.contains(code)){
            busiCode = busiCode+","+code;
            b.put(DealDetailEnumConst.EXECUTERESULTDESC,busiCode);
        }
    }
    public static void appendBusiCode(String code,Collection<BankReconciliation> bs,String... extendCode) {
        for(BankReconciliation b:bs){
            appendBusiCode(code,b,extendCode);
        }
    }
    public static List<DealDetailRuleExecRecord> getRuleExecRecordList(BankDealDetailContext context){
        return getRuleExecRecordList(context,null,null);
    }
    public static List<DealDetailRuleExecRecord> getRuleExecRecordList(BankDealDetailContext context,Exception e,BankreconciliationIdentifyType currentRule ) {
        List<BankDealDetailWrapper> bankDealDetailWrappers = context.getWrappers();
        if(!CollectionUtils.isEmpty(bankDealDetailWrappers)){
            List<DealDetailRuleExecRecord> dealDetailRuleExecRecords = new ArrayList<>();
            for(BankDealDetailWrapper bankDealDetailWrapper : bankDealDetailWrappers){
                DealDetailRuleExecRecord dealDetailRuleExecRecord = getRuleExecRecord(bankDealDetailWrapper,e,currentRule);
                if(null!=dealDetailRuleExecRecord){
                    dealDetailRuleExecRecords.add(dealDetailRuleExecRecord);
                }
            }
            return dealDetailRuleExecRecords;
        }
        return null;
    }

    public static DealDetailRuleExecRecord getRuleExecRecord(BankDealDetailWrapper bankDealDetailWrapper,Exception e,BankreconciliationIdentifyType currentRule) {
        BankReconciliation bankReconciliation = bankDealDetailWrapper.getBankReconciliation();
        DealDetailRuleExecRecord ruleExecRecord = bankDealDetailWrapper.getDealDetailRuleExecRecord();
        if(null == ruleExecRecord){
            log.error("构建更新过程表实体空指针");
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005CD", "构建更新过程表实体空指针") /* "构建更新过程表实体空指针" */);
        }
        ruleExecRecord.setMainid(bankReconciliation.getId());

        if(!CollectionUtils.isEmpty(bankDealDetailWrapper.getRuleList())){
            String ruleexes = JSON.toJSONString(bankDealDetailWrapper.getRuleList());
            if(ruleexes.length()>DealDetailEnumConst.MAXLENGTH){
                ruleexes = ruleexes.substring(0,DealDetailEnumConst.MAXLENGTH);
            }
            ruleExecRecord.setExerules(ruleexes);
        }
        ruleExecRecord.setCreate_time(new Date());
        long tenantId = Long.parseLong(InvocationInfoProxy.getYxyTenantid());
        String ytenantId = InvocationInfoProxy.getTenantid();
        ruleExecRecord.setTenant_id(tenantId);
        ruleExecRecord.setYtenant_id(ytenantId);
        ruleExecRecord.setOsdid(bankDealDetailWrapper.getOdsId());
        String executeStatus = bankReconciliation.get(DealDetailEnumConst.EXECUTESTATUS);
        if(e == null && DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus().equals(executeStatus)){
            e = new BankDealDetailException(bankReconciliation.get(DealDetailEnumConst.EXECUTERESULTDESC)+"");
        }
        if(e!=null && currentRule!=null ){
            ruleExecRecord.setFailrule(RuleCodeConst.getRuleName(currentRule.getCode()));
            ruleExecRecord.setFailreason(e.getMessage());
        }
        return ruleExecRecord;
    }

    public static Flowhandlesetting queryFlowhandlesetting(String code) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("code").eq(code));
        querySchema.addCondition(queryConditionGroup);
        List<Flowhandlesetting> flowhandlesettings =  MetaDaoHelper.queryObject(Flowhandlesetting.ENTITY_NAME, querySchema, null);
        if(!CollectionUtils.isEmpty(flowhandlesettings)){
            return flowhandlesettings.get(0);
        }
        return null;
    }

    public static boolean isOpenIntelligentDealDetail() {
        String openTeanant = AppContext.getEnvConfig("available.ytenant_id");
        if(!StringUtils.isEmpty(openTeanant)&&openTeanant.contains(InvocationInfoProxy.getTenantid())){
            return true;
        }
        return false;
    }

    /**
     * 流水转BankDealDetailODSModel模型
     * */
    public static BankDealDetailODSModel convertBankReconciliationToOdsModel(BankReconciliation bankReconciliation, int key,String signature,String requestSeqNo,YmsOidGenerator ymsOidGenerator,BaseRefRpcService baseRefRpcService) {
        BankDealDetailODSModel odsModel = new BankDealDetailODSModel();
        odsModel.setRequest_seq_no(requestSeqNo);
        odsModel.setTraceid(DealDetailUtils.getTraceId());
        String id = ymsOidGenerator.nextStrId();
        odsModel.setId(id);
        odsModel.setMainid(bankReconciliation.getId() instanceof String?Long.parseLong(bankReconciliation.getId()):bankReconciliation.getId());
        odsModel.setCreate_time(new Date());
        odsModel.setTenant_id(AppContext.getTenantId());
        odsModel.setYtenant_id(AppContext.getYTenantId());
        odsModel.setProcessstatus(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_NO.getProcessstatus());
        // 币种
        odsModel.setCurrency(bankReconciliation.getCurrency());
        // 交易流水号
        odsModel.setBank_seq_no(bankReconciliation.getBank_seq_no());
        // 交易日期
        Date tran_date = bankReconciliation.getTran_date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        if (tran_date != null){
            String format = sf.format(tran_date);
            if (StringUtils.isNotEmpty(format)){
                odsModel.setTran_date(format.replace("-",""));
            }

        }
        // 交易时间
        Date timeStr = bankReconciliation.getTran_time();
        SimpleDateFormat sftime = new SimpleDateFormat("HH:mm:ss");
        if (tran_date != null && timeStr != null) {
            String format = sftime.format(timeStr);
            if (StringUtils.isNotEmpty(format))
            odsModel.setTran_time(format.replace(":",""));
        }
        // 收付方向
        Direction dcFlag = bankReconciliation.getDc_flag();
        if (dcFlag != null){
            odsModel.setDc_flag(dcFlag.getValue());
        }
        // 交易金额
        odsModel.setTran_amt(bankReconciliation.getTran_amt());
        // 余额
        odsModel.setAcct_bal(bankReconciliation.getAcct_bal());
        if (bankReconciliation.getBankaccount()!= null){
            List<AgentFinancialDTO> agentFinancialDTOS = queryCustomerBankAccountById(Long.valueOf(bankReconciliation.getBankaccount()));
            if (!CollectionUtils.isEmpty(agentFinancialDTOS)){
                odsModel.setAcct_no(agentFinancialDTOS.get(0).getBankAccount());
                odsModel.setAcct_name(agentFinancialDTOS.get(0).getBankAccountName());
            }
        }
        odsModel.setBalance(bankReconciliation.getAcct_bal() != null ? String.valueOf(bankReconciliation.getAcct_bal()) : "");

        try {
            String currency = bankReconciliation.getCurrency();
            if (StringUtils.isNotEmpty(currency)) {
                CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currency);
                odsModel.setCurr_code(currencyTenantDTO.getCode());
            }
        } catch (Exception e) {
            log.error("获取币种异常",e);
        }
        //所属组织
        odsModel.setOrgid(bankReconciliation.getOrgid());
        odsModel.setBankaccount(bankReconciliation.getBankaccount());
        // 对方账号
        odsModel.setTo_acct_no(bankReconciliation.getTo_acct_no());
        // 对方户名
        odsModel.setTo_acct_name(bankReconciliation.getTo_acct_name());
        // 对方账户开户行
        odsModel.setTo_acct_bank(bankReconciliation.getTo_acct_bank());
        // 对方开户行名
        odsModel.setTo_acct_bank_name(bankReconciliation.getTo_acct_bank_name());
        // 钞汇标志
        odsModel.setCash_flag(bankReconciliation.getCash_flag());
        // 操作员
        odsModel.setOper(bankReconciliation.getOper());
        // 起息日
        Date valueDate = bankReconciliation.getValue_date();
        if(valueDate != null){
            odsModel.setValue_date(valueDate);
        }
        // 用途
        odsModel.setUse_name(bankReconciliation.getUse_name());
        // 摘要
        odsModel.setRemark(bankReconciliation.getRemark());
        // 附言
        odsModel.setRemark01(bankReconciliation.getRemark01());
        // 银行对账编号
        odsModel.setBank_check_code(bankReconciliation.getBankcheckno());
        // 唯一标识码
        odsModel.setUnique_no(bankReconciliation.getUnique_no());
        // 数据来源
        odsModel.setAccesschannel(key);
        //生成唯一签名
        odsModel.setContentsignature(signature);
        //'接入ods渠道',
        odsModel.setAccesschannel(DealDetailEnumConst.AccessChannelEnum.MANUAL.getKey());

        return odsModel;
    }
    /**
     * 根据客户银行id查询客户银行账户
     *
     * @param id 客户银行账号id
     * @return
     * @throws Exception
     */
    public static List<AgentFinancialDTO> queryCustomerBankAccountById(Long id)  {
        if (id == null) {
            return null;
        }
        AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
        agentFinancialQryDTO.setId(id);
        try {
            return queryCustomerBankAccountByCondition(agentFinancialQryDTO);
        } catch (Exception e) {
            log.error("根据客户银行id查询客户银行账户异常", e);
            return null;
        }
    }
    /**
     * 根据条件查询客户银行账户
     *
     * @param agentFinancialQryDTO 查询条件实体
     * @return
     * @throws Exception
     */
    public static List<AgentFinancialDTO> queryCustomerBankAccountByCondition(AgentFinancialQryDTO agentFinancialQryDTO) throws Exception {
        if (agentFinancialQryDTO.getFields() == null || agentFinancialQryDTO.getFields().length == 0) {
            agentFinancialQryDTO.setFields(new String[]{AgentFinancialFieldKeyConstant.MERCHANT_ID, AgentFinancialFieldKeyConstant.ID, AgentFinancialFieldKeyConstant.STOP_STATUS, AgentFinancialFieldKeyConstant.BANK_ACCOUNT,
                    AgentFinancialFieldKeyConstant.BANK_ACCOUNT_NAME, AgentFinancialFieldKeyConstant.OPEN_BANK, AgentFinancialFieldKeyConstant.MERCHANT_NAME, AgentFinancialFieldKeyConstant.IS_DEFAULT});
        }
        IMerchantServiceV2 merchantService = AppContext.getBean(IMerchantServiceV2.class);
        return merchantService.listMerchantAgentFinancial(agentFinancialQryDTO);
    }

    public static void addCallBackToBankReconciliation(BankReconciliation bankReconciliation,IDealDetailCallBack callBack) {
        if(null == bankReconciliation){
            return;
        }
        List<IDealDetailCallBack> callBacks = bankReconciliation.get(DealDetailEnumConst.BANKRECONCILIATION_CALLACK);
        if(CollectionUtils.isEmpty(callBacks)){
            callBacks = new ArrayList<>();
            bankReconciliation.put(DealDetailEnumConst.BANKRECONCILIATION_CALLACK,callBacks);
        }
        callBacks.add(callBack);
    }

    /**
     * 银行流水转包装类
     * */
    public static List<BankDealDetailWrapper> convertBankReconciliationToWrapper(List<BankReconciliation> processBankReconciliationList){

        return convertBankReconciliationToWrapper(processBankReconciliationList,true);
    }
    public static List<BankDealDetailWrapper> convertBankReconciliationToWrapper(List<BankReconciliation> processBankReconciliationList,boolean isUseOds){
        List<BankDealDetailWrapper> bankDealDetailProcessList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(processBankReconciliationList)){
            for(BankReconciliation bankReconciliation : processBankReconciliationList){
                if(isUseOds){
                    if(bankReconciliation.get(DealDetailEnumConst.ODSID)==null){
                        throw new BankDealDetailException(bankReconciliation.getId()+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005CF", "流水对应odsid为空") /* "流水对应odsid为空" */);
                    }
                }
                if(bankReconciliation.getId() == null){
                    log.error("银行对账单id不能为空");
                    throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005CC", "银行对账单id不能为空") /* "银行对账单id不能为空" */);
                }
                BankDealDetailWrapper bankDealDetailWrapper = new BankDealDetailWrapper();
                bankDealDetailWrapper.setBankReconciliation(bankReconciliation);
                bankDealDetailWrapper.setOdsId(bankReconciliation.get(DealDetailEnumConst.ODSID));
                bankDealDetailWrapper.setBankReconciliationId(bankReconciliation.getId());
                bankDealDetailProcessList.add(bankDealDetailWrapper);
            }
        }
        return bankDealDetailProcessList;
    }

    public static void recordException(String traceId, String requestSeqNo, Exception e) {
        try{
            String exceptionMsg = ExceptionUtils.getStackTrace(e);
            IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
            List<DealDetailRuleExecRecord> dealDetailRuleExecRecords = new ArrayList<>();
            DealDetailRuleExecRecord record = new DealDetailRuleExecRecord();
            record.setRequestseqno(requestSeqNo);
            record.setTraceid(traceId);
            record.setExceptionmsg(exceptionMsg);
            record.setCreate_time(new Date());
            record.setTenant_id(AppContext.getTenantId());
            record.setYtenant_id(AppContext.getYTenantId());
            dealDetailRuleExecRecords.add(record);
            bankDealDetailAccessDao.insertRuleExecRecords(dealDetailRuleExecRecords);
        }catch (Exception e1){
            log.error("异常堆栈入库失败",e1);
        }
    }
    public static void recordExecuteDedulicationProcessing(String traceId, String requestSeqNo, List<BankReconciliation> bankReconciliations,String reason) {
        try{
            IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
            List<DealDetailRuleExecRecord> dealDetailRuleExecRecords = new ArrayList<>();
            for(BankReconciliation bankReconciliation : bankReconciliations){
                DealDetailRuleExecRecord record = new DealDetailRuleExecRecord();
                record.setRequestseqno(requestSeqNo);
                record.setTraceid(traceId);
                record.setExceptionmsg(reason);
                record.setMainid(bankReconciliation.getId());
                record.setOsdid(bankReconciliation.get(DealDetailEnumConst.ODSID));
                record.setCreate_time(new Date());
                record.setTenant_id(AppContext.getTenantId());
                record.setYtenant_id(AppContext.getYTenantId());
                dealDetailRuleExecRecords.add(record);
            }
            bankDealDetailAccessDao.insertRuleExecRecords(dealDetailRuleExecRecords);
        }catch (Exception e1){
            log.error("异常堆栈入库失败",e1);
        }
    }

    public static void setSmartCheckNoInfo(BankReconciliation bankRecord) {
        // 解析出财资统一码并设置为true
        if (parseSmartCheckNo(bankRecord)) {
            return;
        }
        // 未解析出财资统一码，生成财资统一码并进行设置
        String smartCheckNo = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
        bankRecord.setSmartcheckno(smartCheckNo);
        bankRecord.setIsparsesmartcheckno(false);
    }

    private static boolean parseSmartCheckNo(BankReconciliation bankRecord) {
        Direction direction = bankRecord.getDc_flag();
        //财资统一对账码解析；只有支出明细才需要解析
        if (Direction.Debit != direction) {
            return false;
        }
        List<String> list  = new ArrayList<>();
        if (StringUtils.isNotEmpty(bankRecord.getRemark())) {
            list.add(bankRecord.getRemark());
        }
        if (StringUtils.isNotEmpty(bankRecord.getRemark01())) {
            list.add(bankRecord.getRemark01());
        }
        if (StringUtils.isNotEmpty(bankRecord.getUse_name())) {
            list.add(bankRecord.getUse_name());
        }
        String patternString = SMARTCHECKNO_SPLIT_SYMBOL+"([a-zA-Z0-9]{6,8})"+SMARTCHECKNO_SPLIT_SYMBOL;
        // 创建 Pattern 对象
        Pattern pattern = Pattern.compile(patternString);
        String subSearchString;
        for (String searchString : list) {
            // 默认财资统一码是 前后_和6-8位数字字母
            if (searchString.length() < 8) {
                continue;
            }
            if (searchString.length() > 10) {
                subSearchString = searchString.substring(searchString.length() - 10);
            } else {
                subSearchString = searchString;
            }
            // 创建 Matcher 对象
            Matcher matcher = pattern.matcher(subSearchString);
            if (matcher.find()) {
                // 获取匹配的内容
                String matchedContent = SMARTCHECKNO_SPLIT_SYMBOL + matcher.group(1) + SMARTCHECKNO_SPLIT_SYMBOL; // 添加回前后两个_
                if (StringUtils.isNotEmpty(matchedContent) && searchString.endsWith(matchedContent)) {
                    bankRecord.setSmartcheckno(matchedContent);
                    bankRecord.setIsparsesmartcheckno(true);
                    return true;
                }
            }
        }
        return false;
    }
}

