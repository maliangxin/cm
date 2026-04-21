package com.yonyoucloud.fi.cmp.payapplicationbill.rule.business;

import com.google.common.collect.Maps;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.uap.billcode.BillCodeContext;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.payapplicationbill.*;
import com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.service.OapProcessService;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.CmpBillCodeMappingConfUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.fi.cmp.util.business.BillCopyCheckService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.MerchantDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>保存付款申请单之前的规则</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-15 16:38
 */
@Slf4j
@Component("beforeSavePayApplyBillRule")
public class BeforeSavePayApplyBillRule extends AbstractCommonRule {
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    BillCopyCheckService billCopyCheckService;

    @Autowired
    private OapProcessService oapProcessService;

    @Autowired
    VendorQueryService vendorQueryService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        billContext.getBillnum();
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101503"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180691","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
        }
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006A7", "在财务新架构环境下，不允许新增付款申请单。") /* "在财务新架构环境下，不允许新增付款申请单。" */);
        }
        assert bills != null;
        for (BizObject e : bills) {
            if ("copy".equals(e.get("actionType"))) {
                copyCheck(e);
            }
            if (EntityStatus.Insert.equals(e.getEntityStatus()) || EntityStatus.Update.equals(e.getEntityStatus())) {
                String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
                List<BizObject> linesCheck = (List) e.get(childrenFieldCheck);
                if (ValueUtils.isNotEmptyObj(linesCheck)) {
                    Map<Long,BigDecimal> balanceDBMap = new HashMap<>();
                    if (EntityStatus.Update.equals(e.getEntityStatus())) {
                        BizObject bizObject = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, e.getId(), 2);
                        List<BizObject> bizObjectDB = bizObject.get(childrenFieldCheck);
                        for (BizObject sub : bizObjectDB) {
                            balanceDBMap.put(sub.getId(), sub.getBigDecimal("paymentApplyAmount"));
                        }
                    }
                    BigDecimal oriSumLines = BigDecimal.ZERO;
                    for (BizObject line : linesCheck) {
                        EntityStatus lineStatus = line.getEntityStatus();
                        if (EntityStatus.Delete.equals(lineStatus)){
                            balanceDBMap.keySet().removeIf(key -> line.getId().equals(key));
                        } else {
                            //原币金额
                            BigDecimal oriSum = line.get("paymentApplyAmount");
                            BigDecimal unpaidAmount = line.get("unpaidAmount");
                            if (!ValueUtils.isNotEmptyObj(oriSum)) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101504"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180699","付款申请金额不能为空！") /* "付款申请金额不能为空！" */);
                            }
                            if (oriSum.compareTo(BigDecimal.ZERO) <= 0) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101505"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418069E","付款申请金额不能小于等于0！") /* "付款申请金额不能小于等于0！" */);
                            }
                            if (oriSum.compareTo(unpaidAmount) != 0) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101506"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418069F","付款申请金额与未付金额不匹配!") /* "付款申请金额与未付金额不匹配!" */);
                            }
                            oriSumLines = BigDecimalUtils.safeAdd(oriSum, oriSumLines);
                            balanceDBMap.put(line.getId(), line.getBigDecimal("paymentApplyAmount"));
                        }
                    }
                    BigDecimal oriSumHead = e.get("paymentApplyAmountSum");
                    if (EntityStatus.Insert.equals(e.getEntityStatus())){
                        if (ValueUtils.isNotEmptyObj(oriSumHead) && oriSumHead.compareTo(oriSumLines) != 0) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101507"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806A0","单据的表头金额与明细金额总和不相等！") /* "单据的表头金额与明细金额总和不相等！" */);
                        }
                    } else if (EntityStatus.Update.equals(e.getEntityStatus())) {
                        Optional<BigDecimal> optionalBalance = balanceDBMap.values().stream().reduce(BigDecimalUtils::safeAdd);
                        if (optionalBalance.isPresent()) {
                            BigDecimal lineSum = optionalBalance.get();
                            if (ValueUtils.isNotEmptyObj(oriSumHead) && oriSumHead.compareTo(lineSum) != 0) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101507"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806A0","单据的表头金额与明细金额总和不相等！") /* "单据的表头金额与明细金额总和不相等！" */);
                            }
                        }
                    }
                }
            }
            validFromOpenApi(billContext, e, billDataDto);
            List<PayApplicationBill_b> payApplicationBillbes = e.get("payApplicationBill_b");
            if (ValueUtils.isNotEmptyObj(payApplicationBillbes)) {
                if (null != e.get("srcitem") && (e.get("srcitem").equals(SourceMatters.PurchaseOrderMaterial.getValue())
                        || e.get("srcitem").equals(SourceMatters.MattersDue.getValue())
                        || e.get("srcitem").equals(SourceMatters.PurchaseOrderPlan.getValue())
                        || e.get("srcitem").equals(SourceMatters.OutsourcingOrderProductLine.getValue())
                        || e.get("srcitem").equals(SourceMatters.OutsourcingOrderAll.getValue())
                        || e.get("srcitem").equals(SourceMatters.MattersDueInit.getValue())
                )) {
                    List<Long> ids = new ArrayList<>();
                    List<Long> primaryIdes = new ArrayList<>();
                    for (int i = 0; i < payApplicationBillbes.size(); i++) {
                        PayApplicationBill_b payApplicationBillb = payApplicationBillbes.get(i);
                        ids.add(payApplicationBillb.getSourceautoid());
                        Long sourceid = payApplicationBillb.getSourceid();
                        if (!primaryIdes.contains(sourceid)) {
                            primaryIdes.add(sourceid);
                        }
                    }

                    if (e.get("srcitem").equals(SourceMatters.PurchaseOrderMaterial.getValue())) {
                        checkPurchaseorder(payApplicationBillbes, ids, primaryIdes);
                    }
                    if (e.get("srcitem").equals(SourceMatters.MattersDue.getValue()) || e.get("srcitem").equals(SourceMatters.MattersDueInit.getValue())) {
                        checkOapDetails(payApplicationBillbes, ids, primaryIdes, paramMap,e);
                    }
                    // 采购计划明细行推付款申请保存前金额校验
                    if (e.get("srcitem").equals(SourceMatters.PurchaseOrderPlan.getValue())) {
                        checkPurchasePlanLine(payApplicationBillbes, ids, primaryIdes, paramMap, e);
                    }

                    if (e.get("srcitem").equals(SourceMatters.OutsourcingOrderAll.getValue()) || e.get("srcitem").equals(SourceMatters.OutsourcingOrderProductLine.getValue())) {
                        boolean srcItemFlag = e.get("srcitem").equals(SourceMatters.OutsourcingOrderAll.getValue());
                        checkProductionOrderFullBill(payApplicationBillbes, ids, srcItemFlag, e);
                    }
                }
            }
            if (PaymentObject.Supplier.getValue() == Short.parseShort(e.get("caobject").toString())) {
                if (e.get("supplier") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101508"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418067E","收付款对象类型为供应商，供应商不能为空！") /* "收付款对象类型为供应商，供应商不能为空！" */);
                }
                e.set(IBussinessConstant.CUSTOMER, null);
                e.set("customerbankaccount", null);
                e.set("employee", null);
            }
            if (PaymentObject.Customer.getValue() == Short.parseShort(e.get("caobject").toString())) {
                if (e.get(IBussinessConstant.CUSTOMER) == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101509"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180684","收付款对象类型为客户，客户不能为空！") /* "收付款对象类型为客户，客户不能为空！" */);
                }
                e.set("supplier", null);
                e.set("supplierbankaccount", null);
                e.set("employee", null);
            }
            if (PaymentObject.Employee.getValue() == Short.parseShort(e.get("caobject").toString())) {
                if (e.get("employee") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101510"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418068B","收付款对象类型为员工，员工不能为空！") /* "收付款对象类型为员工，员工不能为空！" */);
                }
                e.set(IBussinessConstant.CUSTOMER, null);
                e.set("customerbankaccount", null);
                e.set("supplier", null);
                e.set("supplierbankaccount", null);
            }
            if (PaymentObject.Other.getValue() == Short.parseShort(e.get("caobject").toString())) {
                e.set(IBussinessConstant.CUSTOMER, null);
                e.set("customerbankaccount", null);
                e.set("supplier", null);
                e.set("supplierbankaccount", null);
                e.set("employee", null);
                e.set("staffbankaccount", null);
            }
            short payBillStatus = Short.parseShort(e.get("payBillStatus").toString());
            short approvalStatus = Short.parseShort(e.get("approvalStatus").toString());
            if (payBillStatus == PayBillStatus.Open.getValue()) {
                e.set("payBillStatus", PayBillStatus.Auditing.getValue());
            }
            if (approvalStatus == ApprovalStatus.Free.getValue()) {
                e.set("approvalStatus", ApprovalStatus.Approving.getValue());
            }
            if (!ValueUtils.isNotEmptyObj(e.get("sourceSystem"))) {
                e.set("sourceSystem", 4);
            }
            if (!ValueUtils.isNotEmptyObj(e.get("srcitem"))) {
                e.set("srcitem", 5);
            }
            List<Map<String, Object>> listMap = new ArrayList<>(CONSTANT_EIGHT);
            // 当来源单据为应付事项时，组装回写金额参数
            if (Short.parseShort(e.get("srcitem").toString()) == SourceMatters.MattersDue.getValue()) {
                assemblingBackWriteParameter(paramMap, e, listMap);
            }
            log.error("Pay apply bill save or update bill ,BizObject = {}", JsonUtils.toJson(e));
        }
        return new RuleExecuteResult(paramMap);
    }

    private static void assemblingBackWriteParameter(Map<String, Object> paramMap, BizObject e, List<Map<String, Object>> listMap) {
        if (EntityStatus.Update.equals(e.getEntityStatus())) {
            if (ValueUtils.isNotEmptyObj(e.get("payApplicationBill_b"))) {
                List<Map<String, Object>> payApplyBillChildList = e.get("payApplicationBill_b");
                for (Map<String, Object> iter : payApplyBillChildList) {
                    log.error("Pay apply bill save or update bill write back purchase ,id = {}, payApplicationBill_b = {} ", iter.get("id"), iter);
                    Map<String, Object> dataMapUpdate = new HashMap<>(CONSTANT_EIGHT);
                    Object sourceOrderType = iter.get("sourceOrderType");
                    if (ValueUtils.isNotEmptyObj(sourceOrderType) && (Short.parseShort(sourceOrderType.toString()) == SourceOrderType.OutsourcedOrder.getValue() ||
                            Short.parseShort(sourceOrderType.toString()) == SourceOrderType.PurchaseOrder.getValue())) {
                        if ("Update".equals(iter.get("_status").toString())) {
                            PayApplicationBill_b payApplicationBillB;
                            try {
                                payApplicationBillB = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, iter.get("id"), 3);
                            } catch (Exception ex) {
                                log.error(ex.getMessage());
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101511"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001CC", "查询付款单异常") /* "查询付款单异常" */);
                            }
                            if (new BigDecimal(iter.get("paymentApplyAmount").toString()).compareTo(payApplicationBillB.getPaymentApplyAmount()) != 0) {
                                dataMapUpdate.put("paymentApplyAmount", BigDecimalUtils.safeSubtract(new BigDecimal(iter.get("paymentApplyAmount").toString()), payApplicationBillB.getPaymentApplyAmount()));
                            }
                        } else if ("Insert".equals(iter.get("_status").toString())) {
                            dataMapUpdate.put("paymentApplyAmount", new BigDecimal(iter.get("paymentApplyAmount").toString()));
                        } else if ("Delete".equals(iter.get("_status").toString())) {
                            dataMapUpdate.put("paymentApplyAmount", BigDecimalUtils.safeSubtract(BigDecimal.ZERO, new BigDecimal(iter.get("paymentApplyAmount").toString())));
                        }
                        if (Short.parseShort(sourceOrderType.toString()) == SourceOrderType.OutsourcedOrder.getValue()) {
                            dataMapUpdate.put("orderId", ValueUtils.isNotEmptyObj(iter.get("topsrcbillitemid")) ? iter.get("topsrcbillitemid").toString() : null);
                        } else if (Short.parseShort(sourceOrderType.toString()) == SourceOrderType.PurchaseOrder.getValue()) {
                            dataMapUpdate.put("polineId", ValueUtils.isNotEmptyObj(iter.get("topsrcbillitemid")) ? iter.get("topsrcbillitemid").toString() : null);
                        }
                        //发票ID
                        dataMapUpdate.put("lineId", ValueUtils.isNotEmptyObj(iter.get("srcinvoicebillitemid")) ? iter.get("srcinvoicebillitemid").toString() : null);
                        listMap.add(dataMapUpdate);
                    }
                }
            }
        } else if (EntityStatus.Insert.equals(e.getEntityStatus())) {
            List<Map<String, Object>> payApplyBillChildList = e.get("payApplicationBill_b");
            for (Map<String, Object> iter : payApplyBillChildList) {
                Map<String, Object> dataMapInsert = new HashMap<>(CONSTANT_EIGHT);
                dataMapInsert.put("paymentApplyAmount", new BigDecimal(iter.get("paymentApplyAmount").toString()));
                Object sourceOrderType = iter.get("sourceOrderType");
                if (ValueUtils.isNotEmptyObj(sourceOrderType) && (Short.parseShort(sourceOrderType.toString()) == SourceOrderType.OutsourcedOrder.getValue() ||
                        Short.parseShort(sourceOrderType.toString()) == SourceOrderType.PurchaseOrder.getValue())) {
                    if (Short.parseShort(sourceOrderType.toString()) == SourceOrderType.OutsourcedOrder.getValue()) {
                        dataMapInsert.put("orderId", ValueUtils.isNotEmptyObj(iter.get("topsrcbillitemid")) ? iter.get("topsrcbillitemid").toString() : null);
                    } else if (Short.parseShort(sourceOrderType.toString()) == SourceOrderType.PurchaseOrder.getValue()) {
                        dataMapInsert.put("polineId", ValueUtils.isNotEmptyObj(iter.get("topsrcbillitemid")) ? iter.get("topsrcbillitemid").toString() : null);
                    }
                }
                //发票ID
                dataMapInsert.put("lineId", ValueUtils.isNotEmptyObj(iter.get("srcinvoicebillitemid")) ? iter.get("srcinvoicebillitemid").toString() : null);
                listMap.add(dataMapInsert);
            }
        } else if (EntityStatus.Delete.equals(e.getEntityStatus())) {
            Long id = e.getId();
            PayApplicationBill payApplicationBill;
            try {
                payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, id, 3);
            } catch (Exception ex) {
                log.error(ex.getMessage());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101511"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001CC", "查询付款单异常") /* "查询付款单异常" */);
            }
            if (ValueUtils.isNotEmptyObj(payApplicationBill)) {
                List<PayApplicationBill_b> payApplicationBillBs = payApplicationBill.payApplicationBill_b();
                for (PayApplicationBill_b payApplicationBillB : payApplicationBillBs) {
                    Map<String, Object> dataMapDelete = new HashMap<>(CONSTANT_EIGHT);
                    Short sourceOrderType = payApplicationBillB.getSourceOrderType();
                    dataMapDelete.put("paymentApplyAmount", payApplicationBillB.get("paymentApplyAmount"));
                    if (Short.parseShort(sourceOrderType.toString()) == SourceOrderType.OutsourcedOrder.getValue() ||
                            Short.parseShort(sourceOrderType.toString()) == SourceOrderType.PurchaseOrder.getValue()) {
                        if (Short.parseShort(sourceOrderType.toString()) == SourceOrderType.OutsourcedOrder.getValue()) {
                            dataMapDelete.put("orderId", ValueUtils.isNotEmptyObj(payApplicationBillB.getTopsrcbillitemid()) ? payApplicationBillB.getTopsrcbillitemid() : null);
                        } else if (Short.parseShort(sourceOrderType.toString()) == SourceOrderType.PurchaseOrder.getValue()) {
                            dataMapDelete.put("polineId", ValueUtils.isNotEmptyObj(payApplicationBillB.getTopsrcbillitemid()) ? payApplicationBillB.getTopsrcbillitemid() : null);
                        }
                    }
                    dataMapDelete.put("lineId", ValueUtils.isNotEmptyObj(payApplicationBillB.getSrcinvoicebillitemid()) ? payApplicationBillB.getSrcinvoicebillitemid() : null);
                    listMap.add(dataMapDelete);
                }
            }
        }
        log.error("Pay apply bill save or update bill write back purchase ,billId = {},  payApplyBillOriSum = {}", e.getId(), listMap);
        paramMap.put("payApplyBillOriSum", listMap);
        e.put("listMap", listMap);
    }

    private void checkProductionOrderFullBill(List<PayApplicationBill_b> payApplicationBillbes, List<Long> ids, boolean srcItemFlag, BizObject biz) throws Exception {
        List<Map<String, Object>> orderSubcontractList;
        final CtmJSONObject map = new CtmJSONObject();
        if (srcItemFlag) {
            orderSubcontractList = queryDataByFullNameAndDomain("po.order.Subcontract", "productionorder", ids, "id,totalMoneyFC as balance,requestedPaymentTC");
            orderSubcontractList.forEach(t -> map.put(t.get(PRIMARY_ID).toString(), BigDecimalUtils.safeSubtract(
                    t.get("balance") != null ? new BigDecimal(t.get("balance").toString()) : BigDecimal.ZERO,
                    t.get("requestedPaymentTC") != null ? new BigDecimal(t.get("requestedPaymentTC").toString()) : BigDecimal.ZERO)
            ));
        } else {
            Map<String, Object> mainMap = new HashMap<>();
            mainMap.put("code", biz.get("code"));
            Map<String, Object> params = new HashMap<>(CONSTANT_EIGHT);
            params.put(PAYMENT_TYPE, REQUESTED_PAYMENT);
            params.put(PRODUCT_IDS, ids);
            // 发送请求，获取可付款申请金额
            CtmJSONObject json = oapProcessService.queryOutsourcingOrderRequestedAmount(mainMap, params);
            map.putAll(json);
        }
        if (!ValueUtils.isNotEmptyObj(map)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101512"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806A2","委外订单数据已经不存在了，单据已删除或上游单据已弃审，请检查数据！") /* "委外订单数据已经不存在了，单据已删除或上游单据已弃审，请检查数据！" */);
        }
        log.error("verify oap data, map = {}", JsonUtils.toJson(map));
        List<String> childIds = payApplicationBillbes.stream().map(e -> e.get("id").toString()).collect(Collectors.toList());

        Map<Object, Map<String, Object>> childDataMap = new HashMap<>();
        try {
            List<Map<String, Object>> mapList = MetaDaoHelper.queryByIds(PayApplicationBill_b.ENTITY_NAME, "id, sourceid, sourceautoid, paymentApplyAmount", childIds);
            for (Map<String, Object> objectMap : mapList) {
                childDataMap.put(objectMap.get("id"), objectMap);
            }
        } catch (Exception exception) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101513"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180673","修改数据异常，请稍后重试！") /* "修改数据异常，请稍后重试！" */);
        }
        payApplicationBillbes.forEach(p -> {
            if (p.getEntityStatus().name().equals(UPDATE)) {
                Map<String, Object> objectMap = childDataMap.get(p.getId());
                String sourceAutoId = objectMap.get("sourceautoid").toString();
                BigDecimal paymentApplyAmount = new BigDecimal(objectMap.get("paymentApplyAmount").toString());
                if (p.getPaymentApplyAmount().compareTo(paymentApplyAmount) > 0) {
                    BigDecimal oapBalance = new BigDecimal(map.get(sourceAutoId).toString());
                    BigDecimal subtractAmount = BigDecimalUtils.safeAdd(oapBalance, paymentApplyAmount);
                    if (subtractAmount.compareTo(p.getPaymentApplyAmount()) < 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101514"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180689","付款申请金额大于允许的最大金额") /* "付款申请金额大于允许的最大金额" */);
                    }
                }
            }
            if (p.getEntityStatus().name().equals(INSERT)) {
                if (p.getPaymentApplyAmount().compareTo(new BigDecimal(map.get(p.getSourceautoid().toString()).toString())) > 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101514"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180689","付款申请金额大于允许的最大金额") /* "付款申请金额大于允许的最大金额" */);
                }
            }
        });
    }

    /**
     * 整单复制功能跳转过来的保存请求，做字段启用性校验
     *
     * @param bizObject
     * @throws Exception
     */
    private void copyCheck(BizObject bizObject) throws Exception {
        //整单复制功能过来的保存，需进行字段的启用有效性校验
        if ("copy".equals(bizObject.get("actionType"))) {
            //会计主体-必输
            if (bizObject.get(IBussinessConstant.ACCENTITY) != null) {
                List<Map<String, Object>> accentityList = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(bizObject.get(IBussinessConstant.ACCENTITY));
                if (!CollectionUtils.isEmpty(accentityList)) {
                    Object accEntityFlag = accentityList.get(0).get("stopstatus");
                    if (accEntityFlag != null && "1".equals(accEntityFlag.toString())) {//0是启用，1是未启用
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101515"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418067C","会计主体未启用，保存失败！") /* "会计主体未启用，保存失败！" */);
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101516"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418067F","未查询到对应的会计主体，保存失败！") /* "未查询到对应的会计主体，保存失败！" */);
                }
            }
            //付款申请组织-必输
            if (bizObject.get("org") != null) {
                List<Map<String, Object>> orgMVList = QueryBaseDocUtils.getOrgMVById(bizObject.get("org"));
                if (!CollectionUtils.isEmpty(orgMVList)) {
                    Object accEntityFlag = orgMVList.get(0).get("stopstatus");
                    if (accEntityFlag != null && "1".equals(accEntityFlag.toString())) {//0是启用，1是未启用
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101517"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180686","付款申请组织未启用，保存失败！") /* "付款申请组织未启用，保存失败！" */);
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101518"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180688","未查询到对应的付款申请组织，保存失败！") /* "未查询到对应的付款申请组织，保存失败！" */);
                }
            }
            //交易类型-必输
            if (bizObject.get("tradetype") != null) {
                Map tradeTypeMap = QueryBaseDocUtils.queryTransTypeById(bizObject.get("tradetype"));
                if (tradeTypeMap != null && tradeTypeMap.size() > 0) {
                    Object tradeTypeFlag = tradeTypeMap.get("enable");
                    if (tradeTypeFlag != null && "2".equals(tradeTypeFlag.toString())) {//1是启用，2是未启用
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101519"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418068D","交易类型未启用，保存失败！") /* "交易类型未启用，保存失败！" */);
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101520"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418068E","未查询到对应的交易类型，保存失败！") /* "未查询到对应的交易类型，保存失败！" */);
                }
            }
            //结算方式-必输
            if (bizObject.get("settlemode") != null) {
                SettleMethodModel settleModeMap = baseRefRpcService.querySettleMethodsById(bizObject.get("settlemode").toString());
                if (settleModeMap != null) {
                    Object settleModeFlag = settleModeMap.getIsEnabled();
                    if (settleModeFlag != null && !(boolean) settleModeFlag) {//true是启用，false是未启用
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101521"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180693","结算方式未启用，保存失败！") /* "结算方式未启用，保存失败！" */);
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101522"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180694","未查询到对应的结算方式，保存失败！") /* "未查询到对应的结算方式，保存失败！" */);
                }
            }
            //币种-必输
            if (bizObject.get("currency") != null) {
                CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(bizObject.get("currency"));
                if (currencyTenantDTO != null) {
                    Object currencyFlag = currencyTenantDTO.getEnable();
                    if (currencyFlag != null && "2".equals(currencyFlag.toString())) {//1是启用，2是未启用
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101523"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180697","币种未启用，保存失败！") /* "币种未启用，保存失败！" */);
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101524"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180698","未查询到对应的币种，保存失败！") /* "未查询到对应的币种，保存失败！" */);
                }
            }
            //收款客户-非必输
            if (bizObject.get(IBussinessConstant.CUSTOMER) != null) {
                MerchantDTO merchantByIdAndOrg = QueryBaseDocUtils.getMerchantByIdAndOrg(bizObject.get(IBussinessConstant.CUSTOMER), bizObject.get(IBussinessConstant.ACCENTITY));
                if (merchantByIdAndOrg != null) {
                    if (merchantByIdAndOrg.getDetailStopStatus().booleanValue()) {//0是启用，1是未启用
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101525"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418069A","收款客户未启用，保存失败！") /* "收款客户未启用，保存失败！" */);
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101526"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418069D","未查询到对应的收款客户，保存失败！") /* "未查询到对应的收款客户，保存失败！" */);
                }
            }
            if (bizObject.get("customerbankaccount") != null) {
                List<AgentFinancialDTO> customerBankAccountMap = QueryBaseDocUtils.queryCustomerBankAccountById(bizObject.getLong("customerbankaccount"));
                if (customerBankAccountMap != null && customerBankAccountMap.size() > 0) {
                    Boolean stopstatus = customerBankAccountMap.get(0).getStopStatus();
                    if (stopstatus) {//0是启用，1是未启用
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101527"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806A1","收款客户账户未启用，保存失败！") /* "收款客户账户未启用，保存失败！" */);
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101528"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806A3","未查询到对应的收款客户账户，保存失败！") /* "未查询到对应的收款客户账户，保存失败！" */);
                }
            }
            //供应商银行账户没有校验
            if (bizObject.get("supplierbankaccount") != null) {
                VendorBankVO bankAccount = vendorQueryService.getVendorBanksByAccountId(bizObject.get("supplierbankaccount"));
                if (bankAccount != null) {
                    if (null != bankAccount.getStopstatus()) {
                        Boolean supplierFlag = (Boolean) bankAccount.get("stopstatus");
                        if (supplierFlag) {//false是启用，true是未启用
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101529"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806A5","供应商账户未启用，保存失败！") /* "供应商账户未启用，保存失败！" */);
                        }
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101530"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806A7","未查询到对应的供应商账户，保存失败！") /* "未查询到对应的供应商账户，保存失败！" */);
                }
            }
            //供应商-非必输---平台接口在日常暂时遗留
            Map<String,Integer> cacheMap = new HashMap<>();
            billCopyCheckService.checkSupplier(bizObject.get("supplier"), bizObject.get(IBussinessConstant.ACCENTITY),cacheMap);
            cacheMap.clear();
            //部门-非必输
            if (bizObject.get("dept") != null) {
                List<Map<String, Object>> deptList = QueryBaseDocUtils.queryDeptById(bizObject.get("dept"));
                if (!CollectionUtils.isEmpty(deptList)) {
                    Object stopstatus = deptList.get(0).get("stopstatus");
                    if (stopstatus != null && "1".equals(stopstatus.toString())) {//0是启用，1是未启用
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101531"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180695","部门未启用，保存失败！") /* "部门未启用，保存失败！" */);
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101532"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180696","未查询到对应的部门，保存失败！") /* "未查询到对应的部门，保存失败！" */);
                }
            }
            //员工？？？？员工没有启用状态？？？？，先不进行校验
            //业务员？？？？业务员没有启用状态？？？？，先不进行校验
            //项目-非必输
            if (bizObject.get("project") != null) {
                List<Map<String, Object>> projectList = QueryBaseDocUtils.queryProjectById(bizObject.get("project"));
                if (!CollectionUtils.isEmpty(projectList)) {
                    Object projectFlag = projectList.get(0).get("enable");
                    if (projectFlag != null && "2".equals(projectFlag.toString())) {//1是启用，2是未启用
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101533"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180675","项目未启用，保存失败！") /* "项目未启用，保存失败！" */);
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101534"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180678","未查询到对应的项目，保存失败！") /* "未查询到对应的项目，保存失败！" */);
                }
            }
            //费用项目-非必输
            if (bizObject.get("expenseitem") != null) {
                List<Map<String, Object>> expenseItemList = QueryBaseDocUtils.queryExpenseItemById(bizObject.get("expenseitem"));
                if (!CollectionUtils.isEmpty(expenseItemList)) {
                    Object expenseItemFlag = expenseItemList.get(0).get("enabled");
                    if (expenseItemFlag != null && !(boolean) expenseItemFlag) {//true是启用，false是未启用
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101535"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180680","费用项目未启用，保存失败！") /* "费用项目未启用，保存失败！" */);
                    }
                    //判断是否勾选财资服务
                    // Object propertyBusinessFlag = expenseItemList.get(0).get("propertybusiness");
                    // if (propertyBusinessFlag != null && !propertyBusinessFlag.equals("1")){ // 1为勾选，其他为没勾选
                    //     throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101536"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1613096893618323459") /* "所选费用项目中存在未启用财资业务领域的情况，保存失败！" */);
                    // }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101537"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180685","未查询到对应的费用项目，保存失败！") /* "未查询到对应的费用项目，保存失败！" */);
                }
            }

            if (null != bizObject.get("payApplicationBill_b")) {
                List<PayApplicationBill_b> payApplicationBill_bList = bizObject.getBizObjects("payApplicationBill_b", PayApplicationBill_b.class);
                if (!CollectionUtils.isEmpty(payApplicationBill_bList)) {
                    for (PayApplicationBill_b payApplicationBill_b : payApplicationBill_bList) {
                        //款项类型
                        if (payApplicationBill_b.getQuickType() != null) {
                            Map<String, Object> condition = new HashMap<String, Object>();
                            condition.put("id", payApplicationBill_b.getQuickType());
                            List<Map<String, Object>> payQuickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
                            if (!CollectionUtils.isEmpty(payQuickType)) {
                                Map<String, Object> payQuickTypeMap = payQuickType.get(0);
                                if (null != payQuickTypeMap.get("stopstatus")) {
                                    Boolean stopstatus = (Boolean) payQuickTypeMap.get("stopstatus");
                                    if (stopstatus) {//0是启用，1是未启用
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101538"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180690","款项类型未启用，保存失败！") /* "款项类型未启用，保存失败！" */);
                                    }
                                }
                            } else {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101539"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180692","未查询到对应的款项类型，保存失败！") /* "未查询到对应的款项类型，保存失败！" */);
                            }
                        }
                        //部门-非必输
                        if (payApplicationBill_b.getDept() != null) {
                            List<Map<String, Object>> deptList = QueryBaseDocUtils.queryDeptById(payApplicationBill_b.getDept());
                            if (!CollectionUtils.isEmpty(deptList)) {
                                Object stopstatus = deptList.get(0).get("stopstatus");
                                if (stopstatus != null && "1".equals(stopstatus.toString())) {//0是启用，1是未启用
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101531"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180695","部门未启用，保存失败！") /* "部门未启用，保存失败！" */);
                                }
                            } else {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101532"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180696","未查询到对应的部门，保存失败！") /* "未查询到对应的部门，保存失败！" */);
                            }
                        }
                        //费用项目-非必输
                        if (payApplicationBill_b.getExpenseitem() != null) {
                            List<Map<String, Object>> expenseItemList = QueryBaseDocUtils.queryExpenseItemById(payApplicationBill_b.getExpenseitem());
                            if (!CollectionUtils.isEmpty(expenseItemList)) {
                                Object expenseItemFlag = expenseItemList.get(0).get("enabled");
                                if (expenseItemFlag != null && !(boolean) expenseItemFlag) {//true是启用，false是未启用
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101535"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180680","费用项目未启用，保存失败！") /* "费用项目未启用，保存失败！" */);
                                }
                                //判断是否勾选财资服务
//                                Object propertyBusinessFlag = expenseItemList.get(0).get("propertybusiness");
//                                if (propertyBusinessFlag != null && !propertyBusinessFlag.equals("1")){ // 1为勾选，其他为没勾选
//                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101540"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1613096893618323459") /* "所选费用项目中存在未启用财资业务领域的情况，保存失败！" */);
//                                }
                            } else {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101537"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180685","未查询到对应的费用项目，保存失败！") /* "未查询到对应的费用项目，保存失败！" */);
                            }
                        }
                        //项目-非必输
                        if (payApplicationBill_b.getProject() != null) {
                            List<Map<String, Object>> projectList = QueryBaseDocUtils.queryProjectById(payApplicationBill_b.getProject());
                            if (!CollectionUtils.isEmpty(projectList)) {
                                Object projectFlag = projectList.get(0).get("enable");
                                if (projectFlag != null && "2".equals(projectFlag.toString())) {//1是启用，2是未启用
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101533"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180675","项目未启用，保存失败！") /* "项目未启用，保存失败！" */);
                                }
                            } else {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101534"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180678","未查询到对应的项目，保存失败！") /* "未查询到对应的项目，保存失败！" */);
                            }
                        }
                        //付款申请组织-必输
                        if (payApplicationBill_b.getOrg() != null) {
                            List<Map<String, Object>> orgMVList = QueryBaseDocUtils.getOrgMVById(payApplicationBill_b.getOrg());
                            if (!CollectionUtils.isEmpty(orgMVList)) {
                                Object accEntityFlag = orgMVList.get(0).get("stopstatus");
                                if (accEntityFlag != null && "1".equals(accEntityFlag.toString())) {//0是启用，1是未启用
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101517"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180686","付款申请组织未启用，保存失败！") /* "付款申请组织未启用，保存失败！" */);
                                }
                            } else {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101518"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180688","未查询到对应的付款申请组织，保存失败！") /* "未查询到对应的付款申请组织，保存失败！" */);
                            }
                        }
                    }
                }
            }

        }
    }

    private void validFromOpenApi(BillContext billContext, BizObject bizObject,BillDataDto billDataDto) throws Exception {
        if ((bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true)) || billDataDto.getFromApi()) {
            String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
            List<BizObject> linesCheck = (List) bizObject.get(childrenFieldCheck);
            accountValid(bizObject, linesCheck);
            if (bizObject.get("vouchdate") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101541"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806A8","单据日期不能为空！") /* "单据日期不能为空！" */);
            }
            if (bizObject.get("billtype") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101542"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806A9","单据类型不能为空！") /* "单据类型不能为空！" */);
            }
            if (bizObject.get("org") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101543"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806AB","付款申请组织不能为空！") /* "付款申请组织不能为空！" */);
            }
            if (bizObject.get("tradetype") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101544"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806AD","交易类型不能为空！") /* "交易类型不能为空！" */);
            }

            if (bizObject.get("currency") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101545"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180672","币种不能为空！") /* "币种不能为空！" */);
            }
            if (bizObject.get("natCurrency") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101546"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180674","本币币种不能为空！") /* "本币币种不能为空！" */);
            }
            if (bizObject.get(IBussinessConstant.ACCENTITY) == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101547"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180676","会计主体不能为空！") /* "会计主体不能为空！" */);
            }
            if (bizObject.get("caobject") == null || !(bizObject.getShort("caobject") == 1 || bizObject.getShort("caobject") == 2 || bizObject.getShort("caobject") == 3 || bizObject.getShort("caobject") == 4)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101548"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418067A","付款对象出错！") /* "付款对象出错！" */);
            }
            if (bizObject.get("supplier") != null && bizObject.get(IBussinessConstant.CUSTOMER) != null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101549"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418067B","供应商与客户不能同时存在！") /* "供应商与客户不能同时存在！" */);
            }
            if (bizObject.get("supplier") != null && bizObject.get("employee") != null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101550"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418067D","供应商与员工不能同时存在！") /* "供应商与员工不能同时存在！" */);
            }
            if (bizObject.get(IBussinessConstant.CUSTOMER) != null && bizObject.get("employee") != null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101551"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180681","客户与员工不能同时存在！") /* "客户与员工不能同时存在！" */);
            }
            short billtype = Short.parseShort(bizObject.get("billtype").toString());
            if (billtype != 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101552"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180683","单据类型不存在！") /* "单据类型不存在！" */);
            }
            short srcitem = Short.parseShort(bizObject.get("srcitem").toString());
            if (srcitem != 1 &&
                    srcitem != 2 &&
                    srcitem != 3 &&
                    srcitem != 4 &&
                    srcitem != 5 &&
                    srcitem != 6) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101553"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418068A","来源事项不存在！") /* "来源事项不存在！" */);
            }
            short sourceSystem = Short.parseShort(bizObject.get("sourceSystem").toString());
            if (sourceSystem != 1 &&
                    sourceSystem != 2 &&
                    sourceSystem != 3 &&
                    sourceSystem != 4 &&
                    sourceSystem != 5 &&
                    sourceSystem != 6) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101554"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418068F","来源系统不存在！") /* "来源系统不存在！" */);
            }
            if (EntityStatus.Insert.equals(bizObject.getEntityStatus())) {
                CmpCommonUtil.checkPayBillExist(UPCODE, bizObject.get(UPCODE), bizObject.getEntityName());

                IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
                BillCodeContext billCodeContext = billCodeComponentService.getBillCodeContext(CmpBillCodeMappingConfUtils.getBillCode("cmp_payapplicationbill"),
                        InvocationInfoProxy.getTenantid(), "CM", String.valueOf(bizObject.get("org").toString()),
                        false, new BillCodeObj(bizObject));
                Integer billnumMode = billCodeContext.getBillnumMode();
                if (billnumMode == 1) {
                    String billCode = billCodeComponentService.getBillCode(CmpBillCodeMappingConfUtils.getBillCode("cmp_payapplicationbill"),
                            PayApplicationBill.ENTITY_NAME, InvocationInfoProxy.getTenantid(), "CM",
                            true, String.valueOf(bizObject.get("org").toString()),
                            false, new BillCodeObj(bizObject));
                    bizObject.put("code", billCode);
                }
            }

            bizObject.put("payBillStatus", 2);
            bizObject.put("approvalStatus", 2);
            bizObject.put("status", 0);
            bizObject.put("verifystate", 0);
        }
    }

    private void accountValid(BizObject e, List<BizObject> linesCheck) throws Exception {
        if (ValueUtils.isNotEmptyObj(linesCheck) && e.getEntityStatus().name().equals("Insert")) {
            BigDecimal oriSumLines = BigDecimal.ZERO;
            for (BizObject line : linesCheck) {
                //原币金额
                BigDecimal oriSum = line.get("paymentApplyAmount");
                if (!ValueUtils.isNotEmptyObj(oriSum)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101504"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180699","付款申请金额不能为空！") /* "付款申请金额不能为空！" */);
                }
                if (line.get("quickType") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101555"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418069B","款项类型不能为空！") /* "款项类型不能为空！" */);
                }
                BigDecimal unpaidAmount = line.get("unpaidAmount");
                if (unpaidAmount.compareTo(oriSum) != 0) {
                    line.put("unpaidAmount", oriSum);
                }
                oriSumLines = BigDecimalUtils.safeAdd(oriSum, oriSumLines);
            }
            BigDecimal oriSumHead = e.get("paymentApplyAmountSum");
            if (ValueUtils.isNotEmptyObj(oriSumHead) && oriSumHead.compareTo(oriSumLines) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101507"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806A0","单据的表头金额与明细金额总和不相等！") /* "单据的表头金额与明细金额总和不相等！" */);
            }
            BigDecimal unpaidAmountSum = e.get("unpaidAmountSum");
            if (unpaidAmountSum.compareTo(oriSumHead) != 0) {
                e.put("unpaidAmountSum", oriSumHead);
            }
        }
        if (ValueUtils.isNotEmptyObj(linesCheck) && e.getEntityStatus().name().equals("Update")) {
            BigDecimal paymentApplyAmountSum = new BigDecimal(e.get("paymentApplyAmountSum").toString());
            BigDecimal unpaidAmountSum = ValueUtils.isNotEmptyObj(e.get("unpaidAmountSum")) ?
                    new BigDecimal(e.get("unpaidAmountSum").toString()) : paymentApplyAmountSum;
            if (unpaidAmountSum.compareTo(paymentApplyAmountSum) != 0) {
                e.put("unpaidAmountSum", paymentApplyAmountSum);
            }
            BizObject bizObj = MetaDaoHelper.findById(e.getEntityName(), e.getId());
            if (!ValueUtils.isNotEmptyObj(bizObj)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101556"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806A6","修改的单据不存在！") /* "修改的单据不存在！" */);
            }
            BigDecimal oriSumLines = new BigDecimal(bizObj.get("paymentApplyAmountSum").toString());
            for (BizObject line : linesCheck) {
                //原币金额
                BigDecimal oriSum = new BigDecimal(line.get("paymentApplyAmount").toString());
                if (!ValueUtils.isNotEmptyObj(oriSum)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101504"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180699","付款申请金额不能为空！") /* "付款申请金额不能为空！" */);
                }
                BigDecimal unpaidAmount = new BigDecimal(line.get("unpaidAmount").toString());
                if (unpaidAmount.compareTo(oriSum) != 0) {
                    line.put("unpaidAmount", oriSum);
                }
                if (line.getEntityStatus().name().equals("Insert")) {
                    oriSumLines = BigDecimalUtils.safeAdd(oriSum, oriSumLines);
                } else if (line.getEntityStatus().name().equals("Update")) {
                    if (!ValueUtils.isNotEmptyObj(line.getId())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101557"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806AA","数据修改时，ID不能为空！") /* "数据修改时，ID不能为空！" */);
                    }
                    BizObject currentBill = MetaDaoHelper.findById(line.getEntityName(), line.getId());
                    if (!ValueUtils.isNotEmptyObj(currentBill)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101558"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806AC","修改的数据不存在！") /* "修改的数据不存在！" */);
                    }
                    oriSumLines = BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(oriSum, new BigDecimal(currentBill.get("paymentApplyAmount").toString())), oriSumLines);
                } else if (line.getEntityStatus().name().equals("Delete")) {
                    oriSumLines = BigDecimalUtils.safeSubtract(oriSumLines, oriSum);
                }
            }
            BigDecimal oriSumHead = e.get("paymentApplyAmountSum");
            boolean flag1 = (!ValueUtils.isNotEmptyObj(oriSumHead));
            boolean flag2 = (ValueUtils.isNotEmptyObj(oriSumHead) && (oriSumHead.compareTo(oriSumLines) != 0));
            if (flag1 || flag2) {
                e.put("paymentApplyAmountSum", oriSumLines);
                e.put("unpaidAmountSum", oriSumLines);
            }
        }
    }

    /**
     * <h2>应付事项推付款申请金额上线校验</h2>
     *
     * @param payApplicationBillBs: 付款申请详情数据入参
     * @param ids: 应付事项ID集合
     * @return void
     * @author Sun GuoCai
     * @since 2021/3/25 8:59
     */
    private void checkOapDetails(List<PayApplicationBill_b> payApplicationBillBs, List<Long> ids, List<Long> primaryIdes, Map<String, Object> paramMap, BizObject bizObject) throws Exception {
        List<Map<String, Object>> purchaseOrder = queryDataByFullNameAndDomain(OAPMAIN, FIARAP, primaryIdes, "*");
        purchaseOrder.stream().forEach(purchase -> {
            if (!(null != purchase.get("auditstatus") && String.valueOf(purchase.get("auditstatus")).equals("1"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101559"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180687","应付事项已弃审") /* "应付事项已弃审" */);
            }
        });
        List<Map<String, Object>> oapDetails = queryDataByFullNameAndDomain(OAPDETAIL, FIARAP, ids, "*");
        Map<Long, BigDecimal> map = new HashMap<>(CONSTANT_EIGHT);
        oapDetails.forEach(t -> map.put(Long.valueOf(t.get(PRIMARY_ID).toString()), BigDecimalUtils.safeSubtract(
                t.get(BALANCE) != null ? new BigDecimal(t.get(BALANCE).toString()) : BigDecimal.ZERO,
                t.get(OCCUPYAMOUNT) != null ? new BigDecimal(t.get(OCCUPYAMOUNT).toString()) : BigDecimal.ZERO)
        ));
        if (!ValueUtils.isNotEmptyObj(map)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101560"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180677","应付事项单据已经不存在了，单据已删除或上游单据已弃审，请检查数据！") /* "应付事项单据已经不存在了，单据已删除或上游单据已弃审，请检查数据！" */);
        }
        if(log.isInfoEnabled()) {
            log.info("verify oap data, map = {}", JsonUtils.toJson(map));
        }
        Map<Long, BigDecimal> writeBackMap = new HashMap<>(CONSTANT_EIGHT);
        List<Long> outsourcingOrderSubIdList = new ArrayList<>();
        payApplicationBillBs.forEach(p -> {
            if (p.getEntityStatus().name().equals(UPDATE)) {
                PayApplicationBill_b payApplicationBillB;
                try {
                    payApplicationBillB = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, p.getId());
                } catch (Exception exception) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101513"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180673","修改数据异常，请稍后重试！") /* "修改数据异常，请稍后重试！" */);
                }
                if (ValueUtils.isNotEmptyObj(payApplicationBillB)) {
                    if (p.getPaymentApplyAmount().compareTo(payApplicationBillB.getPaymentApplyAmount()) > 0) {
                        BigDecimal oapBalance = map.get(payApplicationBillB.getSourceautoid());
                        BigDecimal subtractAmount = BigDecimalUtils.safeAdd(oapBalance, payApplicationBillB.getPaymentApplyAmount());
                        if (subtractAmount.compareTo(p.getPaymentApplyAmount()) < 0) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101514"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180689","付款申请金额大于允许的最大金额") /* "付款申请金额大于允许的最大金额" */);
                        }
                    }
                }
            }
            if (p.getEntityStatus().name().equals(INSERT)) {
                if (p.getPaymentApplyAmount().compareTo(map.get(p.getSourceautoid())) > 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101514"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180689","付款申请金额大于允许的最大金额") /* "付款申请金额大于允许的最大金额" */);
                }
            }
            boolean isSourcePurchaseOrder = ValueUtils.isNotEmptyObj(p.get("sourceOrderType"))
                    && Short.parseShort(p.get("sourceOrderType").toString()) == SourceOrderType.PurchaseOrder.getValue();
            if (ValueUtils.isNotEmptyObj(p.getOrderNo()) && isSourcePurchaseOrder) {
                if ("Update".equals(p.getEntityStatus().name())) {
                    try {
                        PayApplicationBill_b payApplicationBillB = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, p.getId(), 3);
                        BigDecimal decimal = BigDecimalUtils.safeSubtract(p.getPaymentApplyAmount(), payApplicationBillB.getPaymentApplyAmount());
                        if (ValueUtils.isNotEmptyObj(writeBackMap.get(Long.valueOf(p.getTopsrcbillitemid())))) {
                            writeBackMap.put(Long.valueOf(p.getTopsrcbillitemid()),
                                    BigDecimalUtils.safeAdd(writeBackMap.get(Long.valueOf(p.getTopsrcbillitemid())), decimal));
                        } else {
                            if (decimal.compareTo(BigDecimal.ZERO) != 0) {
                                writeBackMap.put(Long.valueOf(p.getTopsrcbillitemid()), decimal);
                            }
                        }
                    } catch (Exception ex) {
                        log.error(ex.getMessage());
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101561"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180671","查询付款单异常") /* "查询付款单异常" */);
                    }
                } else if ("Insert".equals(p.getEntityStatus().name())) {
                    if (ValueUtils.isNotEmptyObj(writeBackMap.get(Long.valueOf(p.getTopsrcbillitemid())))) {
                        writeBackMap.put(Long.valueOf(p.getTopsrcbillitemid()),
                                BigDecimalUtils.safeAdd(writeBackMap.get(Long.valueOf(p.getTopsrcbillitemid())), p.getPaymentApplyAmount()));
                    } else {
                        writeBackMap.put(Long.valueOf(p.getTopsrcbillitemid()), p.getPaymentApplyAmount());
                    }
                }
            }
            boolean isSourceOutsourcingOrder = ValueUtils.isNotEmptyObj(p.get("sourceOrderType"))
                    && Short.parseShort(p.get("sourceOrderType").toString()) == SourceOrderType.OutsourcedOrder.getValue();
            if (isSourceOutsourcingOrder) {
                outsourcingOrderSubIdList.add(Long.parseLong(p.get(TOP_SRC_BILL_ITEM_ID).toString()));
            }
        });
        List<Map<String, Object>> saveWriteBackInfo = new ArrayList<>(CONSTANT_EIGHT);
        if (ValueUtils.isNotEmptyObj(writeBackMap)) {
            for (Map.Entry<Long, BigDecimal> entry : writeBackMap.entrySet()) {
                Map<String, Object> writeBackData = new HashMap<>(CONSTANT_EIGHT);
                writeBackData.put("childId", entry.getKey());
                writeBackData.put("backWriteType", "2");
                writeBackData.put("saveBackMoney", entry.getValue());
                saveWriteBackInfo.add(writeBackData);
            }
            log.error("verify order data, saveWriteBackInfo = {}", JsonUtils.toJson(saveWriteBackInfo));
            paramMap.put("saveWriteBackInfo", saveWriteBackInfo);
        }
        // 调用委外提供的批量接口
        if (CollectionUtils.isNotEmpty(outsourcingOrderSubIdList)) {
            Map<String, Object> mainMap = new HashMap<>();
            mainMap.put("code", bizObject.get("code"));
            Map<String, Object> params = new HashMap<>(CONSTANT_EIGHT);
            params.put(PAYMENT_TYPE, REQUESTED_PAYMENT);
            params.put(PRODUCT_IDS, outsourcingOrderSubIdList);
            CtmJSONObject jsonObject = oapProcessService.queryOutsourcingOrderRequestedAmount(mainMap, params);
            log.error("oap push pay apply bill, get outsourcing small balance data, jsonObject = {},params= {}", CtmJSONObject.toJSONString(jsonObject), params);
            payApplicationBillBs.forEach(p -> {
                String topsrcbillitemid = p.getTopsrcbillitemid();
                if (ValueUtils.isNotEmptyObj(jsonObject.get(topsrcbillitemid))) {
                    BigDecimal orderRequestAmount = new BigDecimal(jsonObject.get(topsrcbillitemid).toString().equals("0E-8") ? "0" : jsonObject.get(topsrcbillitemid).toString());
                    BigDecimal oapRequestAmount = new BigDecimal(map.get(p.getSourceautoid()).toString());
                    BigDecimal requestedAmount = orderRequestAmount.compareTo(oapRequestAmount) >= 0 ? oapRequestAmount : orderRequestAmount;
                    if (p.getEntityStatus().name().equals(UPDATE)) {
                        PayApplicationBill_b payApplicationBillB;
                        try {
                            payApplicationBillB = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, p.getId());
                        } catch (Exception exception) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101513"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180673","修改数据异常，请稍后重试！") /* "修改数据异常，请稍后重试！" */);
                        }
                        if (ValueUtils.isNotEmptyObj(payApplicationBillB)) {
                            if (p.getPaymentApplyAmount().compareTo(payApplicationBillB.getPaymentApplyAmount()) > 0) {
                                BigDecimal subtractAmount = BigDecimalUtils.safeAdd(requestedAmount, payApplicationBillB.getPaymentApplyAmount());
                                if (subtractAmount.compareTo(p.getPaymentApplyAmount()) < 0) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101562"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180679","付款申请金额大于委外订单允许的最大可付款申请金额！") /* "付款申请金额大于委外订单允许的最大可付款申请金额！" */);
                                }
                            }
                        }
                    }
                    if (p.getEntityStatus().name().equals(INSERT)) {
                        if (p.getPaymentApplyAmount().compareTo(requestedAmount) > 0) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101562"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180679","付款申请金额大于委外订单允许的最大可付款申请金额！") /* "付款申请金额大于委外订单允许的最大可付款申请金额！" */);
                        }
                    }
                }
            });
        }
    }

    public static List<Map<String, Object>> queryDataByFullNameAndDomain(String fullname, String domain, List<Long> ids, String queryField) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname(fullname);
        billContext.setDomain(domain);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect(queryField);
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * 采购订单交易金额
     *
     * @param payApplicationBillbes
     * @param ids
     * @throws Exception
     */
    public void checkPurchaseorder(List<PayApplicationBill_b> payApplicationBillbes, List<Long> ids, List<Long> primaryIdes) throws Exception {
        List<Map<String, Object>> purchaseOrder = queryDataByFullNameAndDomain(PURCHASEORDER, UPU, primaryIdes, "*");
        purchaseOrder.stream().forEach(purchase -> {
            if (!(null != purchase.get("status") && String.valueOf(purchase.get("status")).equals("1"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101563"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418068C","采购订单已弃审") /* "采购订单已弃审" */);
            }
        });
        //查询采购订单信息 --需要判断付款申请金额是否大于可用金额，判断是否已推付款单
        List<Map<String, Object>> purchaseOrders = queryDataByFullNameAndDomain(PURCHASEORDERS, UPU, ids, "*");
        Map<Long, Object> data = Maps.newHashMap();
        List<Long> totalPayAmountIdes = new ArrayList<>();
        if (!CollectionUtils.isEmpty(purchaseOrders)) {
            for (int j = 0; j < purchaseOrders.size(); j++) {
                Map<String, Object> purchaseOrderMap = purchaseOrders.get(j);
                // 应付金额
                BigDecimal oriSum = BigDecimalUtils.computeShouldPay(purchaseOrderMap);
                BigDecimal totalPayApplyAmount = ValueUtils.isNotEmptyObj(purchaseOrderMap.get("totalPayApplyAmount"))
                        ? (BigDecimal) purchaseOrderMap.get("totalPayApplyAmount") : BigDecimal.ZERO;
                BigDecimal totalPayOriMoney = ValueUtils.isNotEmptyObj(purchaseOrderMap.get("totalPayOriMoney"))
                        ?(BigDecimal) purchaseOrderMap.get("totalPayOriMoney") : BigDecimal.ZERO;
                if (null == totalPayApplyAmount) {
                    totalPayApplyAmount = BigDecimal.ZERO;
                }
                if (0 > totalPayApplyAmount.compareTo(BigDecimal.ZERO)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101514"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180689","付款申请金额大于允许的最大金额") /* "付款申请金额大于允许的最大金额" */);
                }
                data.put((Long) purchaseOrderMap.get("id"), BigDecimal.ZERO);
                if (purchaseOrderMap.get("paymentClose") != null && (boolean) purchaseOrderMap.get("paymentClose")) {
                    totalPayAmountIdes.add((Long) purchaseOrderMap.get("id"));
                    continue;
                }
                BigDecimal temp = BigDecimalUtils.safeSubtract(oriSum, totalPayApplyAmount.compareTo(totalPayOriMoney) >=0
                        ? totalPayApplyAmount: totalPayOriMoney);
                if (0 > temp.compareTo(BigDecimal.ZERO)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101514"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180689","付款申请金额大于允许的最大金额") /* "付款申请金额大于允许的最大金额" */);
                }
                data.put((Long) purchaseOrders.get(j).get("id"), temp);
            }
            //遍历判断申请金额是否满足条件
            payApplicationBillbes.forEach(payApplicationBillb -> {
                if (payApplicationBillb.getEntityStatus().name().equals("Insert")) {
                    Long payApplicationBilliId = payApplicationBillb.getSourceautoid();
                    if (data.containsKey(payApplicationBilliId)) {
                        if (payApplicationBillb.getPaymentApplyAmount().compareTo((BigDecimal) data.get(payApplicationBilliId)) > 0) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101514"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180689","付款申请金额大于允许的最大金额") /* "付款申请金额大于允许的最大金额" */);
                        }
                    } else {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101564"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418069C","采购订单物料信息不存在") /* "采购订单物料信息不存在" */);
                    }
                }
                if (payApplicationBillb.getEntityStatus().name().equals("Update")) {
                    PayApplicationBill_b payApplicationBillB;
                    try {
                        payApplicationBillB = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, payApplicationBillb.getId());
                    } catch (Exception ex) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101513"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180673","修改数据异常，请稍后重试！") /* "修改数据异常，请稍后重试！" */);
                    }
                    if (ValueUtils.isNotEmptyObj(payApplicationBillB)) {
                        Long sourceautoid = payApplicationBillb.getSourceautoid();
                        BigDecimal newPaymentApplyAmount = payApplicationBillb.getPaymentApplyAmount();
                        BigDecimal oldPaymentApplyAmount = payApplicationBillB.getPaymentApplyAmount();
                        if (newPaymentApplyAmount.compareTo(oldPaymentApplyAmount) > 0) {
                            BigDecimal tempBalance = BigDecimalUtils.safeSubtract(newPaymentApplyAmount, oldPaymentApplyAmount);
                            if (!CollectionUtils.isEmpty(totalPayAmountIdes) && totalPayAmountIdes.contains(sourceautoid)) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101565"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806A4","采购订单已推付款单，不能进行更新操作") /* "采购订单已推付款单，不能进行更新操作" */);
                            }
                            if (data.containsKey(payApplicationBillb.getSourceautoid())) {
                                if (tempBalance.compareTo((BigDecimal) data.get(sourceautoid)) > 0) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101514"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180689","付款申请金额大于允许的最大金额") /* "付款申请金额大于允许的最大金额" */);
                                }
                            } else {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101564"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418069C","采购订单物料信息不存在") /* "采购订单物料信息不存在" */);
                            }
                        }
                    }
                }
            });
        }
    }


    /**
     * <h2>采购订单计划明细行推付款申请金额上线校验</h2>
     *
     * @param payApplicationBillBs : 付款申请详情数据入参
     * @param ids :
     * @param primaryIdes :
     * @param paramMap :
     * @author Sun GuoCai
     * @since 2022/6/30 10:51
     */
    private void checkPurchasePlanLine(List<PayApplicationBill_b> payApplicationBillBs, List<Long> ids, List<Long> primaryIdes, Map<String, Object> paramMap, BizObject e) throws Exception {
        List<Map<String, Object>> purchaseOrder = queryDataByFullNameAndDomain(PURCHASEORDER, UPU, primaryIdes, "*");
        purchaseOrder.stream().forEach(purchase -> {
            if (!(null != purchase.get("status") && String.valueOf(purchase.get("status")).equals("1"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101563"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418068C","采购订单已弃审") /* "采购订单已弃审" */);
            }
        });
        List<Map<String, Object>> oapDetails = queryDataByFullNameAndDomain("pu.purchaseorder.PaymentExeDetail", UPU, ids, "*");
        Map<Long, BigDecimal> map = new HashMap<>(CONSTANT_EIGHT);
        oapDetails.forEach(t -> map.put(Long.valueOf(t.get(PRIMARY_ID).toString()), BigDecimalUtils.safeSubtract(
                t.get("payMoney") != null ? new BigDecimal(t.get("payMoney").toString()) : BigDecimal.ZERO,
                t.get("totalApplyPaymentAmount") != null ? new BigDecimal(t.get("totalApplyPaymentAmount").toString()) : BigDecimal.ZERO)
        ));
        if (!ValueUtils.isNotEmptyObj(map)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101560"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180677","应付事项单据已经不存在了，单据已删除或上游单据已弃审，请检查数据！") /* "应付事项单据已经不存在了，单据已删除或上游单据已弃审，请检查数据！" */);
        }
        if(log.isInfoEnabled()) {
            log.info("verify oap data, map = {}", JsonUtils.toJson(map));
        }
        BigDecimal paymentApplyAmountSum = BigDecimal.ZERO;
        String srcPurchasePlanItemId = null;
        Map<String, BigDecimal> srcPurchaseOrderMaterialLineMap = new HashMap<>();
        for (PayApplicationBill_b p : payApplicationBillBs) {
            if (p.getEntityStatus().name().equals(UPDATE)) {
                PayApplicationBill_b payApplicationBillB;
                try {
                    payApplicationBillB = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, p.getId());
                } catch (Exception exception) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101513"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180673","修改数据异常，请稍后重试！") /* "修改数据异常，请稍后重试！" */);
                }
                srcPurchasePlanItemId = p.getSrcpurchaseplanitemid();
                if (p.getPaymentApplyAmount().compareTo(payApplicationBillB.getPaymentApplyAmount()) == 0) {
                    continue;
                }
                if (ValueUtils.isNotEmptyObj(payApplicationBillB)) {
                    if (p.getPaymentApplyAmount().compareTo(payApplicationBillB.getPaymentApplyAmount()) > 0) {
                        BigDecimal oapBalance = map.get(payApplicationBillB.getSourceautoid());
                        BigDecimal subtractAmount = BigDecimalUtils.safeAdd(oapBalance, payApplicationBillB.getPaymentApplyAmount());
                        if (subtractAmount.compareTo(p.getPaymentApplyAmount()) < 0) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101514"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180689","付款申请金额大于允许的最大金额") /* "付款申请金额大于允许的最大金额" */);
                        }
                    }
                }
                // 修改时，计算回写采购订单物料明细行的回写金额
                String srcPurchaseOrderMaterialLineId = p.getSrcPurchaseOrderMaterialLineId();
                if (ValueUtils.isNotEmptyObj(srcPurchaseOrderMaterialLineMap.get(srcPurchaseOrderMaterialLineId))) {
                    srcPurchaseOrderMaterialLineMap.put(srcPurchaseOrderMaterialLineId,
                            BigDecimalUtils.safeAdd(srcPurchaseOrderMaterialLineMap.get(srcPurchaseOrderMaterialLineId),
                                    BigDecimalUtils.safeSubtract(p.getPaymentApplyAmount(), payApplicationBillB.getPaymentApplyAmount())));
                } else {
                    srcPurchaseOrderMaterialLineMap.put(srcPurchaseOrderMaterialLineId, BigDecimalUtils.safeSubtract(p.getPaymentApplyAmount(), payApplicationBillB.getPaymentApplyAmount()));
                }
                // 修改时，计算回写采购订单计划行的回写金额
                paymentApplyAmountSum = BigDecimalUtils.safeAdd(paymentApplyAmountSum, BigDecimalUtils.safeSubtract(p.getPaymentApplyAmount(), payApplicationBillB.getPaymentApplyAmount()));
            }
            if (p.getEntityStatus().name().equals(INSERT)) {
                if (p.getPaymentApplyAmount().compareTo(map.get(p.getSourceautoid())) > 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101514"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180689","付款申请金额大于允许的最大金额") /* "付款申请金额大于允许的最大金额" */);
                }
                srcPurchasePlanItemId = p.getSrcpurchaseplanitemid();
                // 保存时，计算回写采购订单物料明细行的回写金额
                String srcPurchaseOrderMaterialLineId = p.getSrcPurchaseOrderMaterialLineId();
                if (ValueUtils.isNotEmptyObj(srcPurchaseOrderMaterialLineMap.get(srcPurchaseOrderMaterialLineId))) {
                    srcPurchaseOrderMaterialLineMap.put(srcPurchaseOrderMaterialLineId,
                            BigDecimalUtils.safeAdd(srcPurchaseOrderMaterialLineMap.get(srcPurchaseOrderMaterialLineId),
                                    p.getPaymentApplyAmount()));
                } else {
                    srcPurchaseOrderMaterialLineMap.put(srcPurchaseOrderMaterialLineId, p.getPaymentApplyAmount());
                }
                // 保存时，计算回写采购订单计划行的回写金额
                paymentApplyAmountSum = BigDecimalUtils.safeAdd(paymentApplyAmountSum, p.getPaymentApplyAmount());
            }
        }
        List<Map<String, Object>> srcPurchaseOrderMaterialLineList= new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : srcPurchaseOrderMaterialLineMap.entrySet()) {
            Map<String, Object> splitMap = new HashMap<>();
            splitMap.put("srcPurchaseOrderMaterialLineId", entry.getKey());
            splitMap.put("paymentApplyAmountSum", entry.getValue());
            srcPurchaseOrderMaterialLineList.add(splitMap);
        }
        Map<String, Object> backWritePurchasePlanLine = new HashMap<>();
        backWritePurchasePlanLine.put("srcpurchaseplanitemid",  srcPurchasePlanItemId);
        backWritePurchasePlanLine.put("srcPurchaseOrderMaterialLineList",  srcPurchaseOrderMaterialLineList);
        backWritePurchasePlanLine.put("paymentApplyAmountSum", paymentApplyAmountSum);
        paramMap.put("backWritePurchasePlanLine", backWritePurchasePlanLine);
        e.put("backWritePurchasePlanLine", backWritePurchasePlanLine);
        log.error("save back write purchase plan amount backWritePurchasePlanLine = {}", backWritePurchasePlanLine);
    }

}
