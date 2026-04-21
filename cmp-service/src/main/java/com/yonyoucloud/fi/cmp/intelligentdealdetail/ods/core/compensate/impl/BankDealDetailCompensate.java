package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.compensate.impl;

import com.alibaba.fastjson.JSONObject;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailRuleResult;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.BankDealDetailMatchChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.model.DealDetailRuleExecRecord;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common.CmpCheckRuleCommonProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author guoyangy
 * @Date 2024/10/11 14:02
 * @Description 流水处理补偿逻辑
 * @Version 1.0
 */
@Service
@Slf4j
public class BankDealDetailCompensate extends DefaultBankDealDetailCompensate {
    @Override
    public void compensateInContext(Date startDate, Date endDate, CtmJSONObject param) throws Exception {
        List<BankReconciliation> bankReconciliations = queryBankReconcilitionByPage(startDate, endDate,param);
        if (CollectionUtils.isEmpty(bankReconciliations)) {
            log.error("【智能流水-流水补偿】未查询到流水");
            return;
        }
        this.setOdsIdToBankReconciliation(bankReconciliations);

        Map<Short, List<BankReconciliation>> bankReconciliationMap = bankReconciliations.stream().collect(Collectors.groupingBy(bankReconciliation->Optional.ofNullable(bankReconciliation.getProcessstatus()).orElse((short)1)));

        List<BankReconciliation> rematchList = new ArrayList<>();
        rematchList.addAll(bankReconciliations);

//        rematchList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus())).orElse(new ArrayList<>()));
//        rematchList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_ERROR.getStatus())).orElse(new ArrayList<>()));
//        rematchList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_START.getStatus())).orElse(new ArrayList<>()));
//        rematchList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_SUCC.getStatus())).orElse(new ArrayList<>()));
        List<BankReconciliation> matchsuccList = new ArrayList<>();
        matchsuccList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_SUCC.getStatus())).orElse(new ArrayList<>()));
        matchsuccList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_RECEIVEPAY_SUCC.getStatus())).orElse(new ArrayList<>()));
        matchsuccList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_GENERATE_SUCC.getStatus())).orElse(new ArrayList<>()));

        List<BankReconciliation> relatedList = new ArrayList<>();
        relatedList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_START.getStatus())).orElse(new ArrayList<>()));
        relatedList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_FAIL.getStatus())).orElse(new ArrayList<>()));

        List<BankReconciliation> credentialtList = new ArrayList<>();
        credentialtList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_START.getStatus())).orElse(new ArrayList<>()));
        credentialtList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_FAIL.getStatus())).orElse(new ArrayList<>()));

        List<BankReconciliation> generatebillList = new ArrayList<>();
        generatebillList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_START.getStatus())).orElse(new ArrayList<>()));
        generatebillList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus())).orElse(new ArrayList<>()));

        List<BankReconciliation> publishList = new ArrayList<>();
        publishList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_FAIL.getStatus())).orElse(new ArrayList<>()));
        publishList.addAll(Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_START.getStatus())).orElse(new ArrayList<>()));

        //重新执行辨识匹配
        if (!CollectionUtils.isEmpty(rematchList)) {
            try {
                List<String> keys = this.getBatchLockKey(rematchList);
                CtmLockTool.executeInOneServiceExclusivelyBatchLock(keys, 10L, TimeUnit.MINUTES, (int lockstatus) -> {
                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                        log.error("【流水补偿】获取锁失败");
                        return;
                    }
                    List<BankDealDetailWrapper> bankDealDetailWrappers = DealDetailUtils.convertBankReconciliationToWrapper(rematchList,false);
                    bankDealDetailManageFacade.bankDealDetailManageAccess(bankDealDetailWrappers, rematchList, BankDealDetailMatchChainImpl.get().code(null), DealDetailUtils.getTraceId(), "from compensate");
                    log.error("【流水补偿】,租户{},status={}补偿完成", DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START);
                });
            } catch (Exception e) {
                log.error("【流水补偿】处理异常", e);
            }
        }
        //辨识匹配成功
        matchsuccList = null;
        if (!CollectionUtils.isEmpty(matchsuccList)) {
            //收付单据匹配
            if (!CollectionUtils.isEmpty(relatedList)) {
                matchsuccList.addAll(relatedList);
            }
            try {
                this.executeProcess(matchsuccList, RuleCodeConst.SYSTEM005);
            } catch (Exception e) {
                log.error("【智能流水补偿】执行收付单据匹配及后续流程异常", e);
            }
        }
        //业务单据匹配
        if (!CollectionUtils.isEmpty(credentialtList)) {
            //todo 待接入
        }
        //生单
        generatebillList = null;
        if (!CollectionUtils.isEmpty(generatebillList)) {
            //发布认领
            if (!CollectionUtils.isEmpty(publishList)) {
                generatebillList.addAll(publishList);
            }
            try {
                this.executeProcess(generatebillList, RuleCodeConst.SYSTEM007);
            } catch (Exception e) {
                log.error("【智能流水补偿】执行生单及后续流程异常", e);
            }
        }
    }

    private void executeProcess(List<BankReconciliation> bankReconciliations, String ruleCode) throws Exception {
        List<String> keys = this.getBatchLockKey(bankReconciliations);
        CtmLockTool.executeInOneServiceExclusivelyBatchLock(keys, 5L, TimeUnit.MINUTES, (int lockstatus) -> {
            if (lockstatus == LockStatus.GETLOCK_FAIL) {
                log.error("【流水补偿】获取锁失败");
                return;
            }
            Map<String, BankReconciliation> relatedBankReconciliationMap = bankReconciliations.stream().collect(Collectors.toMap(BankReconciliation::getId, each -> each, (value1, value2) -> value1));
            List<DealDetailRuleExecRecord> execRecords = Optional.ofNullable(this.getDealDetailRuleExecRecords(bankReconciliations)).orElse(new ArrayList<>());
            Map<Long, DealDetailRuleExecRecord> execRecordMap = execRecords.stream().collect(Collectors.toMap(DealDetailRuleExecRecord::getMainid, each -> each, (key1, key2) -> key1));
            log.error("【补偿任务】辨识匹配成功流水补偿，流水业务表获取流水数量{},过程表查出来的流水记录数量{}", bankReconciliations.size(), execRecords.size());
            List<BankDealDetailWrapper> bankDealDetailWrappers = new ArrayList<>();
            for (BankReconciliation bankReconciliation : bankReconciliations) {
                Long bankReconciliationId = bankReconciliation.getId();
                DealDetailRuleExecRecord ruleExecRecord = Optional.ofNullable(execRecordMap.get(bankReconciliationId)).orElse(new DealDetailRuleExecRecord());
                String exerules = ruleExecRecord.getExerules();
                List<BankDealDetailRuleResult> ruleResults = null;
                if (!StringUtils.isEmpty(exerules)) {
                    try {
                        ruleResults = JSONObject.parseArray(exerules, BankDealDetailRuleResult.class);
                    } catch (Exception e) {
                        log.error("【智能流水-流水补偿】解析过程表数据异常");
                    }
                }
                BankDealDetailWrapper bankDealDetailWrapper = this.getBankDealDetailWrapper(relatedBankReconciliationMap, ruleExecRecord, ruleResults);
                bankDealDetailWrappers.add(bankDealDetailWrapper);
            }
            if (!CollectionUtils.isEmpty(bankDealDetailWrappers)) {
                DefaultBankDealDetailChain.callDealDetailProcess(ruleCode, bankDealDetailWrappers);
            }
        });

    }

    /**
     * 设置odsid到流水中
     * @param bankReconciliations
     */
    private void setOdsIdToBankReconciliation(List<BankReconciliation> bankReconciliations) {
        List<Long> bankReconciliationIdList = new ArrayList<>();
        bankReconciliations.stream().forEach(bankReconciliation -> {
            bankReconciliationIdList.add(bankReconciliation.getId());
        });
        List<BankDealDetailODSModel> bankDealDetailODSModelList = bankDealDetailAccessDao.batchQueryODSByMainid(bankReconciliationIdList);
        Map<Long, String> idrelated = new HashMap<>();
        if (!CollectionUtils.isEmpty(bankDealDetailODSModelList)) {
            idrelated = bankDealDetailODSModelList.stream().collect(Collectors.toMap(BankDealDetailODSModel::getMainid, BankDealDetailODSModel::getId));
        }
        Map<Long, String> finalIdrelated = idrelated;
        bankReconciliations.stream().forEach(bankReconciliation -> {
            Long id = bankReconciliation.getId();
            String odsId = finalIdrelated.get(id);
            bankReconciliation.put(DealDetailEnumConst.ODSID, StringUtils.isEmpty(odsId) ? 0 + "" : odsId);
        });
    }

    private List<DealDetailRuleExecRecord> getDealDetailRuleExecRecords(List<BankReconciliation> bankReconciliations) {
        List<Object> bankReconciliationIds = bankReconciliations.stream().map(BankReconciliation::getId).collect(Collectors.toList());
        List<DealDetailRuleExecRecord> execRecords = bankDealDetailAccessDao.queryDealDetailRuleExecRecordByMainid(bankReconciliationIds);
        return execRecords;
    }

    private BankDealDetailWrapper getBankDealDetailWrapper(Map<String, BankReconciliation> bankReconciliationMap, DealDetailRuleExecRecord ruleExecRecord, List<BankDealDetailRuleResult> ruleResults) {
        BankReconciliation currentBank = bankReconciliationMap.get(ruleExecRecord.getMainid());
        Long bankreconciliationId = ruleExecRecord.getMainid();
        //为流水赋值osdid
        bankReconciliationMap.get(bankreconciliationId).put(DealDetailEnumConst.ODSID, ruleExecRecord.getOsdid());
        BankDealDetailWrapper bankDealDetailWrapper = DealDetailUtils.convertBankReconciliationToWrapper(Arrays.asList(currentBank),false).get(0);
        bankDealDetailWrapper.setDealDetailRuleExecRecord(ruleExecRecord);
        bankDealDetailWrapper.setRuleList(ruleResults);
        return bankDealDetailWrapper;
    }


    private List<String> getBatchLockKey(List<BankReconciliation> bankReconciliationList) {
        List<String> keys = new ArrayList<>();
        bankReconciliationList.stream().forEach(bankReconciliation -> {
            keys.add(bankReconciliation.getId() + "_dealdetail_compensate");
        });
        return keys;
    }

    private List<BankReconciliation> queryBankReconcilitionByPage(Date startDate, Date endDate,CtmJSONObject param) {
        try {
            //endDate = DateUtils.dateAddDays(endDate,1);
            QuerySchema querySchema = new QuerySchema().addSelect("*");
            querySchema.addOrderBy(new QueryOrderby(ICmpConstant.TRAN_DATE, "desc"));
                QueryConditionGroup processstatus = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("processstatus").in(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus(),
                        DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_SUCC.getStatus(),
                        DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_ERROR.getStatus(),
                        DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_RECEIVEPAY_SUCC.getStatus(),
                        DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_GENERATE_SUCC.getStatus(),
                        DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_SUCC.getStatus(),
                        DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_FAIL.getStatus(),
                        DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_FAIL.getStatus(),
                        DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus(),
                        DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_SUCC.getStatus(),
                        DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_FAIL.getStatus()));
                processstatus.addCondition(org.imeta.orm.schema.QueryCondition.name(ICmpConstant.TRAN_DATE).between(startDate, endDate));
                QueryConditionGroup conditionGroup = QueryConditionGroup.or(processstatus);
                QueryConditionGroup conditionGroupOr = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("processstatus").eq(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_START.getStatus()));
                conditionGroupOr.addCondition(org.imeta.orm.schema.QueryCondition.name(ICmpConstant.TRAN_DATE).between(startDate, endDate));

                QueryConditionGroup conditionGroupOr2 = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("processstatus").eq(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_START.getStatus()));
                conditionGroupOr2.addCondition(org.imeta.orm.schema.QueryCondition.name(ICmpConstant.TRAN_DATE).between(startDate, endDate));

                QueryConditionGroup conditionGroupOr3 = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("processstatus").eq(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_START.getStatus()));
                conditionGroupOr3.addCondition(org.imeta.orm.schema.QueryCondition.name(ICmpConstant.TRAN_DATE).between(startDate, endDate));

                QueryConditionGroup conditionGroupOr4 = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("processstatus").eq(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_START.getStatus()));
                conditionGroupOr4.addCondition(org.imeta.orm.schema.QueryCondition.name(ICmpConstant.TRAN_DATE).between(startDate, endDate));

                QueryConditionGroup conditionGroupOr5 = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("processstatus").eq(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_START.getStatus()));
                conditionGroupOr5.addCondition(org.imeta.orm.schema.QueryCondition.name(ICmpConstant.TRAN_DATE).between(startDate, endDate));

                QueryConditionGroup conditionGroupOr6 = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("processstatus").is_null());
                conditionGroupOr6.addCondition(org.imeta.orm.schema.QueryCondition.name(ICmpConstant.TRAN_DATE).between(startDate, endDate));

                conditionGroup.addCondition(conditionGroupOr);
                conditionGroup.addCondition(conditionGroupOr2);
                conditionGroup.addCondition(conditionGroupOr3);
                conditionGroup.addCondition(conditionGroupOr4);
                conditionGroup.addCondition(conditionGroupOr5);
                conditionGroup.addCondition(conditionGroupOr6);



            QueryConditionGroup conditionGroupLast = QueryConditionGroup.and(conditionGroup);
            if (StringUtils.isNotEmpty(param.getString(ICmpConstant.ACCENTITY))) {
                String accentity = param.getString(ICmpConstant.ACCENTITY);
                String[] split = accentity.split(";");
                List<String> accentityList = new ArrayList<>();
                for (int i= 0; i < split.length; i++){
                    accentityList.add(split[i]);
                }
                QueryConditionGroup conditionGroupOr7 = QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ACCENTITY).in(accentityList));
                conditionGroupLast.addCondition(conditionGroupOr7);
            }
            if (StringUtils.isNotEmpty(param.getString(ICmpConstant.CURRENCY))) {
                QueryConditionGroup conditionGroupOr8 = QueryConditionGroup.and(QueryCondition.name(ICmpConstant.CURRENCY).eq(param.getString(ICmpConstant.CURRENCY)));
                conditionGroupLast.addCondition(conditionGroupOr8);
            }
            if (StringUtils.isNotEmpty(param.getString(ICmpConstant.BANKTYPE_LOW))) {
                String banktype = param.getString(ICmpConstant.BANKTYPE_LOW);
                String[] split = banktype.split(";");
                List<String> banktypeList = new ArrayList<>();
                for (int i= 0; i < split.length; i++){
                    banktypeList.add(split[i]);
                }
                QueryConditionGroup conditionGroupOr9 = QueryConditionGroup.and(QueryCondition.name(ICmpConstant.BANKTYPE_LOW).in(banktypeList));
                conditionGroupLast.addCondition(conditionGroupOr9);
            }
            if (StringUtils.isNotEmpty(param.getString(ICmpConstant.BANK_ACCOUNT))) {
                String bankAccount = param.getString(ICmpConstant.BANK_ACCOUNT);
                String[] split = bankAccount.split(";");
                List<String> bankAccountList = new ArrayList<>();
                for (int i= 0; i < split.length; i++){
                    bankAccountList.add(split[i]);
                }
                QueryConditionGroup conditionGroupOr10 = QueryConditionGroup.and(QueryCondition.name(ICmpConstant.BANK_ACCOUNT).in(bankAccountList));
                conditionGroupLast.addCondition(conditionGroupOr10);
            }
            //将已关联的数据过滤出来，不在去执行sql
            QueryConditionGroup conditionGroupOr11 = QueryConditionGroup.and(QueryCondition.name("associationstatus").not_eq("1"));
            conditionGroupLast.addCondition(conditionGroupOr11);
            QueryConditionGroup conditionGroupOr12 = QueryConditionGroup.and(QueryCondition.name("ispublish").not_eq("1"));
            conditionGroupLast.addCondition(conditionGroupOr12);
            QueryConditionGroup conditionGroupOr13 = QueryConditionGroup.and(QueryCondition.name("serialdealendstate").not_eq("1"));
            conditionGroupLast.addCondition(conditionGroupOr13);


            try{
                String odsNum = AppContext.getEnvConfig("ods.query.num","2000");
                querySchema.addPager(0, Integer.parseInt(odsNum));
                querySchema.addCondition(conditionGroupLast);
            }catch (Exception e){
                log.error("智能流水弥补调度任务设置大小异常");
            }



            List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);

            if (CollectionUtils.isEmpty(bankReconciliations)) {
                return null;
            }
            bankReconciliations = bankReconciliations.stream().distinct().collect(Collectors.toList());
            return bankReconciliations;
        } catch (Exception e) {
            log.error("流水查询异常", e);
        }
        return null;
    }
}
