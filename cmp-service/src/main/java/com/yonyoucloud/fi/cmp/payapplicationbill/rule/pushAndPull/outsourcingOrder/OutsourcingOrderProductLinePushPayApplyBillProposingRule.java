package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.outsourcingOrder;

import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;
import static com.yonyoucloud.fi.cmp.constant.IMsgConstant.OUTSOURCING_ORDER_PRODUCT_LINE_PUSH_PAY_APPLY_BILL;
import static com.yonyoucloud.fi.cmp.constant.IMsgConstant.PO_SUBCONTRACT_ORDER;

/**
 * <h1>OutsourcingOrderProductLinePushPayApplyBillProposingRule</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-07-13 16:14
 */
@Slf4j
@Component("outsourcingOrderProductLinePushPayApplyBillProposingRule")
@RequiredArgsConstructor
public class OutsourcingOrderProductLinePushPayApplyBillProposingRule extends AbstractCommonRule {

    private final VendorQueryService vendorQueryService;
    private final CmCommonService cmCommonService;
    private final BaseRefRpcService baseRefRpcService;
    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;

    private static final String OUTSOURCING_ORDER_APPLY = "outsourcing-order-apply";

    @Override
    @SuppressWarnings("unchecked")
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<Map<String, Object>> oMakes = (List<Map<String, Object>>) paramMap.get("omake");
        List<Map<String, Object>> sourceDataList = (List<Map<String, Object>>) paramMap.get("sourceDatas");
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("sourceDataList", CtmJSONObject.toJSONString(sourceDataList));
        Map<String, Object> externalData = (Map<String, Object>) paramMap.get("externalData");
        List<String> allowPushIdList = (List<String>) externalData.get("orderProduct_id");
        Iterator<Map<String, Object>> iteratorMake = oMakes.iterator();
        Map<String, BigDecimal> balanceSumList = new ConcurrentHashMap<>(CONSTANT_EIGHT);
        // 公共数据缓存的Map
        Map<String, Map<String, Object>> ownDataCacheMap = new ConcurrentHashMap<>(CONSTANT_EIGHT);
        try {
            while (iteratorMake.hasNext()) {
                Map<String, Object> map = iteratorMake.next();
                List<Map<String, Object>> orderProduct = (List<Map<String, Object>>) map.get("orderProduct");
                Iterator<Map<String, Object>> iterator = orderProduct.iterator();
                BigDecimal paymentApplyAmountSum = BigDecimal.ZERO;
                while (iterator.hasNext()) {
                    Map<String, Object> next = iterator.next();
                    String id = next.get("id").toString();
                    if (!allowPushIdList.contains(id)) {
                        iterator.remove();
                        continue;
                    }
                    BigDecimal oriSum = ValueUtils.isNotEmptyObj(next.get("orderSubcontractProduct!paymentTCCal")) ?
                            new BigDecimal(next.get("orderSubcontractProduct!paymentTCCal").toString()) : BigDecimal.ZERO;
                    if (BigDecimal.ZERO.compareTo(oriSum) == 0){
                        BigDecimal invoicedQuantityPU = ValueUtils.isNotEmptyObj(next.get("orderSubcontractProduct!invoicedQuantityPU")) ?
                                new BigDecimal(next.get("orderSubcontractProduct!invoicedQuantityPU").toString()) : BigDecimal.ZERO;
                        if (BigDecimal.ZERO.compareTo(invoicedQuantityPU) < 0){
                            BigDecimal invoicedMoneyTC = ValueUtils.isNotEmptyObj(next.get("orderSubcontractProduct!invoicedMoneyTC")) ?
                                    new BigDecimal(next.get("orderSubcontractProduct!invoicedMoneyTC").toString()) : BigDecimal.ZERO;
                            BigDecimal subcontractQuantityPU = ValueUtils.isNotEmptyObj(next.get("orderSubcontractProduct!subcontractQuantityPU")) ?
                                    new BigDecimal(next.get("orderSubcontractProduct!subcontractQuantityPU").toString()) : BigDecimal.ZERO;
                            BigDecimal oriTaxUnitPrice = ValueUtils.isNotEmptyObj(next.get("orderSubcontractProduct!oriTaxUnitPrice")) ?
                                    new BigDecimal(next.get("orderSubcontractProduct!oriTaxUnitPrice").toString()) : BigDecimal.ZERO;
                            BigDecimal balance = BigDecimalUtils.safeAdd(invoicedMoneyTC,
                                    BigDecimalUtils.safeSubtract(subcontractQuantityPU, invoicedQuantityPU));
                            if (BigDecimal.ZERO.compareTo(balance) < 0 && BigDecimal.ZERO.compareTo(oriTaxUnitPrice) < 0) {
                                oriSum = BigDecimalUtils.safeMultiply(balance, oriTaxUnitPrice);
                            }
                        } else {
                            oriSum = ValueUtils.isNotEmptyObj(next.get("orderSubcontractProduct!oriSum")) ?
                                    new BigDecimal(next.get("orderSubcontractProduct!oriSum").toString()) : BigDecimal.ZERO;
                        }
                    }
                    BigDecimal balance = oriSum;
                    BigDecimal requestedPaymentTC = ValueUtils.isNotEmptyObj(next.get("orderSubcontractProduct!requestedPaymentTC"))
                            ? new BigDecimal(next.get("orderSubcontractProduct!requestedPaymentTC").toString()) : BigDecimal.ZERO;
                    if (BigDecimal.ZERO.compareTo(requestedPaymentTC) != 0) {
                        balance = BigDecimalUtils.safeSubtract(oriSum, requestedPaymentTC);
                        if (BigDecimal.ZERO.compareTo(balance) >= 0) {
                            iterator.remove();
                        } else {
                            next.put("orderSubcontractProduct!paymentTCCal", balance);
                            next.put("orderSubcontractProduct!oriSum", balance);
                        }
                    } else {
                        next.put("orderSubcontractProduct!oriSum", balance);
                    }
                    paymentApplyAmountSum = BigDecimalUtils.safeAdd(paymentApplyAmountSum, balance);
                }
                if (CollectionUtils.isEmpty(orderProduct)) {
                    iteratorMake.remove();
                    continue;
                }
                map.put("orderSubcontract!totalMoneyTC", paymentApplyAmountSum);

                String key = map.get("orderSubcontract!tcOrgId") +
                        "_" +
                        map.get("orderSubcontract!subcontractVendorId") +
                        "_" +
                        map.get("orderSubcontract!tcId");
                if (ValueUtils.isNotEmptyObj(balanceSumList.get(key))) {
                    BigDecimal bigDecimal = balanceSumList.get(key);
                    balanceSumList.put(key, BigDecimalUtils.safeAdd(bigDecimal, paymentApplyAmountSum));
                } else {
                    balanceSumList.put(key, paymentApplyAmountSum);
                }

                ownDataCacheMap = new ConcurrentHashMap<>(CONSTANT_EIGHT);
                // 交易类型id
                tradeTypeProcess(map, ownDataCacheMap);
                // 供应商
                supplierProcess(map, ownDataCacheMap);
                // 款项类型
                quickTypeProcess(map, ownDataCacheMap);


            }
            if (CollectionUtils.isEmpty(oMakes) || oMakes.size() == 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100955"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180225","该委外订单已无可付款申请的金额!") /* "该委外订单已无可付款申请的金额!" */);
            }
            for (Map<String, Object> map : oMakes) {
                String key = map.get("orderSubcontract!tcOrgId") +
                        "_" +
                        map.get("orderSubcontract!subcontractVendorId") +
                        "_" +
                        map.get("orderSubcontract!tcId");
                if (ValueUtils.isNotEmptyObj(balanceSumList.get(key))) {
                    map.put("orderSubcontract!totalMoneyTC", balanceSumList.get(key));
                }
            }
            logMap.put(OMAKE, CtmJSONObject.toJSONString(oMakes));
            recordBusinessLog(logMap, oMakes.get(0).get(CODE).toString());
            balanceSumList.clear();
            if (ValueUtils.isNotEmptyObj(ownDataCacheMap)) ownDataCacheMap.clear();
        } catch (Exception e) {
            if (CollectionUtils.isNotEmpty(oMakes)){
                logMap.put(OMAKE, CtmJSONObject.toJSONString(oMakes));
                recordBusinessLog(logMap, oMakes.get(0).get("code").toString());
            }
            log.error("UpuPurchaseOrderPlanLinPushPayApplyBillProposingRule, error, yTenantId = {}, code = {},e = {}",
                    AppContext.getCurrentUser().getYTenantId(),
                    CollectionUtils.isNotEmpty(oMakes)?oMakes.get(0).get("code").toString() : "There is no eligible data!",
                    e.getMessage());
            if (ValueUtils.isNotEmptyObj(ownDataCacheMap)) ownDataCacheMap.clear();
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100956"),e.getMessage());
        }
        return new RuleExecuteResult();
    }

    private void supplierProcess(Map<String, Object> map, Map<String, Map<String, Object>> paramsCacheMap) {
        try {
            Object supplierId = map.get("orderSubcontract!subcontractVendorId");
            if (supplierId != null) {
                String supplier = SUPPLIER_UNDERLINE + supplierId;
                Map<String, Object> supplierCacheValue = paramsCacheMap.get(supplier);
                if (ValueUtils.isNotEmptyObj(supplierCacheValue)) {
                    map.put(SUPPLIER_BANK_ACCOUNT, supplierCacheValue.get(SUPPLIER_BANK_ACCOUNT));
                    map.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT_NAME, supplierCacheValue.get(SUPPLIER_BANK_ACCOUNT_ACCOUNT_NAME));
                    map.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT, supplierCacheValue.get(SUPPLIER_BANK_ACCOUNT_ACCOUNT));
                    map.put(SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME, supplierCacheValue.get(SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME));
                    map.put(SUPPLIER_CORRESPONDENT_CODE, supplierCacheValue.get(SUPPLIER_CORRESPONDENT_CODE));
                } else {
                    Map<String, Object> conditionSupplierId = new HashMap<>(CONSTANT_EIGHT);
                    conditionSupplierId.put("vendor", supplierId);
                    conditionSupplierId.put("defaultbank", true);
                    Object currency = map.get("orderSubcontract!tcId");
                    if (ValueUtils.isNotEmptyObj(currency)) {
                        conditionSupplierId.put("currency", currency);
                    }
                    conditionSupplierId.put("stopstatus", "0");
                    List<VendorBankVO> bankAccounts = vendorQueryService.getVendorBanksByCondition(conditionSupplierId);
                    Map<String, Object> supplierCache = new HashMap<>();
                    if (bankAccounts.size() > 0) {
                        map.put(SUPPLIER_BANK_ACCOUNT, bankAccounts.get(0).getId());
                        map.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT_NAME, bankAccounts.get(0).getAccountname());
                        map.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT, bankAccounts.get(0).getAccount());// 供应商银行账号
                        map.put(SUPPLIER_CORRESPONDENT_CODE, bankAccounts.get(0).getCorrespondentcode());// 供应商联行号

                        supplierCache.put(SUPPLIER_BANK_ACCOUNT, bankAccounts.get(0).getId());
                        supplierCache.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT_NAME, bankAccounts.get(0).getAccountname());
                        supplierCache.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT, bankAccounts.get(0).getAccount());// 供应商银行账号
                        supplierCache.put(SUPPLIER_CORRESPONDENT_CODE, bankAccounts.get(0).getCorrespondentcode()); // 供应商联行号

                        BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).getOpenaccountbank());
                        if (depositBank != null) {
                            map.put(SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME, depositBank.getName()); // 供应商账户银行网点
                            supplierCache.put(SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME, depositBank.getName()); // 供应商账户银行网点
                        } else {
                            map.put(SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME, null); // 供应商账户银行网点
                            supplierCache.put(SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME, null); // 供应商账户银行网点
                        }

                    } else {
                        supplierCache.put(SUPPLIER_BANK_ACCOUNT, null);
                        supplierCache.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT_NAME, null);
                        supplierCache.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT, null);
                        supplierCache.put(SUPPLIER_CORRESPONDENT_CODE, null);
                    }
                    paramsCacheMap.put(supplier, supplierCache);
                }

            }
        } catch (Exception e) {
            log.error("query futureBankAccounts fail!, e = {}", e.getMessage());
        }
    }

    private void quickTypeProcess(Map<String, Object> map, Map<String, Map<String, Object>> paramsCacheMap) {
        try {
            Map<String, Object> quickType = paramsCacheMap.get(QUICK_TYPE);
            if (ValueUtils.isNotEmptyObj(quickType)) {
                map.put(QUICK_TYPE, quickType.get(QUICK_TYPE));
                map.put(QUICKTYPE_NAME, quickType.get(QUICKTYPE_NAME));
                map.put(QUICKTYPE_CODE, quickType.get(QUICKTYPE_CODE));
            } else {
                Map<String, Object> quickTypeCache = new HashMap<>(CONSTANT_EIGHT);
                Map<String, Object> condition = new HashMap<>();
                condition.put("code", "5");//预付款
                List<Map<String, Object>> quickTypeMap = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
                if (ValueUtils.isNotEmpty(quickTypeMap)) {
                    map.put(QUICK_TYPE, MapUtils.getLong(quickTypeMap.get(CONSTANT_ZERO), PRIMARY_ID));
                    map.put(QUICKTYPE_NAME, MapUtils.getString(quickTypeMap.get(CONSTANT_ZERO), NAME));
                    map.put(QUICKTYPE_CODE, MapUtils.getString(quickTypeMap.get(CONSTANT_ZERO), CODE));

                    quickTypeCache.put(QUICK_TYPE, MapUtils.getLong(quickTypeMap.get(CONSTANT_ZERO), PRIMARY_ID));
                    quickTypeCache.put(QUICKTYPE_NAME, MapUtils.getString(quickTypeMap.get(CONSTANT_ZERO), NAME));
                    quickTypeCache.put(QUICKTYPE_CODE, MapUtils.getString(quickTypeMap.get(CONSTANT_ZERO), CODE));
                }
                paramsCacheMap.put(QUICK_TYPE, quickTypeCache);
            }
        } catch (Exception e) {
            log.error("query quickType fail!, e = {}", e.getMessage());
        }
    }

    private void tradeTypeProcess(Map<String, Object> map, Map<String, Map<String, Object>> paramsCacheMap) {
        try {
            Map<String, Object> tradeType = paramsCacheMap.get(TRADE_TYPE);
            if (ValueUtils.isNotEmptyObj(tradeType)) {
                map.put(DEFAULT_TRADETYPE, tradeType.get(DEFAULT_TRADETYPE));
                map.put(DEFAULT_TRADETYPE_NAME, tradeType.get(DEFAULT_TRADETYPE_NAME));
                map.put(DEFAULT_TRADETYPE_CODE, tradeType.get(DEFAULT_TRADETYPE_CODE));
            } else {
                Map<String, Object> tradeTypeMap = cmCommonService.queryTransTypeById(FICM3, CONSTANT_STR_ZERO, OUTSOURCING_ORDER_APPLY);
                map.put(DEFAULT_TRADETYPE, tradeTypeMap.get(PRIMARY_ID));
                map.put(DEFAULT_TRADETYPE_NAME, tradeTypeMap.get(NAME));
                map.put(DEFAULT_TRADETYPE_CODE, tradeTypeMap.get(CODE));

                Map<String, Object> tradeTypeCache = new HashMap<>(CONSTANT_EIGHT);
                tradeTypeCache.put(DEFAULT_TRADETYPE, tradeTypeMap.get(PRIMARY_ID));
                tradeTypeCache.put(DEFAULT_TRADETYPE_NAME, tradeTypeMap.get(NAME));
                tradeTypeCache.put(DEFAULT_TRADETYPE_CODE, tradeTypeMap.get(CODE));
                paramsCacheMap.put(TRADE_TYPE, tradeTypeCache);
            }
        } catch (Exception e) {
            log.error("query tradeType fail!, e = {}", e.getMessage());
        }
    }

    private void recordBusinessLog(Map<String, Object> logMap, String code) {
        try {
            ctmcmpBusinessLogService.saveBusinessLog(logMap, code, "",
                    PO_SUBCONTRACT_ORDER_LIST, PO_SUBCONTRACT_ORDER, OUTSOURCING_ORDER_PRODUCT_LINE_PUSH_PAY_APPLY_BILL);
        } catch (Exception e) {
            log.error("UpuPurchaseOrderPlanLinPushPayApplyBillProposingRule, write Business Log, yTenantId = {}, code = {}, e = {}",
                    AppContext.getCurrentUser().getYTenantId(), code, e.getMessage());
        }
    }
}
