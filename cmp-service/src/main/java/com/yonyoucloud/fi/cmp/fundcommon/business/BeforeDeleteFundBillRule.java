package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.Relatedinterest;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.PushCsplStatusEnum;
import com.yonyoucloud.fi.cmp.enums.SystemIntegrationParamsEnum;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollectionSubWithholdingRelation;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanService;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPaymentSubWithholdingRelation;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.drft.api.openapi.ICtmDrftEndorePaybillRpcService;
import com.yonyoucloud.fi.drft.post.vo.base.BaseResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资金收付款单删除后规则
 *
 * @author mal
 * @version 1.0
 * @since 2022-02-15 16:38
 */
@Slf4j
@Component("beforeDeleteFundBillRule")
@RequiredArgsConstructor
public class BeforeDeleteFundBillRule extends AbstractCommonRule {

    private final IFundCommonService fundCommonService;
    private final FundBillAdaptationFundPlanService fundBillAdaptationFundPlanService;

    @Resource
    private ICtmDrftEndorePaybillRpcService ctmDrftEndorePaybillRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        String billnum = billContext.getBillnum();
        if (bills != null && bills.size() > 0) {
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100367"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180112", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540056B", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */) /* "传入的billnum为空，请检查" */);
            }
        }
        assert bills != null;
        List<Map<String, Object>> noteMaps = new ArrayList<>();
        for (BizObject bizObject : bills) {
            if (IBillNumConstant.FUND_PAYMENT.equals(billnum) || IBillNumConstant.FUND_PAYMENTLIST.equals(billnum)) {
                FundPayment currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizObject.getId(), 3);
                String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
                List<BizObject> fundPaymentList = currentBill.get(childrenFieldCheck);
                // 当为票据结算时，删除时，释放票据
                // 组装票据占用参数
                for (BizObject subBiz : fundPaymentList) {
                    fundCommonService.deleteNoteList(noteMaps, 2, currentBill, subBiz);
                }
                if (ValueUtils.isNotEmptyObj(noteMaps)) {
                    noteRelease(bizObject, noteMaps);
                }

                for (BizObject bizObjSub : fundPaymentList) {
                    // 判断款项类型是否为利息
                    List<FundPaymentSubWithholdingRelation> fundPaymentSubWithholdingRelation = bizObjSub.getBizObjects("FundPaymentSubWithholdingRelation", FundPaymentSubWithholdingRelation.class);
                    // 是否有关联了预提单
                    if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                        List<Map<String, Object>> accrualsWithholdingMap = MetaDaoHelper.queryByIds(AccrualsWithholding.ENTITY_NAME, "*",
                                fundPaymentSubWithholdingRelation.stream().map(e -> Long.parseLong(e.get(ICmpConstant.WITHHOLDING_ID).toString())).toArray(Long[]::new));
                        List<AccrualsWithholding> accrualsWithholdingList = new ArrayList<>();
                        for (Map<String, Object> map : accrualsWithholdingMap) {
                            AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
                            accrualsWithholding.init(map);
                            // 更新结息单的【关联结息单】字段状态为未关联
                            accrualsWithholding.setRelatedinterest(Relatedinterest.relatedUnAssociated.getValue());
                            accrualsWithholding.setSrcbillmainid(null);
                            accrualsWithholding.setSrcbillnum(null);
                            accrualsWithholding.setSrcbilltype(null);
                            accrualsWithholdingList.add(accrualsWithholding);
                        }
                        EntityTool.setUpdateStatus(accrualsWithholdingList);
                        MetaDaoHelper.update(AccrualsWithholding.ENTITY_NAME, accrualsWithholdingList);

                        MetaDaoHelper.delete(FundPaymentSubWithholdingRelation.ENTITY_NAME, fundPaymentSubWithholdingRelation);
                    }
                    if(ValueUtils.isNotEmptyObj(bizObjSub.get("checkId"))){
                        CheckStock addCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, bizObjSub.get("checkId"));
                        if (addCheckStock != null) {
                            //转账单查询为空说明是新增,此时需要对支票进行预占
                            addCheckStock.setOccupy((short) 0);
                            addCheckStock.setEntityStatus(EntityStatus.Update);
                            MetaDaoHelper.update(CheckStock.ENTITY_NAME, addCheckStock);
                        }
                    }
                }

                // 释放资金计划预占
                boolean checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundPayment.getValue());
                List<BizObject> preReleaseFundBillForFundPlanProjectList = new ArrayList<>();
                if (checkFundPlanIsEnabled) {
                    for (BizObject bizObjSub : fundPaymentList) {
                        if (bizObjSub.get("fundPlanProject") !=null
                                && Objects.equals(bizObjSub.getInteger("isToPushCspl"), PushCsplStatusEnum.PRE_OCCUPIED.getValue())){
                            preReleaseFundBillForFundPlanProjectList.add(bizObjSub);
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(preReleaseFundBillForFundPlanProjectList)) {
                    fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(billnum, currentBill, preReleaseFundBillForFundPlanProjectList, null, null, "pre");
                }
            }
            if (IBillNumConstant.FUND_COLLECTION.equals(billnum) || IBillNumConstant.FUND_COLLECTIONLIST.equals(billnum)) {
                FundCollection currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizObject.getId(), 3);
                String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
                List<BizObject> fundCollectionList = currentBill.get(childrenFieldCheck);
                // 当为票据结算时，删除时，释放票据
                // 组装票据占用参数
                for (BizObject subBiz : fundCollectionList) {
                    fundCommonService.deleteNoteList(noteMaps, 1, currentBill, subBiz);
                }
                if (ValueUtils.isNotEmptyObj(noteMaps)) {
                    noteRelease(currentBill, noteMaps);
                }

                // 资金收付单据删除时，更新预提单上的关联状态为未关联
                for (BizObject bizObjSub : fundCollectionList) {
                    List<FundCollectionSubWithholdingRelation> fundCollectionSubWithholdingRelation = bizObjSub.getBizObjects("FundCollectionSubWithholdingRelation", FundCollectionSubWithholdingRelation.class);
                    // 是否有关联了预提单
                    if (CollectionUtils.isNotEmpty(fundCollectionSubWithholdingRelation)) {
                        List<Map<String, Object>> accrualsWithholdingMap = MetaDaoHelper.queryByIds(AccrualsWithholding.ENTITY_NAME, "*",
                                fundCollectionSubWithholdingRelation.stream().map(e -> Long.parseLong(e.get(ICmpConstant.WITHHOLDING_ID).toString())).toArray(Long[]::new));
                        List<AccrualsWithholding> accrualsWithholdingList = new ArrayList<>();
                        for (Map<String, Object> map : accrualsWithholdingMap) {
                            AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
                            accrualsWithholding.init(map);
                            // 更新结息单的【关联结息单】字段状态为未关联
                            accrualsWithholding.setRelatedinterest(Relatedinterest.relatedUnAssociated.getValue());
                            accrualsWithholding.setSrcbillmainid(null);
                            accrualsWithholding.setSrcbillnum(null);
                            accrualsWithholding.setSrcbilltype(null);
                            accrualsWithholdingList.add(accrualsWithholding);
                        }
                        EntityTool.setUpdateStatus(accrualsWithholdingList);
                        MetaDaoHelper.update(AccrualsWithholding.ENTITY_NAME, accrualsWithholdingList);

                        MetaDaoHelper.delete(FundCollectionSubWithholdingRelation.ENTITY_NAME, fundCollectionSubWithholdingRelation);
                    }
                    if(ValueUtils.isNotEmptyObj(bizObjSub.get("checkId"))){
                        CheckStock addCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, bizObjSub.get("checkId"));
                        if (addCheckStock != null) {
                            //转账单查询为空说明是新增,此时需要对支票进行预占
                            addCheckStock.setOccupy((short) 0);
                            addCheckStock.setEntityStatus(EntityStatus.Update);
                            MetaDaoHelper.update(CheckStock.ENTITY_NAME, addCheckStock);
                        }
                    }

                }

                //20231030需求 协同生成的资金收款单，清空协同发起方资金付的关联信息
                boolean isCooperateFundCollection = ValueUtils.isNotEmptyObj(currentBill.getShort(ICmpConstant.BILLTYPE))
                        && EventType.cooperate_fund_collection.getValue() == currentBill.getShort(ICmpConstant.BILLTYPE);
                if(isCooperateFundCollection){
                    QuerySchema querySchema = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(bizObject.getId()));
                    querySchema.addCondition(group);
                    List<FundCollection_b> list = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema, null);
                    for (FundCollection_b collection_b : list){
                        FundPayment_b fundPayment_b = MetaDaoHelper.findById(FundPayment_b.ENTITY_NAME,collection_b.getSrcbillid());
                        if(fundPayment_b != null){
                            fundPayment_b.setSynergybillid(null);
                            fundPayment_b.setSynergybillno(null);
                            fundPayment_b.setSynergybillitemno(null);
                            fundPayment_b.setIssynergy(false);
                            EntityTool.setUpdateStatus(fundPayment_b);
                            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME,fundPayment_b);
                        }
                    }
                }

                // 释放资金计划预占
                List<BizObject> preReleaseFundBillForFundPlanProjectList = new ArrayList<>();
                for (BizObject bizObjSub : fundCollectionList) {
                    if (bizObjSub.get("fundPlanProject") != null
                            && Objects.equals(bizObjSub.getInteger("isToPushCspl"), PushCsplStatusEnum.PRE_OCCUPIED.getValue())) {
                        preReleaseFundBillForFundPlanProjectList.add(bizObjSub);
                    }
                }
                if (CollectionUtils.isNotEmpty(preReleaseFundBillForFundPlanProjectList)) {
                    fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(billnum, currentBill, preReleaseFundBillForFundPlanProjectList, null, null, "pre");
                }
            }
        }
        return new RuleExecuteResult();
    }


    private void noteRelease(BizObject bizObject, List<Map<String, Object>> noteMaps) {
        try {
            BaseResultVO jsonObject = ctmDrftEndorePaybillRpcService.settleReleaseBillNew(noteMaps);
            log.error("fund bill note release success! code={}, id={}, inputParameter={}, outputParameter={}",
                    bizObject.get("code"), bizObject.getId(), CtmJSONObject.toJSONString(noteMaps), CtmJSONObject.toJSONString(jsonObject));
        } catch (Exception e) {
            log.error("fund bill note release fail! code={}, id={}, inputParameter={}, errorMsg={}",
                    bizObject.get("code"), bizObject.getId(), CtmJSONObject.toJSONString(noteMaps), e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100368"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180110", "单据明细行结算方式为票据结算，释放票据失败，请检查数据!") /* "单据明细行结算方式为票据结算，释放票据失败，请检查数据!" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180111", "：") /* "：" */ + e.getMessage());
        }
    }

}
