package com.yonyoucloud.fi.cmp.fundcollection.service;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.ctm.stwb.reqvo.AgentPaymentReqVO;
import com.yonyoucloud.ctm.stwb.reqvo.SettleFailChangeReqVO;
import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationCount;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatusConverter;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundCommonServiceImpl;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.transferaccount.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资金收款业务实现类
 * @author maliangn  2021-12-03
 */
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor()
public class FundCollectionServiceImpl implements FundCollectionService {

    private final ReWriteBusCorrDataService reWriteBusCorrDataService;
    @Autowired
    private FundCommonServiceImpl fundCommonService;
    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;

    @Override
    public CtmJSONObject submit(FundCollection fundCollection) throws Exception {
//        StwbBillHandUtil.acceptFundCollection(fundCollection);

        return null;
    }

    /**
     *  整单委托拒绝
     *  主要逻辑 更新子表【是否委托驳回】 更新为是
     *  结算状态更新为结算止付
     * @param jsonObject
     * @throws Exception
     */
    @Override
    public void entrustReject(CtmJSONObject jsonObject) throws Exception {
        String id = getId(jsonObject);
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(id),
                QueryCondition.name(ICmpConstant.SETTLE_STATUS).not_eq(FundSettleStatus.SettleFailed.getValue()));
        QuerySchema schema = QuerySchema.create().addSelect("*");
        schema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(FundCollection_b.ENTITY_NAME, schema);
        check(list);
        List<FundCollection_b> collectionBList = steamTransfer(list);
        long count = collectionBList.stream().filter(fundCollection -> fundCollection.getEntrustReject() == 1).count();
        if (count == collectionBList.size()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100005"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00274", "已经整单委托拒绝，不可重复拒绝") /* "已经整单委托拒绝，不可重复拒绝" */);
        }
        List<FundCollection_b> bills = collectionBList.stream().filter(fundCollection ->
                (null == fundCollection.getEntrustReject() || fundCollection.getEntrustReject() != 1)).collect(Collectors.toList());//过滤已经被委托拒绝的单子
        updateFundCollections(bills);//更新逻辑

        // CZFW-303696:关联后可以进行委托拒绝，委托拒绝后也没有取消关联对账单
        List<CtmJSONObject> cancelAssociationDataList = getCancelAssociationDataMainList(id);
        if (CollectionUtils.isNotEmpty(cancelAssociationDataList)) {
            for (CtmJSONObject ctmJSONObject : cancelAssociationDataList) {
                reWriteBusCorrDataService.resDelData(ctmJSONObject);
            }
        }

        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        //调结算工作台
        if (enableSimplify) {
            for (FundCollection_b item : bills) {
                AgentPaymentReqVO agentPaymentReqVO = new AgentPaymentReqVO();
                agentPaymentReqVO.setSettleDetailAId(item.getSettledId());
                agentPaymentReqVO.setStatementdetailstatus(Integer.valueOf(FundSettleStatus.SettleFailed.getValue()));
                fundCommonService.sendEventToSettleBenchDetail(agentPaymentReqVO);
            }
        } else {
            List<Long> settledIds = list.stream().map(item -> Long.parseLong((String) item.get("settledId"))).collect(Collectors.toList());
            SettleFailChangeReqVO settleFailChangeReqVO = new SettleFailChangeReqVO();
            settleFailChangeReqVO.setSettleDetailIdList(settledIds);
            RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).newUpdateSettlementDetailList(settleFailChangeReqVO);
        }
    }

    /**
     * 子表委托拒绝
     * @param jsonObject
     * @throws Exception
     */
    @Override
    public void entrustRejectSub(CtmJSONObject jsonObject) throws Exception {
        String id = getId(jsonObject);//获取id
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        QuerySchema schema = QuerySchema.create().addSelect("*");
        schema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(FundCollection_b.ENTITY_NAME, schema);
        check(list);
        List<FundCollection_b> fundCollection_bs = steamTransfer(list);
        FundCollection_b fundCollection_b = fundCollection_bs.get(0);
        if (fundCollection_b.getEntrustReject() == 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100005"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00274", "已经整单委托拒绝，不可重复拒绝") /* "已经整单委托拒绝，不可重复拒绝" */);
        }
        //更新子表【是否委托驳回】 更新为是 结算状态更新为结算止付
        fundCollection_b.setEntrustReject(1);
        fundCollection_b.setFundSettlestatus(FundSettleStatus.SettleFailed);
        fundCollection_b.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundCollection_b.getFundSettlestatus()));
        fundCollection_b.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollection_b);

        // CZFW-303696:关联后可以进行委托拒绝，委托拒绝后也没有取消关联对账单
        List<CtmJSONObject> cancelAssociationDataList = getCancelAssociationDataSubList(fundCollection_b);
        if (CollectionUtils.isNotEmpty(cancelAssociationDataList)) {
            for (CtmJSONObject ctmJSONObject : cancelAssociationDataList) {
                reWriteBusCorrDataService.resDelData(ctmJSONObject);
            }
        }

        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        //调结算工作台
        if (enableSimplify) {
            AgentPaymentReqVO agentPaymentReqVO = new AgentPaymentReqVO();
            agentPaymentReqVO.setSettleDetailAId(fundCollection_b.getSettledId());
            agentPaymentReqVO.setStatementdetailstatus(Integer.valueOf(FundSettleStatus.SettleFailed.getValue()));
            fundCommonService.sendEventToSettleBenchDetail(agentPaymentReqVO);
        } else {
            CtmJSONObject params = new CtmJSONObject();
            long settledId = Long.parseLong(fundCollection_b.getSettledId());
            List<Long> settledIds = new ArrayList<>();
            settledIds.add(settledId);
            params.put("settleDetailIdList", settledIds);
            SettleFailChangeReqVO paramJSONObject = new SettleFailChangeReqVO();
            paramJSONObject.setSettleDetailIdList(settledIds);
            RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).newUpdateSettlementDetailList(paramJSONObject);
        }
    }

    public void updateFundCollections(List<FundCollection_b> collectionBList) throws Exception {
        List<FundCollection_b> updateList = collectionBList.stream().peek(_b -> {
            _b.setFundSettlestatus(FundSettleStatus.SettleFailed); //结算止付
            _b.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(_b.getFundSettlestatus()));
            _b.setEntrustReject(1); //是否驳回 1是 0否
            _b.setEntityStatus(EntityStatus.Update);
        }).collect(Collectors.toList());
        MetaDaoHelper.update(FundCollection_b.ENTITY_NAME,updateList);
    }

    public List<FundCollection_b> steamTransfer(List<Map<String, Object>> list){
        return list.stream().map(map_ -> {
            FundCollection_b fundCollection_b = new FundCollection_b();
            fundCollection_b.init(map_);
            return fundCollection_b;
        }).collect(Collectors.toList());
    }

    private String getId(CtmJSONObject jsonObject){
        String id = jsonObject.getString("id");
        AssertUtil.isNotBlank(id,()->new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00273", "单据不存在，请刷新后重试！") /* "单据不存在，请刷新后重试！" */)); /*单据不存在*/
        return id;
    }

    private void check(List<Map<String, Object>> list){
        if (CollectionUtils.isEmpty(list)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100006"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00273", "单据不存在，请刷新后重试！") /* "单据不存在，请刷新后重试！" */);
        }
    }

    private List<CtmJSONObject> getCancelAssociationDataMainList(Object busid) throws Exception {
        List<CtmJSONObject> listreq = new ArrayList<CtmJSONObject>();
        FundCollection payment = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, busid);
        Short associationcount = payment.getAssociationcount();
        List<FundCollection_b> fundCollectionBList = new ArrayList<>();
        QuerySchema querySchema = QuerySchema.create().addSelect("id,billClaimId,bankReconciliationId,associationStatus,pubts");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(busid));
        querySchema.addCondition(group);
        fundCollectionBList = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isNotEmpty(fundCollectionBList)) {
            for (FundCollection_b paymentB : fundCollectionBList) {
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
                                List<BizObject> bizObjects = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema1, null);
                                if (bizObjects != null && bizObjects.size() > 0) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100007"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180831","本单据对应的银行对账单已生成实际业务单据，不允许删除") /* "本单据对应的银行对账单已生成实际业务单据，不允许删除" */);
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
        }
        return listreq;
    }

    private List<CtmJSONObject> getCancelAssociationDataSubList(FundCollection_b paymentB) throws Exception {
        List<CtmJSONObject> listreq = new ArrayList<>();
        if (paymentB.getAssociationStatus() != null && AssociationStatus.Associated.getValue() == paymentB.getAssociationStatus().shortValue()) {
            CtmJSONObject jsonReq = new CtmJSONObject();
            if (org.imeta.core.lang.StringUtils.isNotEmpty(paymentB.getBankReconciliationId())) {
                jsonReq.put("busid", Long.parseLong(paymentB.getBankReconciliationId()));
                BankReconciliation byId = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, paymentB.getBankReconciliationId(), 3);
                if (byId == null) {
                    return Collections.emptyList();
                }
                Short associationcount1 = byId.getAssociationcount();
                if (associationcount1 != null && AssociationCount.Second.getValue() == associationcount1) {
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankReconciliationId").eq(paymentB.getBankReconciliationId()), QueryCondition.name("id").not_eq(paymentB.getId()));
                    querySchema1.addCondition(group1);
                    List<BizObject> bizObjects = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema1, null);
                    if (bizObjects != null && bizObjects.size() > 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100007"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180831", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080022", "本单据对应的银行对账单已生成实际业务单据，不允许删除") /* "本单据对应的银行对账单已生成实际业务单据，不允许删除" */) /* "本单据对应的银行对账单已生成实际业务单据，不允许删除" */);
                    }
                }
            }
            if (org.imeta.core.lang.StringUtils.isNotEmpty(paymentB.getBillClaimId())) {
                jsonReq.put("claimid", Long.parseLong(paymentB.getBillClaimId()));
            }
            jsonReq.put("stwbbusid", paymentB.getId());
            listreq.add(jsonReq);
        }
        return listreq;
    }


}
