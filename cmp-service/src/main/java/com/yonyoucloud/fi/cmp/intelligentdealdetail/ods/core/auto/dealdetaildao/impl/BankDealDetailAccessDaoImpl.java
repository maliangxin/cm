package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.impl;

import com.alibaba.fastjson.JSON;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyou.iuap.yms.param.ResultSetProcessor;
import com.yonyou.iuap.yms.param.SQLParameter;
import com.yonyou.iuap.yms.param.condtition.query.QueryCondition;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.CtmAppContext;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ClaimCompleteType;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataService;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.flowhandlesetting.Flowhandlesetting;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.converts.BankdealDetailOdsConvertManager;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.ODSBasicInfoObject;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSFailModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailOperLogModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.functional.IDealDetailCallBack;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.model.DealDetailRuleExecRecord;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailExceptionCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.sqls.BankReconciliationSql;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.sqls.ODSSql;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.OdsCommonUtils;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;
/**
 * @Author guoyangy
 * @Date 2024/7/23 17:24
 * @Description todo
 * @Version 1.0
 */
@Service
@Slf4j
public class BankDealDetailAccessDaoImpl extends ODSBasicInfoObject implements IBankDealDetailAccessDao {

    @Resource
    BankdealDetailOdsConvertManager bankdealDetailOdsConvertManager;

    // 假设这些常量定义在类中或某个常量类中
    private static final short CONFIRM_STATUS_YES = 1;
    private static final short CONFIRM_STATUS_NO = 0;
    private static final short CONFIRM_TYPE_ARTIFICIAL = 1;

    @Override
    public Long getBankReconciliationTotalCountByProcessstatus(String processstatus) {
        if(StringUtils.isEmpty(processstatus)){
            return 0L;
        }
        String querySql = BankReconciliationSql.TOTALCOUNTBYCONCATINFO;
        SQLParameter sqlParameter = new SQLParameter();
        sqlParameter.addParam(Short.parseShort(processstatus));

        Object totalCount = ymsJdbcApi.queryForObject(querySql, sqlParameter, new ResultSetProcessor() {
            @Override
            public Object handleResultSet(ResultSet rs) throws SQLException {
                if(rs.next()){
                    return rs.getLong(BankReconciliationSql.TOTALCOUNT);
                }
                return null;
            }
            @Override
            public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                return null;
            }
        });
        return totalCount == null ? 0 : Long.parseLong(totalCount.toString());
    }

    @Override
    public List<Long> getBankReconciliationIdByProcessstatus(String processstatus) {
        if(StringUtils.isEmpty(processstatus)){
            return null;
        }
        String querySql = BankReconciliationSql.GETALLBANKRECONCILIATIONIDBYPROCESSSTATUS;
        SQLParameter sqlParameter = new SQLParameter();
        sqlParameter.addParam(Short.parseShort(processstatus));

        List<Long> bankReconciliationList =  ymsJdbcApi.queryForList(querySql, sqlParameter, new ResultSetProcessor() {
            @Override
            public Object handleResultSet(ResultSet rs) throws SQLException {
                List<Long> bankReconciliationList = new ArrayList<>();
                while(rs.next()){
                    Long id = rs.getLong("id");
                    bankReconciliationList.add(id);
                }
                return bankReconciliationList;
            }
            @Override
            public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                return null;
            }
        });
        List<Long> bankIds = bankReconciliationList.stream().distinct().collect(Collectors.toList());
        return bankIds;
    }

    @Override
    public List<BankReconciliation> getBankReconciliationByPage(String concatInfo,int pageSize) throws Exception {
//        QuerySchema querySchema = new QuerySchema().addSelect("*").addPager(0,pageSize);
//        QueryConditionGroup conditionGroup = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("processstatus").eq(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_IMPORT_INIT.getStatus()));
//        conditionGroup.addCondition(org.imeta.orm.schema.QueryCondition.name(BankReconciliationSql.CONDITION).eq(concatInfo));
//        querySchema.addCondition(conditionGroup);
//        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
//        if(!CollectionUtils.isEmpty(bankReconciliations)){
//            bankReconciliations = bankReconciliations.stream().distinct().collect(Collectors.toList());
//        }
//        return bankReconciliations;
        return null;
    }

    @Override
    public List<BankReconciliation> getBankReconciliationByPage(Short processstatus,List<Long> bankReconciliationIds) throws Exception {

        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("processstatus").eq(processstatus));
        conditionGroup.addCondition(org.imeta.orm.schema.QueryCondition.name("id").in(bankReconciliationIds));
        querySchema.addCondition(conditionGroup);
        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        if(!CollectionUtils.isEmpty(bankReconciliations)){
            bankReconciliations = bankReconciliations.stream().distinct().collect(Collectors.toList());
        }
        return bankReconciliations;
    }

    @Override
    public List<BankReconciliation> getBankReconciliationByIdsAndprocessstatus(List<Long> ids,Short processstatus) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("processstatus").eq(processstatus));
        conditionGroup.addCondition(org.imeta.orm.schema.QueryCondition.name("id").in(ids));
        querySchema.addCondition(conditionGroup);
        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        if(!CollectionUtils.isEmpty(bankReconciliations)){
            bankReconciliations = bankReconciliations.stream().distinct().collect(Collectors.toList());
        }
        return bankReconciliations;
    }

    @Override
    public List<BankReconciliation> getBankReconciliationById(Long id) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("id").eq(id));
        querySchema.addCondition(conditionGroup);
        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        if(!CollectionUtils.isEmpty(bankReconciliations)){
            bankReconciliations = bankReconciliations.stream().distinct().collect(Collectors.toList());
            return bankReconciliations;
        }
        return bankReconciliations;
    }

    @Override
    @Transactional(propagation=Propagation.REQUIRED,rollbackFor = RuntimeException.class)
    public void updateProcessAfterExcepton(List<BankReconciliation> saveBankReconciliationList,
                                           List<BankReconciliation>updateBankReconciliationList,
                                           List<BankReconciliation> needTODBList,
                                           BankDealDetailContext context,
                                           BankDealDetailException exception,
                                           BankreconciliationIdentifyType currentRule) {
        IBankDealDetailAccessDao bankDealDetailAccessDao = CtmAppContext.getBean(IBankDealDetailAccessDao.class);
        int processstatus = DealDetailUtils.getNeedUpdateProcessstatusByRule(context);
        if(!CollectionUtils.isEmpty(saveBankReconciliationList)){
            //流水新增异常,修改ods表状态为未消费
            List<Object> bankReconciliationIds = saveBankReconciliationList.stream().map(BankReconciliation::getId).collect(Collectors.toList());
            String opertype = RuleCodeConst.getOperType(currentRule.getCode());
            if(RuleCodeConst.OPERTYPE_MATCH.equals(opertype)){
                saveBankReconciliationList.stream().forEach(b->b.setProcessstatus(Short.parseShort(processstatus+"")));
                bankDealDetailAccessDao.batchConvertAndSaveBankDealDetail(saveBankReconciliationList);
                bankDealDetailAccessDao.batchSaveBankReconciliation(saveBankReconciliationList);
            }else if(RuleCodeConst.OPERTYPE_PROCESS.equals(opertype)){
                bankDealDetailAccessDao.batchUpdateOdsProcessstatusByIds(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_PROCESSFAIL.getProcessstatus(),bankReconciliationIds);
            }
        }
        if(!CollectionUtils.isEmpty(updateBankReconciliationList)){
            //批量更新流水状态
            bankDealDetailAccessDao.batchUpdateDealDetailProcessstatusByIds(Short.parseShort(processstatus+""),updateBankReconciliationList);
        }
        //判断是否需要更新过程表
        //调辨识规则异常还没来得及修改执行过程对象
        this.updateDealDetailRuleExecRecords(context,exception,currentRule,bankDealDetailAccessDao);
    }

    private void updateDealDetailRuleExecRecords(BankDealDetailContext context,Exception exception,BankreconciliationIdentifyType currentRule,IBankDealDetailAccessDao bankDealDetailAccessDao) {
        List<DealDetailRuleExecRecord> dealDetailRuleExecRecords = DealDetailUtils.getRuleExecRecordList(context,exception,currentRule);
        if (!CollectionUtils.isEmpty(dealDetailRuleExecRecords)){
            String opertype = RuleCodeConst.getOperType(currentRule.getCode());
            if(RuleCodeConst.OPERTYPE_MATCH.equals(opertype)){
                bankDealDetailAccessDao.insertRuleExecRecords(dealDetailRuleExecRecords);
            }else if(RuleCodeConst.OPERTYPE_PROCESS.equals(opertype)){
                bankDealDetailAccessDao.updateRuleExecRecords(dealDetailRuleExecRecords);
            }
        }
    }
    @Override
    public List<BankDealDetailODSModel> batchQueryODSByContentSign(Set<String> contentSignatureList){
        if(CollectionUtils.isEmpty(contentSignatureList)){
            return null;
        }
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.in("contentsignature",contentSignatureList);
        SQLParameter sqlParameter = queryCondition.getSQLParameter();
        String querySql = ODSSql.QUERY_CONTENTSIGN_FROM_ODS+queryCondition.getCondition();
        List<BankDealDetailODSModel> existsSign = ymsJdbcApi.queryForDTOList(querySql,sqlParameter,BankDealDetailODSModel.class);
        return existsSign;
    }
    @Override
    public List<BankDealDetailODSModel> queryODSByMainid(Long mainid){
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.in("mainid",mainid);
        SQLParameter sqlParameter = queryCondition.getSQLParameter();
        String querySql = ODSSql.QUERY_CONTENTSIGN_FROM_ODS+queryCondition.getCondition();
        List<BankDealDetailODSModel> existsSign = ymsJdbcApi.queryForDTOList(querySql,sqlParameter,BankDealDetailODSModel.class);
        return existsSign;
    }
    @Override
    public List<BankDealDetailODSModel> batchQueryODSByMainid(List<Long> mainids){
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.in("mainid",mainids);
        SQLParameter sqlParameter = queryCondition.getSQLParameter();
        String querySql = ODSSql.QUERY_CONTENTSIGN_FROM_ODS+queryCondition.getCondition();
        List<BankDealDetailODSModel> existsSign = ymsJdbcApi.queryForDTOList(querySql,sqlParameter,BankDealDetailODSModel.class);
        return existsSign;
    }
    @Override
    public void dealDetailInsert(BankDealDetailOperLogModel dealDetailOperLogModel, List<BankDealDetailODSModel> odsDealDetails, List<BankDealDetailODSFailModel> exceptionDealDetails) {
        //step1: 入库
//        if(null!=dealDetailOperLogModel){
//            ymsJdbcApi.insert(dealDetailOperLogModel);
//        }
        if(!CollectionUtils.isEmpty(odsDealDetails)){
            ymsJdbcApi.insert(odsDealDetails);
        }
        try {
            if(!CollectionUtils.isEmpty(exceptionDealDetails)){
                List<String> contentsignatureList = exceptionDealDetails.stream().map(BankDealDetailODSFailModel::getContentsignature).collect(Collectors.toList());
                String paramsQuery = "";
                if (!CollectionUtils.isEmpty(contentsignatureList) && contentsignatureList.size()>1){
                    for (int i=0;i<contentsignatureList.size()-1;i++) {
                        paramsQuery+="'"+contentsignatureList.get(i)+"',";
                    }
                }
                if (!CollectionUtils.isEmpty(contentsignatureList)){
                    paramsQuery+="'"+contentsignatureList.get(contentsignatureList.size()-1)+"'";
                }
                String sql = "select contentsignature from cmp_bankdealdetail_ods_fail where contentsignature in ("+paramsQuery+")";
                List<String> contentsignatures = ymsJdbcApi.queryForList(sql, new ResultSetProcessor() {
                    @Override
                    public Object handleResultSet(ResultSet rs) throws SQLException {
                        List<String> list = new ArrayList<>();
                        while (rs.next()) {
                            String contentsignature = rs.getString(1);
                            list.add(contentsignature);
                        }
                        return list;
                    }
                    @Override
                    public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                        return null;
                    }
                });
                //不存在再插入
                List<BankDealDetailODSFailModel> insertDealDetails = exceptionDealDetails.stream().filter(model -> !contentsignatures.contains(model.getContentsignature())).collect(Collectors.toList());
                ymsJdbcApi.insert(insertDealDetails);
            }
        }catch (Exception e){
            log.error("批量ODS异常表插入失败",e);
        }
    }
    @Override
    public void dealDetailInsertAndUpdate(List<BankDealDetailODSModel> odsDealDetails, List<BankDealDetailODSModel> updateDealDetails,int processstatus,String traceId,String requestSeqNo) {
        //step1: 入库
        if(!CollectionUtils.isEmpty(odsDealDetails)){
            ymsJdbcApi.insert(odsDealDetails);
        }
        if(!CollectionUtils.isEmpty(updateDealDetails)){
            List<Object> odsList = updateDealDetails.stream().map(BankDealDetailODSModel::getId).collect(Collectors.toList());
            QueryCondition condition = new QueryCondition<>();
            condition.in("id", odsList);
            List<Object> objects = condition.getSQLParameter().getParameters();
            SQLParameter params = new SQLParameter();
            params.addParam(processstatus);
            params.addParam(traceId);
            params.addParam(requestSeqNo);
            params.addAllParam(objects);
            ymsJdbcApi.update(ODSSql.UPDATE_ODS_BYPROCESSSTATUSANDTRACEID+condition.getCondition(),params);

            //todo 银行流水procoessstatus！=25情况下修改为processstatus=1[因为ods的状态改为1了，流水状态也同步改成1]
        }
    }

    @Override
    public void batchUpdateDealDetailProcessstatusToInitByIdsWithoutPubts(int processstatus, List<BankReconciliation> updateBankReconciliationList) {
        if(CollectionUtils.isEmpty(updateBankReconciliationList)){
            return;
        }
        try{
            String updateSql = ODSSql.UPDATE_DEALDETAIL_PROCESSSTATUS_BY_IDS_WITHOUT_PUBTS;
            List<SQLParameter> sqlParameters = new ArrayList<>();
            for(BankReconciliation b:updateBankReconciliationList){
                SQLParameter params = new SQLParameter();
                params.addParam(processstatus);
                params.addParam((Long)b.getId());
                params.addParam(InvocationInfoProxy.getTenantid());
                sqlParameters.add(params);
            }
            ymsJdbcApi.batchUpdate(updateSql,sqlParameters);
        }catch (Exception e){
            log.error("批量修改流水表状态异常",e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400773", "批量修改流水表状态异常") /* "批量修改流水表状态异常" */);
        }
    }
    @Override
    public void batchUpdateODSprocessstatus(List<BankReconciliation> bankReconciliations, int odsprocessstatus) {
        List<SQLParameter> sqlParameters = this.getSqlParameters(bankReconciliations,odsprocessstatus+"");
        if(CollectionUtils.isEmpty(sqlParameters)){
            return;
        }
        this.deleteOds(bankReconciliations);
        ymsJdbcApi.batchUpdate(ODSSql.UPDATE_PROCESS_STATUS_AND_MAIN_ID_ODS_BY_ID,sqlParameters);
    }

    private void deleteOds(List<BankReconciliation> bankReconciliations){
        try{
            List<String> bankReconciliationIds = bankReconciliations.stream().filter(bankReconciliation -> !Objects.isNull(bankReconciliation.getId())).map(bankReconciliation->bankReconciliation.getId().toString()).collect(Collectors.toList());
            List<String> odsIds = bankReconciliations.stream().filter(bankReconciliation -> !Objects.isNull(bankReconciliation.get(DealDetailEnumConst.ODSID))).map(bankReconciliation->bankReconciliation.get(DealDetailEnumConst.ODSID).toString()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(bankReconciliationIds) || CollectionUtils.isEmpty(odsIds)){
                return;
            }
            StringBuilder bankReconciliationIdsString = new StringBuilder();
            for (int i= 0; i < bankReconciliationIds.size()-1; i++){
                bankReconciliationIdsString.append("'"+bankReconciliationIds.get(i)+"',");
            }
            bankReconciliationIdsString.append("'"+bankReconciliationIds.get(bankReconciliationIds.size()-1)+"'");
            StringBuilder odsIdsString = new StringBuilder();
            for (int i= 0; i < odsIds.size()-1; i++){
                odsIdsString.append("'"+odsIds.get(i)+"',");
            }
            odsIdsString.append("'"+odsIds.get(odsIds.size()-1)+"'");
            String UPDATE_ODS_BYPROCESSSTATUSANDTRACEID="select id from cmp_bankdealdetail_ods where id not in ("+odsIdsString+") and mainid in ("+bankReconciliationIdsString+") and ytenant_id='"+InvocationInfoProxy.getTenantid()+"'";
            List<BankDealDetailODSModel> list_ods = ymsJdbcApi.queryForList(UPDATE_ODS_BYPROCESSSTATUSANDTRACEID, new ResultSetProcessor() {
                @Override
                public Object handleResultSet(ResultSet rs) throws SQLException {
                    List<BankDealDetailODSModel> list = new ArrayList<>();
                    while (rs.next()) {
                        String id = rs.getString(1);
                        BankDealDetailODSModel bankDealDetailODSModel = new BankDealDetailODSModel();
                        bankDealDetailODSModel.setId(id);
                        list.add(bankDealDetailODSModel);
                    }
                    return list;
                }
                @Override
                public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                    return null;
                }
            });
            if (!com.yonyou.cloud.utils.CollectionUtils.isEmpty(list_ods)){
                ymsJdbcApi.remove(list_ods);
            }
        }catch (Exception e){
            log.error("批量删除ods异常",e);
        }
    }

    @Override
    public void batchUpdateODSprocessstatus(Map<String, List<BankReconciliation>> resultMap) {
        List<SQLParameter> sqlParameters = new ArrayList<>();
        for(Map.Entry<String,List<BankReconciliation>> entry : resultMap.entrySet()){
            String processstatus = entry.getKey();
            List<BankReconciliation> bankReconciliationList = entry.getValue();
            if(!CollectionUtils.isEmpty(bankReconciliationList)){
                this.deleteOds(bankReconciliationList);
                sqlParameters.addAll(Optional.ofNullable(this.getSqlParameters(bankReconciliationList,processstatus)).orElse(new ArrayList<>()));
            }
        }
        if(CollectionUtils.isEmpty(sqlParameters)){
            return;
        }
        ymsJdbcApi.batchUpdate(ODSSql.UPDATE_PROCESS_STATUS_AND_MAIN_ID_ODS_BY_ID,sqlParameters);
    }

    public List<SQLParameter> getSqlParameters(List<BankReconciliation> bankReconciliations,String processStatus) {
        if(CollectionUtils.isEmpty(bankReconciliations)){
            return null;
        }
        List<SQLParameter> sqlParameters = bankReconciliations.stream()
                .map(bankReconciliation -> {
                    SQLParameter sqlParameter = new SQLParameter();
                    sqlParameter.addParam(processStatus);
                    sqlParameter.addParam((Long)bankReconciliation.getId());
                    sqlParameter.addParam((String) bankReconciliation.get(DealDetailEnumConst.ODSID));
                    return sqlParameter;
                }).collect(Collectors.toList());
        return sqlParameters;
    }
    @Override
    public void batchSaveBankReconciliation(List<BankReconciliation> bankReconciliationList) {
        try {
            bankReconciliationList.stream().forEach(e -> {
                e.setEntityStatus(EntityStatus.Insert);
                e.setCreateTime(new Date());
                e.setDataOrigin(DateOrigin.DownFromYQL);
                CommonSaveUtils.setOppositeobjectidToBankReconciliationField(e, e.getOppositetype(), e.getOppositeobjectid() != null ? Long.parseLong(e.getOppositeobjectid()) : null);
                try {
                    if (!e.containsKey("isrepeat") || e.getIsRepeat() == null) {
                        //疑重判断
                        e.setIsRepeat((short) BankDealDetailConst.REPEAT_INIT);
                        Map<String, Object> map = new HashMap<>();
                        map.put("startDate", DateUtils.convertToStr(e.getTran_date(),"yyyy-MM-dd"));
                        CtmCmpCheckRepeatDataService checkRepeatDataService = AppContext.getBean(CtmCmpCheckRepeatDataService.class);
                        checkRepeatDataService.deal4FactorsBankDealDetail(Collections.singletonList(e), map);
                    }
                }catch (Exception ex){
                    log.error("疑重处理失败！",ex);
                    e.setIsRepeat((short) BankDealDetailConst.REPEAT_INIT);
                }
            });
            CmpMetaDaoHelper.insert(BankReconciliation.ENTITY_NAME,bankReconciliationList);
        }catch (Exception e){
            log.error("银行对账单插入失败",e);
            throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_SAVE_ERROR.getErrCode(),BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_SAVE_ERROR.getMsg()+e.getMessage());
        }
    }
    @Override
    public void batchUpdateBankReconciliation(List<BankReconciliation> bankReconciliationList) {
        try {
            bankReconciliationList.stream().forEach(e->{
                e.setEntityStatus(EntityStatus.Update);
                CommonSaveUtils.setOppositeobjectidToBankReconciliationField(e, e.getOppositetype(),  StringUtils.isNotEmpty(e.getOppositeobjectid()) ? Long.parseLong(e.getOppositeobjectid()) : null);// e.setPubts(null);

                //关联或生单
                if (e.getAssociationstatus() == null || AssociationStatus.Associated.getValue() != e.getAssociationstatus()){
                    //关联关系
                    e.remove("associationstatus");
                    //待确认
                    e.remove("relationstatus");
                    //完结状态
                    if (e.getSerialdealtype() != null && ClaimCompleteType.NoProcess.getValue() != e.getSerialdealtype()){
                        e.remove("serialdealendstate");
                    }
                    //自动关联状态
                    e.remove("autodealstate");
                    //是否自动生单
                    e.remove("isautocreatebill");
                }else if (e.getAssociationstatus() != null && AssociationStatus.Associated.getValue() == e.getAssociationstatus() &&
                        e.getSerialdealendstate() != null && SerialdealendState.END.getValue() != e.getSerialdealendstate()){
                    //完结状态(当关联状态是已关联的则必须已完结状态才会更新完结状态)
                    e.remove("serialdealendstate");
                }
            });
            CommonSaveUtils.updateBankReconciliation(bankReconciliationList);
        }catch (Exception e){
            log.error("银行对账单更新失败",e);
            throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_UPDATE_ERROR.getErrCode(),BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_UPDATE_ERROR.getMsg());
        }
    }
    @Override
    public void batchUpdateOdsProcessstatusByIds(int processstatus, List<Object> odsList) {
        if(CollectionUtils.isEmpty(odsList)){
            return;
        }
        try{
            QueryCondition condition = new QueryCondition<>();
            condition.in("id", odsList);
            condition.eq("ytenant_id", InvocationInfoProxy.getTenantid());
            List<Object> idsparam = condition.getSQLParameter().getParameters();
            String updateSql = ODSSql.UPDATE_ODS_PROCESSSTATUS_BY_IDS+condition.getCondition();
            SQLParameter params = new SQLParameter();
            params.addParam(processstatus);
            params.addAllParam(idsparam);
            ymsJdbcApi.update(updateSql,params);
        }catch (Exception e){
            log.error("批量修改ods流水状态异常",e);
            throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_ODS_UPDATE_ERROR.getErrCode(),BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_ODS_UPDATE_ERROR.getMsg());
        }
    }
    @Override
    public void batchUpdateOdsProcessstatusByIds(String sql, List<SQLParameter> parameter) {
        if(CollectionUtils.isEmpty(parameter)){
            return;
        }
        try{
            ymsJdbcApi.batchUpdate(ODSSql.UPDATE_PROCESS_STATUS_ODS_BY_ID, parameter);
        }catch (Exception e){
            log.error("批量修改ods流水状态异常",e);
            throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_ODS_UPDATE_ERROR.getErrCode(),BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_ODS_UPDATE_ERROR.getMsg());
        }
    }
    @Override
    public void batchUpdateDealDetailProcessstatusByIds(int processstatus, List<BankReconciliation> updateBankReconciliationList) {
        if(CollectionUtils.isEmpty(updateBankReconciliationList)){
            return;
        }
        try{
            String updateSql = ODSSql.UPDATE_DEALDETAIL_PROCESSSTATUS_BY_IDS;
            List<SQLParameter> sqlParameterList = new ArrayList<>();
            for(BankReconciliation b: updateBankReconciliationList){
                SQLParameter params = new SQLParameter();
                params.addParam(processstatus);
                params.addParam((Long)b.getId());
                params.addParam(InvocationInfoProxy.getTenantid());
                sqlParameterList.add(params);
            }
            ymsJdbcApi.batchUpdate(updateSql,sqlParameterList);
        }catch (Exception e){
            log.error("批量修改流水表状态异常",e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400773", "批量修改流水表状态异常") /* "批量修改流水表状态异常" */);
        }
    }
    @Override
    public void batchConvertAndSaveBankDealDetail(List<BankReconciliation> bankReconciliationList) {
        if(CollectionUtils.isEmpty(bankReconciliationList)){
            return;
        }
        try {
            List<BankDealDetail> bankDealDetails = bankdealDetailOdsConvertManager.convertBankReconciliationTOBankDealDetail(bankReconciliationList);
            CmpMetaDaoHelper.insert(BankDealDetail.ENTITY_NAME, bankDealDetails);
        }catch (Exception e){
            log.error("BankReconciliation实体转BankDealDetail实体异常",e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400774", "BankReconciliation实体转BankDealDetail实体异常") /* "BankReconciliation实体转BankDealDetail实体异常" */);
        }
    }
    @Transactional(propagation = Propagation.NOT_SUPPORTED, rollbackFor = Exception.class)
    @Override
    public void updateProcessstatusOnlyPreHandle(Short processStatus, int odsProcessStatus, List<BankReconciliation> bankReconciliationList) {
        if(CollectionUtils.isEmpty(bankReconciliationList)){
            return ;
        }
        List<SQLParameter> odsSqlParameters = new ArrayList<>();
        List<SQLParameter> bankreconciliationSqlParameters = new ArrayList<>();
        for(BankReconciliation bankReconciliation:bankReconciliationList){
            String odsId = bankReconciliation.get(DealDetailEnumConst.ODSID);
            if(StringUtils.isEmpty(odsId)){
                log.error("更新银行流水和ods表的processstatus字段检查到流水账odsid为空");
//                throw new BankDealDetailException("更新银行流水和ods表的processstatus字段检查到流水账odsid为空");
            }else{
                SQLParameter odsSQLParameter = new SQLParameter();
                odsSQLParameter.addParam(odsProcessStatus);
                odsSQLParameter.addParam(odsId);
                odsSqlParameters.add(odsSQLParameter);
            }
            if(bankReconciliation.getId() == null){
                log.error("更新银行流水和ods表的processstatus字段检查到流水主键为空");
                throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400777", "更新银行流水和ods表的processstatus字段检查到流水主键为空") /* "更新银行流水和ods表的processstatus字段检查到流水主键为空" */);
            }else {
                SQLParameter bankSQLParameter = new SQLParameter();
                bankSQLParameter.addParam(processStatus);
                bankSQLParameter.addParam((Long)bankReconciliation.getId());
                bankreconciliationSqlParameters.add(bankSQLParameter);
            }
        }
        IBankDealDetailAccessDao bankDealDetailAccessDao = CtmAppContext.getBean(IBankDealDetailAccessDao.class);
        bankDealDetailAccessDao.updateProcessstatus(odsSqlParameters,bankreconciliationSqlParameters);
    }
    /**
     * 修改ods和流水状态
     * */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void updateProcessstatus(List<SQLParameter> odsSqlParameters,List<SQLParameter> bankreconciliationSqlParameters){
        try{
            if(CollectionUtils.isNotEmpty(odsSqlParameters)){
                ymsJdbcApi.batchUpdate(ODSSql.UPDATE_PROCESS_STATUS_ODS_BY_ID,odsSqlParameters);
            }
            if(CollectionUtils.isNotEmpty(bankreconciliationSqlParameters)){
                ymsJdbcApi.batchUpdate(ODSSql.UPDATE_PROCESS_STATUS_BANK_RECONCILIATION_BY_ID,bankreconciliationSqlParameters);
            }
        }catch (Exception e){
            log.error("更新银行流水和ods状态失败",e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400775", "更新银行流水和ods状态失败") /* "更新银行流水和ods状态失败" */);
        }
    }
    @Transactional(rollbackFor = RuntimeException.class,propagation = Propagation.REQUIRED)
    @Override
    public void doHandleBankReconciliationToDB(List<BankReconciliation> saveBankReconciliationList, List<BankReconciliation> updateBankReconciliationList, List<BankReconciliation> saveOrUpdateBankReconciliationList, List<DealDetailRuleExecRecord> dealDetailRuleExecRecords, List<SQLParameter> sqlParameters,String opertype,String currentRuleCode)
    {
        try{
            List<BankReconciliation> callbacks = new ArrayList<>();
            if (!CollectionUtils.isEmpty(saveBankReconciliationList)) {
                callbacks.addAll(saveBankReconciliationList);
            }
            if (!CollectionUtils.isEmpty(updateBankReconciliationList)) {
                //更新逻辑，后面的事务传播行为是require_new,先查询pubts并给流水赋值，防止规则中对流水做更新(不能移除)
                if (StringUtils.isNotEmpty(currentRuleCode) && RuleCodeConst.SYSTEM023.equals(currentRuleCode)){
                    updateBankReconciliationList = DealDetailUtils.getBankReconciliationPubtsBeforeUpdateForRule(updateBankReconciliationList);
                }else {
                    updateBankReconciliationList = DealDetailUtils.getBankReconciliationPubtsBeforeUpdate(updateBankReconciliationList);
                }
                callbacks.addAll(updateBankReconciliationList);
            }
            //step1:执行回调，业务上有些操作需要跟流水入库在一个事务
            if(!CollectionUtils.isEmpty(callbacks)){
                for(BankReconciliation bankReconciliation:callbacks){
                    List<IDealDetailCallBack> iDealDetailCallbacks = (List<IDealDetailCallBack>)bankReconciliation.get(DealDetailEnumConst.BANKRECONCILIATION_CALLACK);
                    if(CollectionUtils.isEmpty(iDealDetailCallbacks)){
                        continue;
                    }
                    for(IDealDetailCallBack iDealDetailCallback : iDealDetailCallbacks){
                        if(null!=iDealDetailCallback){
                            try{
                                iDealDetailCallback.call();
                                log.error("流水{}已执行回调",bankReconciliation.getId().toString());
                            }catch (Exception e){
                                log.error("流水{}执行回调异常",bankReconciliation.getId().toString(),e);
                                throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_CALLBACK_ERROR.getErrCode(),BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_CALLBACK_ERROR.getMsg());
                            }finally{
                                bankReconciliation.remove(DealDetailEnumConst.BANKRECONCILIATION_CALLACK);
                                iDealDetailCallback=null;
                            }
                        }
                    }
                }
            }

            //step2:调新增、更新流水api
            if (!CollectionUtils.isEmpty(saveBankReconciliationList)) {
                this.batchSaveBankReconciliation(saveBankReconciliationList);
                IBankDealDetailAccessDao bankDealDetailAccessDao = CtmAppContext.getBean(IBankDealDetailAccessDao.class);
                bankDealDetailAccessDao.batchConvertAndSaveBankDealDetail(saveBankReconciliationList);
                DealDetailUtils.getBankReconciliationListAfterUpdateProcessstatus(saveBankReconciliationList);
            }
            if (!CollectionUtils.isEmpty(updateBankReconciliationList)) {
                //更新逻辑，后面的事务传播行为是require_new,先查询pubts并给流水赋值，防止规则中对流水做更新(不能移除)||回单关联比较特殊：receiptassociation字段在内存中是旧值但是事务是新值，所以此处需要走查询数据库重新赋值
                if (StringUtils.isNotEmpty(currentRuleCode) && RuleCodeConst.SYSTEM023.equals(currentRuleCode)){
                    updateBankReconciliationList = DealDetailUtils.getBankReconciliationPubtsBeforeUpdateForRule(updateBankReconciliationList);
                }else {
                    updateBankReconciliationList = DealDetailUtils.getBankReconciliationPubtsBeforeUpdate(updateBankReconciliationList);
                }
                this.batchUpdateBankReconciliation(updateBankReconciliationList);
                //更新完后的逻辑，查询pubts并给流水赋值，防止规则中对流水做更新,(不能移除)
                if (StringUtils.isNotEmpty(currentRuleCode) && RuleCodeConst.SYSTEM023.equals(currentRuleCode)){
                    //只有生单的数据需要走规
                    updateBankReconciliationList = DealDetailUtils.getBankReconciliationPubtsBeforeUpdateForRule(updateBankReconciliationList);
                }else {
                    updateBankReconciliationList = DealDetailUtils.getBankReconciliationPubtsBeforeUpdate(updateBankReconciliationList);
                }

            }

            //step2:过程表更新或新增
            if (!CollectionUtils.isEmpty(dealDetailRuleExecRecords)){
                if(RuleCodeConst.OPERTYPE_MATCH.equals(opertype)){
                    //挂账后生单可以继续发布就会出现问题，所以此处需要进行判断
                    List<DealDetailRuleExecRecord> updateDealDetailRuleExecRecords = new ArrayList<>();
                    List<DealDetailRuleExecRecord> insertDealDetailRuleExecRecords = new ArrayList<>();
                    for (DealDetailRuleExecRecord dealDetailRuleExecRecord:dealDetailRuleExecRecords){
                        String fullrules = dealDetailRuleExecRecord.getFullrules();
                        //更新
                        if(fullrules.contains(OdsCommonUtils.BANKDEAL_DETAIL_FULL_RULES_KEY_END)){
                            updateDealDetailRuleExecRecords.add(dealDetailRuleExecRecord);
                        }else {
                            fullrules=fullrules+OdsCommonUtils.BANKDEAL_DETAIL_FULL_RULES_KEY_END_SPLIT+OdsCommonUtils.BANKDEAL_DETAIL_FULL_RULES_KEY_END;
                            dealDetailRuleExecRecord.setFullrules(fullrules);
                            insertDealDetailRuleExecRecords.add(dealDetailRuleExecRecord);
                        }
                    }
                    if (!CollectionUtils.isEmpty(insertDealDetailRuleExecRecords)){
                        this.insertRuleExecRecords(insertDealDetailRuleExecRecords);
                    }
                    if (!CollectionUtils.isEmpty(updateDealDetailRuleExecRecords)){
                        this.updateRuleExecRecords(updateDealDetailRuleExecRecords);
                    }
                }else if(RuleCodeConst.OPERTYPE_PROCESS.equals(opertype)){
                    this.updateRuleExecRecords(dealDetailRuleExecRecords);
                }
            }
            //step3:ods状态更新
            if (!CollectionUtils.isEmpty(sqlParameters)) {
                this.batchUpdateOdsProcessstatusByIds(ODSSql.UPDATE_PROCESS_STATUS_ODS_BY_ID, sqlParameters);
            }

        }catch (BankDealDetailException e){
            this.transErrorProcess(e,saveBankReconciliationList,updateBankReconciliationList,saveOrUpdateBankReconciliationList,dealDetailRuleExecRecords,sqlParameters);
            throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_TRANSACTION_SUBMIT_ERROR.getErrCode(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400776", "银行流水事务执行失败【") /* "银行流水事务执行失败【" */+e.getMessage()+"】");//@notranslate
        }catch (Exception e){
            log.error("流水报错或更新出现非业务异常",e);
            throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_TRANSACTION_SUBMIT_ERROR.getErrCode(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400776", "银行流水事务执行失败【") /* "银行流水事务执行失败【" */+e.getMessage()+"】");//@notranslate
        }
    }
    @Override
    public void insertRuleExecRecords(List<DealDetailRuleExecRecord> dealDetailRuleExecRecords) {
        try{
            if(!CollectionUtils.isEmpty(dealDetailRuleExecRecords)){
                ymsJdbcApi.insert(dealDetailRuleExecRecords);
            }
        }catch (Exception e){
            log.error("过程表新增异常",e);
            throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_PROCESSING_ADD_ERROR.getErrCode(),BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_PROCESSING_ADD_ERROR.getMsg());
        }
    }

    @Override
    public void updateRuleExecRecords(List<DealDetailRuleExecRecord> dealDetailRuleExecRecords) {
        try{
            if(!CollectionUtils.isEmpty(dealDetailRuleExecRecords)){
                ymsJdbcApi.update(dealDetailRuleExecRecords);
            }
        }catch (Exception e){
            log.error("过程表更新异常",e);
            throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_PROCESSING_UPDATE_ERROR.getErrCode(),BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_PROCESSING_UPDATE_ERROR.getMsg());
        }
    }
    @Override
    public List<BankDealDetailODSModel> batchQueryODSByTranceidAndRequestseqnoAndodsstatus(String traceId,String requestNo,int odsstatus) {
        SQLParameter sqlParameter = new SQLParameter();
        sqlParameter.addParam(traceId);
        sqlParameter.addParam(requestNo);
        sqlParameter.addParam(odsstatus);
        List<BankDealDetailODSModel> bankDealDetailResponseRecords = ymsJdbcApi.queryForDTOList(ODSSql.QUERY_ODS_BY_TRACE_ID_AND_REQUEST_SEQ_NO_AND_ODSSTATUS,sqlParameter,BankDealDetailODSModel.class);
        return bankDealDetailResponseRecords;
    }
    @Override
    public List<BankDealDetailODSModel> batchQueryODSByTranceidAndRequestseqno(String traceId,String requestNo) {
        SQLParameter sqlParameter = new SQLParameter();
        sqlParameter.addParam(traceId);
        sqlParameter.addParam(requestNo);
        List<BankDealDetailODSModel> bankDealDetailResponseRecords = ymsJdbcApi.queryForDTOList(ODSSql.QUERY_ODS_BY_TRACE_ID_AND_REQUEST_SEQ_NO,sqlParameter,BankDealDetailODSModel.class);
        return bankDealDetailResponseRecords;
    }
    @Override
    public List<Long> batchQueryBankReconciliationByIdsAndProcessstatus(List<Long> ids,Short processstatus) {
        //查询过程表，确定流水需要执行哪个处理流程
        QueryCondition<Long> q = new QueryCondition();
        q.in("id",ids);
        String querySql = ODSSql.QUERY_DEALDETAIL_BY_FINISH_PROCESSSTATUS+" "+q.getCondition()+" and processstatus=?";
        List<Object> parameters = q.getSQLParameter().getParameters();
        SQLParameter sqlParameter = new SQLParameter();
        sqlParameter.addAllParam(parameters);
        sqlParameter.addParam(processstatus);
        List<Long> existsIds = ymsJdbcApi.queryForList(querySql, sqlParameter, new ResultSetProcessor() {
            @Override
            public Object handleResultSet(ResultSet rs) throws SQLException {
                List<Long> existsIds = new ArrayList<>();
                while(rs.next()){
                    existsIds.add(rs.getLong("id"));
                }
                return existsIds;
            }
            @Override
            public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                return null;
            }
        });
        return existsIds;
    }
    @Override
    public List<BankReconciliation> batchQueryBankReconciliationByIds(List<Long> ids){
        //查询过程表，确定流水需要执行哪个处理流程
        QueryCondition<Long> q = new QueryCondition();
        q.in("id",ids);
        String querySql = ODSSql.QUERY_DEALDETAIL_BY_FINISH_PROCESSSTATUS+" "+q.getCondition();
        List<Object> parameters = q.getSQLParameter().getParameters();
        SQLParameter sqlParameter = new SQLParameter();
        sqlParameter.addAllParam(parameters);
        List<BankReconciliation> bankReconciliationList = ymsJdbcApi.queryForList(querySql, sqlParameter, new ResultSetProcessor() {
            @Override
            public Object handleResultSet(ResultSet rs) throws SQLException {
                List<BankReconciliation> bankReconciliationList = new ArrayList<>();
                while(rs.next()){
                    BankReconciliation bankReconciliation = new BankReconciliation();
                    Long id = rs.getLong("id");
                    Short processstatus = rs.getShort("processstatus");
                    String bank_seq_no = rs.getString("bank_seq_no");
                    bankReconciliation.setId(id);
                    bankReconciliation.setProcessstatus(processstatus);
                    bankReconciliation.setBank_seq_no(bank_seq_no);
                    bankReconciliationList.add(bankReconciliation);
                }
                return bankReconciliationList;
            }
            @Override
            public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                return null;
            }
        });
        return bankReconciliationList;
    }
    @Override
    public List<BankreconciliationIdentifyType> getBankreconciliationIdentifyTypeListByTenantId() {
        SQLParameter sqlParameter = new SQLParameter();
        sqlParameter.addParam(InvocationInfoProxy.getTenantid());
        IYmsJdbcApi ymsJdbcApi = (IYmsJdbcApi)CtmAppContext.getBean("busiBaseDAO");
        List<BankreconciliationIdentifyType> bankreconciliationIdentifyTypes = ymsJdbcApi.queryForList(ODSSql.QUERY_RULETYPE, sqlParameter, new ResultSetProcessor() {
            @Override
            public Object handleResultSet(ResultSet rs) throws SQLException {
                List<BankreconciliationIdentifyType> bankreconciliationIdentifyTypes = new ArrayList<>();
                while(rs.next()){
                    Long id = rs.getLong("id");
                    String code = rs.getString("code");
                    int excuteorder = rs.getInt("excuteorder");
                    Short identifytype = rs.getShort("identifytype");
                    BankreconciliationIdentifyType bankreconciliationIdentifyType = new BankreconciliationIdentifyType();
                    bankreconciliationIdentifyType.setId(id);
                    bankreconciliationIdentifyType.setCode(code);
                    bankreconciliationIdentifyType.setExcuteorder(excuteorder);
                    bankreconciliationIdentifyType.setIdentifytype(identifytype);
                    bankreconciliationIdentifyTypes.add(bankreconciliationIdentifyType);
                }
                return bankreconciliationIdentifyTypes;
            }
            @Override
            public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                return null;
            }
        });
        return bankreconciliationIdentifyTypes;
    }

    private void transErrorProcess(BankDealDetailException e,List<BankReconciliation> saveBankReconciliationList, List<BankReconciliation> updateBankReconciliationList, List<BankReconciliation> saveOrUpdateBankReconciliationList,List<DealDetailRuleExecRecord> dealDetailRuleExecRecords, List<SQLParameter> sqlParameters) {
        int errCode = e.getErrCode();
        if(errCode == BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_SAVE_ERROR.getErrCode()){
            log.error("【流水事务提交】新增流水异常,{}", JSON.toJSONString(saveBankReconciliationList),e);
        }
        PDDocument p;
        if(errCode == BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_UPDATE_ERROR.getErrCode()){
            log.error("【流水事务提交】更新流水异常,{}", JSON.toJSONString(updateBankReconciliationList),e);
        }
        if(errCode == BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_ODS_UPDATE_ERROR.getErrCode()){
            log.error("【流水事务提交】更新ods异常",e);
        }
        if(errCode == BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_PROCESSING_ADD_ERROR.getErrCode()){
            log.error("【流水事务提交】流程过程新增异常,{}", JSON.toJSONString(dealDetailRuleExecRecords),e);
        }
        if(errCode == BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_PROCESSING_UPDATE_ERROR.getErrCode()){
            log.error("【流水事务提交】流程过程更新异常,{}", JSON.toJSONString(dealDetailRuleExecRecords),e);
        }
        if(errCode == BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_CALLBACK_ERROR.getErrCode()){
            log.error("【流水事务提交】回调异常", e);
        }
    }

    @Override
    public List<DealDetailRuleExecRecord> queryDealDetailRuleExecRecordByMainid(List<Object> bankReconciliationIds) {
        try{
            //查询过程表，确定流水需要执行哪个处理流程
            QueryCondition<DealDetailRuleExecRecord> q = new QueryCondition();
            q.in("processing.mainid",bankReconciliationIds);
            String querySql = ODSSql.QUERY_DEALDETAIL_PROCESSING_BY_MAINID+" "+q.getCondition();
            List<Object> parameters = q.getSQLParameter().getParameters();
            SQLParameter sqlParameter = new SQLParameter();
            sqlParameter.addAllParam(parameters);
            List<DealDetailRuleExecRecord> execRecords = ymsJdbcApi.queryForDTOList(querySql,sqlParameter,DealDetailRuleExecRecord.class);
            return execRecords;
        }catch (Exception e){
            log.error("查询过程表异常",e);
        }
        return null;
    }
    @Override
    public List<BankReconciliation> queryExistBankReconciliations(String paramName, Set<String> conditionList) throws Exception {

        String querySql = null;
        QueryCondition queryCondition = new QueryCondition();
        QuerySchema querySchemaPay = QuerySchema.create().addSelect("id,unique_no,bank_seq_no,tran_date,tran_time,tran_amt,dc_flag,bankaccount,acct_bal,concat_info,to_acct_no,to_acct_name,pubts");
        QueryConditionGroup groupPay = null;
        if("concat_info".equals(paramName)){
//            querySql = "select distinct id,unique_no,bank_seq_no,tran_date,tran_time,tran_amt,dc_flag,bankaccount,acct_bal,concat_info,to_acct_no,to_acct_name,pubts from ctmcmp.cmp_bankreconciliation ";
//            queryCondition.in("concat_info",conditionList);
            groupPay = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("concat_info").in(conditionList));
        }
        if("unique_no".equals(paramName)){
//            querySql = "select distinct id,unique_no,bank_seq_no,tran_date,tran_time,tran_amt,dc_flag,bankaccount,acct_bal,concat_info,to_acct_no,to_acct_name,pubts from ctmcmp.cmp_bankreconciliation ";
//            queryCondition.in("unique_no",conditionList);
            groupPay = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("unique_no").in(conditionList));
        }
        querySchemaPay.addCondition(groupPay);
        List<BankReconciliation> existsBankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchemaPay, null);

//        SQLParameter sqlParameter = queryCondition.getSQLParameter();
//        querySql = querySql+queryCondition.getCondition()+" and ytenant_id='"+AppContext.getYTenantId()+"'";
//        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        List<BankReconciliation> existsBankReconciliationList = ymsJdbcApi.queryForList(querySql,sqlParameter,new ResultSetProcessor() {
//            @Override
//            public Object handleResultSet(ResultSet rs) throws SQLException {
//                List<BankReconciliation> bankReconciliationList = new ArrayList<>();
//                while(rs.next()){
//                    BankReconciliation bankReconciliation = new BankReconciliation();
//
//                    Long id = rs.getLong("id");
//                    String unique_no = rs.getString("unique_no");
//                    String bank_seq_no = rs.getString("bank_seq_no");
//                    Date tran_date = rs.getDate("tran_date");
//                    Date tran_time = rs.getDate("tran_time");
//                    BigDecimal tran_amt = rs.getBigDecimal("tran_amt");
//                    Short dc_flag = rs.getShort("dc_flag");
//                    String bankaccount = rs.getString("bankaccount");
//                    BigDecimal acct_bal = rs.getBigDecimal("acct_bal");
//                    String concat_info = rs.getString("concat_info");
//                    String to_acct_no = rs.getString("to_acct_no");
//                    String to_acct_name = rs.getString("to_acct_name");
//                    String pubtsString=rs.getString("pubts");
//                    Date pubts = sf.parse(pubtsString);
//                    bankReconciliation.setId(id);
//                    bankReconciliation.setUnique_no(unique_no);
//                    bankReconciliation.setBank_seq_no(bank_seq_no);
//                    bankReconciliation.setTran_date(tran_date);
//                    bankReconciliation.setTran_time(tran_time);
//                    bankReconciliation.setTran_amt(tran_amt);
//                    bankReconciliation.setDc_flag(dc_flag==2? Direction.Credit:Direction.Debit);
//                    bankReconciliation.setBankaccount(bankaccount);
//                    bankReconciliation.setAcct_bal(acct_bal);
//                    bankReconciliation.setConcat_info(concat_info);
//                    bankReconciliation.setTo_acct_no(to_acct_no);
//                    bankReconciliation.setTo_acct_name(to_acct_name);
//                    bankReconciliation.setPubts(pubts);
//                    bankReconciliationList.add(bankReconciliation);
//                }
//                return bankReconciliationList;
//            }
//            @Override
//            public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
//                return null;
//            }
//        });
        return existsBankReconciliationList;
    }

    @Override
    public List<BankReconciliationbusrelation_b> queryBankReconciliationBusByBillId(List<Long> billIdList){
        if(CollectionUtils.isEmpty(billIdList)){
            return null;
        }
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.in("billid",billIdList);
        queryCondition.eq("ytenant_id",InvocationInfoProxy.getTenantid());
        queryCondition.eq("relationstatus",DealDetailEnumConst.RELATIONSTATUS_SUCC);
        SQLParameter sqlParameter = queryCondition.getSQLParameter();
        return ymsJdbcApi.queryForList(ODSSql.QUERY_BANKRECONCILIATION_BUS_RELATION_B+queryCondition.getCondition(),
                sqlParameter, new ResultSetProcessor() {
            @Override
            public Object handleResultSet(ResultSet rs) throws SQLException {
                List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = new ArrayList<>();
                while(rs.next()){
                    Long id = rs.getLong("id");
                    Long billid = rs.getLong("billid");
                    BankReconciliationbusrelation_b bankReconciliationbusrelation_b = new BankReconciliationbusrelation_b();
                    bankReconciliationbusrelation_b.setId(id);
                    bankReconciliationbusrelation_b.setBillid(billid);
                    bankReconciliationbusrelation_bs.add(bankReconciliationbusrelation_b);
                }
                return bankReconciliationbusrelation_bs;
            }
            @Override
            public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                return null;
            }
        });
    }
    @Override
    public List<BankReconciliationbusrelation_b> queryBankReconciliationBusByBillIdAndBankReconciliationId(Long bankReconciliationId,Long billId){
        if(null==bankReconciliationId || null==billId){
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400778", "查询关联明细，流水和结算明细主键不能空") /* "查询关联明细，流水和结算明细主键不能空" */);
        }
        SQLParameter sqlParameter = new SQLParameter();
        sqlParameter.addParam(InvocationInfoProxy.getTenantid());
        sqlParameter.addParam(billId);
        sqlParameter.addParam(bankReconciliationId);
        return ymsJdbcApi.queryForList(ODSSql.QUERY_BANKRECONCILIATION_BUS_RELATION_B_BYBILLIDANDBANKRECONCILITIONID,
                sqlParameter, new ResultSetProcessor() {
                    @Override
                    public Object handleResultSet(ResultSet rs) throws SQLException {
                        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = new ArrayList<>();
                        while(rs.next()){
                            Long id = rs.getLong(1);
                            BankReconciliationbusrelation_b bankReconciliationbusrelation_b = new BankReconciliationbusrelation_b();
                            bankReconciliationbusrelation_b.setId(id);
                            bankReconciliationbusrelation_bs.add(bankReconciliationbusrelation_b);
                        }
                        return bankReconciliationbusrelation_bs;
                    }
                    @Override
                    public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                        return null;
                    }
                });
    }

    @Override
    public void updateBankReconciliationBusRelationStatus(Short relationStatus, Short relationType,Long bankReeconciliationId) {
        if(null == relationStatus|| null==relationType){
            return;
        }
        try{
            String updateSql = ODSSql.UPDATE_BANKRECONCILIATION_BUS_RELATIONSTATUS;
            SQLParameter params = new SQLParameter();
            params.addParam(relationStatus);
            params.addParam(relationType);
            params.addParam(bankReeconciliationId);
            params.addParam(InvocationInfoProxy.getTenantid());
            ymsJdbcApi.update(updateSql,params);
        }catch (Exception e){
            log.error("【智能流水】-修改关联明细状态异常",e);
            throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.UPDATE_BANKRECONCILIATIONRELATIONBUS_ERROR.getErrCode(),BankDealDetailExceptionCodeEnum.UPDATE_BANKRECONCILIATIONRELATIONBUS_ERROR.getMsg());
        }

    }

    @Override
    public List<Flowhandlesetting> queryFlowhandlesetting(String flow_type, String enable, String object, String association_mode){

        SQLParameter sqlParameter = new SQLParameter();
        sqlParameter.addParam(InvocationInfoProxy.getTenantid());
        sqlParameter.addParam(flow_type);
        sqlParameter.addParam(enable);
        sqlParameter.addParam(object);
        sqlParameter.addParam(association_mode);
        return ymsJdbcApi.queryForList(ODSSql.QUERY_FLOWHANDLESETTING,
                sqlParameter, new ResultSetProcessor() {
                    @Override
                    public Object handleResultSet(ResultSet rs) throws SQLException {
                        List<Flowhandlesetting> flowhandlesettingList = new ArrayList<>();
                        while(rs.next()){
                            Flowhandlesetting flowhandlesetting = new Flowhandlesetting();
                            short artiConfirmValue = rs.getShort("is_arti_confirm");
                            //流水处理流程表数据转换
                            short isArtiConfirm = (artiConfirmValue == CONFIRM_TYPE_ARTIFICIAL) ? CONFIRM_STATUS_YES : CONFIRM_STATUS_NO;
                            flowhandlesetting.setIsArtiConfirm(isArtiConfirm);
                            flowhandlesetting.setIsRandomAutoConfirm(rs.getShort("is_random_auto_confirm"));
                            flowhandlesettingList.add(flowhandlesetting);
                        }
                        return flowhandlesettingList;
                    }
                    @Override
                    public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                        return null;
                    }
                });
    }

    public void deleteOdsByTraceIdAndRequestSeqNo(String traceId, String requestSeqNo) {
        try {
            String UPDATE_ODS_BYPROCESSSTATUSANDTRACEID = "select id from cmp_bankdealdetail_ods where traceid='" + traceId + "' and request_seq_no='" + requestSeqNo +"'";
            List<BankDealDetailODSModel> list_ods = ymsJdbcApi.queryForList(UPDATE_ODS_BYPROCESSSTATUSANDTRACEID, new ResultSetProcessor() {
                @Override
                public Object handleResultSet(ResultSet rs) throws SQLException {
                    List<BankDealDetailODSModel> list = new ArrayList<>();
                    while (rs.next()) {
                        String id = rs.getString(1);
                        BankDealDetailODSModel bankDealDetailODSModel = new BankDealDetailODSModel();
                        bankDealDetailODSModel.setId(id);
                        list.add(bankDealDetailODSModel);
                    }
                    return list;
                }

                @Override
                public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                    return null;
                }
            });
            if (!CollectionUtils.isEmpty(list_ods)) {
                ymsJdbcApi.remove(list_ods);
            }
        }catch (Exception e){
            log.error("批量删除ods异常", e);
        }
    }

}
