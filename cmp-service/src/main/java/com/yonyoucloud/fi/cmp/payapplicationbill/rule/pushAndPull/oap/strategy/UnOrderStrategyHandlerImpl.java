package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.strategy;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>来源源头不是采购订单和委外订单的应付事项单据</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-11-18 13:23
 */
@Component
@Slf4j
public class UnOrderStrategyHandlerImpl implements SourceOrderTypeStrategyHandler{
    private OapProcessService oapProcessService;
    @Autowired
    public void setOapProcessService(OapProcessService oapProcessService) {
        this.oapProcessService = oapProcessService;
    }
    @Override
    public List<Map<String, Object>> process(List<Map<String, Object>> fromArOapList, Map<String, Object> map) throws Exception {
        BigDecimal paymentApplyAmountSumOapAmount = BigDecimal.ZERO;
        // 计算应付事项可申请金额
        calculateOapAmount(paymentApplyAmountSumOapAmount, fromArOapList);
        // 计算完可付款申请金额后，若没有可付款申请金额则直接返回
        if (oapProcessService.paymentApplyAmountSumProcess(fromArOapList, map, paymentApplyAmountSumOapAmount)) return fromArOapList;
        // 处理采购发票id和明细行
        oapProcessService.invoiceAndSupplierCommonHandler(map, fromArOapList);
        return fromArOapList;
    }

    @Override
    public void afterPropertiesSet() {
        SourceOrderTypeStrategyHandlerFactory.register(SourceOrderType.MattersOap.getValue(), this);
    }

    /**
     * <h2>计算可付款申请金额</h2>
     *
     * @param paymentApplyAmountSum : 累计付款申请金额
     * @param fromArOapList : 非订单类型的明细数据集合
     * @author Sun GuoCai
     * @since 2022/11/19 17:14
     */
    private void calculateOapAmount(BigDecimal paymentApplyAmountSum, List<Map<String, Object>> fromArOapList) {
        if (ValueUtils.isNotEmptyObj(fromArOapList)) {
            Iterator<Map<String, Object>> mapIterator = fromArOapList.iterator();
            while (mapIterator.hasNext()) {
                Map<String, Object> next = mapIterator.next();
                paymentApplyAmountSum = nonOrderRequestedAmountProcess(paymentApplyAmountSum, mapIterator, next);
            }
        }
    }


    /**
     * <h2>非订单类型的应付事项可付款申请金额计算方式：余额（balance）-预占用（occupyAmount）</h2>
     *
     * @param paymentApplyAmountSum : 累计付款申请金额
     * @param iter : 非订单类型的明细数据集合
     * @param mapSub : 应付事项明细数据
     * @return java.math.BigDecimal
     * @author Sun GuoCai
     * @since 2022/11/19 17:15
     */
    private BigDecimal nonOrderRequestedAmountProcess(BigDecimal paymentApplyAmountSum, Iterator<Map<String, Object>> iter, Map<String, Object> mapSub) {
        BigDecimal oriSum;
        BigDecimal balance = (BigDecimal) mapSub.get(BALANCE);
        BigDecimal occupyAmount = BigDecimal.ZERO;
        if (mapSub.get(OCCUPYAMOUNT) != null) {
            occupyAmount = (BigDecimal) mapSub.get(OCCUPYAMOUNT);
        }
        mapSub.put(PAID_AMOUNT, BigDecimal.ZERO);
        oriSum = BigDecimalUtils.safeSubtract(balance, occupyAmount);
        mapSub.put(IBussinessConstant.ORI_SUM, oriSum);
        if (null == mapSub.get(BALANCE) || (BigDecimal.ZERO.compareTo(oriSum) >= CONSTANT_ZERO)) {
            iter.remove();
        } else {
            paymentApplyAmountSum = BigDecimalUtils.safeAdd(paymentApplyAmountSum, oriSum);
        }
        return paymentApplyAmountSum;
    }


}