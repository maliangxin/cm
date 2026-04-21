package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatusConverter;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.BillMapEnum;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CODE;

/**
 * <h1>资金收付款单删除凭证</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-01-10 10:15
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class FundCommonDeleteVoucherRule extends AbstractCommonRule {

    private final CmpVoucherService cmpVoucherService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bizObject : bills) {
            log.error("Fund bill delete voucher handler first, code={}, id={}, voucherStatusOriginal={}, voucherStatus={}, tenantId={}",
                    bizObject.getId(), bizObject.get("code"), bizObject.get("voucherstatus_original"),
                    bizObject.get("voucherstatus"), InvocationInfoProxy.getTenantid());
            BizObject bizObjectNew = null;
            List<BizObject> subList = new ArrayList<>();
            String entityName = null;
            CtmJSONObject jsonObject = new CtmJSONObject();
            if (FundCollection.ENTITY_NAME.equals(billContext.getFullname())) {
                bizObjectNew = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizObject.getId());
                bizObjectNew.put("_entityName", FundCollection.ENTITY_NAME);
                jsonObject.put("billnum", IBillNumConstant.FUND_COLLECTION);
                jsonObject.put("id", bizObject.getId());
                subList = bizObjectNew.get("FundCollection_b");
                entityName = FundCollection_b.ENTITY_NAME;
            }
            if (FundPayment.ENTITY_NAME.equals(billContext.getFullname())) {
                bizObjectNew = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizObject.getId());
                bizObjectNew.put("_entityName", FundPayment.ENTITY_NAME);
                jsonObject.put("billnum", IBillNumConstant.FUND_PAYMENT);
                jsonObject.put("id", bizObject.getId());
                subList = bizObjectNew.get("FundPayment_b");
                entityName = FundPayment_b.ENTITY_NAME;
            }
            //过滤已经被委托拒绝的数据 不执行更新
            subList.forEach(e -> {
                boolean isNotEntrustReject = e.getShort(ICmpConstant.SETTLE_STATUS) != FundSettleStatus.SettleFailed.getValue()
                        && (e.get("entrustReject") == null || e.getShort("entrustReject") != 1);
                //清空结算成功金额、结算成功时间、结算支付金额
                if (isNotEntrustReject){
                    e.set("settlesuccessSum", new BigDecimal(0));
                    e.set("settleSuccessTime", null);
                    e.set("settleerrorSum", new BigDecimal(0));
                    //若结算状态不是已结算补单，则结算状态修改为待结算
                    if (e.getShort(ICmpConstant.SETTLE_STATUS) != FundSettleStatus.SettlementSupplement.getValue()
                            && e.getShort(ICmpConstant.SETTLE_STATUS) != FundSettleStatus.Refund.getValue() ) {
                        e.set(ICmpConstant.SETTLE_STATUS, FundSettleStatus.WaitSettle.getValue());
                    }
                    e.set("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(FundSettleStatus.find(e.get(ICmpConstant.SETTLE_STATUS))));
                }
            });
            EntityTool.setUpdateStatus(subList);
            MetaDaoHelper.update(entityName, subList);
            // 解决撤回单据不刷新子表的问题
            if (FundCollection.ENTITY_NAME.equals(billContext.getFullname())) {
                bizObject.set("FundCollection_b",subList);
            } else if (FundPayment.ENTITY_NAME.equals(billContext.getFullname())) {
                bizObject.set("FundPayment_b",subList);
            }
            assert bizObjectNew != null;
            bizObjectNew.put("voucherstatus_original", bizObject.get("voucherstatus_original"));
            log.error("Fund bill delete voucher handler second, code={}, id={}, voucherstatus_original={}, voucherstatus={}, tenantId={}",
                    bizObjectNew.getId(), bizObjectNew.get("code"), bizObjectNew.get("voucherstatus_original"),
                    bizObjectNew.get("voucherstatus"), InvocationInfoProxy.getTenantid());
           /* if (ValueUtils.isNotEmptyObj(bizObject.get("voucherstatus_original")) && Short.parseShort(bizObject.get("voucherstatus_original").toString()) == VoucherStatus.POST_FAIL.getValue()) {
                return new RuleExecuteResult();
            }
            if (ValueUtils.isNotEmptyObj(bizObject.get("voucherstatus_original")) && Short.parseShort(bizObject.get("voucherstatus_original").toString()) == VoucherStatus.NONCreate.getValue()) {
                return new RuleExecuteResult();
            }
            boolean isToBePost = ValueUtils.isNotEmptyObj(bizObject.get("voucherstatus_original")) && Short.parseShort(bizObject.get("voucherstatus_original").toString()) == VoucherStatus.TO_BE_POST.getValue();
            if (isToBePost) {
                return new RuleExecuteResult();
            }*/
            CtmJSONObject logparam = new CtmJSONObject();
            logparam.put("bizObject", bizObject);
            logparam.put("bizObjectNew", bizObjectNew);
            BillMapEnum billMapEnum = BillMapEnum.getEnumByBillNum(billContext.getBillnum());
            String defaultServiceCode = IServicecodeConstant.FUNDPAYMENT;
            String serviceCode = defaultServiceCode;
            if(billMapEnum != null){
                serviceCode = billMapEnum.getServiceCode();
            }
            AppContext.getBean(CTMCMPBusinessLogService.class).saveBusinessLog(logparam, bizObject.getJavaObject(CODE,String.class), "删除事项前日志", serviceCode, "删除事项前日志", "删除事项前日志");//@notranslate
            boolean checked;
            checked = cmpVoucherService.isChecked(jsonObject);
            if (checked) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100357"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803BC","该单据凭证已勾对，不能撤回！") /* "该单据凭证已勾对，不能撤回！" */);
            }

            CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResultTry(bizObjectNew);
            log.error("Fund bill delete voucher handler third, code={}, id={}, deleteResult={}, tenantId={}",
                    bizObjectNew.getId(), bizObjectNew.get("code"),deleteResult.toString(), InvocationInfoProxy.getTenantid());
            if (!deleteResult.getBoolean("dealSucceed")) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100358"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803BD","删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
            }
        }
        return new RuleExecuteResult();
    }
}
