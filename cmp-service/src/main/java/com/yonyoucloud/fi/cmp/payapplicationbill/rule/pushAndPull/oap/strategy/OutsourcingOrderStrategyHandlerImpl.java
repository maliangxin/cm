package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.strategy;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.payapplicationbill.SourceOrderType;
import com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.factory.SourceOrderTypeStrategyHandlerFactory;
import com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.service.OapProcessService;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>来源源头是委外订单的应付事项单据</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-11-18 13:23
 */
@Component
@Slf4j
public class OutsourcingOrderStrategyHandlerImpl implements SourceOrderTypeStrategyHandler{

    private OapProcessService oapProcessService;

    @Autowired
    public void setOapProcessService(OapProcessService oapProcessService) {
        this.oapProcessService = oapProcessService;
    }

    @Override
    public List<Map<String, Object>> process(List<Map<String, Object>> fromTopOutsourcingOrderList, Map<String, Object> map) throws Exception {
        // 计算源头为委外订单的应付事项可申请金额
        BigDecimal paymentApplyAmountSumOutsourcingAmount = BigDecimal.ZERO;
        calculateOutsourcingRequestedAmount(map, paymentApplyAmountSumOutsourcingAmount, fromTopOutsourcingOrderList);
        // 计算完可付款申请金额后，若没有可付款申请金额则直接返回
        if (oapProcessService.paymentApplyAmountSumProcess(fromTopOutsourcingOrderList, map, paymentApplyAmountSumOutsourcingAmount)) return fromTopOutsourcingOrderList;
        // 处理源头订单的主子表id
        oapProcessService.sourceOrderDataHandle(fromTopOutsourcingOrderList);
        // 处理采购发票id和明细行
        oapProcessService.invoiceAndSupplierCommonHandler(map, fromTopOutsourcingOrderList);
        return fromTopOutsourcingOrderList;
    }


    @Override
    public void afterPropertiesSet() {
        SourceOrderTypeStrategyHandlerFactory.register(SourceOrderType.OutsourcedOrder.getValue(), this);
    }

    /**
     * <h2>组装请求参数，请求采购订单接口，获取可付款申请金额</h2>
     *
     * @param map : 表头数据
     * @param paymentApplyAmountSum : 累计付款申请金额
     * @param fromTopPurchaseOrderList : 明细数据集合
     * @author Sun GuoCai
     * @since 2022/11/19 17:01
     */
    private void calculateOutsourcingRequestedAmount(Map<String, Object> map, BigDecimal paymentApplyAmountSum, List<Map<String, Object>> fromTopPurchaseOrderList) throws Exception {
        Iterator<Map<String, Object>> iterator1 = fromTopPurchaseOrderList.iterator();
        Map<String, Object> params = new HashMap<>(CONSTANT_EIGHT);
        ArrayList<Long> list = new ArrayList<>();
        Map<String, BigDecimal> oapDetailRequestAmount = new HashMap<>();
        // 组装参数
        while (iterator1.hasNext()) {
            Map<String, Object> mapSub = iterator1.next();
            Long key = Long.parseLong(mapSub.get(TOP_SRC_BILL_ITEM_ID).toString());
            list.add(key);
            log.error("oap push pay apply bill, get outsourcing small balance data, maps = {}, params = {}", list, params);
            BigDecimal balance = (BigDecimal) mapSub.get(BALANCE);
            BigDecimal occupyAmount = BigDecimal.ZERO;
            if (mapSub.get(OCCUPYAMOUNT) != null) {
                occupyAmount = (BigDecimal) mapSub.get(OCCUPYAMOUNT);
            }
            BigDecimal oriSum = BigDecimalUtils.safeSubtract(balance, occupyAmount);
            if(oriSum.compareTo(BigDecimal.ZERO) <= 0) {
                iterator1.remove();
                continue;
            }
            oapDetailRequestAmount.put(mapSub.get(ID).toString().concat("_")
                    .concat(mapSub.get(TOP_SRC_BILL_ITEM_ID).toString()), oriSum);
        }
        params.put(PAYMENT_TYPE, REQUESTED_PAYMENT);
        params.put(PRODUCT_IDS, list);
        // 发送请求，获取可付款申请金额
        CtmJSONObject jsonObject = oapProcessService.queryOutsourcingOrderRequestedAmount(map, params);
        log.error("oap push pay apply bill, get outsourcing small balance data, oapDetailRequestAmount={}, jsonObject = {}"
                ,oapDetailRequestAmount, CtmJSONObject.toJSONString(jsonObject));
        if (!ValueUtils.isNotEmptyObj(jsonObject)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102605"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807DB","单据【"
                    + map.get(CODE).toString()
                    + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807DC","获取源头委外订单剩余付款申请金额失败！") /* "获取源头委外订单剩余付款申请金额失败！" */));
        }
        Iterator<Map<String, Object>> iterator2 = fromTopPurchaseOrderList.iterator();
        // 处理可付款申请金额，并计算累计付款申请金额
        while (iterator2.hasNext()) {
            Map<String, Object> mapSub = iterator2.next();
            String topSrcBillItemId = mapSub.get(TOP_SRC_BILL_ITEM_ID).toString();
            if (ValueUtils.isNotEmptyObj(jsonObject.get(topSrcBillItemId)) && (new BigDecimal(jsonObject.get(topSrcBillItemId).toString()).compareTo(BigDecimal.ZERO) > CONSTANT_ZERO)) {
                mapSub.put(PAID_AMOUNT, BigDecimal.ZERO);
                BigDecimal orderRequestAmount = new BigDecimal(jsonObject.get(topSrcBillItemId).toString());
                String concatId = mapSub.get(ID).toString().concat("_").concat(topSrcBillItemId);
                BigDecimal oapRequestAmount = new BigDecimal(oapDetailRequestAmount.get(concatId).toString());
                BigDecimal requestedAmount = orderRequestAmount.compareTo(oapRequestAmount) >= 0 ? oapRequestAmount : orderRequestAmount;
                mapSub.put(IBussinessConstant.ORI_SUM, requestedAmount);
                jsonObject.put(topSrcBillItemId, BigDecimalUtils.safeSubtract(orderRequestAmount, requestedAmount));
                paymentApplyAmountSum = BigDecimalUtils.safeAdd(paymentApplyAmountSum, new BigDecimal(jsonObject.get(topSrcBillItemId).toString()));
            } else {
                iterator2.remove();
            }
        }
    }

}
