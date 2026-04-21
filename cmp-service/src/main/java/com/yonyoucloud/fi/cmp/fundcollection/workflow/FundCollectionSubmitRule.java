package com.yonyoucloud.fi.cmp.fundcollection.workflow;

import com.security.cert.cmp.CMPException;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.lock.YmsScopeLockManager;
import com.yonyou.ucf.mdd.common.constant.MddConstants;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.SystemIntegrationParamsEnum;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.intelligentapproval.CmpIntelligentAudit;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.BillCopyCheckService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 资金收款单提交规则
 *
 * @version 1.0
 * @since 2021-12-13 10:16
 */
@Slf4j
@Component
public class FundCollectionSubmitRule extends AbstractCommonRule {
    @Qualifier("stwbCollectionBillServiceImpl")
    @Autowired
    private StwbBillService stwbCollectionBillService;

    @Resource
    private CmCommonService<Object> commonService;

    @Resource
    private IFundCommonService fundCommonService;

    @Autowired
    BillCopyCheckService billCopyCheckService;

    @Resource
    private FundBillAdaptationFundPlanService fundBillAdaptationFundPlanService;

    @Autowired
    private CmpIntelligentAudit cmpIntelligentAudit;

    @Autowired
    @Qualifier("ymsGlobalScopeLockManager")
    protected YmsScopeLockManager ymsScopeLockManager;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        Map<String,String> billContextParameters = new HashMap<>();
        billContextParameters.put("extend",billContext.getParameter("extend"));
        billContextParameters.put("serviceCode",billContext.getParameter("ficmp0024"));
        billContextParameters.put("businessStepCode",billContext.getParameter("businessStepCode"));
        billContextParameters.put("cmdname",billContext.getParameter("cmdname"));
        billContextParameters.put("businessActName",billContext.getParameter("businessActName"));
        billContextParameters.put("sbillno",billContext.getParameter("sbillno"));
        billContextParameters.put("terminalType",billContext.getParameter("terminalType"));
        billContext.setParameters(billContextParameters);
        AppContext.setThreadContext(MddConstants.PARAM_SERVICE_CODE, "ficmp0024");
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizObject.getId(), 2);
            if (fundCollection == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100698"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180383","单据不存在 id:") /* "单据不存在 id:" */ + bizObject.getId());
            }
            short verifystate = Short.parseShort(bizObject.get("verifystate").toString());
            if (verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100308"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00033", "单据已提交，不能进行重复提交!") /* "单据已提交，不能进行重复提交!" */);
            }
            if (verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100758"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00140", "单据已终止流程，不能进行提交！") /* "单据已终止流程，不能进行提交！" */);
            }
            if (StringUtils.isEmpty(fundCollection.getTradetype())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100518"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180437","交易类型不能为空！") /* "交易类型不能为空！" */);
            }
            if (!ymsScopeLockManager.tryTxScopeLock(fundCollection.getId().toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100544"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180438","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
            }
            Map<String, Integer> checkCacheMap = new HashMap<>(256);
            verifyFundCollectionSubBank(fundCollection, checkCacheMap, fundCollection.getAccentity());
            List<FundCollection_b> fundCollection_bs=fundCollection.FundCollection_b();
            for (FundCollection_b b : fundCollection_bs){
                b.set("transNumber", null);
            }
            EntityTool.setUpdateStatus(fundCollection_bs);
            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundCollection_bs);
            bizObject.set("FundPayment_b",fundCollection_bs);
            checkCacheMap.clear();
            /*// 提交前来源于第三方的单据调用资金结算接口进行数据校验
            if (EventSource.ThreePartyReconciliation.getValue() == bizObject.getShort(ICmpConstant.SRC_ITEM) ||
                    bizObject.getShort(ICmpConstant.BILLTYPE) == EventType.CashMark.getValue()) {*/
            boolean settleFlagBool = fundCollection.getSettleflag() == null || fundCollection.getSettleflag() == 1;
            List<FundCollection_b> fundCollectionB_bList = fundCollection.get("FundCollection_b");
            List<FundCollection_b> needCheckList = fundCollectionB_bList.stream()
                    .filter(sub->(!sub.getFundSettlestatus().equals(FundSettleStatus.SettlementSupplement)&&!sub.getFundSettlestatus().equals(FundSettleStatus.Refund)))
                    .collect(Collectors.toList());
            if(settleFlagBool && CollectionUtils.isNotEmpty(needCheckList)) {
                List<BizObject> currentBillList = new ArrayList<>();
                currentBillList.add(fundCollection);
                stwbCollectionBillService.pushBill(currentBillList, true);// 推送资金结算
            }
            //}
            // 提交前附件，提交后不能删除，适配billtplgroup_base表cStyle字段，auditTime作为区分时间戳
            bizObject.set("auditTime", new Date());

            // 资金计划项目额度占用
            if (fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundCollection.getValue())) {
                fundBillAdaptationFundPlanService.fundCollectionSubmitEmployFundPlan(fundCollection);
            }
            Map<String, Object> autoConfigMap = commonService.queryAutoConfigByAccentity(bizObject.get(ICmpConstant.ACCENTITY));
            if (!Objects.isNull(autoConfigMap) && null != autoConfigMap.get("isShareVideo") && (Boolean) autoConfigMap.get("isShareVideo")) {
                //走影像
                try{
                    BillBiz.executeRule("shareSubmit", billContext, paramMap);
                } catch (Exception e){
                    //影像扫描异常没有抛出的问题
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101802"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050000", "影像系统异常信息：") /* "影像系统异常信息：" */+e.getMessage());
                }
            }
            if (null != fundCollection && (null == fundCollection.getIsWfControlled() || !fundCollection.getIsWfControlled())) {
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule("audit", billContext, paramMap);
                result.setCancel(true);
            }
            //新增逻辑 当“事项类型=统收统支协同单”、且资金收款单所有子表明细结算状态均为“结算止付”时
            // ，提示“当前单据已进行委托收款整单拒绝，不允许提交！”
            EventType billtype = fundCollection.getBilltype();
            if (EventType.Unified_Synergy.equals(billtype)) {
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(bizObject.getId()));
                QuerySchema schema = QuerySchema.create().addSelect("*");
                schema.addCondition(group);
                List<Map<String, Object>> list = MetaDaoHelper.query(FundCollection_b.ENTITY_NAME, schema);
                if (ValueUtils.isNotEmptyObj(list)) {
                    //过滤子表 结算状态不为 【结算止付】的数据
                    long count = list.stream().map(map_ -> {
                        FundCollection_b fundCollection_b = new FundCollection_b();
                        fundCollection_b.init(map_);
                        return fundCollection_b;
                    }).filter(fundCollection_b -> !FundSettleStatus.SettleFailed.equals(fundCollection_b.getFundSettlestatus())).count();
                    if (count == 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101803"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0013F", "当前单据已进行委托收款整单拒绝，不允许提交！") /* "当前单据已进行委托收款整单拒绝，不允许提交！" *//* 当前单据已进行委托收款整单拒绝，不允许提交！ */);
                    }
                }
            }
            cmpIntelligentAudit.auditStart(fundCollection, IBillNumConstant.FUND_COLLECTION, ICmpConstant.CM_CMP_FUND_COLLECTION, BusinessPart.submit.getValue());
        }
        return result;
    }

    private void verifyFundCollectionSubBank(BizObject bizObject, Map<String, Integer> checkCacheMap, String accentity) throws Exception {
        List<FundCollection_b> billbs = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);//资金付款单子表
        for (FundCollection_b billb : billbs) {
            //校验子表信息
            checkFundCollectionSubInfo(billb, checkCacheMap, accentity);
            if (billb.getCaobject().equals(CaObject.Customer) || billb.getCaobject().equals(CaObject.Supplier)) {
                fundCommonService.checkCaObjectAccountNoEqual(billb.getCaobject().getValue(), billb.getOppositeaccountid(), billb.getOppositeaccountno());
            }
        }
        fundCommonService.checkStaffOppositeAccount(IBillNumConstant.FUND_COLLECTION,bizObject);
    }

    private void checkFundCollectionSubInfo(FundCollection_b billb, Map<String, Integer> checkCacheMap, String accentity) throws Exception {
        if (ValueUtils.isNotEmptyObj(billb.get("settleCurrency"))) {
            BillCopyCheckService.checkSettleCurrency(billb, checkCacheMap);
        }
        short caObject = billb.getCaobject().getValue();
        if (caObject == 1) {//客户
            if (ValueUtils.isNotEmptyObj(billb.getOppositeobjectid()) &&
                    ValueUtils.isNotEmptyObj(checkCacheMap.get("oppositeobjectid_customer_" + billb.getOppositeobjectid()))) {
                billCopyCheckService.checkCustomerByid(billb.getOppositeobjectid(), accentity, checkCacheMap);
            }
            if (ValueUtils.isNotEmptyObj(billb.getOppositeaccountid()) &&
                    ValueUtils.isNotEmptyObj(checkCacheMap.get("oppositeaccountid_customer_" + billb.getOppositeaccountid()))) {
                billCopyCheckService.checkCustomerbankaccountById(billb.getOppositeaccountid(), checkCacheMap);
            }
        }
        if (caObject == 2) {//供应商
            if (ValueUtils.isNotEmptyObj(billb.getOppositeobjectid()) &&
                    ValueUtils.isNotEmptyObj(checkCacheMap.get("oppositeobjectid_supplier_" + billb.getOppositeobjectid()))) {
                billCopyCheckService.checkSupplier(Long.valueOf(billb.getOppositeobjectid()), accentity, checkCacheMap);
            }
            Map conditon = new HashMap<>();
            conditon.put("id", billb.getOppositeaccountid());
            if (ValueUtils.isNotEmptyObj(billb.getOppositeaccountid()) &&
                    ValueUtils.isNotEmptyObj(checkCacheMap.get("oppositeaccountid_supplier_" + billb.getOppositeaccountid()))) {
                billCopyCheckService.checkSupplierbankaccountById(conditon, billb.getOppositeaccountid(), checkCacheMap);
            }
        }

    }
}
