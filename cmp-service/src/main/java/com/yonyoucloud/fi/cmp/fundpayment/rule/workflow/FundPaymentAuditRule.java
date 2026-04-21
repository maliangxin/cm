package com.yonyoucloud.fi.cmp.fundpayment.rule.workflow;

import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.iuap.log.model.BusinessObject;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.iuap.log.util.BusiObjectBuildUtil;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.constant.CtmConstants;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.workbench.ICtmApplicationService;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.dataSignature.DataSignatureUtil;
import com.yonyoucloud.fi.cmp.util.dataSignature.entity.DataSignatureEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

/**
 * <h1>资金付款单审核规则</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-13 10:16
 */
@Slf4j
@Component
public class FundPaymentAuditRule extends AbstractCommonRule {

    @Qualifier("stwbPaymentBillServiceImpl")
    @Autowired
    private StwbBillService stwbBillService;
    @Autowired
    private CmCommonService<Object> commonService;
    @Autowired
    private IFundCommonService fundCommonService;
    @Resource
    private IApplicationService appService;

    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            CtmJSONObject data = new CtmJSONObject();
            log.info("FundPaymentAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
            FundPayment currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizobject.getId());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100715"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418048A", "单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }
            // region 国机相关，风险参数赋值问题修复,修复空值不传递的问题
            FundPayment fromWebData = new FundPayment();
            fromWebData.init(bizobject);
            List<FundPayment_b> fundPayment_bs = currentBill.FundPayment_b();
            for (FundPayment_b b : fundPayment_bs) {
                List<FundPayment_b> fromWebData_bs = fromWebData.FundPayment_b();
                if (fromWebData_bs == null) {
                    continue;
                }
                for (FundPayment_b f : fromWebData_bs) {
                    if (b.getId().equals(f.getId())) {
                        b.setRiskPayType(f.getRiskPayType() == null ? "" : f.getRiskPayType());
                    }
                }
            }
            currentBill.setFundPayment_b(fundPayment_bs);
            // endregion
            log.info("FundPaymentAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100716"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418048C", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            Date date = BillInfoUtils.getBusinessDate();
            if (null != billContext.getDeleteReason()) {
                if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {// 删除流程实例
                    return new RuleExecuteResult();
                }
            }
            Date currentDate = BillInfoUtils.getBusinessDate();
            if (currentDate.compareTo(date) < 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100717"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418048B", "审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
            }
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<FundPayment_b> billbs = currentBill.get("FundPayment_b");
            for (FundPayment_b billb : billbs) {
                // 待结算的资金付款单才进行验签
                if (billb.getFundSettlestatus().getValue() == FundSettleStatus.WaitSettle.getValue()) {
                    // 验签
                    DataSignatureEntity dataSignatureEntity = DataSignatureEntity.builder().opoppositeObjectName(billb.getOppositeobjectname()).
                            oppositeAccountName(billb.getOppositeaccountname()).tradeAmount(billb.getOriSum()).oriSign(billb.getSignature()).build();
                    DataSignatureUtil.unSignMsg(dataSignatureEntity);
                }
                totalAmount = totalAmount.add(billb.getOriSum());
            }
            //子表验签通过后验证子表合计金额是否和主表相等
            if(totalAmount.compareTo(currentBill.getOriSum())!= 0 ){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400705", "资金付款单金额与付款明细汇总金额不相等") /* "资金付款单金额与付款明细汇总金额不相等" */);
            }
            boolean settleFlagBool = currentBill.getSettleflag() == null || currentBill.getSettleflag() == 1;
            List<BizObject> updateList = new ArrayList<>();
            if (settleFlagBool) {
                // 判断如果是保存并提交按钮，如果未启用审批流，则先不推结算（因为单据还没入库，结算那边调用单据转换规则会报错，未在库里查到数据）
                boolean isSaveAndSubmit = ICmpConstant.CMD_SAVE_AND_SUBMIT.equals(billContext.getParameter(ICmpConstant.CMD_NAME));
                boolean isWfControlled = ValueUtils.isNotEmptyObj(bizobject.get(ICmpConstant.IS_WFCONTROLLED)) && bizobject.getBoolean(ICmpConstant.IS_WFCONTROLLED);
                boolean saveAndSubmitBool = isSaveAndSubmit && !isWfControlled;
                if (!saveAndSubmitBool) {
                    // 更新结算状态为：结算中
                    for (BizObject biz : billbs) {
                        Short settleStatus = biz.getShort("settlestatus");
                        // 提交更新为结算中
                        if (settleStatus != null && settleStatus == FundSettleStatus.WaitSettle.getValue()) {
                            //金额为0的 结算状态变成结算成功
                            if(biz.getBigDecimal("oriSum").compareTo(BigDecimal.ZERO)==0){
                                biz.put("settlestatus", FundSettleStatus.SettleSuccess.getValue());
                                biz.put("settlesuccessSum", BigDecimal.ZERO);
                                biz.put("settleSuccessTime", Objects.isNull(AppContext.getCurrentUser().getBusinessDate()) ? new Date() : AppContext.getCurrentUser().getBusinessDate());
                                FundPayment_b fundPaymentSub = (FundPayment_b) biz.clone();
                                // 资金付款单协同生成资金收款单
                                CtmJSONObject result =
                                        fundCommonService.fundPaymentBillCoordinatedGeneratorFundCollectionBill(fundPaymentSub);
                                log.error("generator Fund Collection Bill process done, result = {}", CtmJSONObject.toJSONString(result));
                                if (ValueUtils.isNotEmptyObj(result)
                                        && ValueUtils.isNotEmptyObj(result.getString("status"))
                                        && "200".equals(result.getString("status"))) {
                                    biz.put("synergybillid",result.getString("id"));
                                    biz.put("synergybillno",result.getString("code"));
                                    biz.put("synergybillitemno",result.getString("lineno"));
                                    biz.put("issynergy",true);
                                    fundPaymentSub.setSynergybillid(result.getString("id"));
                                    fundPaymentSub.setSynergybillno(result.getString("code"));
                                    fundPaymentSub.setSynergybillitemno(result.getString("lineno"));
                                    fundPaymentSub.setIssynergy(true);
                                    log.error("FundPaymentAuditRule" +
                                            "-paymentBillCoordinatedGeneratorFundCollectionBill, fundPaymentSub={}", CtmJSONObject.toJSONString(fundPaymentSub));
                                }
                                if (ValueUtils.isNotEmptyObj(result)) {
                                    BusinessObject businessObject = BusiObjectBuildUtil.build(IServicecodeConstant.FUNDCOLLECTION,
                                            FundCollection.ENTITY_NAME, OperCodeTypes.publish,
                                            IMsgConstant.FUND_COLLECTION, IMsgConstant.COOPERATE_FUND_COLLECTION, result);
                                    IBusinessLogService businessLogService = AppContext.getBean(IBusinessLogService.class);
                                    businessLogService.saveBusinessLog(businessObject);
                                }


                            } else {
                                if (!enableSimplify) {
                                    biz.put("settlestatus", FundSettleStatus.SettleProssing.getValue());
                                }
                            }
                            updateList.add(biz);
                        }
                    }
                    if (CollectionUtils.isNotEmpty(updateList)) {
                        EntityTool.setUpdateStatus(updateList);
                        MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, updateList);
                    }
                    List<BizObject> currentBillList = new ArrayList<>();
                    currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizobject.getId());
                    currentBillList.add(ValueUtils.isNotEmptyObj(currentBill) ? currentBill : bizobject);
                    stwbBillService.pushBill(currentBillList, false);// 推送资金结算
                }
            } else {
                Integer autoSettleFlag = currentBill.getInteger("autoSettleFlag");
                if (autoSettleFlag == 1) {
                    List<FundPayment_b> list = currentBill.FundPayment_b();
                    for (FundPayment_b item : list) {
                        if (item.getFundSettlestatus().getValue() == FundSettleStatus.WaitSettle.getValue()) {
                            item.set("settlestatus", FundSettleStatus.SettleSuccess.getValue());
                            updateList.add(item);
                        }
                    }
                    if (CollectionUtils.isNotEmpty(updateList)) {
                        EntityTool.setUpdateStatus(updateList);
                        MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, updateList);
                    }
                }
            }

            // 更新该币种银行结息账号预提规则的上次结息结束日=本次结息结束日
            List<BizObject> fundPaymentBList = currentBill.get(ICmpConstant.FUND_PAYMENT_B);
            fundCommonService.updateWithholdingRuleSettingLastInterestSettlementDate(fundPaymentBList, ICmpConstant.CONSTANT_TWO);

            // 已审核
            bizobject.putAll(currentBill);
            // 设置是否结算成功后过账字段值
            fundCommonService.setSettleSuccessPostValue(bizobject, bizobject.get(ICmpConstant.ACCENTITY));
            bizobject.set("auditstatus", AuditStatus.Complete.getValue());
            bizobject.set("auditorId", AppContext.getCurrentUser().getId());
            bizobject.set("auditor", AppContext.getCurrentUser().getName());
            bizobject.set("auditDate", new Date());
            bizobject.set("auditTime", new Date());

            // 资金收付款单如果在现金参数里：是否结算成功时做账为是时，则先不过账，最后统一过账
            Map<String, Object> autoConfig = commonService.queryAutoConfigByAccentity(currentBill.getAccentity());
            boolean isSettleSuccessToPost;
            if(autoConfig != null ){
                // 资金收付款单如果在现金参数里：是否结算成功时做账为是时，则先不过账，最后统一过账
                if(autoConfig.get("isSettleSuccessToPost") != null){
                    isSettleSuccessToPost = (Boolean) autoConfig.get("isSettleSuccessToPost");
                }else{
                    Map<String, Object> autoConfigTenant = commonService.queryAutoConfigTenant();
                    isSettleSuccessToPost = autoConfigTenant != null && autoConfigTenant.get("isSettleSuccessToPost") != null && (Boolean) autoConfigTenant.get("isSettleSuccessToPost");
                }
            }else{
                Map<String, Object> autoConfigTenant = commonService.queryAutoConfigTenant();
                isSettleSuccessToPost = autoConfigTenant != null && autoConfigTenant.get("isSettleSuccessToPost") != null && (Boolean) autoConfigTenant.get("isSettleSuccessToPost");
            }
            if (isSettleSuccessToPost && currentBill.getVoucherstatus().getValue() != VoucherStatus.TO_BE_POST.getValue()) {
                boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
                if (!enableEVNT) {
                    log.error("客户环境未安装事项中台服务");
                    bizobject.set("voucherstatus", VoucherStatus.NONCreate.getValue());
                } else {
                    bizobject.set("voucherstatus", VoucherStatus.TO_BE_POST.getValue());
                }
            }
            log.error("fund payment bill audit, data={}, code={}", CtmJSONObject.toJSONString(data), bizobject.get("code"));
        }
        return new RuleExecuteResult();
    }

}
