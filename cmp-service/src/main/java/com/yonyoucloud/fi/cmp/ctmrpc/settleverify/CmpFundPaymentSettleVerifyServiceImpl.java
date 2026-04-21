package com.yonyoucloud.fi.cmp.ctmrpc.settleverify;


import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.BusinessException;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.settle.itf.param.*;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.api.settleverify.CmpFundPaymentSettleVerifyService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.PushCsplStatusEnum;
import com.yonyoucloud.fi.cmp.enums.SystemIntegrationParamsEnum;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanService;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.drft.api.openapi.ICtmDrftEndorePaybillRpcService;
import com.yonyoucloud.fi.drft.post.vo.base.BaseResultVO;
import com.yonyoucloud.fi.drft.post.vo.output.SettleUseBillResVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <h1>资金付款单结算检查接口</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-06-28 9:33
 */
@Slf4j
@Service
public class CmpFundPaymentSettleVerifyServiceImpl implements CmpFundPaymentSettleVerifyService {
    @Override
    public SettleOperResult[] validate(SettleOperType operType, SettleOperContext context) throws BusinessException {
        CtmJSONObject recordLog = new CtmJSONObject();
        recordLog.put("1.CmpFundPaymentSettleVerifyServiceImpl#validate, operType", operType);
        recordLog.put("2.CmpFundPaymentSettleVerifyServiceImpl#validate, context", context);
        log.error("1.CmpFundPaymentSettleVerifyServiceImpl#validate, operType={}, context={}", operType, context);
        List<SettleOperResult> settleOperatorResultList = new ArrayList<>();
        Settlement settlement = context.getSettlement();
        if (settlement == null || settlement.getBodys() == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101496"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C819AA60480005B", "待检查的结算单数据为空!") /* "待检查的结算单数据为空!" */);
        }
        SettleBody[] settlementBody = settlement.getBodys();
        Set<String> settleIds = new HashSet<>();
        for (SettleBody settleBody : settlementBody) {
            String id = settleBody.getId();
            settleIds.add(id);
        }
        recordLog.put("3.CmpFundPaymentSettleVerifyServiceImpl#validate, settleIds", settleIds);
        if (CollectionUtils.isNotEmpty(settleIds)) {
            // 根据结算单明细id查询资金付款单
            QuerySchema querySchema = QuerySchema
                    .create()
                    .addSelect("id, code, verifystate,pubts, FundPayment_b.settledId, FundPayment_b.entrustReject");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
            queryConditionGroup.addCondition(QueryCondition.name("FundPayment_b.settledId").in(settleIds));
            querySchema.addCondition(queryConditionGroup);
            try {
                List<Map<String, Object>> mapList = MetaDaoHelper.query(FundPayment.ENTITY_NAME, querySchema);
                log.error("2.CmpFundPaymentSettleVerifyServiceImpl#validate, operType={}, context={}，mapList={}", operType, context, mapList);
                recordLog.put("4.CmpFundPaymentSettleVerifyServiceImpl#validate, mapList", mapList);
                if (CollectionUtils.isEmpty(mapList)) {
                    for (SettleBody body : settlementBody) {
                        SettleOperResult settleOperResult = new SettleOperResult(settlement.getId(), body.getId());
                        settleOperResult.setPass(Boolean.TRUE);
                        settleOperatorResultList.add(settleOperResult);
                    }
                    return settleOperatorResultList.toArray(new SettleOperResult[0]);
                }

                Map<Object, List<Map<String, Object>>> fundFundpaymentBSettledId = mapList.stream().collect(Collectors.groupingBy(item -> item.get("FundPayment_b_settledId")));
                List<Map<String, Object>> result = new ArrayList<>();
                for (Map.Entry<Object, List<Map<String, Object>>> listEntry : fundFundpaymentBSettledId.entrySet()) {
                    List<Map<String, Object>> list = listEntry.getValue();
                    if (list.size() == 1){
                        result.addAll(list);
                    } else {
                        Map<String, Object> data = null;
                        for (Map<String, Object> map : list) {
                            if(map.get("pubts") instanceof Timestamp){
                                if (data == null || ((Timestamp) map.get("pubts")).compareTo((Timestamp)data.get("pubts"))> 0) {
                                    data = map;
                                }
                            } else if (map.get("pubts") instanceof Date) {
                                if (data == null || ((Date) map.get("pubts")).compareTo((Date)data.get("pubts"))> 0) {
                                    data = map;
                                }
                            } else {
                                if (data == null || map.get("pubts").toString().compareTo(data.get("pubts").toString()) > 0) {
                                    data = map;
                                }
                            }

                        }
                        result.add(data);
                    }
                }
                
                List<Object> ids = new ArrayList<>();
                for (Map<String, Object> resultMap : result) {
                    Object id = resultMap.get("id");
                    ids.add(id);
                }

                // 根据id批量查询数据
                QuerySchema schema = QuerySchema.create().addSelect("*");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                // 只查询来源为现金的数据 只有这类数据需要升级
                conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
                schema.addCondition(conditionGroup);
                // 查询子表信息
                QuerySchema detailSchema = QuerySchema.create().name("FundPayment_b").addSelect("*");
                schema.addCompositionSchema(detailSchema);
                List<BizObject> bizObjects = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, schema, null);
                recordLog.put("5.CmpFundPaymentSettleVerifyServiceImpl#validate, bizObjects", bizObjects);

                Map<Long, BizObject> bizMap = bizObjects.stream().collect(Collectors.toMap(item -> item.getLong("id"), item -> item));

                for (Map<String, Object> resultMap : result) {
                    SettleOperResult settleOperResult =
                            new SettleOperResult(settlement.getId(), resultMap.get(
                                    "FundPayment_b_settledId").toString());
                    Long id = Long.parseLong(resultMap.get("id").toString());
                    BizObject bizObject = bizMap.get(id);
                    // 校验逻辑：单据审批状态为初识开立，明细行上的结算状态为待结算即可删除单据
                    short verifyState = bizObject.getShort(ICmpConstant.VERIFY_STATE);
                    List<BizObject> bodyItems = bizObject.getBizObjects("FundPayment_b", BizObject.class);
                    boolean boolVerifyState =
                            verifyState == VerifyState.INIT_NEW_OPEN.getValue()
                                    || verifyState == VerifyState.REJECTED_TO_MAKEBILL.getValue();
                    if (boolVerifyState) {
                        settleOperResult.setPass(true);
                    } else {
                        settleOperResult.setPass(false);
                        String errorMsg;
                        if (SettleOperType.CANCE_SETTLE.equals(operType)) {
                            errorMsg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CB5B0280438002C", "协同生成的资金付款单【%s】已发生后续业务，不允许取消结算，请检查！") /* "协同生成的资金付款单【%s】已发生后续业务，不允许取消结算，请检查！" */,
                                    resultMap.get(ICmpConstant.CODE).toString());
                        } else {
                            errorMsg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CB5B0280438002D", "协同生成的资金付款单【%s】已发生后续业务，不允许退回待结算，请检查！") /* "协同生成的资金付款单【%s】已发生后续业务，不允许退回待结算，请检查！" */,
                                    resultMap.get(ICmpConstant.CODE).toString());
                        }
                        settleOperResult.setErrorMessage(errorMsg);
                    }
                    settleOperatorResultList.add(settleOperResult);
                }
                recordLog.put("6.CmpFundPaymentSettleVerifyServiceImpl#validate, settleOperatorResultList", settleOperatorResultList);
                return settleOperatorResultList.toArray(new SettleOperResult[0]);

            } catch (Exception e) {
                log.error("CmpFundPaymentSettleVerifyServiceImpl validate errorMsg={}", e.getMessage());
                recordLog.put("7.CmpFundPaymentSettleVerifyServiceImpl#validate, errorMsg", e.getMessage());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101497"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C819AA604800059", "根据结算明细id查询资金付款单数据异常!") /* "根据结算明细id查询资金付款单数据异常!" */ + e.getMessage());
            } finally{
                CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                ctmcmpBusinessLogService.saveBusinessLog(
                        recordLog,
                        settlement.getCode(),
                        IMsgConstant.SETTLE_CANCEL_VALIDATE_FUND_PAYMENT,
                        IServicecodeConstant.FUNDPAYMENT,
                        IMsgConstant.FUND_PAYMENT,
                        OperCodeTypes.abandon.getDefaultOperateName());
            }
        }
        return new SettleOperResult[0];
    }

    @Override
    public void handle(SettleOperType operType, SettleOperContext context) throws Exception {
        CtmJSONObject recordLog = new CtmJSONObject();
        recordLog.put("1.CmpFundPaymentSettleVerifyServiceImpl#handle, operType", operType);
        recordLog.put("2.CmpFundPaymentSettleVerifyServiceImpl#handle, context", context);
        log.error("1.CmpFundPaymentSettleVerifyServiceImpl#handle, operType={}, context={}", operType, context);
        SettleOperResult[] validate = validate(operType, context);
        for (SettleOperResult settleOperResult : validate) {
            if (!settleOperResult.isPass())
                throw new Exception(settleOperResult.getErrorMessage());
        }
        Settlement settlement = context.getSettlement();
        // 加锁
        String settlementId = settlement.getId();
        recordLog.put("3.CmpFundPaymentSettleVerifyServiceImpl#handle, settlementId", settlementId);
        CtmLockTool.executeInOneServiceLock(settlementId, 90L, 90L, TimeUnit.SECONDS, (int lockStatus) -> {
            if (lockStatus == LockStatus.GETLOCK_FAIL) {
                // 枷锁失败
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101498"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CB5B0280438002E", "取消结算资金付款单逻辑处理加锁异常！") /* "取消结算资金付款单逻辑处理加锁异常！" */);
            }
            try {
                logicalProcessing(settlement, recordLog);
            } catch (Exception e) {
                recordLog.put("8.CmpFundPaymentSettleVerifyServiceImpl#handle, errorMsg", e.getMessage());
                log.error("0.CmpFundPaymentSettleVerifyServiceImpl#handle.error:operType={},settlement={}, e={}", operType, settlement, e.getMessage());
                throw e;
            } finally {
                CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                ctmcmpBusinessLogService.saveBusinessLog(
                        recordLog,
                        settlement.getCode(),
                        IMsgConstant.SETTLE_CANCEL_VALIDATE_FUND_PAYMENT,
                        IServicecodeConstant.FUNDPAYMENT,
                        IMsgConstant.FUND_PAYMENT,
                        OperCodeTypes.abandon.getDefaultOperateName());
            }
        });


    }

    private void logicalProcessing(Settlement settlement,CtmJSONObject recordLog) throws Exception {
        if (settlement != null && settlement.getBodys() != null) {
            Set<String> settleIds = getSettleIds(settlement);
            recordLog.put("4.CmpFundPaymentSettleVerifyServiceImpl#handle, settleIds", settleIds);
            // 结算单明细id查询资金付款单明细数据
            List<FundPayment_b> fundPayment_bs = getFundPaymentSubData(settleIds);
            recordLog.put("5.CmpFundPaymentSettleVerifyServiceImpl#handle, fundPayment_bs", fundPayment_bs);
            if (ValueUtils.isNotEmptyObj(fundPayment_bs)) {
                Map<Long, List<FundPayment_b>> listMap = fundPayment_bs.stream().collect(Collectors.groupingBy(item -> item.getLong("mainid")));
                Map<Long, FundPayment> bizMap = getLongFundPaymentMap(fundPayment_bs);

                Map<String, Object> rollBackMap = new HashMap<>();
                Map<Long,String> updateMainBillAmountMap = new HashMap<>();
                // 待删除的主表数据
                List<FundPayment> delFundPayment= new ArrayList<>();
                // 待更新的主表数据
                List<FundPayment> updateFundPayment= new ArrayList<>();
                // 待删除的子表数据
                List<FundPayment_b> delFundPaymentSub= new ArrayList<>();
                dataHandle(listMap, bizMap, delFundPayment, delFundPaymentSub, updateMainBillAmountMap, updateFundPayment);
                log.error("5.CmpFundPaymentSettleVerifyServiceImpl#handle, updateFundPayment={}", updateFundPayment);
                FundBillAdaptationFundPlanService fundBillAdaptationFundPlanService = AppContext.getBean(FundBillAdaptationFundPlanService.class);
                IFundCommonService fundCommonService = AppContext.getBean(IFundCommonService.class);
                // 释放资金计划预占
                boolean checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundPayment.getValue());
                if (!delFundPayment.isEmpty() && checkFundPlanIsEnabled) {
                    List<BizObject> preReleaseFundBillForFundPlanProjectList = new ArrayList<>();
                    for (BizObject item : delFundPayment) {
                        for (BizObject bizObjSub : item.getBizObjects("FundPayment_b", FundPayment_b.class)) {
                            if (bizObjSub.get("fundPlanProject") != null
                                    && Objects.equals(bizObjSub.getInteger("isToPushCspl"), PushCsplStatusEnum.PRE_OCCUPIED.getValue())) {
                                preReleaseFundBillForFundPlanProjectList.add(bizObjSub);
                            }
                        }
                        if (CollectionUtils.isNotEmpty(preReleaseFundBillForFundPlanProjectList)) {
                            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_PAYMENT, item,
                                    preReleaseFundBillForFundPlanProjectList, null, null, "pre");
                        }
                    }
                }
                if (!delFundPaymentSub.isEmpty()) {
                    for (BizObject bizObjSub : delFundPaymentSub) {
                        List<BizObject> preReleaseFundBillForFundPlanProjectList = new ArrayList<>();
                        if (bizObjSub.get("fundPlanProject") != null
                                && Objects.equals(bizObjSub.getInteger("isToPushCspl"), PushCsplStatusEnum.PRE_OCCUPIED.getValue())) {
                            preReleaseFundBillForFundPlanProjectList.add(bizObjSub);
                        }
                        if (CollectionUtils.isNotEmpty(preReleaseFundBillForFundPlanProjectList) && !Objects.isNull(bizMap.get(Long.parseLong(bizObjSub.get("mainid").toString())))) {
                            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_PAYMENT, bizMap.get(Long.parseLong(bizObjSub.get("mainid").toString())),
                                    preReleaseFundBillForFundPlanProjectList, null, null, "pre");
                        }
                    }
                }

                List<Map<String, Object>> noteMaps = new ArrayList<>();
                // 释放票据
                if (!delFundPayment.isEmpty()) {
                    for (BizObject item : delFundPayment) {
                        for (BizObject subBiz : item.getBizObjects("FundPayment_b", FundPayment_b.class)) {
                            fundCommonService.deleteNoteList(noteMaps, 2, item, subBiz);
                        }
                    }
                }
                if (!delFundPaymentSub.isEmpty()) {
                    for (BizObject subBiz : delFundPaymentSub) {
                        fundCommonService.deleteNoteList(noteMaps, 2, bizMap.get(Long.parseLong(subBiz.get("mainid").toString())), subBiz);
                    }
                }
                if (ValueUtils.isNotEmptyObj(noteMaps)) {
                    try {
                        ICtmDrftEndorePaybillRpcService ctmDrftEndorePaybillRpcService = AppContext.getBean(ICtmDrftEndorePaybillRpcService.class);
                        BaseResultVO jsonObject = ctmDrftEndorePaybillRpcService.settleReleaseBillNew(noteMaps);
                        log.error("fund bill note release success! inputParameter={}, outputParameter={}", CtmJSONObject.toJSONString(noteMaps), CtmJSONObject.toJSONString(jsonObject));
                    } catch (Exception e) {
                        log.error("fund bill note release fail! inputParameter={}, errorMsg={}", CtmJSONObject.toJSONString(noteMaps), e.getMessage());
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100368"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180110", "单据明细行结算方式为票据结算，释放票据失败，请检查数据!") /* "单据明细行结算方式为票据结算，释放票据失败，请检查数据!" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180111", "：") /* "：" */ + e.getMessage());
                    }
                }

                //释放支票
                if (!delFundPayment.isEmpty()) {
                    for (BizObject item : delFundPayment) {
                        for (BizObject bizObjSub : item.getBizObjects("FundPayment_b", FundPayment_b.class)) {
                            if (bizObjSub.get("checkId") != null) {
                                CheckStock addCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, bizObjSub.get("checkId"));
                                if (addCheckStock != null) {
                                    addCheckStock.setOccupy((short) 0);
                                    addCheckStock.setEntityStatus(EntityStatus.Update);
                                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, addCheckStock);
                                }
                            }
                        }
                    }
                }
                if (!delFundPaymentSub.isEmpty()) {
                    for (BizObject bizObjSub : delFundPaymentSub) {
                        if (bizObjSub.get("checkId") != null) {
                            CheckStock addCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, bizObjSub.get("checkId"));
                            if (addCheckStock != null) {
                                addCheckStock.setOccupy((short) 0);
                                addCheckStock.setEntityStatus(EntityStatus.Update);
                                MetaDaoHelper.update(CheckStock.ENTITY_NAME, addCheckStock);
                            }
                        }
                    }
                }

                rollBackMap.put("bizMap", bizMap);
                // 事务回滚数据
                rollBackMap.put("delFundPayment",delFundPayment);
                rollBackMap.put("updateMainBillAmountMap", updateMainBillAmountMap);
                rollBackMap.put("delFundPaymentSub", delFundPaymentSub);
                YtsContext.setYtsContext("DELETE_PAYMENT_BILL_HANDLE", rollBackMap);
                log.error("6.CmpFundPaymentSettleVerifyServiceImpl#handle, rollBackMap={}", rollBackMap);
                recordLog.put("6.CmpFundPaymentSettleVerifyServiceImpl#handle, updateFundPayment", updateFundPayment);

                // 删除整单数据
                if (CollectionUtils.isNotEmpty(delFundPayment)) {
                    delFundPayment.forEach(item -> item.setEntityStatus(EntityStatus.Delete));
                    MetaDaoHelper.delete(FundPayment.ENTITY_NAME, delFundPayment);
                }

                // 删除子表明细数据
                if (CollectionUtils.isNotEmpty(delFundPaymentSub)) {
                    delFundPaymentSub.forEach(item -> item.setEntityStatus(EntityStatus.Delete));
                    MetaDaoHelper.delete(FundPayment_b.ENTITY_NAME, delFundPaymentSub);
                }

                // 更新主表数据
                EntityTool.setUpdateStatus(updateFundPayment);
                MetaDaoHelper.update(FundPayment.ENTITY_NAME, updateFundPayment);
                recordLog.put("7.CmpFundPaymentSettleVerifyServiceImpl#handle, rollBackMap", rollBackMap);
            }
        }
    }

    private static void dataHandle(Map<Long, List<FundPayment_b>> listMap, Map<Long, FundPayment> bizMap, List<FundPayment> delFundPayment, List<FundPayment_b> delFundPaymentSub, Map<Long, String> updateMainBillAmountMap, List<FundPayment> updateFundPayment) {
        for (Map.Entry<Long, List<FundPayment_b>> entry : listMap.entrySet()) {
            List<FundPayment_b> fundPaymentSubList = entry.getValue();
            Long mainId = entry.getKey();
            FundPayment bizObject = bizMap.get(mainId);
            List<FundPayment_b> bodyItems = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
            boolean allMatchPage = fundPaymentSubList.stream().allMatch(item -> item.getEntrustReject() != 1);
            boolean allMatchDB = bodyItems.stream().allMatch(item -> item.getEntrustReject() != 1);
            if (allMatchPage && allMatchDB && fundPaymentSubList.size() == bodyItems.size()){
                // 整单取消结算，直接删除主数据
                delFundPayment.add(bizObject);
            } else {
                // 不是整单取消结算
                // 1.删除明细行
                List<FundPayment_b> fundPaymentBList = fundPaymentSubList.stream()
                        .filter(item -> item.getEntrustReject() != 1).collect(Collectors.toList());
                log.error("7.CmpFundPaymentSettleVerifyServiceImpl#handle, fundPaymentBList={}", fundPaymentBList);
                if (CollectionUtils.isNotEmpty(fundPaymentBList)){
                    delFundPaymentSub.addAll(fundPaymentBList);
                    // 2.更新主表原币、本币金额
                    BigDecimal oriSumMain = BigDecimal.ZERO;
                    BigDecimal oriNatMain = BigDecimal.ZERO;
                    for (FundPayment_b fundPaymentB : fundPaymentBList) {
                        oriSumMain = BigDecimalUtils.safeAdd(oriSumMain, fundPaymentB.getOriSum());
                        oriNatMain = BigDecimalUtils.safeAdd(oriNatMain, fundPaymentB.getNatSum());
                    }
                    // 3，预留数据，用于事务回滚
                    updateMainBillAmountMap.put(Long.parseLong(bizObject.getId().toString()), bizObject.getOriSum()+"|"+bizObject.getNatSum());

                    bizObject.setOriSum(BigDecimalUtils.safeSubtract(bizObject.getOriSum(), oriSumMain));
                    bizObject.setNatSum(BigDecimalUtils.safeSubtract(bizObject.getNatSum(), oriNatMain));
                    // 4.收集待更新的主表数据
                    updateFundPayment.add(bizObject);
                }
            }
        }
    }

    private static @NotNull Map<Long, FundPayment> getLongFundPaymentMap(List<FundPayment_b> fundPayment_bs) throws Exception {
        // 收集资金付款单主表id
        Set<String> mainidIds = fundPayment_bs.stream().map(FundPayment_b::getMainid).collect(Collectors.toSet());
        // 根据资金付款单主表id批量查询数据
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(mainidIds));
        querySchema.addCondition(conditionGroup);
        // 查询子表信息
        QuerySchema detailSchema = QuerySchema.create().name("FundPayment_b").addSelect("*");
        querySchema.addCompositionSchema(detailSchema);
        List<FundPayment> bizObjects = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, querySchema, null);
        Map<Long, FundPayment> bizMap = bizObjects.stream().collect(Collectors.toMap(item -> item.getLong("id"), item -> item));
        log.error("4.CmpFundPaymentSettleVerifyServiceImpl#handle, bizObjects={},bizMap={}", bizObjects, bizMap);
        return bizMap;
    }

    private static List<FundPayment_b> getFundPaymentSubData(Set<String> settleIds) throws Exception {
        if(CollectionUtils.isEmpty(settleIds)){
            log.error("3.CmpFundPaymentSettleVerifyServiceImpl#handle, settleIds为空");
            return new ArrayList<>();
        }
        QuerySchema querySchema = QuerySchema
                .create()
                .addSelect("id, code, verifystate,pubts, FundPayment_b.settledId, FundPayment_b.entrustReject, FundPayment_b.id");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("FundPayment_b.settledId").in(settleIds));
        querySchema.addCondition(queryConditionGroup);
        List<Map<String, Object>> mapList = MetaDaoHelper.query(FundPayment.ENTITY_NAME, querySchema);
        log.error("2.1.CmpFundPaymentSettleVerifyServiceImpl#validate, mapList={}",  mapList);

        Map<Object, List<Map<String, Object>>> fundFundpaymentBSettledId = mapList.stream().collect(Collectors.groupingBy(item -> item.get("FundPayment_b_settledId")));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Object, List<Map<String, Object>>> listEntry : fundFundpaymentBSettledId.entrySet()) {
            List<Map<String, Object>> list = listEntry.getValue();
            if (list.size() == 1) {
                result.addAll(list);
            } else {
                Map<String, Object> data = null;
                for (Map<String, Object> map : list) {
                    if(map.get("pubts") instanceof Timestamp){
                        if (data == null || ((Timestamp) map.get("pubts")).compareTo((Timestamp)data.get("pubts"))> 0) {
                            data = map;
                        }
                    } else if (map.get("pubts") instanceof Date) {
                        if (data == null || ((Date) map.get("pubts")).compareTo((Date)data.get("pubts"))> 0) {
                            data = map;
                        }
                    } else {
                        if (data == null || map.get("pubts").toString().compareTo(data.get("pubts").toString()) > 0) {
                            data = map;
                        }
                    }
                }
                result.add(data);
            }
        }
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> resultMap : result) {
            String id = resultMap.get("FundPayment_b_id").toString();
            ids.add(id);
        }
        if(CollectionUtils.isEmpty(ids)){
            log.error("3.CmpFundPaymentSettleVerifyServiceImpl#handle, 根据settleIds查询资金收付款单据为空");
            return new ArrayList<>();
        }
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup1 = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup1.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(queryConditionGroup1);
        List<FundPayment_b> fundPayment_bs = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, schema, null);
        log.error("3.CmpFundPaymentSettleVerifyServiceImpl#handle, fundPayment_bs={}", fundPayment_bs);
        return fundPayment_bs;
    }

    private static @NotNull Set<String> getSettleIds(Settlement settlement) {
        SettleBody[] settlementBody = settlement.getBodys();
        Set<String> settleIds = new HashSet<>();
        // 收集结算明细id
        for (SettleBody settleBody : settlementBody) {
            String id = settleBody.getId();
            settleIds.add(id);
        }
        log.error("2.CmpFundPaymentSettleVerifyServiceImpl#handle, settleIds={}", settleIds);

        if (!ValueUtils.isNotEmptyObj(settleIds)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100683"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807CC", "单据的数据不能为空！") /* "单据的数据不能为空！" */);
        }
        return settleIds;
    }

    @Override
    public Object rollBackPaymentHandle(SettleOperType operType, SettleOperContext context) throws Exception {
        Map<String, Object> rollBackMap= (Map<String, Object>) YtsContext.getYtsContext("DELETE_PAYMENT_BILL_HANDLE");
        log.error("rollBackPaymentHandle, params={}", CtmJSONObject.toJSONString(rollBackMap));
        if (ValueUtils.isNotEmptyObj(rollBackMap)) {

            // 回滚删除的主表
            List<FundPayment> delFundPayment = (List<FundPayment>) rollBackMap.get("delFundPayment");
            if (CollectionUtils.isNotEmpty(delFundPayment)) {
                for (BizObject bizObject : delFundPayment) {
                    bizObject.setEntityStatus(EntityStatus.Insert);
                    CmpMetaDaoHelper.insert(FundPayment.ENTITY_NAME, bizObject);
                    List<FundPayment_b> fundPayment_bs = bizObject.get("FundPayment_b");
                    CmpMetaDaoHelper.insert(FundPayment_b.ENTITY_NAME, fundPayment_bs);
                }
            }
            // 回滚删除的子表
            List<FundPayment_b> delFundPaymentSub = (List<FundPayment_b>) rollBackMap.get("delFundPaymentSub");
            if (CollectionUtils.isNotEmpty(delFundPaymentSub)) {
                for (FundPayment_b fundPaymentB : delFundPaymentSub) {
                    fundPaymentB.setEntityStatus(EntityStatus.Insert);
                    CmpMetaDaoHelper.insert(FundPayment_b.ENTITY_NAME, fundPaymentB);
                }
            }
            FundBillAdaptationFundPlanService fundBillAdaptationFundPlanService = AppContext.getBean(FundBillAdaptationFundPlanService.class);
            IFundCommonService fundCommonService = AppContext.getBean(IFundCommonService.class);
            // 占用资金计划预占
            if (!delFundPayment.isEmpty()) {
                List<BizObject> preReleaseFundBillForFundPlanProjectList = new ArrayList<>();
                for (BizObject item : delFundPayment) {
                    for (BizObject bizObjSub : item.getBizObjects("FundPayment_b", FundPayment_b.class)) {
                        preReleaseFundBillForFundPlanProjectList.add(bizObjSub);
                    }
                    if (CollectionUtils.isNotEmpty(preReleaseFundBillForFundPlanProjectList)) {
                        fundBillAdaptationFundPlanService.fundBillEmployFundPlan(IBillNumConstant.FUND_PAYMENT, item,
                                preReleaseFundBillForFundPlanProjectList, "pre");
                    }
                }
            }
            Map<Long, FundPayment> bizMap = (Map<Long, FundPayment>) rollBackMap.get("bizMap");
            if (!delFundPaymentSub.isEmpty()) {
                for (BizObject bizObjSub : delFundPaymentSub) {
                    List<BizObject> preReleaseFundBillForFundPlanProjectList = new ArrayList<>();
                    preReleaseFundBillForFundPlanProjectList.add(bizObjSub);
                    if (CollectionUtils.isNotEmpty(preReleaseFundBillForFundPlanProjectList)) {
                        fundBillAdaptationFundPlanService.fundBillEmployFundPlan(IBillNumConstant.FUND_PAYMENT, bizMap.get(Long.parseLong(bizObjSub.get("mainid").toString())),
                                preReleaseFundBillForFundPlanProjectList, "pre");
                    }
                }
            }
            List<Map<String, Object>> noteMaps = new ArrayList<>();
            // 释放票据
            if (!delFundPaymentSub.isEmpty()) {
                for (BizObject item : delFundPaymentSub) {
                    for (BizObject subBiz : item.getBizObjects("FundPayment_b", FundPayment_b.class)) {
                        fundCommonService.deleteNoteList(noteMaps, 2, item, subBiz);
                    }
                }
            }
            if (!delFundPaymentSub.isEmpty()) {
                for (BizObject subBiz : delFundPaymentSub) {
                    fundCommonService.deleteNoteList(noteMaps, 2,
                            bizMap.get(Long.parseLong(subBiz.get("mainid").toString())), subBiz);
                }
            }
            if (ValueUtils.isNotEmptyObj(noteMaps)) {
                try {
                    ICtmDrftEndorePaybillRpcService ctmDrftEndorePaybillRpcService = AppContext.getBean(ICtmDrftEndorePaybillRpcService.class);
                    SettleUseBillResVO jsonObject = ctmDrftEndorePaybillRpcService.settleUseBillNew(noteMaps);
                    log.error("fund bill note occupied success! inputParameter={}, outputParameter={}", CtmJSONObject.toJSONString(noteMaps), CtmJSONObject.toJSONString(jsonObject));
                } catch (Exception e) {
                    log.error("fund bill note occupied fail! inputParameter={}, errorMsg={}", CtmJSONObject.toJSONString(noteMaps), e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102111"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800DA", "单据明细行结算方式为票据结算，占用票据失败，请检查数据!") /* "单据明细行结算方式为票据结算，占用票据失败，请检查数据!" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D4", "：") /* "：" */ + e.getMessage());
                }
            }


            //占用支票
            if (!delFundPayment.isEmpty()) {
                for (BizObject item : delFundPayment) {
                    for (BizObject bizObjSub : item.getBizObjects("FundPayment_b", FundPayment_b.class)) {
                        if (bizObjSub.get("checkId") != null) {
                            CheckStock addCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, bizObjSub.get("checkId"));
                            if (addCheckStock != null) {
                                addCheckStock.setOccupy((short) 1);
                                addCheckStock.setEntityStatus(EntityStatus.Update);
                                MetaDaoHelper.update(CheckStock.ENTITY_NAME, addCheckStock);
                            }
                        }
                    }
                }
            }
            if (!delFundPaymentSub.isEmpty()) {
                for (BizObject bizObjSub : delFundPaymentSub) {
                    if (bizObjSub.get("checkId") != null) {
                        CheckStock addCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, bizObjSub.get("checkId"));
                        if (addCheckStock != null) {
                            addCheckStock.setOccupy((short) 1);
                            addCheckStock.setEntityStatus(EntityStatus.Update);
                            MetaDaoHelper.update(CheckStock.ENTITY_NAME, addCheckStock);
                        }
                    }
                }
            }

            // 回滚更新的主表
            Map<Long,String> updateMainBillAmountMap = (Map<Long, String>) rollBackMap.get("updateMainBillAmountMap");
            List<BizObject> fundPaymentList = new ArrayList<>();
            for (Map.Entry<Long, String> bizEntry : updateMainBillAmountMap.entrySet()) {
                Long id = bizEntry.getKey();
                String value = bizEntry.getValue();
                String[] split = value.split("|");
                BigDecimal oriSum = new BigDecimal(split[0]);
                BigDecimal natSum = new BigDecimal(split[1]);

                BizObject bizObject = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id, 1);
                bizObject.set(ICmpConstant.ORISUM, oriSum);
                bizObject.set(ICmpConstant.NATSUM, natSum);
                fundPaymentList.add(bizObject);
            }
            EntityTool.setUpdateStatus(fundPaymentList);
            MetaDaoHelper.update(FundPayment.ENTITY_NAME, fundPaymentList);

            Map<String, Object> resultResponse = new HashMap<>();
            resultResponse.put("code", 200);
            resultResponse.put("isSuccess", true);
            return resultResponse;
        }
        return new RuleExecuteResult(999, "no data");
    }
}
