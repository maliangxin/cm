package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.strategy;


import com.google.common.collect.Maps;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.payapplicationbill.SourceOrderType;
import com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.factory.SourceOrderTypeStrategyHandlerFactory;
import com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.service.OapProcessService;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>来源源头是采购订单的应付事项单据</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-11-18 13:19
 */
@Component
@Slf4j
public class PurchaseOrderStrategyHandlerImpl implements SourceOrderTypeStrategyHandler {

    private OapProcessService oapProcessService;

    @Autowired
    public void setOapProcessService(OapProcessService oapProcessService) {
        this.oapProcessService = oapProcessService;
    }

    @Override
    public List<Map<String, Object>> process(List<Map<String, Object>> fromTopPurchaseOrderList, Map<String, Object> map) throws Exception {
        // 计算源头为采购订单的应付事项可申请金额
        BigDecimal paymentApplyAmountSumPurchaseAmount = BigDecimal.ZERO;
        // 处理源头订单的主子表id
        oapProcessService.sourceOrderDataHandle(fromTopPurchaseOrderList);
        // 计算此来源分支的累计付款申请金额
        calculatePurchaseRequestedAmount(paymentApplyAmountSumPurchaseAmount, fromTopPurchaseOrderList);
        // 计算完可付款申请金额后，若没有可付款申请金额则直接返回
        if (oapProcessService.paymentApplyAmountSumProcess(fromTopPurchaseOrderList, map, paymentApplyAmountSumPurchaseAmount))
            return fromTopPurchaseOrderList;
        // 处理采购发票id和明细行
        oapProcessService.invoiceAndSupplierCommonHandler(map, fromTopPurchaseOrderList);
        return fromTopPurchaseOrderList;
    }

    @Override
    public void afterPropertiesSet() {
        SourceOrderTypeStrategyHandlerFactory.register(SourceOrderType.PurchaseOrder.getValue(), this);
    }

    /**
     * <h2>组装请求参数，请求采购订单接口，获取可付款申请金额</h2>
     *
     * @param paymentApplyAmountSum : 累计付款申请金额
     * @param fromTopPurchaseOrderList : 明细数据集合
     * @author Sun GuoCai
     * @since 2022/11/19 17:01
     */
    private void calculatePurchaseRequestedAmount(BigDecimal paymentApplyAmountSum, List<Map<String, Object>> fromTopPurchaseOrderList) {
        Map<String, List<Map<String, Object>>> listMap = fromTopPurchaseOrderList.stream().map(bill -> {
            BigDecimal balance = ValueUtils.isNotEmptyObj(bill.get(BALANCE)) ? (BigDecimal) bill.get(BALANCE) : BigDecimal.ZERO;
            BigDecimal occupyAmount = ValueUtils.isNotEmptyObj(bill.get(OCCUPYAMOUNT)) ? (BigDecimal) bill.get(OCCUPYAMOUNT) : BigDecimal.ZERO;
            BigDecimal value = BigDecimalUtils.safeSubtract(balance, occupyAmount);
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            String key = ValueUtils.isNotEmptyObj(bill.get(TOP_SRC_BILL_ITEM_ID)) ? bill.get(TOP_SRC_BILL_ITEM_ID).toString() : null;
            bill.put(ICmpConstant.GROUP_SUM_KEY, key);
            bill.put(ICmpConstant.GROUP_SUM_MONEY, value);
            return bill;
        }).filter(Objects::nonNull).collect(Collectors.groupingBy(b -> b.get(ICmpConstant.GROUP_SUM_KEY).toString()));
        Map<String, BigDecimal> map1 = Maps.newHashMap();
        listMap.forEach((k, v) -> {
            BigDecimal sum = BigDecimal.ZERO;
            if (CollectionUtils.isNotEmpty(v)) {
                sum = v.stream().map(p -> {
                    BigDecimal mo = (BigDecimal) p.get(ICmpConstant.GROUP_SUM_MONEY);
                    return mo;
                }).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            }
            map1.put(k, sum);
        });

        log.error("oap push pay apply bill, get upu small balance data, map = {}", map1);
        // 发送请求，获取可付款申请金额
        Map<String, BigDecimal> resultMap = queryPurchaseOrderRequestedAmount(map1);
        Iterator<Map<String, Object>> iterator = fromTopPurchaseOrderList.iterator();
        // 处理可付款申请金额，并计算累计付款申请金额
        while (iterator.hasNext()) {
            Map<String, Object> mapSub = iterator.next();
            if (!ValueUtils.isNotEmptyObj(mapSub.get(ICmpConstant.GROUP_SUM_KEY))) {
                iterator.remove();
                continue;
            }
            String key = mapSub.get(ICmpConstant.GROUP_SUM_KEY).toString();
            BigDecimal maxSum = ValueUtils.isNotEmptyObj(resultMap.get(key)) ? resultMap.get(key) :BigDecimal.ZERO  ;
            BigDecimal maxDetailMoney = (BigDecimal) mapSub.get(ICmpConstant.GROUP_SUM_MONEY);
            if (maxDetailMoney.compareTo(maxSum) < 0) {
                mapSub.put(IBussinessConstant.ORI_SUM, maxDetailMoney);
                resultMap.put(key, maxSum.subtract(maxDetailMoney));
                paymentApplyAmountSum = BigDecimalUtils.safeAdd(paymentApplyAmountSum, maxDetailMoney);
            } else {
                if (BigDecimal.ZERO.compareTo(maxSum) >= 0) {
                    iterator.remove();
                } else {
                    mapSub.put(IBussinessConstant.ORI_SUM, maxSum);
                    resultMap.put(key, maxSum.subtract(maxSum));
                    paymentApplyAmountSum = BigDecimalUtils.safeAdd(paymentApplyAmountSum, maxSum);
                }
            }
        }
    }

    /**
     * <h2>请求采购订单提供的批量接口，获取可付款申请金额</h2>
     *
     * @param map : 批量入参参数
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2022/11/19 17:04
     */
    private Map<String, BigDecimal> queryPurchaseOrderRequestedAmount(Map<String, BigDecimal> map) {
        String upuUrl = null;
        String json = null;
        try {
            upuUrl = AppContext.getEnvConfig(USTOCK_SERVER_NAME);
            json = CtmJSONObject.toJSONString(map);
            upuUrl += GET_SHOULD_PAY;
            upuUrl += InvocationInfoProxy.getYhtAccessToken();
            Map<String, String> headersMap = new HashMap<>(CONSTANT_EIGHT);
            // 执行post请求的方法
            String resultJson = HttpTookit.doPostWithJson(upuUrl, json, headersMap);
            log.error("Get pay apply balance from upu, map={}, data={}", map, resultJson);
            if (ValueUtils.isNotEmptyObj(resultJson)) {
                CtmJSONObject jsonObject = CtmJSONObject.parseObject(resultJson);
                if (jsonObject.isEmpty()) {
                    return Maps.newHashMap();
                }
                Map<String, BigDecimal> reslt = Maps.newHashMap();
                map.forEach((k, v) -> {
                    Object ob = jsonObject.get(k);
                    if (ob == null) {
                        return;
                    }
                    CtmJSONObject obj = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(ob));
                    BigDecimal max = new BigDecimal(obj.get(ICmpConstant.APPLY_PAY).toString());
                    if (ValueUtils.isNotEmptyObj(obj.get(ICmpConstant.APPLY_PAY)) && (max.compareTo(BigDecimal.ZERO) > 0)) {
                        reslt.put(k, max);
                    }
                });
                return reslt;
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102237"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180287","单据【") /* "单据【" */ + map.get("code").toString() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180288","获取剩余付款或者付款申请金额失败！") /* "获取剩余付款或者付款申请金额失败！" */);
            }
        } catch (Exception e) {
            log.error("oap push pay apply bill, get upu small balance: upuUrl = {}, json = {}, e = {}", upuUrl, json, e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102237"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180287","单据【") /* "单据【" */ + map.get("code").toString() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180288","获取剩余付款或者付款申请金额失败！") /* "获取剩余付款或者付款申请金额失败！" */);
        }
    }

}
