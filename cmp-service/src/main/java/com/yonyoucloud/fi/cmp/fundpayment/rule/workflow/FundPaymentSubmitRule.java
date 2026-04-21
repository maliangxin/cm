package com.yonyoucloud.fi.cmp.fundpayment.rule.workflow;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.lock.YmsScopeLockManager;
import com.yonyou.ucf.mdd.common.constant.MddConstants;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.SystemIntegrationParamsEnum;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanService;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.fundpayment.service.FundPaymentService;
import com.yonyoucloud.fi.cmp.intelligentapproval.CmpIntelligentAudit;
import com.yonyoucloud.fi.cmp.stwb.StwbBillCheckService;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.BillCopyCheckService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h1>资金付款单提交规则</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-13 10:16
 */
@Component
@Slf4j
public class FundPaymentSubmitRule  extends AbstractCommonRule {

    @Qualifier("stwbPaymentBillServiceImpl")
    @Autowired
    private StwbBillService stwbPaymentBillService;

    @Resource
    private StwbBillCheckService stwbBillCheckService;

    @Resource
    private CmCommonService commonService;

    @Resource
    private IFundCommonService fundCommonService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    BillCopyCheckService billCopyCheckService;

    @Resource
    private FundBillAdaptationFundPlanService fundBillAdaptationFundPlanService;

    @Autowired
    private CmpIntelligentAudit cmpIntelligentAudit;

    @Autowired
    private FundPaymentService fundPaymentService;

    @Autowired
    @Qualifier("ymsGlobalScopeLockManager")
    protected YmsScopeLockManager ymsScopeLockManager;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        Map<String,String> billContextParameters = new HashMap<>();
        billContextParameters.put("extend",billContext.getParameter("extend"));
        billContextParameters.put("serviceCode",billContext.getParameter("ficmp0026"));
        billContextParameters.put("businessStepCode",billContext.getParameter("businessStepCode"));
        billContextParameters.put("cmdname",billContext.getParameter("cmdname"));
        billContextParameters.put("businessActName",billContext.getParameter("businessActName"));
        billContextParameters.put("sbillno",billContext.getParameter("sbillno"));
        billContextParameters.put("terminalType",billContext.getParameter("terminalType"));
        billContext.setParameters(billContextParameters);
        AppContext.setThreadContext(MddConstants.PARAM_SERVICE_CODE, "ficmp0026");
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizObject.getId(), 2);
            if (fundPayment == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100698"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180383","单据不存在 id:") /* "单据不存在 id:" */ + bizObject.getId());
            }
            //region 国机相关，风险参数赋值问题修复
            FundPayment fromWebData = new FundPayment();
            fromWebData.init(bizObject);
            fundPayment.setRiskPayFlag(fromWebData.getRiskPayFlag());
            List<FundPayment_b> fundPayment_bs = fundPayment.FundPayment_b();
            for (FundPayment_b b : fundPayment_bs){
                List<FundPayment_b> fromWebData_bs = fromWebData.FundPayment_b();
                if (fromWebData_bs == null){
                    continue;
                }
                for (FundPayment_b f:fromWebData_bs){
                    if (b.getId().equals(f.getId())){
                        b.setRiskPayType(f.getRiskPayType());
                    }
                }
                b.set("transNumber", null);
            }
            if (StringUtils.isEmpty(fundPayment.getTradetype())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100518"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180437","交易类型不能为空！") /* "交易类型不能为空！" */);
            }
            List<FundPayment_b> fundPaymentbs = fundPayment.FundPayment_b();
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (int i = 0; i < fundPaymentbs.size(); i++) {
                totalAmount = totalAmount.add(fundPaymentbs.get(i).getOriSum());
            }
            if(totalAmount.compareTo(fundPayment.getOriSum())!= 0 ){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540078F", "资金付款单金额与付款明细汇总金额不相等") /* "资金付款单金额与付款明细汇总金额不相等" */);
            }
            Map<String, Integer> checkCacheMap = new HashMap<>(256);
            verifyPaymentSubBank(fundPayment, checkCacheMap, fundPayment.getAccentity());
            checkCacheMap.clear();
            //先批量更新子表信息
            EntityTool.setUpdateStatus(fundPayment_bs);
            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPayment_bs);
            bizObject.set("FundPayment_b",fundPayment_bs);
            //endregion

            // 提交前附件，提交后不能删除，适配billtplgroup_base表cStyle字段，auditTime作为区分时间戳
            bizObject.set("auditTime", new Date());

            short verifystate = Short.parseShort(bizObject.get("verifystate").toString());
            if(verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100308"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00033", "单据已提交，不能进行重复提交!") /* "单据已提交，不能进行重复提交!" */ );
            }
            if(verifystate == VerifyState.TERMINATED.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100308"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00033", "单据已提交，不能进行重复提交!") /* "单据已提交，不能进行重复提交!" */ );
            }

            if (!ymsScopeLockManager.tryTxScopeLock(fundPayment.getId().toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100544"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180438","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
            }
            // 提交前来源于第三方的单据调用资金结算接口进行数据校验
            /*// 提交前来源于第三方的单据调用资金结算接口进行数据校验
            if (EventSource.ThreePartyReconciliation.getValue() == bizObject.getShort(ICmpConstant.SRC_ITEM) ||
                    bizObject.getShort(ICmpConstant.BILLTYPE) == EventType.CashMark.getValue()) {*/
            boolean settleFlagBool = fundPayment.getSettleflag() == null || fundPayment.getSettleflag() == 1;
            List<FundPayment_b> fundPayment_bList = fundPayment.get("FundPayment_b");
            List<FundPayment_b> needCheckList = fundPayment_bList.stream()
                    .filter(sub->(!sub.getFundSettlestatus().equals(FundSettleStatus.SettlementSupplement)&&!sub.getFundSettlestatus().equals(FundSettleStatus.Refund)))
                    .filter(sub->sub.getOriSum().compareTo(BigDecimal.ZERO)>0)
                    .collect(Collectors.toList());
            if(settleFlagBool && CollectionUtils.isNotEmpty(needCheckList)){
                fundPayment.setFundPayment_b(needCheckList);
                List<BizObject> currentBillList = new ArrayList<>();
                currentBillList.add(fundPayment);
                stwbPaymentBillService.pushBill(currentBillList, true);// 推送资金结算
            }
            fundPayment.setFundPayment_b(fundPayment_bList);
            //}

            // 资金计划项目额度占用
            if (fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundPayment.getValue())) {
                fundBillAdaptationFundPlanService.fundPaymentSubmitEmployFundPlan(fundPayment);
            }

            Map<String, Object> autoConfigMap = commonService.queryAutoConfigByAccentity(bizObject.get(ICmpConstant.ACCENTITY)) ;
            if(!Objects.isNull(autoConfigMap) && null != autoConfigMap.get("isShareVideo") && (Boolean) autoConfigMap.get("isShareVideo")){
                //走影像
                BillBiz.executeRule("shareSubmit", billContext, paramMap);
            }
            if(null != fundPayment && (null == fundPayment.getIsWfControlled() || !fundPayment.getIsWfControlled()) ){
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule("audit", billContext, paramMap);
                result.setCancel(true);
                // 预算
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_PAYMENT)) {
                    fundPaymentExecuteAudit(fundPayment);
                }
            } else {
                // 预算
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_PAYMENT)) {
                    fundPaymentExecuteSubmit(fundPayment);
                }
            }
            //新增逻辑 当“事项类型=统收统支协同单”、且资金付款单所有子表明细结算状态均为“结算止付”时
            // ，提示“当前单据已进行委托付款整单拒绝，不允许提交！”
            EventType billtype = fundPayment.getBilltype();
            if (EventType.Unified_Synergy.equals(billtype)) {
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(bizObject.getId()));
                QuerySchema schema = QuerySchema.create().addSelect("*");
                schema.addCondition(group);
                List<FundPayment_b> list = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, schema, null);
                if (ValueUtils.isNotEmptyObj(list)) {
                    //过滤子表 结算状态为 【结算止付】的数据
                    long count = list.stream().filter(item -> FundSettleStatus.SettleFailed.equals(item.getFundSettlestatus())).count();
                    if (count == list.size()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100421"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001D2", "当前单据已进行委托付款整单拒绝，不允许提交！") /* "当前单据已进行委托付款整单拒绝，不允许提交！" */);
                    }
                }
            }

            //国际相关，新增结算检查,列表提交时
            if ("cmp_fundpaymentlist".equals(billContext.getBillnum())){
                CtmJSONObject billcheckJson;
                if(paramMap.get("requestData") !=null){
                    if (paramMap.get("requestData").toString().startsWith("{")){ //老架构
                        billcheckJson = CtmJSONObject.parseObject(paramMap.get("requestData").toString());
                    }else if (paramMap.get("requestData").toString().startsWith("[{")){//新架构
                        billcheckJson = CtmJSONArray.parseArray(CtmJSONObject.toJSONString(paramMap.get("requestData"))).getJSONObject(0);
                    }else {
                        billcheckJson= null;
                    }
                }else {
                    billcheckJson = null;
                }

                //需要进行结算检查
                if (billcheckJson!=null && billcheckJson.get("billCheckFlag") !=null && billcheckJson.getBoolean("billCheckFlag")){
                    CtmJSONObject billCheckResult = stwbBillCheckService.fundSubmitBillCheck(fundPayment);
                    if ("1".equals(billCheckResult.getString("checkFlag"))){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100422"),billCheckResult.getString("checkMsg"));
                    }
                }
            }
            cmpIntelligentAudit.auditStart(fundPayment, IBillNumConstant.FUND_PAYMENT,ICmpConstant.CM_CMP_FUND_PAYMENT, BusinessPart.submit.getValue());
        }
        return result;
    }

    private void verifyPaymentSubBank(BizObject bizObject, Map<String, Integer> checkCacheMap, String accentity) throws Exception {
        List<FundPayment_b> billbs = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);//资金付款单子表
        for (FundPayment_b billb : billbs) {
            //校验子表信息
            checkFundPaymentSubInfo(billb, checkCacheMap, accentity);
            if (billb.getCaobject().equals(CaObject.Customer) || billb.getCaobject().equals(CaObject.Supplier)) {
                fundCommonService.checkCaObjectAccountNoEqual(billb.getCaobject().getValue(), billb.getOppositeaccountid(), billb.getOppositeaccountno());
            }
        }
        fundCommonService.checkStaffOppositeAccount(IBillNumConstant.FUND_PAYMENT,bizObject);
    }

    private void checkFundPaymentSubInfo(FundPayment_b billb, Map<String, Integer> checkCacheMap, String accentity) throws Exception {
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



    /**
     * 更新预算占用状态
     * @param fundPayment
     * @throws Exception
     */
    private void fundPaymentExecuteSubmit(FundPayment fundPayment) throws Exception {
        List<FundPayment_b> updateList = new ArrayList();
        for (FundPayment_b fundPayment_b : fundPayment.FundPayment_b()) {
            Short budgeted = fundPayment_b.getIsOccupyBudget();
            // 已经预占仍要预占或者已经实占，直接跳过不执行了
            if (budgeted != null && ((budgeted!=OccupyBudget.UnOccupy.getValue()))) {
                continue;
            }
            // 是否委托驳回，为是的时候，无需占用预算，
            if (fundPayment_b.getEntrustReject()!=null && fundPayment_b.getEntrustReject()==1) {
                continue;
            }
            // 退票的不占用预算，无需占用预算，
            if (fundPayment_b.getFundSettlestatus()!=null && FundSettleStatus.Refund.getValue() == fundPayment_b.getFundSettlestatus().getValue() ) {
                continue;
            }
            updateList.add(fundPayment_b);
        }
        // updateList为空，无需处理
        if (!updateList.isEmpty()) {
            ResultBudget resultBudget = cmpBudgetManagerService.gcExecuteBatchSubmit(fundPayment, updateList, IBillNumConstant.FUND_PAYMENT, BillAction.SUBMIT);
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
                MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, updateList);
            }
        }
    }

    /**
     * 更新预算占用状态
     * @param fundPayment
     * @throws Exception
     */
    private void fundPaymentExecuteAudit(FundPayment fundPayment) throws Exception {
        // 已经调用过审批规则了，fundPayment需要重新查询一遍
        fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, fundPayment.getId(), 2);
        List<FundPayment_b> fundPayment_bs = new ArrayList<>();
        for (FundPayment_b fundPayment_b : fundPayment.FundPayment_b()) {
            Short budgeted = fundPayment_b.getIsOccupyBudget();
            // 已经预占仍要预占或者已经实占，直接跳过不执行了
            if (budgeted != null && ((budgeted==OccupyBudget.PreSuccess.getValue()))) {
                continue;
            }
            // 是否委托驳回，为是的时候，无需占用预算，
            if (fundPayment_b.getEntrustReject()!=null && fundPayment_b.getEntrustReject()==1) {
                continue;
            }
            // 退票的不占用预算，无需占用预算，
            if (fundPayment_b.getFundSettlestatus()!=null && FundSettleStatus.Refund.getValue() == fundPayment_b.getFundSettlestatus().getValue() ) {
                continue;
            }
//            if (fundPayment_b.getFundSettlestatus()!=null && fundPayment_b.getFundSettlestatus().equals(FundSettleStatus.Refund)) {
//                continue;
//            }
            fundPayment_bs.add(fundPayment_b);
        }
        if (fundPayment_bs.size() > 0) {
            ResultBudget resultBudget = cmpBudgetManagerService.gcExecuteBatchSubmit(fundPayment, fundPayment_bs, IBillNumConstant.FUND_PAYMENT, BillAction.APPROVE_PASS);
            if (resultBudget.isSuccess()) {
                if (resultBudget.getIds() != null && !resultBudget.getIds().isEmpty()) {
                    fundPayment_bs.stream().forEach( fundPayment_b -> {
                        if (resultBudget.getIds().contains(fundPayment_b.getId().toString())) {
                            fundPayment_b.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                            fundPayment_b.setEntityStatus(EntityStatus.Update);
                        }
                    });
                } else {
                    fundPayment_bs.stream().forEach( fundPayment_b -> {
                        fundPayment_b.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                        fundPayment_b.setEntityStatus(EntityStatus.Update);
                    });
                }
                MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPayment_bs);
            }
        }
    }
}
