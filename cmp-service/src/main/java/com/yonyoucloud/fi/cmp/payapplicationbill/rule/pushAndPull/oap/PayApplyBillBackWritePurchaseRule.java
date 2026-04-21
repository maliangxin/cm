package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.event.model.BusinessEventBuilder;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.payapplicationbill.SourceMatters;
import com.yonyoucloud.fi.cmp.util.SendEventMessageUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;


/**
 * <h1>应付事项推送的付款申请通过事件中心回写采购订单/采购发票金额</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021/5/28 14:03
 */
@Slf4j
@Component("payApplyBillBackWritePurchaseRule")
public class PayApplyBillBackWritePurchaseRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > CONSTANT_ZERO) {
            BizObject bizobject = bills.get(CONSTANT_ZERO);
            if(!"fiarap.arap_oap".equals(bizobject.get("source")) && !"yonyoufi.arap_oap".equals(bizobject.getString(SOURCE))){
                return new RuleExecuteResult();
            }
            Short srcItem = MapUtils.getShort(bizobject, SRC_ITEM);
            if (SourceMatters.MattersDue.getValue() != srcItem) {
                return new RuleExecuteResult();
            }
            log.error("Pay apply bill back write source order amount, source type, billId = {}, yTenantId = {}, srcItem = {}, source = {}",
                    bizobject.getId(), InvocationInfoProxy.getTenantid(), srcItem, bizobject.getString(SOURCE));
            String action = billContext.getAction();
            Map<String, BizObject> backWriteMessage = buildMsg(bizobject, action, paramMap);
            if (ValueUtils.isNotEmptyObj(backWriteMessage)) {
                backWritePurchase(backWriteMessage, bizobject, billContext);
            }
        }
        return new RuleExecuteResult();
    }

    @SuppressWarnings("unchecked")
    private Map<String, BizObject> buildMsg(BizObject bizobject, String action, Map<String, Object> paramMap) {
        // 来源事项应为应付事项
        List<Map<String, Object>> originDataMap;
        if (ValueUtils.isNotEmptyObj(bizobject.get(LIST_MAP))) {
            originDataMap = bizobject.get(LIST_MAP);
        } else if (ValueUtils.isNotEmptyObj(paramMap.get(PAY_APPLY_BILL_ORI_SUM))) {
            originDataMap = (List<Map<String, Object>>) paramMap.get(PAY_APPLY_BILL_ORI_SUM);
        } else {
            return null;
        }
        log.error("Pay apply bill back write source order amount, input parameter ,billId = {}, action = {}, originDataMap = {}",
                bizobject.getId(), action, originDataMap);
        if (CollectionUtils.isNotEmpty(originDataMap)) {
            CtmJSONArray poLineJsonArray = new CtmJSONArray();
            CtmJSONArray orderJsonArray = new CtmJSONArray();
            for (Map<String, Object> iter : originDataMap) {
                CtmJSONObject jsonObject = new CtmJSONObject();
                jsonObject.put(TOTAL_PAY_AMOUNT, null);
                jsonObject.put(TOTAL_PAY_APPLY_AMOUNT, iter.get(PAYMENT_APPLY_AMOUNT));
                if (ValueUtils.isNotEmptyObj(iter.get(POLINE_ID)) || ValueUtils.isNotEmptyObj(iter.get(LINE_ID))) {
                    //订单行ID
                    jsonObject.put(POLINE_ID, iter.get(POLINE_ID));
                    //发票行ID
                    jsonObject.put(LINE_ID, iter.get(LINE_ID));
                    poLineJsonArray.add(jsonObject);
                }
                if (ValueUtils.isNotEmptyObj(iter.get(ORDER_ID))) {
                    // 委外行id
                    jsonObject.put(ORDER_ID, iter.get(ORDER_ID));
                    orderJsonArray.add(jsonObject);
                }
            }
            if (SAVE.equals(action)) {
                action = CREATE;
            } else {
                action = DELETE;
            }
            // 组装消息内容
            return createBackWriteMessage(action, poLineJsonArray, orderJsonArray);
        }
        return null;
    }

    @NotNull
    private static Map<String, BizObject> createBackWriteMessage(String action, CtmJSONArray poLineJsonArray, CtmJSONArray orderJsonArray) {
        BizObject poLineBackWriteMessage = null;
        if (!poLineJsonArray.isEmpty()) {
            poLineBackWriteMessage = new BizObject();
            poLineBackWriteMessage.set(ACTION, action);
            poLineBackWriteMessage.set(TYPE, PAYMENT_REQUEST);
            poLineBackWriteMessage.set(USER_OBJECT, poLineJsonArray);

        }
        BizObject orderBackWriteMessage = null;
        if (!orderJsonArray.isEmpty()) {
            orderBackWriteMessage = new BizObject();
            orderBackWriteMessage.set(ACTION, action);
            orderBackWriteMessage.set(TYPE, PAYMENT_REQUEST);
            orderBackWriteMessage.set(USER_OBJECT, orderJsonArray);

        }
        Map<String, BizObject> backWriteMessage = new HashMap<>();
        backWriteMessage.put(PO_LINE, poLineBackWriteMessage);
        backWriteMessage.put(ORDER, orderBackWriteMessage);
        log.error("Pay apply bill back write source order amount, assembling message, backWriteMessage = {}", backWriteMessage);
        return backWriteMessage;
    }

    private void backWritePurchase(Map<String, BizObject> backWriteMessage, BizObject bizobject, BillContext billContext) throws Exception {
        // 给委外订单发消息
        assemblingBackWriteMessage(backWriteMessage, bizobject, billContext, ORDER, PRODUCT_PAYMENT_BACK_WRITE);
        // 给采购订单发消息
        assemblingBackWriteMessage(backWriteMessage, bizobject, billContext, PO_LINE, PAY_APPLY_BILL_TO_UPU);
    }

    private void assemblingBackWriteMessage(Map<String, BizObject> backWriteMessage, BizObject bizobject, BillContext billContext, String order2, String productPaymentBackWrite) throws Exception {
        if (ValueUtils.isNotEmptyObj(backWriteMessage) && ValueUtils.isNotEmptyObj(backWriteMessage.get(order2))) {
            BizObject order = backWriteMessage.get(order2);
            BusinessEventBuilder businessEventBuilder = new BusinessEventBuilder();
            businessEventBuilder.setSourceId(CMP_PAY_APPLY_BILL);
            businessEventBuilder.setEventType(productPaymentBackWrite);
            commonHandler(bizobject, billContext, order, businessEventBuilder);
        }
    }

    private void commonHandler(BizObject bizobject, BillContext billContext, BizObject message, BusinessEventBuilder businessEventBuilder) throws Exception {
        businessEventBuilder.setUserObject(message);
        if (ValueUtils.isNotEmptyObj(bizobject)) {
            businessEventBuilder.setBillId(bizobject.getId().toString());
            businessEventBuilder.setBillCode(bizobject.get("code").toString());
        }
        if (ValueUtils.isNotEmptyObj(billContext)) {
            businessEventBuilder.setBillno(billContext.getBillnum());
        }
        SendEventMessageUtils.sendEventMessageEosByParams(businessEventBuilder);
    }
}

