package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.constant.IStwbConstant;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.SystemIntegrationParamsEnum;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanService;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.intelligentapproval.CmpIntelligentAudit;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.drft.api.openapi.ICtmDrftEndorePaybillRpcService;
import com.yonyoucloud.fi.drft.post.vo.base.BaseResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资金收付款单保存后规则
 *
 * @author mal
 * @version 1.0
 * @since 2022-02-15 16:38
 */
@Slf4j
@Component
public class AfterSaveFundBillRule extends AbstractCommonRule {

    public static final String FUND_PAYMENT_B_FULLNAME = "cmp.fundpayment.FundPayment_b";
    public static final String FUND_COLLECTION_B_FULLNAME = "cmp.fundcollection.FundCollection_b";

    @Qualifier("stwbPaymentBillServiceImpl")
    @Autowired
    private StwbBillService stwbPaymentBillService;

    @Qualifier("stwbCollectionBillServiceImpl")
    @Autowired
    private StwbBillService stwbCollectionBillService;

    @Resource
    private IFundCommonService fundCommonService;
    @Resource
    private CmCommonService commonService;

    @Resource
    private ICtmDrftEndorePaybillRpcService ctmDrftEndorePaybillRpcService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    FundBillAdaptationFundPlanService fundBillAdaptationFundPlanService;

    @Autowired
    private CmpIntelligentAudit cmpIntelligentAudit;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        String billnum = billContext.getBillnum();
        if (bills != null && bills.size() > 0) {
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101195"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801FC","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
        }

        assert bills != null;
        List<BizObject> releaseBillMap = new ArrayList<>();
        List<BizObject> useBillMap = new ArrayList<>();
        Integer billDirection = null;
        for (BizObject bizObject : bills) {
            log.error("AfterSaveFundBillRule, id = {}, code = {}", bizObject.getId(), bizObject.get("code"));
            if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
                List<BizObject> currentBillList = new ArrayList<>();
                boolean settleflagBool = bizObject.get("settleflag") == null || bizObject.getBoolean("settleflag");
                BizObject bizObjectFundPlan = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizObject.getId());
                bizObject.set(ICmpConstant.CODE, bizObjectFundPlan.getString(ICmpConstant.CODE));
                // 保存前调用资金结算接口进行数据校验，注：除来源于第三方平台的单据，来源于第三方平台的单据在提交时校验
                if (EventSource.ThreePartyReconciliation.getValue() != bizObject.getShort(ICmpConstant.SRC_ITEM)) {
                    if (settleflagBool){
                        List<FundCollection_b> fundCollectionBNewList = new ArrayList<>();
                        List<FundCollection_b> fundCollectionBs = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
                        for (FundCollection_b fundCollectionB : fundCollectionBs) {
                            if (!EntityStatus.Delete.equals(fundCollectionB.getEntityStatus())){
                                fundCollectionBNewList.add(fundCollectionB);
                            }
                        }
                        BizObject bizObjectNew = bizObject.clone();
                        bizObjectNew.set("FundCollection_b", fundCollectionBNewList);
                        currentBillList.add(bizObjectNew);
                        List<FundCollection_b> needCheckList = fundCollectionBNewList.stream()
                                .filter(sub->(!sub.getFundSettlestatus().equals(FundSettleStatus.SettlementSupplement)&&!sub.getFundSettlestatus().equals(FundSettleStatus.Refund)))
                                .collect(Collectors.toList());
                        if(settleflagBool && CollectionUtils.isNotEmpty(needCheckList)) {
                            stwbCollectionBillService.pushBill(currentBillList, true);// 推送资金结算
                        }
                    }
                }

                List<Map<String, Object>> fundCollectionBChildList = bizObject.get("FundCollection_b");
                for (Map<String, Object> iter : fundCollectionBChildList) {
                    if ("Insert".equals(iter.get("_status").toString())) {
                        if (ValueUtils.isNotEmptyObj(iter.get("settlemode"))) {
                            // 占用
                            Integer serviceAttrPage = commonService.getServiceAttr(Long.parseLong(iter.get("settlemode").toString()));
                            if (IStwbConstant.SERVICEATTR_DIRT == serviceAttrPage && ValueUtils.isNotEmptyObj(iter.get("noteno"))) {
                                if (BigDecimal.ZERO.compareTo(new BigDecimal(iter.get(ICmpConstant.ORISUM).toString())) <0){
                                    useBillMap.add(new BizObject(iter));
                                }
                            }
                        }
                    } else if ("Delete".equals(iter.get("_status").toString())) {
                        if (ValueUtils.isNotEmptyObj(iter.get("settlemode"))) {
                            // 释放
                            Integer serviceAttrPage = commonService.getServiceAttr(Long.parseLong(iter.get("settlemode").toString()));
                            if (IStwbConstant.SERVICEATTR_DIRT == serviceAttrPage && ValueUtils.isNotEmptyObj(iter.get("noteno"))) {
                                if (BigDecimal.ZERO.compareTo(new BigDecimal(iter.get(ICmpConstant.ORISUM).toString())) <0){
                                    releaseBillMap.add(new BizObject(iter));
                                }
                            }
                        }
                    }
                }
                billDirection = 1;
            }
            if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                // 审批中单据，编辑保存，先释放，再占用预算；
                FundPayment oldBizObject = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizObject.getId());
                if (oldBizObject != null) {
                    bizObject.set(ICmpConstant.CODE, oldBizObject.getString(ICmpConstant.CODE));
                    preBudget(oldBizObject,oldBizObject.FundPayment_b(),IBillNumConstant.FUND_PAYMENT);
                }
                List<Map<String, Object>> fundPaymentBChildList = bizObject.get("FundPayment_b");
                for (Map<String, Object> iter : fundPaymentBChildList) {
                    if ("Insert".equals(iter.get("_status").toString())) {
                        if (ValueUtils.isNotEmptyObj(iter.get("settlemode"))) {
                            // 占用
                            Integer serviceAttrPage = commonService.getServiceAttr(Long.parseLong(iter.get("settlemode").toString()));
                            if (IStwbConstant.SERVICEATTR_DIRT == serviceAttrPage && ValueUtils.isNotEmptyObj(iter.get("noteno"))) {
//                                if (BigDecimal.ZERO.compareTo(new BigDecimal(iter.get(ICmpConstant.ORISUM).toString())) <0){
                                    useBillMap.add(new BizObject(iter));
//                                }
                            }
                        }
                    } else if ("Delete".equals(iter.get("_status").toString())) {
                        if (ValueUtils.isNotEmptyObj(iter.get("settlemode"))) {
                            // 释放
                            Integer serviceAttrPage = commonService.getServiceAttr(Long.parseLong(iter.get("settlemode").toString()));
                            if (IStwbConstant.SERVICEATTR_DIRT == serviceAttrPage && ValueUtils.isNotEmptyObj(iter.get("noteno"))) {
//                                if (BigDecimal.ZERO.compareTo(new BigDecimal(iter.get(ICmpConstant.ORISUM).toString())) <0){
                                    releaseBillMap.add(new BizObject(iter));
//                                }
                            }
                        }
                    }
                }
                billDirection = 2;
            }
            // 资金收付款单单据保存或删除行时，票据号的占用与释放
            fundBillDeleteLineOrInsertLineForNoteUseOrRelease(releaseBillMap, useBillMap, billDirection, bizObject);
            // 更新预提单的关联关系字段
            updateAccrualsWithholdingWithRelatedinterest(billnum, bizObject);
            if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                // 保存前调用资金结算接口进行数据校验，注：除来源于第三方平台的单据，来源于第三方平台的单据在提交时校验
                if (EventSource.ThreePartyReconciliation.getValue() != bizObject.getShort(ICmpConstant.SRC_ITEM)
                        && !EntityStatus.Delete.equals(bizObject.getEntityStatus())) {
                    List<BizObject> currentBillList = new ArrayList<>();
                    boolean settleflagBool = bizObject.get("settleflag") == null || bizObject.getBoolean("settleflag");
                    if (settleflagBool){
                        List<FundPayment_b> fundPaymentBNewList = new ArrayList<>();
                        List<FundPayment_b> fundPaymentBs = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
                        for (FundPayment_b fundPaymentB : fundPaymentBs) {
                            if (!EntityStatus.Delete.equals(fundPaymentB.getEntityStatus())){
                                fundPaymentBNewList.add(fundPaymentB);
                            }
                        }
                        BizObject bizObjectNew = bizObject.clone();
                        bizObjectNew.set("FundPayment_b", fundPaymentBNewList);
                        currentBillList.add(bizObjectNew);
                        List<FundPayment_b> needCheckList = fundPaymentBNewList.stream()
                                .filter(sub->(!sub.getFundSettlestatus().equals(FundSettleStatus.SettlementSupplement)&&!sub.getFundSettlestatus().equals(FundSettleStatus.Refund)))
                                .filter(sub->sub.getOriSum().compareTo(BigDecimal.ZERO)>0)
                                .collect(Collectors.toList());
                        if(settleflagBool && CollectionUtils.isNotEmpty(needCheckList)) {
                            bizObjectNew.set("FundPayment_b", needCheckList);
                            stwbPaymentBillService.pushBill(currentBillList, true);// 推送资金结算
                        }
                        bizObjectNew.set("FundPayment_b", fundPaymentBNewList);
                    }
                }
            }

            // 预占资金计划
            preFundPlan(bizObject, billnum);

            String formId = IBillNumConstant.FUND_PAYMENT.equals(billnum) ? ICmpConstant.CM_CMP_FUND_PAYMENT : ICmpConstant.CM_CMP_FUND_COLLECTION;
            cmpIntelligentAudit.auditStart(bizObject, billnum, formId, BusinessPart.save.getValue());
        }
        return new RuleExecuteResult();
    }

    private void preFundPlan(BizObject bizObject, String billnum) throws Exception {
        boolean isOpenBill = Short.parseShort(bizObject.get("verifystate").toString()) == VerifyState.INIT_NEW_OPEN.getValue()
                || Short.parseShort(bizObject.get("verifystate").toString()) == VerifyState.REJECTED_TO_MAKEBILL.getValue();
        if (isOpenBill) {
            boolean checkFundPlanIsEnabled = false;
            if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundPayment.getValue());
            } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
                checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundCollection.getValue());
            }
            log.error("AfterSaveFundBillRule#preFundPlan(), code={}, id={}, billNum={}",
                    bizObject.getString(ICmpConstant.CODE), bizObject.getId(), billnum);
            List<BizObject> preEmployFundBillForFundPlanProjectList = new ArrayList<>();
            List<BizObject> preReleaseFundBillForFundPlanProjectList = new ArrayList<>();
            if (checkFundPlanIsEnabled) {
                List<BizObject> fundSubBList = fundBillAdaptationFundPlanService
                        .fundBillPreEmployOrReleaseFundPlanAfterSaveForInsert(
                                billnum,
                                bizObject,
                                preEmployFundBillForFundPlanProjectList,
                                preReleaseFundBillForFundPlanProjectList
                        );
                for (BizObject fundSub : fundSubBList) {
                    //String tableName;
                    if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                        /*tableName = "cmp_fundpayment_b";*/
                        FundPayment_b fundPaymentB = new FundPayment_b();
                        fundPaymentB.setId(fundSub.getId());
                        fundPaymentB.setIsToPushCspl(fundSub.getInteger("isToPushCspl"));
                        fundPaymentB.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(FUND_PAYMENT_B_FULLNAME,fundPaymentB);
                    } else {
                        /*tableName = "cmp_fundcollection_b";*/
                        FundCollection_b fundCollectionB = new FundCollection_b();
                        fundCollectionB.setId(fundSub.getId());
                        fundCollectionB.setIsToPushCspl(fundSub.getInteger("isToPushCspl"));
                        fundCollectionB.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(FUND_COLLECTION_B_FULLNAME, fundCollectionB);
                    }
                    /*Map<String, Object> params = new HashMap<>();
                    params.put("ytenantId", InvocationInfoProxy.getTenantid());
                    params.put("id", fundSub.getId());
                    params.put("tableName", tableName);
                    params.put("isToPushCspl", fundSub.getInteger("isToPushCspl"));
                    log.error("com.yonyoucloud.fi.cmp.fundcommon.business.AfterSaveFundBillRule.preFundPlan#InsertAndDelete, params={}", params);
                    SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateFundBillSubById", params);*/
                }
            } else {
                fundBillAdaptationFundPlanService.fundPlanProjectPreEmployOrReleaseNotControl(billnum, bizObject, preReleaseFundBillForFundPlanProjectList);
            }
            if (CollectionUtils.isNotEmpty(preReleaseFundBillForFundPlanProjectList)) {
                fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(billnum, bizObject, preReleaseFundBillForFundPlanProjectList, null, null, "pre");
            }

            if (CollectionUtils.isNotEmpty(preEmployFundBillForFundPlanProjectList)) {
                fundBillAdaptationFundPlanService.fundBillEmployFundPlan(billnum, bizObject, preEmployFundBillForFundPlanProjectList, "pre");
            }
        }
    }


    private void updateAccrualsWithholdingWithRelatedinterest(String billnum, BizObject bizObject) throws Exception {
        Set<Long> insertIds = new HashSet<>();
        if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            List<BizObject> fundCollectionBChildList = bizObject.get(ICmpConstant.FUND_COLLECTION_B);
            for (BizObject iter : fundCollectionBChildList) {
                Object quickType = iter.get(ICmpConstant.QUICK_TYPE);
                // 判断款项类型是否为利息
                if (fundCommonService.isInterestWithQuickType(quickType)) {
                    // 此为孙表的数据添加和删除
                    List<BizObject> fundCollectionSubWithholdingRelation = iter.get(ICmpConstant.FUND_COLLECTION_SUB_WITHHOLDING_RELATION);
                    if (CollectionUtils.isNotEmpty(fundCollectionSubWithholdingRelation)) {
                        for (BizObject childBizObj : fundCollectionSubWithholdingRelation) {
                            if (EntityStatus.Insert.equals(iter.getEntityStatus()) || EntityStatus.Insert.equals(childBizObj.getEntityStatus())) {
                                insertIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                            }
                        }
                    }

                }
            }
        } else if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            List<BizObject> fundPaymentBChildList = bizObject.get(ICmpConstant.FUND_PAYMENT_B);
            for (BizObject iter : fundPaymentBChildList) {
                Object quickType = iter.get(ICmpConstant.QUICK_TYPE);
                // 判断款项类型是否为利息
                if (fundCommonService.isInterestWithQuickType(quickType)) {
                    // 此为孙表的数据添加
                    List<BizObject> fundPaymentSubWithholdingRelation = iter.get(ICmpConstant.FUND_PAYMENT_SUB_WITHHOLDING_RELATION);
                    if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                        for (BizObject childBizObj : fundPaymentSubWithholdingRelation) {
                            //新增时，孙表的status被平台修改为update，添加子表状态判断，若子表数据是insert,则孙表数据一定是insert
                            if (EntityStatus.Insert.equals(iter.getEntityStatus()) || EntityStatus.Insert.equals(childBizObj.getEntityStatus())) {
                                insertIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                            }
                        }
                    }

                }
            }
        }
        // 孙表添加时，更新预提单记录表关联状态字段为已关联
        if (CollectionUtils.isNotEmpty(insertIds)) {
            List<Map<String, Object>> accrualsWithholdingMap = MetaDaoHelper.queryByIds(AccrualsWithholding.ENTITY_NAME,
                    "*", insertIds.toArray(new Long[0]));
            List<AccrualsWithholding> accrualsWithholdingList = new ArrayList<>();
            for (Map<String, Object> map : accrualsWithholdingMap) {
                AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
                accrualsWithholding.init(map);
                // 更新结息单的【关联结息单】字段状态为未关联
                accrualsWithholding.setRelatedinterest(Relatedinterest.relatedAssociated.getValue());
                accrualsWithholding.setSrcbillmainid(Long.parseLong(bizObject.getId().toString()));
                accrualsWithholding.setSrcbillnum(billnum);
                accrualsWithholding.setSrcbilltype("Voucher");
                accrualsWithholdingList.add(accrualsWithholding);
            }
            EntityTool.setUpdateStatus(accrualsWithholdingList);
            MetaDaoHelper.update(AccrualsWithholding.ENTITY_NAME, accrualsWithholdingList);
        }
    }

    private void fundBillDeleteLineOrInsertLineForNoteUseOrRelease(List<BizObject> releaseBillMap, List<BizObject> useBillMap, Integer billDirection, BizObject bizObject) throws Exception {
        if (releaseBillMap.size() > 0) {
            List<Map<String, Object>> noteMaps = new ArrayList<>();
            for (BizObject subBiz : releaseBillMap) {
                fundCommonService.deleteNoteList(noteMaps, billDirection, bizObject, subBiz);
            }
            if (ValueUtils.isNotEmptyObj(noteMaps)) {
                try {
                    BaseResultVO jsonObject = ctmDrftEndorePaybillRpcService.settleReleaseBillNew(noteMaps);
                    log.error("fund bill note release success! code={}, id={}, inputParameter={}, outputParameter={}",
                            bizObject.get("code"), bizObject.getId(), CtmJSONObject.toJSONString(noteMaps), CtmJSONObject.toJSONString(jsonObject));
                } catch (Exception e) {
                    log.error("fund bill note release fail! code={}, id={}, inputParameter={}, errorMsg={}",
                            bizObject.get("code"), bizObject.getId(), CtmJSONObject.toJSONString(noteMaps), e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101196"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801FB","单据明细行结算方式为票据结算，释放票据失败，请检查数据!") /* "单据明细行结算方式为票据结算，释放票据失败，请检查数据!" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801F9","：") /* "：" */ + e.getMessage());
                }
            }
        }

        // 当为票据结算时，保存时，占用票据
        if (useBillMap.size() > 0) {
            List<Map<String, Object>> noteMaps = new ArrayList<>();
            for (BizObject subBiz : useBillMap) {
                fundCommonService.deleteNoteList(noteMaps, billDirection, bizObject, subBiz);
            }
            if (ValueUtils.isNotEmptyObj(noteMaps)) {
                try {
                    BaseResultVO jsonObject = ctmDrftEndorePaybillRpcService.settleUseBillNew(noteMaps);
                    log.error("fund bill note occupied success! code={}, id={}, inputParameter={}, outputParameter={}",
                            bizObject.get("code"), bizObject.getId(), CtmJSONObject.toJSONString(noteMaps), CtmJSONObject.toJSONString(jsonObject));
                } catch (Exception e) {
                    log.error("fund bill note occupied fail! code={}, id={}, inputParameter={}, errorMsg={}",
                            bizObject.get("code"), bizObject.getId(), CtmJSONObject.toJSONString(noteMaps), e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101197"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801FA","单据明细行结算方式为票据结算，占用票据失败，请检查数据!") /* "单据明细行结算方式为票据结算，占用票据失败，请检查数据!" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801F9","：") /* "：" */ + e.getMessage());
                }
            }
        }

    }

    /**
     * 处理流程中预算相关逻辑，审批中编辑时需要先取消占用预算再进行预算占用
     * @param bizObject
     * @param billbs
     * @param billnum
     * @throws Exception
     */
    private void preBudget(BizObject bizObject, List<FundPayment_b> billbs, String billnum) throws Exception{
        // 审批流状态，非审批中，直接跳过
        if (bizObject.get("verifystate") != null && Short.valueOf(bizObject.get("verifystate").toString()) == VerifyState.SUBMITED.getValue()) {
            if (cmpBudgetManagerService.isCanStart(billnum)) {
                List<FundPayment_b> updateList = new ArrayList();
                for (FundPayment_b billb : billbs) {
                    Short budgeted = billb.getIsOccupyBudget();
                    // 已经预占仍要预占或者已经实占，直接跳过不执行了
                    if (budgeted != null && ((budgeted!=OccupyBudget.UnOccupy.getValue()))) {
                        continue;
                    }
                    // 是否委托驳回，为是的时候，无需占用预算，
                    if (billb.getEntrustReject()!=null && billb.getEntrustReject()==1) {
                        continue;
                    }
                    // 退票的不占用预算，无需占用预算，
                    if (billb.getFundSettlestatus()!=null && FundSettleStatus.Refund.getValue() == billb.getFundSettlestatus().getValue() ) {
                        continue;
                    }
                    updateList.add(billb);
                }
                // updateList为空，无需处理
                if (!updateList.isEmpty()) {
                    ResultBudget resultBudget = cmpBudgetManagerService.gcExecuteBatchSubmit(bizObject, updateList, IBillNumConstant.FUND_PAYMENT, BillAction.SUBMIT);
                    if(resultBudget.isSuccess()){
                        if (resultBudget.getIds() != null && !resultBudget.getIds().isEmpty()) {
                            updateList.stream().forEach( fundPayment_b -> {
                                if (resultBudget.getIds().contains(fundPayment_b.getId().toString())) {
                                    fundPayment_b.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                                    fundPayment_b.setEntityStatus(EntityStatus.Update);
                                }
                            });
                        } else {
                            updateList.stream().forEach( fundPayment_b -> {
                                fundPayment_b.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                                fundPayment_b.setEntityStatus(EntityStatus.Update);
                            });
                        }
                    }
                    MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, updateList);
                }
            }
        }
    }

}
