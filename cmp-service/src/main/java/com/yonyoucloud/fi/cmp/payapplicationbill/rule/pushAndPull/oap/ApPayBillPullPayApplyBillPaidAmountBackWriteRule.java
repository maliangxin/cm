package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayBillStatus;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>应付付款单自结算时，回写付款申请的已付金额和未付金额以及主表的单据状态</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-05-09 10:03
 */
@Slf4j
@Component("apPayBillPullPayApplyBillPaidAmountBackWriteRule")
public class ApPayBillPullPayApplyBillPaidAmountBackWriteRule extends AbstractCommonRule implements ISagaRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("AP pay bill settle after back write pay apply bill paidAmount, tenant_id = {}, paramMap = {}",
                InvocationInfoProxy.getTenantid(), paramMap);
        List<BizObject> bills = getBills(billContext, paramMap);
        Map<Long, BigDecimal> superPaidAmount = new HashMap<>(CONSTANT_EIGHT);
        Map<Long, BigDecimal> subPaidAmount = new HashMap<>(CONSTANT_EIGHT);
        for (BizObject bill : bills) {
            Object srcFlag = bill.get("srcflag");
            if (!"cmppayapplication".equals(srcFlag)) {
                log.error("AP pay bill settle after back write pay apply bill paidAmount fail ! tenant_id = {}, id = {}, srcFlag = {}",
                        InvocationInfoProxy.getTenantid(), bill.getId(), srcFlag);
                return new RuleExecuteResult();
            }
            Object serviceAttr = bill.get(SERVICE_ATTR);
            if (!ValueUtils.isNotEmptyObj(serviceAttr) || CONSTANT_ZERO.equals(serviceAttr) || CONSTANT_ONE.equals(serviceAttr)) {
                log.error("AP pay bill settle after back write pay apply bill paidAmount fail ! settleMode not exist,tenant_id = {}, id = {}, serviceAttr = {}",
                        InvocationInfoProxy.getTenantid(), bill.getId(), serviceAttr);
                return new RuleExecuteResult();
            }
            List<BizObject> children = bill.get(PAY_BILL_B);
            if (!ValueUtils.isNotEmptyObj(children)) {
                log.error("AP pay bill settle after back write pay apply bill paidAmount fail! Bill is Empty , tenant_id = {}, bill = {}",
                        InvocationInfoProxy.getTenantid(), bill);
                return new RuleExecuteResult();
            }
            String action = billContext.getAction();
            // 处理子表已付金额和未付金额
            processSubPayApplicationBillPaidAmount(superPaidAmount, subPaidAmount, children, action);
            // 处理主表已付金额和未付金额以及单据状态
            processSuperPayApplicationBillPaidAmount(superPaidAmount, action);
        }
        return new RuleExecuteResult();
    }

    private void processSubPayApplicationBillPaidAmount(Map<Long, BigDecimal> superPaidAmount, Map<Long, BigDecimal> subPaidAmount, List<BizObject> children, String action) throws Exception {
        for (BizObject bizObject : children) {
            Object srcBillItemId = bizObject.get(SRCBILLITEMID);
            long subId;
            if (ValueUtils.isNotEmptyObj(srcBillItemId)) {
                subId = Long.parseLong(srcBillItemId.toString());
            } else {
                log.error("not find pay bill association pay apply bill sub id, tenant_id = {}, payBillSubId = {}",
                        InvocationInfoProxy.getTenantid(), bizObject.getId());
                continue;
            }
            subPaidAmount.put(subId, bizObject.get(ORISUM));
        }
        Long[] subIds = subPaidAmount.keySet().toArray(new Long[0]);
        List<PayApplicationBill_b> payApplicationBillSubList = new ArrayList<>(CONSTANT_EIGHT);
        List<Map<String, Object>> subPayApplyBillMap = MetaDaoHelper.queryByIds(PayApplicationBill_b.ENTITY_NAME, "*", subIds);
        subPayApplyBillMap.forEach(sub -> {
            PayApplicationBill_b payApplicationBillB = new PayApplicationBill_b();
            payApplicationBillB.init(sub);
            payApplicationBillSubList.add(payApplicationBillB);
        });
        List<PayApplicationBill_b> payApplicationBillSubListRollBack = new ArrayList<>();
        for (PayApplicationBill_b objectMap : payApplicationBillSubList) {
            payApplicationBillSubListRollBack.add((PayApplicationBill_b)SerializationUtils.clone((Serializable)objectMap));
        }
        YtsContext.setYtsContext("payApplicationBillSubList", payApplicationBillSubListRollBack);
        payApplicationBillSubList.forEach(payApplicationBillSub -> {
            Long mainId = payApplicationBillSub.getMainid();
            Long id = payApplicationBillSub.getId();
            BigDecimal oriSum = subPaidAmount.get(id);
            if (AUDIT.equals(action)) {
                if (ValueUtils.isNotEmptyObj(payApplicationBillSub.getPaidAmount())) {
                    payApplicationBillSub.setPaidAmount(BigDecimalUtils.safeAdd(payApplicationBillSub.getPaidAmount(), oriSum));
                } else {
                    payApplicationBillSub.setPaidAmount(oriSum);
                }
                payApplicationBillSub.setUnpaidAmount(BigDecimalUtils.safeSubtract(payApplicationBillSub.getUnpaidAmount(), oriSum));
            } else if (UN_AUDIT.equals(action)) {
                payApplicationBillSub.setPaidAmount(BigDecimalUtils.safeSubtract(payApplicationBillSub.getPaidAmount(), oriSum));
                payApplicationBillSub.setUnpaidAmount(BigDecimalUtils.safeAdd(payApplicationBillSub.getUnpaidAmount(), oriSum));
            }
            if (ValueUtils.isNotEmptyObj(superPaidAmount.get(mainId))) {
                superPaidAmount.put(mainId, BigDecimalUtils.safeAdd(superPaidAmount.get(mainId), oriSum));
            } else {
                superPaidAmount.put(mainId, oriSum);
            }
        });
        log.error("AP pay bill settle after back write pay apply bill paidAmount, action, ={}, tenant_id = {}, subPaidAmount = {}",
                InvocationInfoProxy.getTenantid(), subPaidAmount, action);
        if (CollectionUtils.isNotEmpty(payApplicationBillSubList)) {
            EntityTool.setUpdateStatus(payApplicationBillSubList);
            MetaDaoHelper.update(PayApplicationBill_b.ENTITY_NAME, payApplicationBillSubList);
        }
    }

    private void processSuperPayApplicationBillPaidAmount(Map<Long, BigDecimal> superPaidAmount, String action) throws Exception {
        log.error("AP pay bill settle after back write pay apply bill paidAmount, action = {}, tenant_id = {}, superPaidAmount = {}",
                InvocationInfoProxy.getTenantid(), superPaidAmount, action);
        Long[] superIds = superPaidAmount.keySet().toArray(new Long[0]);
        List<PayApplicationBill> payApplicationBillSuperList = new ArrayList<>(CONSTANT_EIGHT);
        List<Map<String, Object>> superPayApplyBill = MetaDaoHelper.queryByIds(PayApplicationBill.ENTITY_NAME, "*", superIds);
        superPayApplyBill.forEach(supper -> {
            PayApplicationBill payApplicationBill = new PayApplicationBill();
            payApplicationBill.init(supper);
            payApplicationBillSuperList.add(payApplicationBill);
        });
        List<PayApplicationBill> payApplicationBillSuperListRollBack = new ArrayList<>();
        for (PayApplicationBill objectMap : payApplicationBillSuperList) {
            payApplicationBillSuperListRollBack.add((PayApplicationBill)SerializationUtils.clone((Serializable)objectMap));
        }
        YtsContext.setYtsContext("payApplicationBillSuperList", payApplicationBillSuperListRollBack);
        payApplicationBillSuperList.forEach(payApplicationBill -> {
            Long id = payApplicationBill.getId();
            BigDecimal oriSum = superPaidAmount.get(id);
            if (AUDIT.equals(action)) {
                if (oriSum.equals(payApplicationBill.getPaidAmountSum())) {
                    payApplicationBill.setPaidAmountSum(new BigDecimal(0));
                } else {
                    payApplicationBill.setPaidAmountSum(BigDecimalUtils.safeAdd(payApplicationBill.getPaidAmountSum(), oriSum));
                }
                payApplicationBill.setUnpaidAmountSum(BigDecimalUtils.safeSubtract(payApplicationBill.getUnpaidAmountSum(), oriSum));
                EntityTool.setUpdateStatus(payApplicationBill);
                if (payApplicationBill.getPaidAmountSum().equals(payApplicationBill.getPaymentApplyAmountSum())) {
                    payApplicationBill.setPayBillStatus(PayBillStatus.PaymentCompleted);
                } else {
                    payApplicationBill.setPayBillStatus(PayBillStatus.PartialPayment);
                }
            } else if (UN_AUDIT.equals(action)) {
                if (oriSum.equals(payApplicationBill.getPaidAmountSum())) {
                    payApplicationBill.setPaidAmountSum(new BigDecimal(0));
                } else {
                    payApplicationBill.setPaidAmountSum(BigDecimalUtils.safeSubtract(payApplicationBill.getPaidAmountSum(), oriSum));
                }
                payApplicationBill.setUnpaidAmountSum(BigDecimalUtils.safeAdd(payApplicationBill.getUnpaidAmountSum(), oriSum));
                EntityTool.setUpdateStatus(payApplicationBill);
                if (new BigDecimal(0).equals(payApplicationBill.getUnpaidAmountSum()) || null == payApplicationBill.getUnpaidAmountSum()) {
                    payApplicationBill.setPayBillStatus(PayBillStatus.PaymentCompleted);
                } else if (new BigDecimal(0).equals(payApplicationBill.getPaidAmountSum()) || null == payApplicationBill.getPaidAmountSum()) {
                    payApplicationBill.setPayBillStatus(PayBillStatus.PendingPayment);
                } else if (!payApplicationBill.getUnpaidAmountSum().equals(payApplicationBill.getPaidAmountSum())
                        && !new BigDecimal(0).equals(payApplicationBill.getUnpaidAmountSum())
                        && !new BigDecimal(0).equals(payApplicationBill.getPaidAmountSum())) {
                    payApplicationBill.setPayBillStatus(PayBillStatus.PartialPayment);
                } else if (new BigDecimal(0).equals(payApplicationBill.getPaymentApplyAmountSum()) || null == payApplicationBill.getPaymentApplyAmountSum()) {
                    payApplicationBill.setPayBillStatus(PayBillStatus.PendingApproval);
                }
            }
        });
        if (CollectionUtils.isNotEmpty(payApplicationBillSuperList)) {
            EntityTool.setUpdateStatus(payApplicationBillSuperList);
            MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplicationBillSuperList);
        }
    }

    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(log.isInfoEnabled()) {
            log.info("AP pay bill settle after back write pay apply bill paidAmount fail, payApplicationBillSubList = {}", JsonUtils.toJson(YtsContext.getYtsContext("payApplicationBillSubList")));
        }
        List<PayApplicationBill_b> payApplicationBillSubList = (List<PayApplicationBill_b>) YtsContext.getYtsContext("payApplicationBillSubList");
        if (ValueUtils.isNotEmptyObj(payApplicationBillSubList)) {
            List<PayApplicationBill_b> payApplicationBillSubListUpdate = new ArrayList<>();
            for (PayApplicationBill_b payApplicationBillB : payApplicationBillSubList) {
                PayApplicationBill_b payApplicationBillBOld = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, payApplicationBillB.getId(),1);
                payApplicationBillBOld.setPaidAmount(payApplicationBillB.getPaidAmount());
                payApplicationBillBOld.setUnpaidAmount(payApplicationBillB.getUnpaidAmount());
                payApplicationBillSubListUpdate.add(payApplicationBillBOld);
            }
            EntityTool.setUpdateStatus(payApplicationBillSubListUpdate);
            MetaDaoHelper.update(PayApplicationBill_b.ENTITY_NAME, payApplicationBillSubListUpdate);
        }
        if(log.isInfoEnabled()) {
            log.info("AP pay bill settle after back write pay apply bill paidAmount fail, payApplicationBillSuperList = {}", JsonUtils.toJson(YtsContext.getYtsContext("payApplicationBillSuperList")));
        }
        List<PayApplicationBill> payApplicationBillSuperList = (List<PayApplicationBill>) YtsContext.getYtsContext("payApplicationBillSuperList");
        if (ValueUtils.isNotEmptyObj(payApplicationBillSuperList)) {
            List<PayApplicationBill> payApplicationBillSuperListUpdate = new ArrayList<>();
            for (PayApplicationBill payApplicationBill : payApplicationBillSuperList) {
                PayApplicationBill payApplicationBillOld = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, payApplicationBill.getId(),1);
                payApplicationBillOld.setPaidAmountSum(payApplicationBill.getPaidAmountSum());
                payApplicationBillOld.setUnpaidAmountSum(payApplicationBill.getUnpaidAmountSum());
                payApplicationBillOld.setPayBillStatus(payApplicationBill.getPayBillStatus());
                payApplicationBillSuperListUpdate.add(payApplicationBillOld);
            }
            EntityTool.setUpdateStatus(payApplicationBillSuperListUpdate);
            MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplicationBillSuperListUpdate);
        }
        return new RuleExecuteResult();
    }
}
