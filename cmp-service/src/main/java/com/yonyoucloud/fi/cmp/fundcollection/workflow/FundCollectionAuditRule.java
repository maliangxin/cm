package com.yonyoucloud.fi.cmp.fundcollection.workflow;

import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.context.InvocationInfoProxy;
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
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatusConverter;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

/**
 * <h1>资金收款单审核规则</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-13 10:16
 */
@Slf4j
@Component
public class FundCollectionAuditRule extends AbstractCommonRule {

    @Qualifier("stwbCollectionBillServiceImpl")
    @Autowired
    StwbBillService stwbBillService;
    @Autowired
    private CmCommonService commonService;
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
            FundCollection currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizobject.getId());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100801"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418051D", "单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100802"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418051F", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            Date date = BillInfoUtils.getBusinessDate();
            if (null != billContext.getDeleteReason()) {
                if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                    return new RuleExecuteResult();
                }
            }
            Date currentDate = BillInfoUtils.getBusinessDate();
            if (currentDate.compareTo(date) < 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100803"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418051E", "审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
            }

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
            // 新增入账类型字段，所有单据都推送待结算数据，不在进行各种判断（提前入账为是，关联次数为2等）
            boolean settleflagBool = currentBill.getSettleflag() == null || currentBill.getSettleflag()==1;
            if (settleflagBool) {
                // 判断如果是保存并提交按钮，如果未启用审批流，则先不推结算（因为单据还没入库，结算那边调用单据转换规则会报错，未在库里查到数据）
                boolean isSaveAndSubmit = ICmpConstant.CMD_SAVE_AND_SUBMIT.equals(billContext.getParameter(ICmpConstant.CMD_NAME));
                boolean isWfControlled = ValueUtils.isNotEmptyObj(bizobject.get(ICmpConstant.IS_WFCONTROLLED)) && bizobject.getBoolean(ICmpConstant.IS_WFCONTROLLED);
                boolean saveAndSubmitBool = isSaveAndSubmit && !isWfControlled;
                if (!saveAndSubmitBool) {
                    //更新结算状态为：结算中
                    List<BizObject> fundCollectionBList = currentBill.get("FundCollection_b");
                    if (CollectionUtils.isNotEmpty(fundCollectionBList)) {
                        List<BizObject> updateList = new ArrayList<>();
                        for (BizObject biz : fundCollectionBList) {
                            Short settleStatus = biz.getShort("settlestatus");
                            //提交更新为结算中
                            if (settleStatus != null && settleStatus == FundSettleStatus.WaitSettle.getValue()) {
                                //金额为0的 结算状态变成结算成功
                                if(biz.getBigDecimal("oriSum").compareTo(BigDecimal.ZERO)==0){
                                    biz.put("settlestatus", FundSettleStatus.SettleSuccess.getValue());
                                    biz.put("settlesuccessSum", BigDecimal.ZERO);
                                    biz.put("settleSuccessTime",
                                            Objects.isNull(AppContext.getCurrentUser().getBusinessDate()) ? new Date() : AppContext.getCurrentUser().getBusinessDate());
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
                            MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, updateList);
                        }
                    }
                    List<BizObject> currentBillList = new ArrayList<>();
                    currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizobject.getId());
                    currentBillList.add(ValueUtils.isNotEmptyObj(currentBill) ? currentBill : bizobject);
                    stwbBillService.pushBill(currentBillList, false);// 推送资金结算
                }
            } else {
                List<FundCollection_b> list = currentBill.FundCollection_b();
                list.forEach(item -> {
                    if (item.getFundSettlestatus().getValue() == FundSettleStatus.WaitSettle.getValue()) {
                        item.setFundSettlestatus(FundSettleStatus.SettleSuccess);
                        item.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(item.getFundSettlestatus()));
                        item.setEntityStatus(EntityStatus.Update);
                    }
                });
                MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, list);
            }

            // 更新该币种银行结息账号预提规则的上次结息结束日=本次结息结束日
            List<BizObject> fundCollectionBList = currentBill.get(ICmpConstant.FUND_COLLECTION_B);
            fundCommonService.updateWithholdingRuleSettingLastInterestSettlementDate(fundCollectionBList, ICmpConstant.CONSTANT_TWO);

            // 已审核
            bizobject.putAll(currentBill);

            // 设置是否结算成功后过账字段值
            fundCommonService.setSettleSuccessPostValue(bizobject, bizobject.get(ICmpConstant.ACCENTITY));
            bizobject.set("auditstatus", AuditStatus.Complete.getValue());
            bizobject.set("auditorId", AppContext.getCurrentUser().getId());
            bizobject.set("auditor", AppContext.getCurrentUser().getName());
            bizobject.set("auditDate", new Date());
            bizobject.set("auditTime", new Date());
            if (isSettleSuccessToPost && currentBill.getVoucherstatus().getValue() != VoucherStatus.TO_BE_POST.getValue()) {
                boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
                if (!enableEVNT) {
                    log.error("客户环境未安装事项中台服务");
                    bizobject.set("voucherstatus", VoucherStatus.NONCreate.getValue());
                }  else {
                    bizobject.set("voucherstatus", VoucherStatus.TO_BE_POST.getValue());
                }
            }
            log.error("fund collection bill audit, data={}, code={}", CtmJSONObject.toJSONString(data), bizobject.get("code"));
        }
        return new RuleExecuteResult();
    }

}
