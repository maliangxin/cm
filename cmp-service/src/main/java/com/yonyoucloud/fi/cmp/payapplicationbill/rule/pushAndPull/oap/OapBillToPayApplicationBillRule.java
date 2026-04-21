package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.factory.SourceOrderTypeStrategyHandlerFactory;
import com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.service.OapProcessService;
import com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.strategy.SourceOrderTypeStrategyHandler;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;
import static com.yonyoucloud.fi.cmp.constant.IMsgConstant.ARAP_OAP;
import static com.yonyoucloud.fi.cmp.constant.IMsgConstant.FIARAP_OAP_PUSH_CTMCMP_PAY_APPLY_BILL;

/**
 * <h1>应付事项推付款申请数据前置处理</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021/6/28 13:40
 */
@Slf4j
@Component("oapBillToPayApplicationBillRule")
@RequiredArgsConstructor
public class OapBillToPayApplicationBillRule extends AbstractCommonRule {
    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;
    private final OapProcessService oapProcessService;
    @Override
    @SuppressWarnings("unchecked")
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<Map<String, Object>> mapList = (List<Map<String, Object>>) paramMap.get(OMAKE);
        Iterator<Map<String, Object>> iterator = mapList.iterator();
        // 数据校验
        List<String> messages = new ArrayList<>(CONSTANT_EIGHT);
        oapProcessService.verificationParameters(mapList, messages);
        if (CollectionUtils.isNotEmpty(messages)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101279"),StringUtils.join(messages.toArray(), DELIMITER));
        }
        // 公共数据缓存的Map
        Map<String, Map<String, Object>> ownDataCacheMap =  new ConcurrentHashMap<>(CONSTANT_EIGHT);
        // 数据处理
        while (iterator.hasNext()) {
            Map<String, Object> oapMainMap = iterator.next();
            List<Map<String, Object>> details = (List<Map<String, Object>>) oapMainMap.get(OAP_DETAIL);
            // 老数据源头订单类型默认值的处理
            oapProcessService.sourceOrderTypeValueProcess(oapMainMap, details);
            // 根据来源订单类型拆分明细数据为已订单类型为key,明细数据为value的map
            Map<Object, List<Map<String, Object>>> detailsMap = details.stream().collect(Collectors.groupingBy(e -> Short.parseShort(e.get(SOURCE_ORDER_TYPE).toString())));
            Iterator<Object> detailsIterator = detailsMap.keySet().iterator();
            List<Map<String, Object>> oapDetailList = new ArrayList<>();
            // 遍历分类的map的key
            while (detailsIterator.hasNext()) {
                short sourceOrderType = Short.parseShort(detailsIterator.next().toString());
                if (!ValueUtils.isNotEmptyObj(detailsMap.get(sourceOrderType))){
                    continue;
                }
                // 根据源头订单类型，调用相应的处理策略类
                SourceOrderTypeStrategyHandler invokeStrategy = SourceOrderTypeStrategyHandlerFactory.getInvokeStrategy(sourceOrderType);
                List<Map<String, Object>> calculation = invokeStrategy.process(detailsMap.get(sourceOrderType), oapMainMap);
                oapDetailList.addAll(calculation);
            }
            if (oapDetailList.size() == 0) {
                iterator.remove();
                continue;
            }
            oapMainMap.put(OAP_DETAIL, oapDetailList);
            BigDecimal paymentApplyAmountSum = new BigDecimal(oapMainMap.get(PAYMENT_APPLY_AMOUNT_SUM).toString());
            // 金额赋值
            oapProcessService.setAmountDefaultValueAndOrg(oapMainMap, paymentApplyAmountSum);
            // 公共数据处理：供应商(默认银行账号，默认开户行地址 .etc),交易类型，款项类型，币种(精度)
            ownDataCacheMap = new ConcurrentHashMap<>(CONSTANT_EIGHT);
            oapProcessService.processOwnCommonData(oapMainMap, ownDataCacheMap, OAPAPPLY);
            try {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put(OMAKE, mapList);
                // 记录业务日志
                ctmcmpBusinessLogService.saveBusinessLog(logMap, oapMainMap.get(CODE).toString(), "",
                        MARAP_OAPLIST, ARAP_OAP, FIARAP_OAP_PUSH_CTMCMP_PAY_APPLY_BILL);
            } catch (Exception e) {
                log.error("OapBillToPayApplicationBillRule, write Business Log, e = {}", e.getMessage());
            }
        }
        if (CollectionUtils.isEmpty(mapList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101280"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180077","应付事项已无可付款金额，无法推单！") /* "应付事项已无可付款金额，无法推单！" */);
        }
        paramMap.put(OMAKE, mapList);
        if(ValueUtils.isNotEmptyObj(ownDataCacheMap)) ownDataCacheMap.clear();
        return new RuleExecuteResult();
    }

}
