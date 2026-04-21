package com.yonyoucloud.fi.cmp.payapplicationbill.rule.business;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.SourceMatters;
import com.yonyoucloud.fi.cmp.payapplicationbill.SourceOrderType;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_EIGHT;

/**
 * <h1>保存付款申请单之前的规则</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-15 16:38
 */
@Slf4j
@Component("beforeDeletePayApplyBillRule")
public class BeforeDeletePayApplyBillRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400554", "在财务新架构环境下，不允许删除付款申请单。") /* "在财务新架构环境下，不允许删除付款申请单。" */);
        }
        for (BizObject e : bills) {
            log.error("Pay apply bill delete bill, id = {}, tenantID = {}, source = {} ,BizObject = {}",
                    e.getId(), InvocationInfoProxy.getTenantid(), e.get("source"), JsonUtils.toJson(e));
            List<BizObject> listMap = new ArrayList<>(CONSTANT_EIGHT);
            short srcItem = ValueUtils.isNotEmptyObj(e.get("srcitem")) ? Short.parseShort(e.get("srcitem").toString()) : (short) -1;
            if (srcItem == SourceMatters.MattersDue.getValue()) {
                Long id = e.getId();
                if ((!ValueUtils.isNotEmptyObj(e.get("source"))) || (!e.get("source").toString().equals("fiarap.arap_oap"))) {
                    e.put("source", "fiarap.arap_oap");
                }
                PayApplicationBill payApplicationBill;
                try {
                    payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, id, 3);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101830"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E8","查询付款单异常") /* "查询付款单异常" */);
                }
                if (ValueUtils.isNotEmptyObj(payApplicationBill)) {
                    List<PayApplicationBill_b> payApplicationBillBs = payApplicationBill.payApplicationBill_b();
                    log.error("Pay apply bill delete bill ,payApplicationBillBs = {}", JsonUtils.toJson(payApplicationBillBs));
                    for (PayApplicationBill_b payApplicationBillB : payApplicationBillBs) {
                        BizObject dataMapDelete = new BizObject();
                        if (!ValueUtils.isNotEmptyObj(payApplicationBillB.getOrderNo()) && !ValueUtils.isNotEmptyObj(payApplicationBillB.getSrcinvoicebillitemid())) {
                            continue;
                        }
                        Short sourceOrderType = payApplicationBillB.getSourceOrderType();
                        dataMapDelete.put("paymentApplyAmount", payApplicationBillB.get("paymentApplyAmount"));
                        if (sourceOrderType == SourceOrderType.PurchaseOrder.getValue()) {
                            dataMapDelete.put("polineId", (ValueUtils.isNotEmptyObj(payApplicationBillB.getTopsrcbillitemid())) ? payApplicationBillB.getTopsrcbillitemid() : null);
                        } else if (sourceOrderType == SourceOrderType.OutsourcedOrder.getValue()) {
                            dataMapDelete.put("orderId", (ValueUtils.isNotEmptyObj(payApplicationBillB.getTopsrcbillitemid())) ? payApplicationBillB.getTopsrcbillitemid() : null);
                        }
                        dataMapDelete.put("lineId", ValueUtils.isNotEmptyObj(payApplicationBillB.getSrcinvoicebillitemid()) ? payApplicationBillB.getSrcinvoicebillitemid() : null);
                        listMap.add(dataMapDelete);
                    }
                }
                log.error("Pay apply bill delete bill write back purchase ,billId = {},  payApplyBillOriSum = {}", e.getId(), listMap);
                paramMap.put("payApplyBillOriSum", listMap);
            } else if (srcItem == SourceMatters.PurchaseOrderPlan.getValue()) {
                Long id = e.getId();
                PayApplicationBill payApplicationBill;
                try {
                    payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, id, 3);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101830"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E8","查询付款单异常") /* "查询付款单异常" */);
                }
                String srcPurchasePlanItemId = null;
                Map<String, BigDecimal> srcPurchaseOrderMaterialLineMap = new HashMap<>();

                if (ValueUtils.isNotEmptyObj(payApplicationBill)) {
                    List<PayApplicationBill_b> payApplicationBillBs = payApplicationBill.payApplicationBill_b();
                    for (PayApplicationBill_b payApplicationBillB : payApplicationBillBs) {
                        if (!ValueUtils.isNotEmptyObj(srcPurchasePlanItemId)) {
                            srcPurchasePlanItemId = payApplicationBillB.getSrcpurchaseplanitemid();
                        }
                        String srcPurchaseOrderMaterialLineId = payApplicationBillB.getSrcPurchaseOrderMaterialLineId();
                        if (ValueUtils.isNotEmptyObj(srcPurchaseOrderMaterialLineMap.get(srcPurchaseOrderMaterialLineId))) {
                            srcPurchaseOrderMaterialLineMap.put(srcPurchaseOrderMaterialLineId,
                                    BigDecimalUtils.safeAdd(srcPurchaseOrderMaterialLineMap.get(srcPurchaseOrderMaterialLineId),
                                            BigDecimalUtils.safeSubtract(BigDecimal.ZERO, payApplicationBillB.getPaymentApplyAmount())));
                        } else {
                            srcPurchaseOrderMaterialLineMap.put(srcPurchaseOrderMaterialLineId,
                                    BigDecimalUtils.safeSubtract(BigDecimal.ZERO, payApplicationBillB.getPaymentApplyAmount()));
                        }

                    }
                }
                List<Map<String, Object>> srcPurchaseOrderMaterialLineList = new ArrayList<>();
                for (Map.Entry<String, BigDecimal> entry : srcPurchaseOrderMaterialLineMap.entrySet()) {
                    BizObject splitMap = new BizObject();
                    splitMap.put("srcPurchaseOrderMaterialLineId", entry.getKey());
                    splitMap.put("paymentApplyAmountSum", entry.getValue());
                    srcPurchaseOrderMaterialLineList.add(splitMap);
                }
                BizObject backWritePurchasePlanLine = new BizObject();
                backWritePurchasePlanLine.put("srcpurchaseplanitemid", srcPurchasePlanItemId);
                backWritePurchasePlanLine.put("srcPurchaseOrderMaterialLineList", srcPurchaseOrderMaterialLineList);
                backWritePurchasePlanLine.put("paymentApplyAmountSum", BigDecimalUtils.safeSubtract(BigDecimal.ZERO, payApplicationBill.getPaymentApplyAmountSum()));
                paramMap.put("backWritePurchasePlanLine", backWritePurchasePlanLine);
                log.error("delete back write purchase plan amount id = {}, backWritePurchasePlanLine = {}", id, backWritePurchasePlanLine);
            } else if (srcItem == SourceMatters.OutsourcingOrderAll.getValue() || srcItem == SourceMatters.OutsourcingOrderProductLine.getValue()) {
                Long id = e.getId();
                PayApplicationBill payApplicationBill;
                try {
                    payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, id, 3);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101830"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E8","查询付款单异常") /* "查询付款单异常" */);
                }
                BizObject productionOrderBalanceWriteDataInfo = new BizObject();
                if (ValueUtils.isNotEmptyObj(payApplicationBill)) {
                    List<PayApplicationBill_b> payApplicationBillBs = payApplicationBill.payApplicationBill_b();
                    for (PayApplicationBill_b payApplicationBillB : payApplicationBillBs) {
                        productionOrderBalanceWriteDataInfo.put("id", payApplicationBillB.getSourceautoid());
                        productionOrderBalanceWriteDataInfo.put("requestedPaymentFC", BigDecimalUtils.safeSubtract(BigDecimal.ZERO, payApplicationBill.getPaymentApplyAmountSum()));
                    }
                }
                paramMap.put("saveWriteBackInfo", productionOrderBalanceWriteDataInfo);
                log.error("delete back write purchase plan amount id = {}, productionOrderBalanceWriteDataInfo = {}", id, productionOrderBalanceWriteDataInfo);
            }
        }
        return new RuleExecuteResult(paramMap);
    }
}
