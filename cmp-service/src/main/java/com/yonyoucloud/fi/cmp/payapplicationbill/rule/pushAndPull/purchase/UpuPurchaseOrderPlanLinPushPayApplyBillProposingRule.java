package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.purchase;

import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
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
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;
import static com.yonyoucloud.fi.cmp.constant.IMsgConstant.ST_PURCHASE_ORDER;
import static com.yonyoucloud.fi.cmp.constant.IMsgConstant.UPU_PURCHASE_PLAN_LINE_PUSH_PAY_APPLY_BILL;

/**
 * <h1>采购订单计划明细行推付款申请前置处理规则</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-06-16 18:29
 */
@Component("upuPurchaseOrderPlanLinPushPayApplyBillProposingRule")
@Slf4j
@RequiredArgsConstructor
public class UpuPurchaseOrderPlanLinPushPayApplyBillProposingRule extends AbstractCommonRule {
    private final CmCommonService cmCommonService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    VendorQueryService vendorQueryService;
    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;

    private static final String ORDER_APPLY = "order-apply";

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<Map<String, Object>> oMakes = (List<Map<String, Object>>) paramMap.get("omake");
        List<Map<String, Object>> sourceDataList = (List<Map<String, Object>>) paramMap.get("sourceDatas");
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("sourceDataList", CtmJSONObject.toJSONString(sourceDataList));
        try {
            String number = null;
            Boolean isAdvancePay = false;
            String srcpurchaseplanitemid = null;
            for (Map<String, Object> sourceData : sourceDataList) {
                List<Map<String, Object>> paymentSchedules = (List<Map<String, Object>>) sourceData.get("paymentSchedules");
                for (Map<String, Object> paymentSchedule : paymentSchedules) {
                    if (ValueUtils.isNotEmptyObj(paymentSchedule.get("pushDown"))) {
                        number = paymentSchedule.get("number").toString();
                        isAdvancePay = (Boolean) paymentSchedules.get(0).get("isAdvancePay");
                        srcpurchaseplanitemid = paymentSchedule.get("id").toString();
                    }
                }
            }

            for (Map<String, Object> map : oMakes) {
                // 交易类型id
                Map<String, Object> tradetypeMap = cmCommonService.queryTransTypeById("FICM3", "0", ORDER_APPLY);
                if (ValueUtils.isEmpty(tradetypeMap)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102122"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807DE", "该单据未查询到相应的交易类型！") /* "该单据未查询到相应的交易类型！" */);
                }
                map.put("tradetype", tradetypeMap.get("id"));
                map.put("tradetype_name", tradetypeMap.get("name"));
                map.put("tradetype_code", tradetypeMap.get("code"));

                Object supplierId = map.get("supplier") != null ? map.get("supplier") : map.get("invoiceVendor");
                Map<String, Object> conditionSupplierId = new HashMap<>(CONSTANT_EIGHT);
                conditionSupplierId.put("vendor", supplierId);
                conditionSupplierId.put("defaultbank", true);
                Object currency = map.get("currency");
                if (ValueUtils.isNotEmptyObj(currency)) {
                    conditionSupplierId.put("currency", currency);
                }
                conditionSupplierId.put("stopstatus", "0");
                List<VendorBankVO> bankAccounts = vendorQueryService.getVendorBanksByCondition(conditionSupplierId);
                if (bankAccounts.size() > 0) {
                    map.put("supplierbankaccount", bankAccounts.get(0).getId());
                    map.put("supplierbankaccount_accountname", bankAccounts.get(0).getAccountname());
                    map.put("supplierbankaccount_account", bankAccounts.get(0).getAccount());
                    map.put("supplierbankaccount_correspondentcode", bankAccounts.get(0).getCorrespondentcode());
                    BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).getOpenaccountbank());
                    if (depositBank != null) {
                        map.put("supplierbankaccount_openaccountbank_name", depositBank.getName()); // 供应商账户银行网点
                    } else {
                        map.put("supplierbankaccount_openaccountbank_name", null); // 供应商账户银行网点
                    }
                }

                CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(map.get("currency").toString());
                if (currencyTenantDTO != null) {
                    map.put("currency_priceDigit", currencyTenantDTO.getPricedigit());
                    map.put("currency_moneyDigit", currencyTenantDTO.getMoneydigit());
                }

                // 款项类型
                Map<String, Object> condition = new HashMap<>();
                if (isAdvancePay) {
                    condition.put("code", "5");// 预付款
                } else {
                    condition.put("code", "6");// 应付款
                }
                List<Map<String, Object>> quickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
                if (ValueUtils.isNotEmpty(quickType)) {

                    map.put("quickType", quickType.get(0).get("id"));
                    map.put("quickType_name", quickType.get(0).get("name"));
                    map.put("quickType_code", quickType.get(0).get("code"));
                }

                List<Map<String, Object>> paymentSchedulesNew = new ArrayList<>();
                BigDecimal paymentApplyAmountSum = BigDecimal.ZERO;
                List<Map<String, Object>> paymentExeDetail = (List<Map<String, Object>>) map.get("paymentExeDetail");
                List<Map<String, Object>> purchaseOrders = (List<Map<String, Object>>) map.get("purchaseOrders");
                Iterator<Map<String, Object>> iterator = paymentExeDetail.iterator();
                BigDecimal payMoneySum = BigDecimal.ZERO;
                while (iterator.hasNext()) {
                    Map<String, Object> mapSub = iterator.next();
                    assert number != null;
                    if (number.equals(mapSub.get("number").toString())) {
                        BigDecimal totalApplyPaymentAmount = BigDecimal.ZERO;
                        if (ValueUtils.isNotEmptyObj(mapSub.get("totalApplyPaymentAmount"))) {
                            totalApplyPaymentAmount = new BigDecimal(mapSub.get("totalApplyPaymentAmount").toString());
                        }
                        BigDecimal payMoney = BigDecimalUtils.safeSubtract(new BigDecimal(mapSub.get("payMoney").toString()), totalApplyPaymentAmount);
                        if (payMoney.compareTo(BigDecimal.ZERO) > 0
                                && (ValueUtils.isNotEmptyObj(mapSub.get("startDateTime")) || ValueUtils.isNotEmptyObj(mapSub.get("copyStartBaseDate")))) {
                            mapSub.put("payMoney", payMoney);
                            mapSub.put("srcpurchaseplanitemid", srcpurchaseplanitemid);
                            paymentApplyAmountSum = BigDecimalUtils.safeAdd(paymentApplyAmountSum, payMoney);
                            Object firstsourceautoid = mapSub.get("firstsourceautoid");
                            purchaseOrders.stream().filter(item -> item.get("id").equals(firstsourceautoid)).findFirst().ifPresent(item -> {
                                mapSub.put("project", item.get("project"));
                                mapSub.put("project_name", item.get("project_name"));
                                BigDecimal totalPayOriMoney = ValueUtils.isNotEmptyObj(mapSub.get("totalPayOriMoney"))
                                        ? (BigDecimal) mapSub.get("totalPayOriMoney") : BigDecimal.ZERO;
                                mapSub.put("totalPayOriMoney", totalPayOriMoney);
                            });
                            payMoneySum = BigDecimalUtils.safeAdd(payMoneySum, payMoney);
                            paymentSchedulesNew.add(mapSub);
                        } else {
                            iterator.remove();
                        }
                    }
                }
                if (paymentSchedulesNew.size() == 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102123"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807DD", "采购订单计划明细行已无可付款申请金额，无法推单！") /* "采购订单计划明细行已无可付款申请金额，无法推单！" */);
                }

                if (ValueUtils.isNotEmptyObj(paymentSchedulesNew.get(0).get("totalPayOriMoney"))) {
                    BigDecimal totalPayOriMoney = new BigDecimal(paymentSchedulesNew.get(0).get("totalPayOriMoney").toString());
                    if (totalPayOriMoney.compareTo(payMoneySum) >=0){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102123"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807DD", "采购订单计划明细行已无可付款申请金额，无法推单！") /* "采购订单计划明细行已无可付款申请金额，无法推单！" */);
                    }
                    BigDecimal balance = BigDecimalUtils.safeSubtract(payMoneySum, totalPayOriMoney);
                    List<Map<String, Object>> paymentSchedulesNewStop = new ArrayList<>();
                    for (Map<String, Object> objectMap : paymentSchedulesNew) {
                        BigDecimal payMoney = new BigDecimal(objectMap.get("payMoney").toString());
                        if (balance.compareTo(payMoney) == 0) {
                            paymentSchedulesNewStop.add(objectMap);
                            break;
                        }else if(balance.compareTo(payMoney) < 0){
                            objectMap.put("payMoney", balance);
                            paymentSchedulesNewStop.add(objectMap);
                            break;
                        } else if (balance.compareTo(payMoney) > 0){
                            paymentSchedulesNewStop.add(objectMap);
                            balance = BigDecimalUtils.safeSubtract(balance, payMoney);
                        }
                    }
                    map.put("paymentExeDetail", paymentSchedulesNewStop);
                } else {
                    map.put("paymentExeDetail", paymentSchedulesNew);
                }

                map.put("paymentApplyAmountSum", paymentApplyAmountSum);
                map.put("unpaidAmountSum", paymentApplyAmountSum);
                logMap.put(OMAKE, CtmJSONObject.toJSONString(oMakes));
                recordBusinessLog(logMap, map.get(CODE).toString());
            }
        } catch (Exception e) {
            logMap.put(OMAKE, CtmJSONObject.toJSONString(oMakes));
            recordBusinessLog(logMap, oMakes.get(0).get("code").toString());
            log.error("UpuPurchaseOrderPlanLinPushPayApplyBillProposingRule, error, yTenantId = {}, code = {},e = {}",
                    AppContext.getCurrentUser().getYTenantId(), oMakes.get(0).get("code").toString(), e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102124"),e.getMessage());
        }
        return new RuleExecuteResult();
    }

    private void recordBusinessLog(Map<String, Object> logMap, String code) {
        try {
            ctmcmpBusinessLogService.saveBusinessLog(logMap, code, "",
                    ST_PURCHASE_ORDER_LIST, ST_PURCHASE_ORDER, UPU_PURCHASE_PLAN_LINE_PUSH_PAY_APPLY_BILL);
        } catch (Exception e) {
            log.error("UpuPurchaseOrderPlanLinPushPayApplyBillProposingRule, write Business Log, yTenantId = {}, code = {}, e = {}",
                    AppContext.getCurrentUser().getYTenantId(), code, e.getMessage());
        }
    }
}
