package com.yonyoucloud.fi.cmp.autocorrsetting;

import com.yonyou.cloud.utils.StringUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationCount;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmcmpReWriteBusRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.billclaim.CorrDataEntityParam;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@Transactional
public class CorrBusRule extends AbstractCommonRule {

    public static final String FUND_PAYMENT_B_FULLNAME = "cmp.fundpayment.FundPayment_b";
    public static final String FUND_COLLECTION_B_FULLNAME = "cmp.fundcollection.FundCollection_b";

    @Autowired
    ReWriteBusCorrDataService reWriteBusCorrDataService;
    @Autowired
    CtmcmpReWriteBusRpcService ctmcmpReWriteBusRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (billContext.getContext() != null && billContext.getContext().get("taskSave") != null
                && billContext.getContext().get("taskSave") instanceof Boolean && (Boolean) billContext.getContext().get("taskSave")) {
            return new RuleExecuteResult();
        }
        String billNum = billContext.getBillnum();
        BizObject bizObject = bills.get(0);
        // 处理需要关联数据
        if ("cmp_fundpayment".equals(billNum)) {
            // 资金付款单
            handleFundPayment(bizObject, billContext);
        } else {
            // 资金收款单
            handleFundCollection(bizObject, billContext);
        }
        return new RuleExecuteResult();
    }

    private void handleFundCollection(BizObject bizObject, BillContext billContext) throws Exception {
        EntityStatus status = bizObject.getEntityStatus();
        String billNum = billContext.getBillnum();
        // 提前入账
        Boolean isadvanceaccounts = bizObject.get("isadvanceaccounts");
        Long busid = Long.valueOf(bizObject.getId().toString());// 业务单据id
        Long bid;// 对账单/认领单id
        List<CorrDataEntityParam> corrDataEntityParamList = new ArrayList<>();
        List<FundCollection_b> fundCollectionOriginal = bizObject.get("FundCollection_b");
        // 删除关联的银行对账单或认领单明细行时，如果是整单删除，需要调接口删除关联关系
        if (EntityStatus.Update.equals(status)) {
            if (!ValueUtils.isNotEmptyObj(fundCollectionOriginal)) {
                return;
            }
            //CZFW-344999 拆分数据时，关联关系回写问题修复
            List<String> claimidList = fundCollectionOriginal.stream().filter(fundCollection_b -> fundCollection_b.getEntityStatus().equals(EntityStatus.Update)).map(FundCollection_b::getBillClaimId).collect(Collectors.toList());
            List<String> bankidList = fundCollectionOriginal.stream().filter(fundCollection_b -> fundCollection_b.getEntityStatus().equals(EntityStatus.Update)).map(FundCollection_b::getBankReconciliationId).collect(Collectors.toList());
            List<FundCollection_b> fundCollectionFromClaimIdBillDelete = new ArrayList<>();
            List<FundCollection_b> fundCollectionFromBankReconciliationBillDelete = new ArrayList<>();
            for (FundCollection_b fundCollection_b : fundCollectionOriginal) {
                EntityStatus entityStatus = fundCollection_b.getEntityStatus();
                if (entityStatus.equals(EntityStatus.Delete)) {
                    if (fundCollection_b.getBillClaimId() != null && !claimidList.contains(fundCollection_b.getBillClaimId())) {
                        fundCollectionFromClaimIdBillDelete.add(fundCollection_b);
                    }
                    if (fundCollection_b.getBankReconciliationId() != null && !bankidList.contains(fundCollection_b.getBankReconciliationId())) {
                        fundCollectionFromBankReconciliationBillDelete.add(fundCollection_b);
                    }
                }
            }
            FundCollection fundCollection1 = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizObject.getId());
            List<FundCollection_b> fundCollectionFromClaimIdBillOrBankReconciliationBillDeleteList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(fundCollectionFromClaimIdBillDelete)) {
                List<FundCollection_b> fundPaymentBs = fundCollection1.getBizObjects("FundCollection_b", FundCollection_b.class);
                List<String> collect = fundPaymentBs.stream().map(FundCollection_b::getBillClaimId).collect(Collectors.toList());
                for (FundCollection_b fundPaymentB : fundCollectionFromClaimIdBillDelete) {
                    if (!collect.contains(fundPaymentB.getBillClaimId())) {
                        fundCollectionFromClaimIdBillOrBankReconciliationBillDeleteList.add(fundPaymentB);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(fundCollectionFromBankReconciliationBillDelete)) {
                List<FundCollection_b> fundPaymentBs = fundCollection1.getBizObjects("FundCollection_b", FundCollection_b.class);
                List<String> collect = fundPaymentBs.stream().map(FundCollection_b::getBankReconciliationId).collect(Collectors.toList());
                for (FundCollection_b fundPaymentB : fundCollectionFromBankReconciliationBillDelete) {
                    if (!collect.contains(fundPaymentB.getBankReconciliationId())) {
                        fundCollectionFromClaimIdBillOrBankReconciliationBillDeleteList.add(fundPaymentB);
                    }
                }
            }
            if (CollectionUtils.isEmpty(fundCollectionFromClaimIdBillOrBankReconciliationBillDeleteList)) {
                return;
            }
            List<CtmJSONObject> listreq = new ArrayList();
            Short associationcount = fundCollection1.getAssociationcount();
            for (FundCollection_b coll : fundCollectionFromClaimIdBillOrBankReconciliationBillDeleteList) {
                if (coll.getAssociationStatus() != null && AssociationStatus.Associated.getValue() == coll.getAssociationStatus().shortValue()) {
                    CtmJSONObject jsonReq = new CtmJSONObject();
                    if (org.imeta.core.lang.StringUtils.isNotEmpty(coll.getBankReconciliationId())) {
                        jsonReq.put("busid", Long.parseLong(coll.getBankReconciliationId()));
                        if (associationcount != null && AssociationCount.First.getValue() == associationcount) {
                            BankReconciliation byId = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, coll.getBankReconciliationId(), 3);
                            if (byId == null) {
                                continue;
                            }
                            Short associationcount1 = byId.getAssociationcount();
                            if (associationcount1 != null && AssociationCount.Second.getValue() == associationcount1) {
                                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankReconciliationId").eq(coll.getBankReconciliationId()), QueryCondition.name("id").not_eq(coll.getId()));
                                querySchema1.addCondition(group1);
                                List<BizObject> bizObjects = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema1, null);
                                if (bizObjects != null && bizObjects.size() > 0) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100007"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180831", "本单据对应的银行对账单已生成实际业务单据，不允许删除") /* "本单据对应的银行对账单已生成实际业务单据，不允许删除" */);
                                }
                            }
                        }
                    }
                    if (org.imeta.core.lang.StringUtils.isNotEmpty(coll.getBillClaimId())) {
                        jsonReq.put("claimid", Long.parseLong(coll.getBillClaimId()));
                    }
                    jsonReq.put("stwbbusid", coll.getId());
                    listreq.add(jsonReq);
                }
            }
            if (CollectionUtils.isNotEmpty(listreq)) {
                for (CtmJSONObject jsonReq : listreq) {
                    reWriteBusCorrDataService.resDelData(jsonReq);
                }
            }
            return;
        }
        List<Long> bankReconciliationIdList = new ArrayList<>();
        List<FundCollection_b> updateFundCollection_bList = new ArrayList<>();
        List<FundCollection_b> fundCollectionFromClaimIdBill = new ArrayList<>();
        List<FundCollection_b> fundCollectionFromBankReconciliationBill = new ArrayList<>();
        Map<String, List<FundCollection_b>> fundCollectionFromClaimIdBillMap = new HashMap<>();
        Map<String, List<FundCollection_b>> fundCollectionFromBankReconciliationBillMap = new HashMap<>();

        Map<String, BigDecimal> fundCollectionFromClaimIdBillOriSumMap = new HashMap<>();
        Map<String, BigDecimal> fundCollectionFromBankReconciliationBillOriSumMap = new HashMap<>();
        for (FundCollection_b collectionB : fundCollectionOriginal) {
            if (collectionB.getBillClaimId() != null) {
                fundCollectionFromClaimIdBill.add(collectionB);
            }
            if (collectionB.getBankReconciliationId() != null) {
                fundCollectionFromBankReconciliationBill.add(collectionB);
                bankReconciliationIdList.add(Long.valueOf(collectionB.getBankReconciliationId()));
            }
        }

        // 流水和认领单集合
        Map<String, String> bankReconciliationIdMap = getBankReconciliationIdMap(bankReconciliationIdList);

        if (CollectionUtils.isNotEmpty(fundCollectionFromClaimIdBill)) {
            fundCollectionFromClaimIdBillMap = fundCollectionFromClaimIdBill.stream().collect(Collectors.groupingBy(FundCollection_b::getBillClaimId));
            Map<String, List<FundCollection_b>> groupedFundPaymentB = fundCollectionFromClaimIdBill.stream().collect(Collectors.groupingBy(FundCollection_b::getBillClaimId));
            fundCollectionFromClaimIdBillOriSumMap = groupedFundPaymentB.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().stream().map(FundCollection_b::getSwapOutAmountEstimate) // 将FundPayment_b集合转为BigDecimal流并求和
                    .reduce(BigDecimalUtils::safeAdd) // 使用reduce方法计算总和
                    .orElse(BigDecimal.ZERO))); // 使用orElse方法提供一个默认值，以防出现空列表的情况
        }
        if (CollectionUtils.isNotEmpty(fundCollectionFromBankReconciliationBill)) {
            fundCollectionFromBankReconciliationBillMap = fundCollectionFromBankReconciliationBill.stream().collect(Collectors.groupingBy(FundCollection_b::getBankReconciliationId));
            Map<String, List<FundCollection_b>> groupedFundPaymentB = fundCollectionFromBankReconciliationBill.stream().collect(Collectors.groupingBy(FundCollection_b::getBankReconciliationId));
            fundCollectionFromBankReconciliationBillOriSumMap = groupedFundPaymentB.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().stream().map(FundCollection_b::getSwapOutAmountEstimate) // 将FundPayment_b集合转为BigDecimal流并求和
                    .reduce(BigDecimalUtils::safeAdd) // 使用reduce方法计算总和
                    .orElse(BigDecimal.ZERO))); // 使用orElse方法提供一个默认值，以防出现空列表的情况
        }
        List<FundCollection_b> fundCollection_bs = new ArrayList<>();
        if (ValueUtils.isNotEmpty(fundCollectionFromClaimIdBillMap)) {
            for (List<FundCollection_b> listEntry : fundCollectionFromClaimIdBillMap.values()) {
                fundCollection_bs.add(listEntry.get(0));
            }
        }
        if (ValueUtils.isNotEmpty(fundCollectionFromBankReconciliationBillMap)) {
            for (List<FundCollection_b> listEntry : fundCollectionFromBankReconciliationBillMap.values()) {
                fundCollection_bs.add(listEntry.get(0));
            }
        }
        for (FundCollection_b coll : fundCollection_bs) {
            buildeCorrDataEntityParam4FundCollection(coll, bizObject, busid, status, billNum, isadvanceaccounts,
                    fundCollectionFromClaimIdBillOriSumMap, fundCollectionFromBankReconciliationBillOriSumMap,
                    corrDataEntityParamList, bankReconciliationIdMap);
        }
        if (CollectionUtils.isNotEmpty(updateFundCollection_bList)) {
            List<FundCollection_b> updateDataList = new ArrayList<>();
            for (FundCollection_b fundPaymentB : updateFundCollection_bList) {
                String smartcheckno = fundPaymentB.getSmartcheckno();
                if (ValueUtils.isNotEmpty(fundCollectionFromBankReconciliationBillMap)) {
                    String bankReconciliationId = fundPaymentB.getBankReconciliationId();
                    if (ValueUtils.isNotEmptyObj(bankReconciliationId)) {
                        List<FundCollection_b> fundCollectionBs = fundCollectionFromBankReconciliationBillMap.get(bankReconciliationId);
                        fundCollectionBs.forEach(item -> {
                            item.setSmartcheckno(smartcheckno);
                            BizObject obj = item.get("characterDefb");
                            if (ValueUtils.isNotEmptyObj(obj)) {
                                obj.setEntityStatus(EntityStatus.Update);
                            }
                            List<BizObject> fundCollectionThirdPartyRelationList = item.get("FundCollectionSubWithholdingRelation");
                            if (CollectionUtils.isNotEmpty(fundCollectionThirdPartyRelationList)) {
                                EntityTool.setUpdateStatus(fundCollectionThirdPartyRelationList);
                            }
                            try {
                                Date pubts = item.getPubts();
                                item.set("pubts", DateUtils.strToDateSql(DateUtils.dateFormat(pubts, DateUtils.DATE_TIME_PATTERN)));
                                Date lastInterestSettlementEndDate = item.getLastInterestSettlementEndDate();
                                if (ValueUtils.isNotEmptyObj(lastInterestSettlementEndDate)) {
                                    item.set("lastInterestSettlementEndDate", DateUtils.strToDateSql(DateUtils.dateFormat(lastInterestSettlementEndDate, DateUtils.DATE_TIME_PATTERN)));
                                }
                                Date currentInterestSettlementStartDate = item.getCurrentInterestSettlementStartDate();
                                if (ValueUtils.isNotEmptyObj(currentInterestSettlementStartDate)) {
                                    item.set("currentInterestSettlementStartDate", DateUtils.strToDateSql(DateUtils.dateFormat(currentInterestSettlementStartDate, DateUtils.DATE_TIME_PATTERN)));
                                }
                                Date currentInterestSettlementEndDate = item.getCurrentInterestSettlementEndDate();
                                if (ValueUtils.isNotEmptyObj(currentInterestSettlementEndDate)) {
                                    item.set("currentInterestSettlementEndDate", DateUtils.strToDateSql(DateUtils.dateFormat(currentInterestSettlementEndDate, DateUtils.DATE_TIME_PATTERN)));
                                }
                            } catch (ParseException e) {
                                log.error("date convert Timestamp fail! errorMsg={}", e.getMessage());
                            }
                        });
                        updateDataList.addAll(fundCollectionBs);
                    }
                }
                if (ValueUtils.isNotEmpty(fundCollectionFromClaimIdBillMap)) {
                    String billClaimId = fundPaymentB.getBillClaimId();
                    if (ValueUtils.isNotEmptyObj(billClaimId)) {
                        List<FundCollection_b> fundCollectionBs = fundCollectionFromClaimIdBillMap.get(billClaimId);
                        fundCollectionBs.forEach(item -> {
                            item.setSmartcheckno(smartcheckno);
                            BizObject obj = item.get("characterDefb");
                            if (obj != null) {
                                obj.setEntityStatus(EntityStatus.Update);
                            }
                            List<BizObject> fundCollectionThirdPartyRelationList = item.get("FundCollectionSubWithholdingRelation");
                            if (CollectionUtils.isNotEmpty(fundCollectionThirdPartyRelationList)) {
                                EntityTool.setUpdateStatus(fundCollectionThirdPartyRelationList);
                            }
                            try {
                                Date pubts = item.getPubts();
                                item.set("pubts", DateUtils.strToDateSql(DateUtils.dateFormat(pubts, DateUtils.DATE_TIME_PATTERN)));
                                Date lastInterestSettlementEndDate = item.getLastInterestSettlementEndDate();
                                if (ValueUtils.isNotEmptyObj(lastInterestSettlementEndDate)) {
                                    item.set("lastInterestSettlementEndDate", DateUtils.strToDateSql(DateUtils.dateFormat(lastInterestSettlementEndDate, DateUtils.DATE_TIME_PATTERN)));
                                }
                                Date currentInterestSettlementStartDate = item.getCurrentInterestSettlementStartDate();
                                if (ValueUtils.isNotEmptyObj(currentInterestSettlementStartDate)) {
                                    item.set("currentInterestSettlementStartDate", DateUtils.strToDateSql(DateUtils.dateFormat(currentInterestSettlementStartDate, DateUtils.DATE_TIME_PATTERN)));
                                }
                                Date currentInterestSettlementEndDate = item.getCurrentInterestSettlementEndDate();
                                if (ValueUtils.isNotEmptyObj(currentInterestSettlementEndDate)) {
                                    item.set("currentInterestSettlementEndDate", DateUtils.strToDateSql(DateUtils.dateFormat(currentInterestSettlementEndDate, DateUtils.DATE_TIME_PATTERN)));
                                }
                            } catch (ParseException e) {
                                log.error("date convert Timestamp fail! errorMsg={}", e.getMessage());
                            }
                        });
                        updateDataList.addAll(fundCollectionBs);
                    }
                }
            }
        }
        ctmcmpReWriteBusRpcService.batchReWriteBankRecilicationForRpc(corrDataEntityParamList);
    }

    /**
     * 根据流水id批量查询流水财资统一对账码
     *
     * @param bankReconciliationIdList
     * @return
     * @throws Exception
     */
    private Map<String, String> getBankReconciliationIdMap(List bankReconciliationIdList) throws Exception {
        Map<String, String> bankReconciliationIdMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(bankReconciliationIdList)) {
            // 根据条件查询认领单
            QuerySchema queryBankReconciliation = QuerySchema.create().addSelect("id,smartcheckno");
            QueryConditionGroup groupBankReconciliation = new QueryConditionGroup();
            groupBankReconciliation.appendCondition(QueryConditionGroup.and(QueryCondition.name("id").in(bankReconciliationIdList)));
            queryBankReconciliation.addCondition(groupBankReconciliation);
            // 先查询流水
            List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, queryBankReconciliation, null);
            for (BankReconciliation bankReconciliation : bankReconciliations) {
                bankReconciliationIdMap.put(bankReconciliation.getId().toString(), bankReconciliation.getSmartcheckno());
            }
        }
        return bankReconciliationIdMap;
    }


    /**
     * 处理资金付款单
     *
     * @param bizObject
     * @param billContext
     * @throws Exception
     */
    private void handleFundPayment(BizObject bizObject, BillContext billContext) throws Exception {
        EntityStatus status = bizObject.getEntityStatus();
        String billNum = billContext.getBillnum();
        // 提前入账
        Boolean isadvanceaccounts = bizObject.get("isadvanceaccounts");
        Long busid = Long.valueOf(bizObject.getId().toString());// 业务单据id
        Long bid;// 对账单/认领单id
        // 资金付款单
        List<CorrDataEntityParam> corrDataEntityParamList = new ArrayList<>();
        if (EventType.InternalTransferProtocol.getValue() == bizObject.getShort(ICmpConstant.BILLTYPE)) {
            return;
        }
        List<FundPayment_b> fundPaymentOriginal = bizObject.get("FundPayment_b");
        // 删除关联的银行对账单或认领单明细行时，如果是整单删除，需要调接口删除关联关系
        if (EntityStatus.Update.equals(status)) {
            if (!ValueUtils.isNotEmptyObj(fundPaymentOriginal)) {
                return;
            }
            //CZFW-344999 拆分数据时，关联关系回写问题修复
            List<String> claimidList = fundPaymentOriginal.stream().filter(fundPayment_b -> fundPayment_b.getEntityStatus().equals(EntityStatus.Update)).map(FundPayment_b::getBillClaimId).collect(Collectors.toList());
            List<String> bankidList = fundPaymentOriginal.stream().filter(fundPayment_b -> fundPayment_b.getEntityStatus().equals(EntityStatus.Update)).map(FundPayment_b::getBankReconciliationId).collect(Collectors.toList());
            List<FundPayment_b> fundPaymentFromClaimIdBillDelete = new ArrayList<>();
            List<FundPayment_b> fundPaymentFromBankReconciliationBillDelete = new ArrayList<>();
            for (FundPayment_b fundPayment_b : fundPaymentOriginal) {
                EntityStatus entityStatus = fundPayment_b.getEntityStatus();
                if (entityStatus.equals(EntityStatus.Delete)) {
                    if (fundPayment_b.getBillClaimId() != null && !claimidList.contains(fundPayment_b.getBillClaimId())) {
                        fundPaymentFromClaimIdBillDelete.add(fundPayment_b);
                    }
                    if (fundPayment_b.getBankReconciliationId() != null && !bankidList.contains(fundPayment_b.getBankReconciliationId())) {
                        fundPaymentFromBankReconciliationBillDelete.add(fundPayment_b);
                    }
                }
            }
            FundPayment fundPayment1 = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizObject.getId());
            List<FundPayment_b> fundPaymentFromClaimIdBillOrBankReconciliationBillDeleteList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(fundPaymentFromClaimIdBillDelete)) {
                List<FundPayment_b> fundPaymentBs = fundPayment1.getBizObjects("FundPayment_b", FundPayment_b.class);
                List<String> collect = fundPaymentBs.stream().map(FundPayment_b::getBillClaimId).collect(Collectors.toList());
                for (FundPayment_b fundPaymentB : fundPaymentFromClaimIdBillDelete) {
                    if (!collect.contains(fundPaymentB.getBillClaimId())) {
                        fundPaymentFromClaimIdBillOrBankReconciliationBillDeleteList.add(fundPaymentB);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(fundPaymentFromBankReconciliationBillDelete)) {
                List<FundPayment_b> fundPaymentBs = fundPayment1.getBizObjects("FundPayment_b", FundPayment_b.class);
                List<String> collect = fundPaymentBs.stream().map(FundPayment_b::getBankReconciliationId).collect(Collectors.toList());
                for (FundPayment_b fundPaymentB : fundPaymentFromBankReconciliationBillDelete) {
                    if (!collect.contains(fundPaymentB.getBankReconciliationId())) {
                        fundPaymentFromClaimIdBillOrBankReconciliationBillDeleteList.add(fundPaymentB);
                    }
                }
            }
            if (CollectionUtils.isEmpty(fundPaymentFromClaimIdBillOrBankReconciliationBillDeleteList)) {
                return;
            }
            List<CtmJSONObject> listreq = new ArrayList<>();
            Short associationcount = fundPayment1.getAssociationcount();
            for (FundPayment_b paymentB : fundPaymentFromClaimIdBillOrBankReconciliationBillDeleteList) {
                if (paymentB.getAssociationStatus() != null && AssociationStatus.Associated.getValue() == paymentB.getAssociationStatus().shortValue()) {
                    CtmJSONObject jsonReq = new CtmJSONObject();
                    if (org.imeta.core.lang.StringUtils.isNotEmpty(paymentB.getBankReconciliationId())) {
                        jsonReq.put("busid", Long.parseLong(paymentB.getBankReconciliationId()));
                        if (associationcount != null && AssociationCount.First.getValue() == associationcount) {
                            BankReconciliation byId = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, paymentB.getBankReconciliationId(), 3);
                            if (byId == null) {
                                continue;
                            }
                            Short associationcount1 = byId.getAssociationcount();
                            if (associationcount1 != null && AssociationCount.Second.getValue() == associationcount1) {
                                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankReconciliationId").eq(paymentB.getBankReconciliationId()), QueryCondition.name("id").not_eq(paymentB.getId()));
                                querySchema1.addCondition(group1);
                                List<BizObject> bizObjects = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema1, null);
                                if (bizObjects != null && bizObjects.size() > 0) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100007"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180831", "本单据对应的银行对账单已生成实际业务单据，不允许删除") /* "本单据对应的银行对账单已生成实际业务单据，不允许删除" */);
                                }
                            }
                        }
                    }
                    if (org.imeta.core.lang.StringUtils.isNotEmpty(paymentB.getBillClaimId())) {
                        jsonReq.put("claimid", Long.parseLong(paymentB.getBillClaimId()));
                    }
                    jsonReq.put("stwbbusid", paymentB.getId());
                    listreq.add(jsonReq);
                }
            }
            if (CollectionUtils.isNotEmpty(listreq)) {
                for (CtmJSONObject jsonReq : listreq) {
                    reWriteBusCorrDataService.resDelData(jsonReq);
                }
            }
            return;
        }
        List<FundPayment_b> updateList = new ArrayList<>();
        List<FundPayment_b> fundPaymentFromClaimIdBill = new ArrayList<>();
        List<Long> bankReconciliationIdList = new ArrayList<>();
        List<FundPayment_b> fundPaymentFromBankReconciliationBill = new ArrayList<>();
        Map<String, List<FundPayment_b>> fundPaymentFromClaimIdBillMap = new HashMap<>();
        Map<String, List<FundPayment_b>> fundPaymentFromBankReconciliationBillMap = new HashMap<>();
        Map<String, BigDecimal> fundPaymentFromClaimIdBillOriSumMap = new HashMap<>();
        Map<String, BigDecimal> fundPaymentFromBankReconciliationBillOriSumMap = new HashMap<>();
        for (FundPayment_b paymentB : fundPaymentOriginal) {
            if (paymentB.getBillClaimId() != null) {
                fundPaymentFromClaimIdBill.add(paymentB);
            }
            if (paymentB.getBankReconciliationId() != null) {
                fundPaymentFromBankReconciliationBill.add(paymentB);
                bankReconciliationIdList.add(Long.valueOf(paymentB.getBankReconciliationId()));
            }
        }

        // 流水和认领单集合
        Map<String, String> bankReconciliationIdMap = getBankReconciliationIdMap(bankReconciliationIdList);

        if (CollectionUtils.isNotEmpty(fundPaymentFromClaimIdBill)) {
            fundPaymentFromClaimIdBillMap = fundPaymentFromClaimIdBill.stream().collect(Collectors.groupingBy(FundPayment_b::getBillClaimId));
            Map<String, List<FundPayment_b>> groupedFundPaymentB = fundPaymentFromClaimIdBill.stream().collect(Collectors.groupingBy(FundPayment_b::getBillClaimId));
            fundPaymentFromClaimIdBillOriSumMap = groupedFundPaymentB.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().stream().map(FundPayment_b::getSwapOutAmountEstimate) // 将FundPayment_b集合转为BigDecimal流并求和
                    .reduce(BigDecimalUtils::safeAdd) // 使用reduce方法计算总和
                    .orElse(BigDecimal.ZERO))); // 使用orElse方法提供一个默认值，以防出现空列表的情况
        }
        if (CollectionUtils.isNotEmpty(fundPaymentFromBankReconciliationBill)) {
            fundPaymentFromBankReconciliationBillMap = fundPaymentFromBankReconciliationBill.stream().collect(Collectors.groupingBy(FundPayment_b::getBankReconciliationId));
            Map<String, List<FundPayment_b>> groupedFundPaymentB = fundPaymentFromBankReconciliationBill.stream().collect(Collectors.groupingBy(FundPayment_b::getBankReconciliationId));
            fundPaymentFromBankReconciliationBillOriSumMap = groupedFundPaymentB.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().stream().map(FundPayment_b::getSwapOutAmountEstimate) // 将FundPayment_b集合转为BigDecimal流并求和
                    .reduce(BigDecimalUtils::safeAdd) // 使用reduce方法计算总和
                    .orElse(BigDecimal.ZERO))); // 使用orElse方法提供一个默认值，以防出现空列表的情况
        }
        List<FundPayment_b> fundPayment_bs = new ArrayList<>();
        if (ValueUtils.isNotEmpty(fundPaymentFromClaimIdBillMap)) {
            for (List<FundPayment_b> listEntry : fundPaymentFromClaimIdBillMap.values()) {
                fundPayment_bs.add(listEntry.get(0));
            }
        }
        if (ValueUtils.isNotEmpty(fundPaymentFromBankReconciliationBillMap)) {
            for (List<FundPayment_b> listEntry : fundPaymentFromBankReconciliationBillMap.values()) {
                fundPayment_bs.add(listEntry.get(0));
            }
        }
        for (FundPayment_b paymentB : fundPayment_bs) {
            buildeCorrDataEntityParam4FundPayment(paymentB, bizObject, busid, status, billNum, isadvanceaccounts,
                    fundPaymentFromClaimIdBillOriSumMap,
                    fundPaymentFromBankReconciliationBillOriSumMap, corrDataEntityParamList, bankReconciliationIdMap);
        }
        if (CollectionUtils.isNotEmpty(updateList)) {
            List<FundPayment_b> updateDataList = new ArrayList<>();
            for (FundPayment_b fundPaymentB : updateList) {
                String smartcheckno = fundPaymentB.getSmartcheckno();
                if (ValueUtils.isNotEmpty(fundPaymentFromBankReconciliationBillMap)) {
                    String bankReconciliationId = fundPaymentB.getBankReconciliationId();
                    if (ValueUtils.isNotEmptyObj(bankReconciliationId)) {
                        List<FundPayment_b> fundPaymentBs = fundPaymentFromBankReconciliationBillMap.get(bankReconciliationId);
                        fundPaymentBs.forEach(item -> {
                            item.setSmartcheckno(smartcheckno);
                            BizObject obj = item.get("characterDefb");
                            if (obj != null) {
                                obj.setEntityStatus(EntityStatus.Update);
                            }
                            List<BizObject> fundPaymentSubWithholdingRelationList = item.get("FundPaymentSubWithholdingRelation");
                            if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelationList)) {
                                EntityTool.setUpdateStatus(fundPaymentSubWithholdingRelationList);
                            }
                            try {
                                Date pubts = item.getPubts();
                                item.set("pubts", DateUtils.strToDateSql(DateUtils.dateFormat(pubts, DateUtils.DATE_TIME_PATTERN)));
                                Date lastInterestSettlementEndDate = item.getLastInterestSettlementEndDate();
                                if (ValueUtils.isNotEmptyObj(lastInterestSettlementEndDate)) {
                                    item.set("lastInterestSettlementEndDate", DateUtils.strToDateSql(DateUtils.dateFormat(lastInterestSettlementEndDate, DateUtils.DATE_TIME_PATTERN)));
                                }
                                Date currentInterestSettlementStartDate = item.getCurrentInterestSettlementStartDate();
                                if (ValueUtils.isNotEmptyObj(currentInterestSettlementStartDate)) {
                                    item.set("currentInterestSettlementStartDate", DateUtils.strToDateSql(DateUtils.dateFormat(currentInterestSettlementStartDate, DateUtils.DATE_TIME_PATTERN)));
                                }
                                Date currentInterestSettlementEndDate = item.getCurrentInterestSettlementEndDate();
                                if (ValueUtils.isNotEmptyObj(currentInterestSettlementEndDate)) {
                                    item.set("currentInterestSettlementEndDate", DateUtils.strToDateSql(DateUtils.dateFormat(currentInterestSettlementEndDate, DateUtils.DATE_TIME_PATTERN)));
                                }
                            } catch (ParseException e) {
                                log.error("date convert Timestamp fail! errorMsg={}", e.getMessage());
                            }
                        });
                        updateDataList.addAll(fundPaymentBs);
                    }
                }
                if (ValueUtils.isNotEmpty(fundPaymentFromClaimIdBillMap)) {
                    String billClaimId = fundPaymentB.getBillClaimId();
                    if (ValueUtils.isNotEmptyObj(billClaimId)) {
                        List<FundPayment_b> fundPaymentBs = fundPaymentFromClaimIdBillMap.get(billClaimId);
                        fundPaymentBs.forEach(item -> {
                            item.setSmartcheckno(smartcheckno);
                            BizObject obj = item.get("characterDefb");
                            if (ValueUtils.isNotEmptyObj(obj)) {
                                obj.setEntityStatus(EntityStatus.Update);
                            }
                            List<BizObject> fundPaymentSubWithholdingRelationList = item.get("FundPaymentSubWithholdingRelation");
                            if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelationList)) {
                                EntityTool.setUpdateStatus(fundPaymentSubWithholdingRelationList);
                            }
                            try {
                                Date pubts = item.getPubts();
                                item.set("pubts", DateUtils.strToDateSql(DateUtils.dateFormat(pubts, DateUtils.DATE_TIME_PATTERN)));
                                Date lastInterestSettlementEndDate = item.getLastInterestSettlementEndDate();
                                if (ValueUtils.isNotEmptyObj(lastInterestSettlementEndDate)) {
                                    item.set("lastInterestSettlementEndDate", DateUtils.strToDateSql(DateUtils.dateFormat(lastInterestSettlementEndDate, DateUtils.DATE_TIME_PATTERN)));
                                }
                                Date currentInterestSettlementStartDate = item.getCurrentInterestSettlementStartDate();
                                if (ValueUtils.isNotEmptyObj(currentInterestSettlementStartDate)) {
                                    item.set("currentInterestSettlementStartDate", DateUtils.strToDateSql(DateUtils.dateFormat(currentInterestSettlementStartDate, DateUtils.DATE_TIME_PATTERN)));
                                }
                                Date currentInterestSettlementEndDate = item.getCurrentInterestSettlementEndDate();
                                if (ValueUtils.isNotEmptyObj(currentInterestSettlementEndDate)) {
                                    item.set("currentInterestSettlementEndDate", DateUtils.strToDateSql(DateUtils.dateFormat(currentInterestSettlementEndDate, DateUtils.DATE_TIME_PATTERN)));
                                }
                            } catch (ParseException e) {
                                log.error("date convert Timestamp fail! errorMsg={}", e.getMessage());
                            }
                        });
                        updateDataList.addAll(fundPaymentBs);
                    }
                }
            }
            // 更新关联资金付款单智能勾兑码
            for (FundPayment_b fundPaymentB : updateDataList) {
                FundPayment_b updateFundPaymentb = new FundPayment_b();
                updateFundPaymentb.setId(fundPaymentB.getId());
                updateFundPaymentb.setSmartcheckno(fundPaymentB.getSmartcheckno());
                updateFundPaymentb.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(FUND_PAYMENT_B_FULLNAME, updateFundPaymentb);
            }
        }
        ctmcmpReWriteBusRpcService.batchReWriteBankRecilicationForRpc(corrDataEntityParamList);
    }


    /**
     * 构建资金收款单关联数据
     *
     * @param coll
     * @param bizObject
     * @param busid
     * @param status
     * @param billNum
     * @param isadvanceaccounts
     * @param fundCollectionFromClaimIdBillOriSumMap
     * @param fundCollectionFromBankReconciliationBillOriSumMap
     * @param corrDataEntityParamList
     * @return
     * @throws Exception
     */
    private void buildeCorrDataEntityParam4FundCollection(FundCollection_b coll, BizObject bizObject, Long busid,
                                                          EntityStatus status, String billNum, Boolean isadvanceaccounts,
                                                          Map<String, BigDecimal> fundCollectionFromClaimIdBillOriSumMap,
                                                          Map<String, BigDecimal> fundCollectionFromBankReconciliationBillOriSumMap,
                                                          List<CorrDataEntityParam> corrDataEntityParamList, Map<String, String> bankReconciliationIdMap
    ) throws Exception {
        boolean relationFlow = coll.getBillClaimId() != null || coll.getBankReconciliationId() != null;
        if (!relationFlow) {
            return;
        }
        //billType = "0";// 枚举一致
        CorrDataEntityParam corrData = new CorrDataEntityParam();
        corrData.setVouchdate(bizObject.get("vouchdate"));
        corrData.setCode(bizObject.get("code"));
        corrData.setStatus(status.name());
        corrData.setMainid(busid);
        corrData.setGenerate(true);
        corrData.setAccentity(bizObject.get(IBussinessConstant.ACCENTITY));
        //CZFW-368598,自动生单自动提交问题修复；老自动生单
        if (bizObject.get("isAuto") != null && bizObject.getBoolean("isAuto")) {
            //自动提交的不进待确认
            corrData.setAuto(false);
            HashMap<String, Object> extendMap = new HashMap<>();
            extendMap.put("isAutoSubimit", true);
            corrData.setExtendFields(extendMap);
        } else {
            corrData.setAuto(false);
        }
        corrData.setBillNum(billNum);
        corrData.setMainid(Long.valueOf(coll.getMainid()));
        corrData.setDept(coll.getDept());
        corrData.setProject(coll.getProject());
        corrData.setBusid(Long.valueOf(coll.getId().toString()));
        corrData.setBillType("17");
        corrData.setIsadvanceaccounts(isadvanceaccounts);
        if (coll.getBillClaimId() != null) {
            String smartcheckno;
            corrData.setOriSum(fundCollectionFromClaimIdBillOriSumMap.get(coll.getBillClaimId()));
            Long bid = Long.parseLong(coll.getBillClaimId());
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (EntityStatus.Insert.equals(status)) {
                String pubts = coll.getSourceMainPubts() + "";
                Date date1 = coll.getSourceMainPubts();
                pubts = sf.format(date1);
                List<BillClaim> listb;
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(bid));
                querySchema.addCondition(group);
                listb = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
                if (CollectionUtils.isEmpty(listb)) {
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("refbill").eq(bid.toString()));
                    querySchema1.addCondition(group1);
                    listb = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema1, null);
                }
                if (CollectionUtils.isNotEmpty(listb)) {
                    BillClaim billClaim1 = listb.get(0);
                    Date date = billClaim1.getPubts();
                    String pus = sf.format(date);
                    // 1130bug修复，并发生单
                    if (!pubts.equals(pus) || AssociationStatus.Associated.getValue() == billClaim1.getAssociationstatus()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                    //财资统一对账码逻辑调整，原对账单上存在，则沿用，没有则重新生成
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaim1.getId()));
                    querySchema1.addCondition(group1);
                    List<BillClaimItem> items = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema1, null);
                    BillClaimItem claimItem = items.get(0);
                    if (claimItem != null) {
                        // 获取关联银行对账单
                        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, claimItem.getBankbill());
                        if (bankReconciliation != null && StringUtils.isNotBlank(bankReconciliation.getSmartcheckno())) {
                            smartcheckno = bankReconciliation.getSmartcheckno();
                        } else {
                            smartcheckno = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
                        }
                        FundCollection_b fundCollection_b = new FundCollection_b();
                        fundCollection_b.setId(coll.getId());
                        fundCollection_b.setSmartcheckno(smartcheckno);
                        fundCollection_b.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(FUND_COLLECTION_B_FULLNAME, fundCollection_b);
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
                corrData.setBankReconciliationPubts(pubts);
            }
            corrData.setBillClaimItemId(bid);
            corrDataEntityParamList.add(corrData);
        } else if (coll.getBankReconciliationId() != null) {
            corrData.setOriSum(fundCollectionFromBankReconciliationBillOriSumMap.get(coll.getBankReconciliationId()));
            Long bid = Long.parseLong(coll.getBankReconciliationId());
            FundCollection_b fundCollection_b = new FundCollection_b();
            fundCollection_b.setId(coll.getId());
            fundCollection_b.setSmartcheckno(bankReconciliationIdMap.get(coll.getBankReconciliationId()));
            fundCollection_b.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FUND_COLLECTION_B_FULLNAME, fundCollection_b);
            corrData.setBankReconciliationId(bid);
            corrDataEntityParamList.add(corrData);
        }
    }

    /**
     * 处理资金付款关联关系
     *
     * @param paymentB
     * @param bizObject
     * @param busid
     * @param status
     * @param billNum
     * @param isadvanceaccounts
     * @param fundPaymentFromClaimIdBillOriSumMap
     * @param fundPaymentFromBankReconciliationBillOriSumMap
     * @param corrDataEntityParamList
     * @throws Exception
     */
    private void buildeCorrDataEntityParam4FundPayment(FundPayment_b paymentB, BizObject bizObject, Long busid,
                                                       EntityStatus status, String billNum, Boolean isadvanceaccounts,
                                                       Map<String, BigDecimal> fundPaymentFromClaimIdBillOriSumMap,
                                                       Map<String, BigDecimal> fundPaymentFromBankReconciliationBillOriSumMap,
                                                       List<CorrDataEntityParam> corrDataEntityParamList,
                                                       Map<String, String> bankReconciliationIdMap) throws Exception {
        // 智能对账,关联数据添加勾兑码
//      String smartcheckno = UUID.randomUUID().toString().replace("-", "");
        //调用资金结算财资统一对账码接口生成
//      String smartcheckno = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
        CorrDataEntityParam corrData = new CorrDataEntityParam();
        Long bid;
        boolean relationFlow = paymentB.getBillClaimId() != null || paymentB.getBankReconciliationId() != null;
        if (!relationFlow) {
            return;
        }
        corrData.setVouchdate(bizObject.get("vouchdate"));
        corrData.setStatus(status.name());
        corrData.setCode(bizObject.get("code"));
        corrData.setMainid(busid);
        corrData.setGenerate(true);
        corrData.setAccentity(bizObject.get(IBussinessConstant.ACCENTITY));
        //CZFW-368598,自动生单自动提交问题修复；老自动生单
        if (bizObject.get("isAuto") != null && bizObject.getBoolean("isAuto")) {
            //自动提交不进待确认
            corrData.setAuto(false);
            HashMap<String, Object> extendMap = new HashMap<>();
            extendMap.put("isAutoSubimit", true);
            corrData.setExtendFields(extendMap);
        } else {
            corrData.setAuto(false);
        }
        corrData.setBillNum(billNum);
        corrData.setDept(paymentB.getDept());
        corrData.setProject(paymentB.getProject());
        corrData.setBusid(Long.valueOf(paymentB.getId().toString()));
        corrData.setBillType("18");
        corrData.setIsadvanceaccounts(isadvanceaccounts);
        // 到账认领单
        if (paymentB.getBillClaimId() != null) {
            corrData.setOriSum(fundPaymentFromClaimIdBillOriSumMap.get(paymentB.getBillClaimId()));
            bid = Long.parseLong(paymentB.getBillClaimId());
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (status.equals(EntityStatus.Insert)) {
                String smartcheckno;
                String pubts = paymentB.getSourceMainPubts() + "";
                Date date1 = paymentB.getSourceMainPubts();
                pubts = sf.format(date1);
                List<BillClaim> listb;
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(bid));
                querySchema.addCondition(group);
                listb = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
                if (CollectionUtils.isEmpty(listb)) {
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("refbill").eq(bid.toString()));
                    querySchema1.addCondition(group1);
                    listb = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema1, null);
                }
                if (CollectionUtils.isNotEmpty(listb)) {
                    BillClaim billClaim1 = listb.get(0);
                    Date date = billClaim1.getPubts();
                    String pus = sf.format(date);
                    // 1130bug修复，并发生单
                    if (!pubts.equals(pus) || AssociationStatus.Associated.getValue() == billClaim1.getAssociationstatus()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                    //财资统一对账码逻辑调整，原对账单上存在，则沿用，没有则重新生成
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaim1.getId()));
                    querySchema1.addCondition(group1);
                    List<BillClaimItem> items = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema1, null);
                    BillClaimItem claimItem = items.get(0);
                    if (claimItem != null) {
                        // 获取关联银行对账单
                        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, claimItem.getBankbill());
                        //财资统一对账码调整
                        if (bankReconciliation != null && StringUtils.isNotBlank(bankReconciliation.getSmartcheckno())) {
                            smartcheckno = bankReconciliation.getSmartcheckno();
                        } else {
                            smartcheckno = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
                        }
                        FundPayment_b fundPayment_b = new FundPayment_b();
                        fundPayment_b.setId(paymentB.getId());
                        fundPayment_b.setSmartcheckno(smartcheckno);
                        fundPayment_b.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(FUND_PAYMENT_B_FULLNAME, fundPayment_b);
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
                corrData.setBankReconciliationPubts(pubts);
            }
            corrData.setBillClaimItemId(bid);
            corrDataEntityParamList.add(corrData);
        } else if (paymentB.getBankReconciliationId() != null) {
            corrData.setOriSum(fundPaymentFromBankReconciliationBillOriSumMap.get(paymentB.getBankReconciliationId()));
            // 银行对账单
            FundPayment_b fundPayment_b = new FundPayment_b();
            fundPayment_b.setId(paymentB.getId());
            fundPayment_b.setSmartcheckno(bankReconciliationIdMap.get(paymentB.getBankReconciliationId()));
            fundPayment_b.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FUND_PAYMENT_B_FULLNAME, fundPayment_b);
            bid = Long.parseLong(paymentB.getBankReconciliationId());
            corrData.setBankReconciliationId(bid);
            corrDataEntityParamList.add(corrData);
        }
    }


}
