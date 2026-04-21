package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oapInit;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.service.OapProcessService;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
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

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;
import static com.yonyoucloud.fi.cmp.constant.IMsgConstant.ARAP_OAP_INIT;
import static com.yonyoucloud.fi.cmp.constant.IMsgConstant.FIARAP_OAP_INIT_PUSH_CTMCMP_PAY_APPLY_BILL;

/**
 * <h1>应付事项期初推付款申请单前置处理规则</h1>
 *
 * code: apEventInitBillPushPayApplicationBill
 * group_code:gcode_apEventInitBillPushPayApplicationBill
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-01-04 11:12
 */
@Slf4j
@Component("apOapInitPushPayApplyBillProposingRule")
@RequiredArgsConstructor
public class ApOapInitPushPayApplyBillProposingRule extends AbstractCommonRule {
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
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100953"),StringUtils.join(messages.toArray(), DELIMITER));
        }
        // 公共数据缓存的Map
        Map<String, Map<String, Object>> ownDataCacheMap = new HashMap<>();
        // 数据处理
        while (iterator.hasNext()) {
            Map<String, Object> oapMainMap = iterator.next();
            List<Map<String, Object>> details = (List<Map<String, Object>>) oapMainMap.get(OAP_DETAIL);
            Iterator<Map<String, Object>> detailsIterator = details.iterator();
            // 遍历分类的map的key
            BigDecimal paymentApplyAmountSum = BigDecimal.ZERO;
            while (detailsIterator.hasNext()) {
                Map<String, Object> mapSub = detailsIterator.next();
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
                    detailsIterator.remove();
                }else {
                    paymentApplyAmountSum = BigDecimalUtils.safeAdd(paymentApplyAmountSum, oriSum);
                }
            }
            if (details.size() == 0) {
                iterator.remove();
                continue;
            }
            // 金额赋值
            oapProcessService.setAmountDefaultValueAndOrg(oapMainMap, paymentApplyAmountSum);
            // 公共数据处理：供应商(默认银行账号，默认开户行地址 .etc),交易类型，款项类型，币种(精度)
            ownDataCacheMap = new ConcurrentHashMap<>(CONSTANT_EIGHT);
            oapProcessService.processOwnCommonData(oapMainMap, ownDataCacheMap, OAP_INIT_APPLY);
            try {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put(OMAKE, mapList);
                // 记录业务日志
                ctmcmpBusinessLogService.saveBusinessLog(logMap, oapMainMap.get(CODE).toString(), "",
                        MARAP_AP_OAPINIT, ARAP_OAP_INIT, FIARAP_OAP_INIT_PUSH_CTMCMP_PAY_APPLY_BILL);
            } catch (Exception e) {
                log.error("apOapInitPushPayApplyBillProposingRule, write Business Log, e = {}", e.getMessage());
            }
        }
        if (CollectionUtils.isEmpty(mapList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100954"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801B6","应付事项已无可付款金额，无法推单！") /* "应付事项已无可付款金额，无法推单！" */);
        }
        paramMap.put(OMAKE, mapList);
        if(ValueUtils.isNotEmptyObj(ownDataCacheMap)) ownDataCacheMap.clear();
        return new RuleExecuteResult();
    }
}
