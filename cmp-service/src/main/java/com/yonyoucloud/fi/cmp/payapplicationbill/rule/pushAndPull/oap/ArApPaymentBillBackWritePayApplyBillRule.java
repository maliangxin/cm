package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayBillStatus;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ValueWrapper;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>应付管理的付款单回写付款申请单</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021-03-26 10:50
 */
@Slf4j
@Component("arApPaymentBillBackWritePayApplyBillRule")
public class ArApPaymentBillBackWritePayApplyBillRule extends AbstractCommonRule implements ISagaRule {
    @Override
    @SuppressWarnings("unchecked")
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("payment back write pay apply bill, paramMap = {}", JsonUtils.toJson(paramMap));
        Map<String, List<BizObject>> allMap = new HashMap<>(CONSTANT_EIGHT);
        if (paramMap.get(BACK_SOURCE_MAP) != null) {
            allMap.putAll((Map<String, List<BizObject>>) paramMap.get(BACK_SOURCE_MAP));
        }
        Map<Long, String> map = new HashMap<>(CONSTANT_EIGHT);
        List<PayApplicationBill> payApplyBillList = new ArrayList<>(CONSTANT_EIGHT);
        for (Map.Entry<String, List<BizObject>> entry : allMap.entrySet()) {
            if (PayApplicationBill.ENTITY_NAME.equals(entry.getKey())) {
                List<BizObject> bizObjectList = allMap.get(entry.getKey());
                for (BizObject bizObject : bizObjectList) {
                    Long mainId = bizObject.get(ID);
                    List<BizObject> payApplicationBillChildList = bizObject.get(PAY_APPLICATION_BILL_B);
                    log.error("payment back write pay apply bill, payApplicationBillChildList = {}", JsonUtils.toJson(payApplicationBillChildList));
                    BigDecimal sumAmount = BigDecimal.ZERO;
                    for (BizObject biz : payApplicationBillChildList) {
                        ValueWrapper valueWrapper = biz.get(PAYMENT_PREEMPT_AMOUNT);
                        if (ValueUtils.isNotEmptyObj(valueWrapper)) {
                            boolean inc = valueWrapper.isInc();
                            BigDecimal value = valueWrapper.value();
                            if (inc) {
                                sumAmount = BigDecimalUtils.safeAdd(sumAmount, value);
                            } else {
                                sumAmount = BigDecimalUtils.safeSubtract(sumAmount, value);
                            }
                        }
                    }
                    PayApplicationBill payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, mainId, 1);
                    if (ValueUtils.isNotEmptyObj(payApplicationBill)) {
                        BigDecimal amount = ValueUtils.isNotEmptyObj(payApplicationBill.getPaymentPreemptAmountSum()) ?
                                payApplicationBill.getPaymentPreemptAmountSum() : BigDecimal.ZERO;
                        map.put(payApplicationBill.getId(), amount + "," + payApplicationBill.getPayBillStatus().getValue());
                        payApplicationBill.setPaymentPreemptAmountSum(BigDecimalUtils.safeAdd(payApplicationBill.getPaymentPreemptAmountSum(), sumAmount));
                        boolean flag = (null == payApplicationBill.getPaymentPreemptAmountSum() || (payApplicationBill.getPaymentPreemptAmountSum().compareTo(BigDecimal.ZERO) == 0));
                        if (flag) {
                            payApplicationBill.setPayBillStatus(PayBillStatus.PendingApproval);
                        }
                        payApplyBillList.add(payApplicationBill);
                    }
                }
            }

        }
        if (CollectionUtils.isEmpty(payApplyBillList)) {
            return new RuleExecuteResult();
        }
        EntityTool.setUpdateStatus(payApplyBillList);
        MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplyBillList);
        YtsContext.setYtsContext(PAY_APPLY_BILL, map);
        return new RuleExecuteResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        Map<Long, String> payApplyBillMap = (Map<Long, String>) YtsContext.getYtsContext(PAY_APPLY_BILL);
        log.error("payment back write pay apply bill, cancel payApplyBillMap = {}", JsonUtils.toJson(payApplyBillMap));
        List<PayApplicationBill> payApplyBillList = new ArrayList<>();
        for (Map.Entry<Long, String> entry : payApplyBillMap.entrySet()) {
            PayApplicationBill payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, entry.getKey(), 1);
            if (ValueUtils.isNotEmptyObj(entry.getValue())) {
                String[] split = entry.getValue().split(",");
                String amount = split[0];
                if (ValueUtils.isNotEmptyObj(amount)) {
                    payApplicationBill.setPaymentPreemptAmountSum(new BigDecimal(split[0]));
                }
                payApplicationBill.setPayBillStatus(PayBillStatus.find(Short.parseShort(split[1])));
                payApplyBillList.add(payApplicationBill);
            }
        }
        EntityTool.setUpdateStatus(payApplyBillList);
        log.error("payment back write pay apply bill, cancel payApplyBillList = {}", JsonUtils.toJson(payApplyBillList));
        MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplyBillList);
        return new RuleExecuteResult();
    }

}
