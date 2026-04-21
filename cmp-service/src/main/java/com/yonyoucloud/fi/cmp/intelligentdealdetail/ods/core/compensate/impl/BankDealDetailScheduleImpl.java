//package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.compensate.impl;
//import com.alibaba.fastjson.JSONObject;
//import com.yonyou.dianju.ekdm.sdk.cert.EkdmCert;
//import com.yonyou.diwork.ott.exexutors.RobotExecutors;
//import com.yonyou.iuap.context.InvocationInfoProxy;
//import com.yonyou.iuap.yms.datasource.ds.FededrateOperator;
//import com.yonyou.iuap.yms.datasource.ds.FededrateQuery;
//import com.yonyou.iuap.yms.datasource.ds.FededrateResult;
//import com.yonyou.iuap.yms.datasource.ds.itf.AbstractTransactionCallback;
//import com.yonyou.iuap.yms.param.SQLParameter;
//import com.yonyou.iuap.yms.param.condtition.query.QueryCondition;
//import com.yonyou.iuap.yms.processor.BaseProcessor;
//import com.yonyou.iuap.yms.processor.DTOListProcessor;
//import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
//import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.sqls.ODSSql;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.ODSBasicInfoObject;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.BankDealDetailAccessFacade;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.impl.ScheduleDealDetailConsumer;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.model.DealDetailConsumerDTO;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailRuleResult;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultBankDealDetailChain;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.model.DealDetailRuleExecRecord;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.compensate.IBankDealDetailSchedule;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
//import com.yonyoucloud.fi.cmp.util.DateUtils;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.imeta.orm.schema.QueryConditionGroup;
//import org.imeta.orm.schema.QuerySchema;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.dao.DataAccessException;
//import org.springframework.jdbc.core.ResultSetExtractor;
//import org.springframework.jdbc.core.SqlParameterValue;
//import org.springframework.jdbc.datasource.DataSourceTransactionManager;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.util.CollectionUtils;
//import javax.annotation.Resource;
//import java.sql.*;
//import java.util.*;
//import java.util.Date;
//import java.util.stream.Collectors;
///**
// * @Author guoyangy
// * @Date 2024/7/10 9:17
// * @Description 流水补偿机制
// * @Version 1.0
// */
//@Service
//@Slf4j
// @Deprecated
 /**
    * @deprecated
    */
//public class BankDealDetailScheduleImpl extends ODSBasicInfoObject implements IBankDealDetailSchedule {
//    @Value("${dealdetail.timeout:30}")
//    public int dealDetailTimeout;
//    @Value("${dealdetail.expire:30}")
//    public int dealDetailExpire;
//    @Resource(name="transactionManager")
//    private DataSourceTransactionManager transactionManager;
//    @Resource
//    private BankDealDetailAccessFacade bankDealDetailAccessFront;
//    @Resource
//    private IBankDealDetailAccessDao bankDealDetailAccessDao;
//    @Resource
//    private ScheduleDealDetailConsumer scheduleDealDetailConsumer;
//   // @Scheduled(cron = "0 0/5 * * * *")
//    @Override
//    public void updateProcessStatusToInit() {
//        long start = System.currentTimeMillis();
//        log.error("【修改状态-补偿任务】，修改流水状态开始执行");
//        Date currentDate = new Date();
//        try{
//            //step1:ods表最近一个月消费中超过30分钟的流水修改为未开始消费
//            this.updateProcessingToInit(ODSSql.QUERY_ODS_BY_PROCESSSTATUS, ODSSql.UPDATE_ODS_PROCESSSTATUS_TO_INIT,BankDealDetailODSModel.class);
//            //step2:流水业务表，流水辨识处理失败修改为未开始
//            SQLParameter failDealDetailMatchParam = new SQLParameter();
//            failDealDetailMatchParam.addParam(DateUtils.dateAddDays(currentDate,dealDetailExpire*(-1)));
//            failDealDetailMatchParam.addParam(currentDate);
//            this.updateByFederate(ODSSql.UPDATE_DEALDETAIL_PROCESSSTATUS_FROM_FAIL_TO_INIT,failDealDetailMatchParam);
//            //step3:流水业务表，开始辨识且超过30分钟修改为未开始
//            this.updateProcessingToInit(ODSSql.QUERY_DEALDETAIL_BY_PROCESSSTATUS, ODSSql.UPDATE_DEALDETAIL_PROCESSSTATUS_TO_INIT,BankReconciliation.class);
//            //step4:流水业务表，业务关联、业务凭据、生单、发布认领中心等处理失败的修改为未开始
//            this.updateByFederate(ODSSql.UPDATE_DEALDETAIL_RELEATED_PROCESSSTATUS_FROM_FAIL_TO_INIT,failDealDetailMatchParam);
//            this.updateByFederate(ODSSql.UPDATE_DEALDETAIL_BUSIRELEATED_PROCESSSTATUS_FROM_FAIL_TO_INIT,failDealDetailMatchParam);
//            this.updateByFederate(ODSSql.UPDATE_DEALDETAIL_GENERATE_PROCESSSTATUS_FROM_FAIL_TO_INIT,failDealDetailMatchParam);
//            this.updateByFederate(ODSSql.UPDATE_DEALDETAIL_PUBLISH_PROCESSSTATUS_FROM_FAIL_TO_INIT,failDealDetailMatchParam);
//            //step5:流水业务表，业务关联、业务凭据、生单、发布认领中心等处理中且超过30分钟的修改为未开始
//            this.updateProcessingToInit(ODSSql.QUERY_DEALDETAIL_BY_RELEATED_PROCESSSTATUS, ODSSql.UPDATE_DEALDETAIL_RELATED_PROCESSSTATUS_TO_INIT,BankReconciliation.class);
//            this.updateProcessingToInit(ODSSql.QUERY_DEALDETAIL_BY_BUSIRELEATED_PROCESSSTATUS, ODSSql.UPDATE_DEALDETAIL_BUSIRELATED_PROCESSSTATUS_TO_INIT,BankReconciliation.class);
//            this.updateProcessingToInit(ODSSql.QUERY_DEALDETAIL_BY_GENERATE_PROCESSSTATUS, ODSSql.UPDATE_DEALDETAIL_GENERATE_PROCESSSTATUS_TO_INIT,BankReconciliation.class);
//            this.updateProcessingToInit(ODSSql.QUERY_DEALDETAIL_BY_PUBLISH_PROCESSSTATUS, ODSSql.UPDATE_DEALDETAIL_PUBLISH_PROCESSSTATUS_TO_INIT,BankReconciliation.class);
//            log.error("【修改状态-补偿任务】，修改流水状态执行完成，耗时{}ms",(System.currentTimeMillis()-start));
//        }catch (Exception e){
//            log.error("【修改状态-补偿任务】修改流水状态失败",e);
//        }
//    }
//   // @Scheduled(cron = "0 0 0/30 * * *")
//    @Override
//    public void executeDealDetailHandler() {
//        long start = System.currentTimeMillis();
//        log.error("【拉起流水-补偿任务】，流水逻辑开始执行");
//        /**
//         * step1:ods补偿，流水未开始消费执行补偿处理
//         * */
//         this.noStartConsumerInODS();
//         /**
//          * step2:流水表补偿，按照租户维度补偿
//          * */
//         this.executeDealDetailByYtenantId();
//        log.error("【拉起流水-补偿任务】，流水逻辑执行完成，耗时{}ms",(System.currentTimeMillis()-start));
//    }
//    private void executeDealDetailByYtenantId() {
//        Date currentDate = new Date();
//        List<String> ytenantIdList = new ArrayList<>();
//        /**
//         * step1:查询出需要补偿的租户信息
//         * */
//        this.getTenantList(currentDate,ytenantIdList);
//        if(CollectionUtils.isEmpty(ytenantIdList)){
//            log.error("未查询出需要补偿的租户信息，本次补偿流程结束");
//            return;
//        }
//        /**
//         * step2:根据租户做补偿
//         * */
//        for(String ytenantId:ytenantIdList){
//            RobotExecutors.runAs(ytenantId,()->{
//                //流水表辨识补偿-未开始
//                this.noStartMatchInBankReconciliation(currentDate);
//                //流水表辨识补偿-辨识成功
//                this.matchSuccInBankReconciliation();
//                //关联处理流程-未开始
//                this.noStartProcessInBankReconciliation(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_NO_START.getStatus(),1,currentDate);
//                //生单处理流程-未开始
//                this.noStartProcessInBankReconciliation(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_NO_START.getStatus(),3,currentDate);
//                //发布认领处理流程-未开始
//                this.noStartProcessInBankReconciliation(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_NO_START.getStatus(),4,currentDate);
//            });
//        }
//    }
//    private void noStartMatchInBankReconciliation(Date currentDate) {
//        try {
//            Short processstatus = DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus();
//            int totalPage = this.needCallCount(currentDate,processstatus);
//            log.error("【补偿任务】查询租户{}未开始辨识流水有{}页，开始拉齐后续流程", InvocationInfoProxy.getTenantid(),totalPage);
//            for(int i=0;i<totalPage;i++){
//                List<BankReconciliation> bankReconciliations = this.queryBankReconcilitionByProcessstatus(currentDate,processstatus);
//                bankDealDetailAccessFront.dealDetailAccessByManual(bankReconciliations);
//            }
//        } catch (Exception e) {
//            log.error("【流水表辨识补偿】-未开始流水处理异常",e);
//        }
//    }
//    private void noStartProcessInBankReconciliation(Short processStatus,Integer index,Date currentDate) {
//        try{
//            int totalPage = this.needCallCount(currentDate,processStatus);
//            log.error("【补偿任务】查询租户{}processstatus={}有{}页，开始拉齐后续流程", InvocationInfoProxy.getTenantid(),processStatus,totalPage);
//            for(int i=0;i<totalPage;i++){
//                List<BankReconciliation> bankReconciliations = this.queryBankReconcilitionByProcessstatus(currentDate,processStatus);
//                if(!CollectionUtils.isEmpty(bankReconciliations)){
//                    Map<String,BankReconciliation> bankReconciliationMap =  bankReconciliations.stream().collect(Collectors.toMap(BankReconciliation::getId,each->each,(value1, value2) -> value1));
//                    List<DealDetailRuleExecRecord> execRecords = this.getDealDetailRuleExecRecords(bankReconciliations);
//                    List<BankDealDetailWrapper> bankDealDetailWrappers = new ArrayList<>();
//                    if(!CollectionUtils.isEmpty(bankReconciliations)){
//                        for(DealDetailRuleExecRecord ruleExecRecord : execRecords){
//                            try{
//                                String exerules = ruleExecRecord.getExerules();
//                                List<BankDealDetailRuleResult> ruleResults = JSONObject.parseArray(exerules,BankDealDetailRuleResult.class);
//                                BankDealDetailWrapper bankDealDetailWrapper = this.getBankDealDetailWrapper(bankReconciliationMap,ruleExecRecord,ruleResults);
//                                bankDealDetailWrappers.add(bankDealDetailWrapper);
//                            }catch (Exception e){
//                                log.error("流水{}ruleexec过长被截断，无法解析", bankReconciliationMap.get(ruleExecRecord.getMainid()),e);
//                            }
//                        }
//                        DefaultBankDealDetailChain.callDealDetailProcess(index,bankDealDetailWrappers);
//                    }
//                }
//            }
//        }catch (Exception e){
//            log.error("【流水表辨识补偿】凭据关联流程补偿失败",e);
//        }
//    }
//
//    private void matchSuccInBankReconciliation() {
//        Date currentDate = new Date();
//        try{
//            Short processstatus = DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_SUCC.getStatus();
//            int totalPage = this.needCallCount(currentDate,processstatus);
//            log.error("【补偿任务】查询租户{}辨识匹配成功有{}页，开始拉齐后续流程", InvocationInfoProxy.getTenantid(),totalPage);
//            for(int i=0;i<totalPage;i++){
//                List<BankReconciliation> bankReconciliations = this.queryBankReconcilitionByProcessstatus(currentDate,processstatus);
//                if(!CollectionUtils.isEmpty(bankReconciliations)){
//                    Map<String,BankReconciliation> bankReconciliationMap =  bankReconciliations.stream().collect(Collectors.toMap(BankReconciliation::getId,each->each,(value1, value2) -> value1));
//                    List<DealDetailRuleExecRecord> execRecords = this.getDealDetailRuleExecRecords(bankReconciliations);
//                    if(!CollectionUtils.isEmpty(execRecords)){
//                        log.error("【补偿任务】辨识匹配成功流水补偿，流水业务表获取流水数量{},过程表查出来的流水记录数量{}",bankReconciliations.size(),execRecords.size());
//                        //需要走关联处理流程
//                        List<BankDealDetailWrapper> system005RuleRecordList = new ArrayList<>();
//                        //需要走生单处理流程
//                        List<BankDealDetailWrapper> system007RuleRecordList = new ArrayList<>();
//                        for(DealDetailRuleExecRecord ruleExecRecord : execRecords){
//                            try{
//                                String exerules = ruleExecRecord.getExerules();
//                                List<BankDealDetailRuleResult> ruleResults = JSONObject.parseArray(exerules,BankDealDetailRuleResult.class);
//                                BankDealDetailRuleResult ruleResult = ruleResults.get(ruleResults.size()-1);
//                                String ruleCode = ruleResult.getRuleName();
//                                BankDealDetailWrapper bankDealDetailWrapper = this.getBankDealDetailWrapper(bankReconciliationMap,ruleExecRecord,ruleResults);
//                                if(StringUtils.isEmpty(ruleCode)){
//                                    system005RuleRecordList.add(bankDealDetailWrapper);
//                                }
//                                if(RuleCodeConst.SYSTEM007.equals(ruleCode)){
//                                    system007RuleRecordList.add(bankDealDetailWrapper);
//                                }
//                                if(ruleCode!=null &&(!RuleCodeConst.SYSTEM021.equals(ruleCode)&&!RuleCodeConst.SYSTEM022.equals(ruleCode)&&!RuleCodeConst.SYSTEM023.equals(ruleCode))){
//                                    system005RuleRecordList.add(bankDealDetailWrapper);
//                                }
//                            }catch (Exception e){
//                                //exerules过长，可能被截串了，无法解析
//                                //todo 修改流水状态为无法拉起
//                                log.error("流水{}ruleexec过长被截断，无法解析", bankReconciliationMap.get(ruleExecRecord.getMainid()),e);
//                            }
//                        }
//                        if(!CollectionUtils.isEmpty(system005RuleRecordList)){
//                            DefaultBankDealDetailChain.callDealDetailProcess(RuleCodeConst.SYSTEM005,system005RuleRecordList);
//                        }
//                        if(!CollectionUtils.isEmpty(system007RuleRecordList)){
//                            DefaultBankDealDetailChain.callDealDetailProcess(RuleCodeConst.SYSTEM007,system007RuleRecordList);
//                        }
//                    }else{
//                        log.error("【补偿任务】辨识匹配成功流水补偿，流水业务表获取流水数量{},过程表查出来的流水记录数量0,不一致，请查明原因",bankReconciliations.size());
//                    }
//                }
//            }
//        }catch (Exception e){
//            log.error("【流水表辨识补偿】对辨识成功流水进行补偿失败",e);
//        }
//    }
//
//    private List<DealDetailRuleExecRecord> getDealDetailRuleExecRecords(List<BankReconciliation> bankReconciliations) {
//        List<Object> bankReconciliationIds = bankReconciliations.stream().map(BankReconciliation::getId).collect(Collectors.toList());
//        List<DealDetailRuleExecRecord> execRecords = bankDealDetailAccessDao.queryDealDetailRuleExecRecordByMainid(bankReconciliationIds);
//        return execRecords;
//    }
//    private BankDealDetailWrapper getBankDealDetailWrapper(Map<String,BankReconciliation> bankReconciliationMap,DealDetailRuleExecRecord ruleExecRecord,List<BankDealDetailRuleResult> ruleResults) {
//        BankReconciliation currentBank = bankReconciliationMap.get(ruleExecRecord.getMainid());
//        Long bankreconciliationId = ruleExecRecord.getMainid();
//        //为流水赋值osdid
//        bankReconciliationMap.get(bankreconciliationId).put(DealDetailEnumConst.ODSID,ruleExecRecord.getOsdid());
//        BankDealDetailWrapper bankDealDetailWrapper= DealDetailUtils.convertBankReconciliationToWrapper(Arrays.asList(currentBank)).get(0);
//        bankDealDetailWrapper.setDealDetailRuleExecRecord(ruleExecRecord);
//        bankDealDetailWrapper.setRuleList(ruleResults);
//        return bankDealDetailWrapper;
//    }
//
//    private List<BankReconciliation> queryBankReconcilitionByProcessstatus(Date currentDate,Short processStatus) {
//        try{
//            // where (T0.processstatus=1 and T0.create_time between '2024-06-11 11:12:06' and '2024-07-11 11:12:06' and T0.ytenant_id='0000L6YTYEY5FUZPXE0000' and T0.tenant_id=2910033146761808) limit 0,50
//            QuerySchema querySchema = new QuerySchema().addSelect("*").addPager(0,DealDetailEnumConst.PAGESIZE);
//            QueryConditionGroup conditionGroup = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("processstatus").eq(processStatus));
//            conditionGroup.addCondition(org.imeta.orm.schema.QueryCondition.name("createTime").between(DateUtils.dateAddDays(currentDate,dealDetailExpire*(-1)),currentDate));//对账截至日期
//            querySchema.addCondition(conditionGroup);
//            List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
//            //addselect(*)如果有扩展字段则会left join扩展表,可能造成查询出重复流水。下面按照流水id去重
//            Map<Long, BankReconciliation> map = new HashMap<>();
//            for (BankReconciliation bankReconciliation : bankReconciliations) {
//                map.putIfAbsent(bankReconciliation.getId(), bankReconciliation);
//            }
//            Collection<BankReconciliation> collections =map.values();
//            List<BankReconciliation> resultList = new ArrayList<>();
//            if(CollectionUtils.isEmpty(collections)){
//                return resultList;
//            }
//            Iterator<BankReconciliation> iterator = collections.iterator();
//            while(iterator.hasNext()){
//                resultList.add(iterator.next());
//            }
//            return resultList;
//        }catch (Exception e){
//            log.error("流水查询异常",e);
//        }
//        return null;
//    }
//    private int needCallCount(Date currentDate,Short processStatus) {
//        try{
//            // where (T0.processstatus=1 and T0.create_time between '2024-06-11 11:12:06' and '2024-07-11 11:12:06' and T0.ytenant_id='0000L6YTYEY5FUZPXE0000' and T0.tenant_id=2910033146761808) limit 0,50
//            QuerySchema querySchema = new QuerySchema().addSelect("count(id)");
//            QueryConditionGroup conditionGroup = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("processstatus").eq(processStatus));
//            conditionGroup.addCondition(org.imeta.orm.schema.QueryCondition.name("createTime").between(DateUtils.dateAddDays(currentDate,dealDetailExpire*(-1)),currentDate));//对账截至日期
//            querySchema.addCondition(conditionGroup);
//            List<Map<String,Object>>bankReconciliations = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchema, null);
//            if(CollectionUtils.isEmpty(bankReconciliations)){
//                return 0;
//            }
//            int totalCount = Integer.valueOf(bankReconciliations.get(0).get("count").toString());
//            return (int)Math.ceil(totalCount/(DealDetailEnumConst.PAGESIZE*1.0));
//        }catch (Exception e){
//            log.error("流水分页异常",e);
//        }
//        return 0;
//    }
//    private void getTenantList(Date currentDate,List<String> ytenantIdList) {
//        try{
//            Map<String,List<String>> resultMap = new FededrateQuery(DealDetailEnumConst.LOGICDATASOURCE, ODSSql.QUERY_NEED_TENANT)
//                    .setParameter(new SqlParameterValue(Types.VARCHAR,DateUtils.dateFormat(DateUtils.dateAddDays(currentDate,dealDetailExpire*(-1)),DateUtils.DATE_TIME_PATTERN)),new SqlParameterValue(Types.VARCHAR,DateUtils.dateFormat(currentDate,DateUtils.DATE_TIME_PATTERN)))
//                    .executeForMap(new ResultSetExtractor<List<String>>() {
//                        @Override
//                        public List<String> extractData(ResultSet rs) throws SQLException, DataAccessException {
//                            List<String> ytenantIdList = new ArrayList<>();
//                            while(rs.next()){
//                                String ytenant_id = rs.getString("ytenant_id");
//                                ytenantIdList.add(ytenant_id);
//                            }
//                            return ytenantIdList;
//                        }
//                    });
//            Set<String> dsSet = resultMap.keySet();
//            if(!CollectionUtils.isEmpty(dsSet)){
//                Iterator<String> dsIterator = dsSet.iterator();
//                while(dsIterator.hasNext()){
//                    String key = dsIterator.next();
//                    ytenantIdList.addAll(resultMap.get(key));
//                }
//            }
//        }catch (Exception e){
//            log.error("【补偿任务】，查询需要补偿的租户信息异常",e);
//            throw new BankDealDetailException("【补偿任务】，查询需要补偿的租户信息异常");
//        }
//    }
//    private void noStartConsumerInODS() {
//        Date currentDate = new Date();
//        try{
//            List<BankDealDetailODSModel> bankDealDetailODSModelList = new ArrayList<>();
//            Map<String,List<BankDealDetailODSModel>> resultMap = new FededrateQuery(DealDetailEnumConst.LOGICDATASOURCE, ODSSql.ODS_COMPENSATE_COSSUMER_SQL)
//                    .setParameter(new SqlParameterValue(Types.VARCHAR,DateUtils.dateFormat(DateUtils.dateAddDays(currentDate,dealDetailExpire*(-1)),DateUtils.DATE_TIME_PATTERN)),new SqlParameterValue(Types.VARCHAR,DateUtils.dateFormat(currentDate,DateUtils.DATE_TIME_PATTERN)))
//                    .executeForMap(new ResultSetExtractor<List<BankDealDetailODSModel>>() {
//                        @Override
//                        public List<BankDealDetailODSModel> extractData(ResultSet rs) throws SQLException, DataAccessException {
//                            BaseProcessor processor = new DTOListProcessor(BankDealDetailODSModel.class);
//                            try {
//                                return (List<BankDealDetailODSModel>)processor.processResultSet(rs);
//                            } catch (Exception e) {
//                                log.error("流水查询失败",e);
//                            }
//                            return null;
//                        }
//                    });
//
//            Set<String> dsSet = resultMap.keySet();
//            if(!CollectionUtils.isEmpty(dsSet)){
//                Iterator<String> dsIterator = dsSet.iterator();
//                while(dsIterator.hasNext()){
//                    String key = dsIterator.next();
//                    bankDealDetailODSModelList.addAll(resultMap.get(key));
//                }
//            }
//            if(CollectionUtils.isEmpty(bankDealDetailODSModelList)){
//                log.error("【拉起流水-补偿任务-ods未消费流水】数据为空，ods补偿任务结束");
//                return;
//            }
//            for(BankDealDetailODSModel bankDealDetailODSModel:bankDealDetailODSModelList){
//                String traceId = bankDealDetailODSModel.getTraceid();
//                String requestSeqNo = bankDealDetailODSModel.getRequest_seq_no();
//                String ytenant_id = bankDealDetailODSModel.getYtenant_id();
//                if(StringUtils.isEmpty(traceId)||StringUtils.isEmpty(requestSeqNo)){
//                    log.error("【补偿任务-ods未消费流水】traceId={},requestSeqNo={} 不能为空",traceId,requestSeqNo);
//                    throw new BankDealDetailException("补偿任务-ods未消费流水,traceId和requestSeqNo不能为空");
//                }
//                RobotExecutors.runAs(ytenant_id,()->{
//                    DealDetailConsumerDTO eventBusModel = new DealDetailConsumerDTO(traceId,requestSeqNo);
////                    eventBus.post(eventBusModel);
//                    scheduleDealDetailConsumer.asyncBankDealDetailConsumer(eventBusModel);
//                    log.error("【拉起流水-补偿任务-ods未消费流水】已发送guava事件,traceId={},requestSeqNo={}",traceId,requestSeqNo);
//                });
//            }
//        }catch (Exception e){
//            log.error("拉起流水-补偿任务-ods未消费流水处理失败",e);
//        }
//    }
//    private  <T> void updateProcessingToInit(String sql,String updateSql,T clzz){
//        List<T> dtoList = new ArrayList<>();
//        Date currentDate = new Date();
//        try{
//            if(((Class) clzz).getSimpleName().equals(BankReconciliation.class.getSimpleName())){
//               Map<String,List<BankReconciliation>> resultMap = new FededrateQuery(DealDetailEnumConst.LOGICDATASOURCE,sql)
//                        .setParameter(new SqlParameterValue(Types.VARCHAR,DateUtils.dateFormat(DateUtils.dateAddDays(currentDate,dealDetailExpire*(-1)),DateUtils.DATE_TIME_PATTERN)),new SqlParameterValue(Types.VARCHAR,DateUtils.dateFormat( DateUtils.dateAddMinutes(currentDate,dealDetailTimeout*(-1)),DateUtils.DATE_TIME_PATTERN)))
//                        .executeForMap(new ResultSetExtractor<List<BankReconciliation>>(){
//                            @Override
//                            public List<BankReconciliation> extractData(ResultSet rs) throws SQLException, DataAccessException {
//                                List<BankReconciliation> bankReconciliations = new ArrayList<>();
//                                while(rs.next()){
//                                    BankReconciliation b = new BankReconciliation();
//                                    Long id = rs.getLong("id");
//                                    Date create_time = rs.getDate("create_time");
//                                    b.setId(id);
//                                    b.setCreateTime(create_time);
//                                    bankReconciliations.add(b);
//                                }
//                                return bankReconciliations;
//                            }
//                        });
//                Set<String> dsSet = resultMap.keySet();
//               if(!CollectionUtils.isEmpty(dsSet)){
//                  Iterator<String> dsIterator = dsSet.iterator();
//                  while(dsIterator.hasNext()){
//                      String key = dsIterator.next();
//                      List<BankReconciliation> bankReconciliations = resultMap.get(key);
//                      dtoList.addAll((List<T>)bankReconciliations);
//                  }
//               }
//
//            }else{
//                Map<String,List<BankDealDetailODSModel>> resultMap = new FededrateQuery(DealDetailEnumConst.LOGICDATASOURCE,sql)
//                        .setParameter(new SqlParameterValue(Types.VARCHAR,DateUtils.dateFormat(DateUtils.dateAddDays(currentDate,dealDetailExpire*(-1)),DateUtils.DATE_TIME_PATTERN)),
//                                new SqlParameterValue(Types.VARCHAR,DateUtils.dateFormat( DateUtils.dateAddMinutes(currentDate,dealDetailTimeout*(-1)),DateUtils.DATE_TIME_PATTERN)))
//                        .executeForMap(new ResultSetExtractor<List<BankDealDetailODSModel>>(){
//                            @Override
//                            public List<BankDealDetailODSModel> extractData(ResultSet rs) throws SQLException, DataAccessException {
//                                List<BankDealDetailODSModel> odsModels = new ArrayList<>();
//                                while(rs.next()){
//                                    BankDealDetailODSModel b = new BankDealDetailODSModel();
//                                    Long id = rs.getLong("id");
//                                    Date create_time = rs.getDate("create_time");
//                                    b.setId(id+"");
//                                    b.setCreate_time(create_time);
//                                    odsModels.add(b);
//                                }
//                                return odsModels;
//                            }
//                        });
//                Set<String> dsSet = resultMap.keySet();
//                if(!CollectionUtils.isEmpty(dsSet)){
//                    Iterator<String> dsIterator = dsSet.iterator();
//                    while(dsIterator.hasNext()){
//                        String key = dsIterator.next();
//                        List<BankDealDetailODSModel> odsModels = resultMap.get(key);
//                        dtoList.addAll((List<T>)odsModels);
//                    }
//                }
//            }
//            if(!CollectionUtils.isEmpty(dtoList)){
//                List<String> ids = new ArrayList<>();
//                for(T t : dtoList){
//                    String id = null;
//                    Date createDate = null;
//                    if(t instanceof BankDealDetailODSModel){
//                        BankDealDetailODSModel odsModel = (BankDealDetailODSModel) t;
//                        id = odsModel.getId();
//                        createDate = odsModel.getCreate_time();
//                    }else if(t instanceof BankReconciliation){
//                        BankReconciliation bankReconciliation = (BankReconciliation) t;
//                        id = (bankReconciliation.getId() instanceof String ? bankReconciliation.getId() : bankReconciliation.getId().toString());
//                        createDate = bankReconciliation.getCreateTime();
//                    }
//                    if(createDate == null|| StringUtils.isEmpty(id)){
//                        continue;
//                    }
//                    ids.add(id);
//                }
//
//                if(!CollectionUtils.isEmpty(ids)){
//                    QueryCondition condition = new QueryCondition<>();
//                    condition.in("id", ids);
//                    SQLParameter sqlParameter = condition.getSQLParameter();
//                    this.updateByFederate(updateSql + condition.getCondition(),sqlParameter);
//                }
//            }
//        }catch (Exception e){
//            log.error("【补偿任务】流水状态更新失败",e);
//        }
//    }
//    private void updateByFederate(String sql, SQLParameter sqlParameter){
//        List<FededrateResult> result = new FededrateOperator(DealDetailEnumConst.LOGICDATASOURCE).setPlatformTransactionManager(transactionManager)
//                .executeWithTransaction(new AbstractTransactionCallback() {
//                    @Override
//                    public Object execute() {
//                        int result = ymsJdbcApi.update(sql,sqlParameter);
//                        log.error("【补偿任务】sql{}更新{}条流水或ods",sql,result);
//                        return result;
//                    }
//                });
//    }
//}