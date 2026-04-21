package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.purchase;


import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;
import static com.yonyoucloud.fi.cmp.constant.IMsgConstant.ST_PURCHASE_ORDER;
import static com.yonyoucloud.fi.cmp.constant.IMsgConstant.UPU_PURCHASE_PUSH_PAY_APPLY_BILL;

@Component("stpurchaseOrderToPayapplicationBillRule")
@RequiredArgsConstructor
public class StpurchaseOrderToPayapplicationBillRule extends AbstractCommonRule {
    @Autowired
    VendorQueryService vendorQueryService;
    private final String ORDERAPPLY="order-apply";
    private final String STATUS="1";  //采购订单单据状态 --已审
    private static final Logger log = LoggerFactory.getLogger(StpurchaseOrderToPayapplicationBillRule.class);

    private final CmCommonService cmCommonService;

    private final BaseRefRpcService baseRefRpcService;

    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<Map<String, Object>> omakes = (List) paramMap.get("omake");
        Boolean ststusFlage = true;
//        Boolean totalPayAmountFlage = true;

        Iterator<Map<String, Object>> iterator = omakes.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> map = iterator.next();
            String status = String.valueOf(map.get("status")) ;
            if(!(!StringUtils.isEmpty(status) && STATUS.equals(status))){
                iterator.remove();
                ststusFlage = false;
                continue;
            }
            ststusFlage = false;
            //交易类型id
            Map<String, Object> tradetypeMap = cmCommonService.queryTransTypeById("FICM3", "0",ORDERAPPLY);
            if (ValueUtils.isEmpty(tradetypeMap)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102304"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180083","该单据未查询到相应的交易类型！") /* "该单据未查询到相应的交易类型！" */);
            }
            map.put("tradetype", tradetypeMap.get("id"));
            map.put("tradetype_name", tradetypeMap.get("name"));
            map.put("tradetype_code", tradetypeMap.get("code"));

            Object supplierId = map.get("supplier") !=null ? map.get("supplier") : map.get("invoiceVendor");
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
                map.put("supplierbankaccount_accountname", bankAccounts.get(0).get("accountname"));
                map.put("supplierbankaccount_account", bankAccounts.get(0).get("account"));
                map.put("supplierbankaccount_correspondentcode", bankAccounts.get(0).getCorrespondentcode()); // 供应商联行号
                if (ValueUtils.isNotEmptyObj(bankAccounts.get(0).get("openaccountbank"))){
                    BankdotVO depositBank =baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).get("openaccountbank").toString());
                    if (depositBank != null) {
                        map.put("supplierbankaccount_openaccountbank_name", depositBank.getName()); // 供应商账户银行网点
                    }else {
                        map.put("supplierbankaccount_openaccountbank_name", null); // 供应商账户银行网点
                    }
                }
            }

            // 款项类型
            Map<String, Object> condition = new HashMap<>();
            condition.put("code", "5");//预付款
            List<Map<String, Object>> quickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
            if (ValueUtils.isNotEmpty(quickType)) {
                map.put("quickType", quickType.get(0).get("id"));
                map.put("quickType_name", quickType.get(0).get("name"));
                map.put("quickType_code", quickType.get(0).get("code"));
            }

            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(map.get("currency").toString());
            if (currencyTenantDTO != null) {
                map.put("currency_priceDigit", currencyTenantDTO.getPricedigit());
                map.put("currency_moneyDigit", currencyTenantDTO.getMoneydigit());
            }

            List<Map<String, Object>> purchaseOrders = (List) map.get("purchaseOrders");
            Iterator<Map<String, Object>> iter = purchaseOrders.iterator();
            while (iter.hasNext()) {
                Map<String, Object> map_sub = iter.next();
                if (null == map_sub.get("totalPayApplyAmount")) {//累计付款申请金额
                    map_sub.put("totalPayApplyAmount",BigDecimal.ZERO);
                }
                boolean isPaymentClose = ValueUtils.isNotEmptyObj(map_sub.get("paymentClose")) && (boolean) map_sub.get("paymentClose");
                if(isPaymentClose){ //付款关闭
                    iter.remove();
                    log.error("StpurchaseOrderToPayapplicationBillRule execute paymentClose:"+ map_sub.get("id"));
                    continue;
                }
                if (null != map_sub.get("totalPayAmount") && 0 > BigDecimal.ZERO.compareTo((BigDecimal) map_sub.get("totalPayAmount"))
                    && 0 == BigDecimal.ZERO.compareTo((BigDecimal) map_sub.get("totalPayApplyAmount"))) {
                    iter.remove();
                    log.error("StpurchaseOrderToPayapplicationBillRule execute totalPayAmount:"+map_sub.get("id"));
                    continue;
                }
                //#( amountPayable - totalPayApplyAmount )
                if (null == map_sub.get("amountPayable")) {
                    iter.remove();
                    log.error("StpurchaseOrderToPayapplicationBillRule execute amountPayable:"+map_sub.get("id"));
                    continue;
                }else{
                    BigDecimal amountPayable = (BigDecimal) map_sub.get("amountPayable");
                    BigDecimal totalPayApplyAmount = ValueUtils.isNotEmptyObj(map_sub.get("totalPayApplyAmount"))
                            ? (BigDecimal) map_sub.get("totalPayApplyAmount") : BigDecimal.ZERO;
                    BigDecimal totalPayOriMoney = ValueUtils.isNotEmptyObj(map_sub.get("totalPayOriMoney"))
                            ?(BigDecimal) map_sub.get("totalPayOriMoney") : BigDecimal.ZERO;
                    if(0 > totalPayApplyAmount.compareTo(BigDecimal.ZERO)){
                        iter.remove();
                        log.error("StpurchaseOrderToPayapplicationBillRule execute totalPayApplyAmount:"+map_sub.get("id"));
                        continue;
                    }
                    BigDecimal balance = totalPayApplyAmount.compareTo(totalPayOriMoney) >=0
                            ? totalPayApplyAmount: totalPayOriMoney;
                    BigDecimal temp = BigDecimalUtils.safeSubtract(amountPayable,balance);
                    if(0 >= temp.compareTo(BigDecimal.ZERO)){
                        iter.remove();
                        log.error("StpurchaseOrderToPayapplicationBillRule execute temp:"+map_sub.get("id"));
                        continue;
                    }
                    map_sub.put("totalPayApplyAmount", balance);
                }
            }
            if(purchaseOrders.size() == 0){
                iterator.remove();
                continue;
            }
//            else{
//                totalPayAmountFlage = false;
//            }
            //不是自动生单 autobill
            map.put("autobill", true);

        }
        if (CollectionUtils.isEmpty(omakes)) {
            if (ststusFlage) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102305"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180084","采购订单已生成付款单！") /* "采购订单已生成付款单！" */);
            }
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102306"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180085","没有符合条件的待生单物料！") /* "没有符合条件的待生单物料！" */);
        }
        paramMap.put("omake", omakes);
        recordBusinessLog(omakes);
        return new RuleExecuteResult();
    }

    private void recordBusinessLog(List<Map<String, Object>> oMakes) {
        String code = "";
        try {
            Map<String, Object> map = new HashMap();
            map.put(OMAKE,oMakes);
            code = oMakes.get(0).get("code").toString();
            ctmcmpBusinessLogService.saveBusinessLog(map, code, "",
                    ST_PURCHASE_ORDER_LIST, ST_PURCHASE_ORDER, UPU_PURCHASE_PUSH_PAY_APPLY_BILL);
        } catch (Exception e) {
            log.error("StpurchaseOrderToPayapplicationBillRule, write Business Log, yTenantId = {}, code = {}, e = {}",
                    AppContext.getCurrentUser().getYTenantId(), code, e.getMessage());
        }
    }

}
