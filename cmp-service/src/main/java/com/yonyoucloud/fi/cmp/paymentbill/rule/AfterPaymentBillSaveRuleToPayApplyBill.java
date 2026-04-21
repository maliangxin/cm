package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayBillStatus;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>付款单拉取付款申请单保存数据时</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-16 10:23
 */
@Slf4j
@Component
public class AfterPaymentBillSaveRuleToPayApplyBill extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103013"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C253A604E00001", "在财务新架构环境下，不允许拉取付款申请单保存付款单。") /* "在财务新架构环境下，不允许拉取付款申请单保存付款单。" */);
        }
        String childrenField = MetaDaoHelper.getChilrenField(billContext.getFullname());
        BillDataDto item = (BillDataDto) this.getParam(paramMap);
        //获取前端传输内容
        List<BizObject> bills = BillInfoUtils.decodeBills(billContext, item.getData());
        bills.forEach(e -> {
            if (Short.parseShort(e.get(BILLTYPE).toString()) == EventType.PayApplyBill.getValue()) {
                if (e.getEntityStatus().name().equals(INSERT)) {
                    try {
                        List<BizObject> lines = e.get(childrenField);
                        insertPaymentByPayApplyBill(lines);
                    } catch (Exception exception) {
                        log.error("新增单据时，更新付款申请单预占金额异常", exception);
                    }
                } else if (e.getEntityStatus().name().equals(UPDATE)) {
                    List<BizObject> lines = e.get(childrenField);
                    if (ValueUtils.isNotEmptyObj(lines)) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<Long, BigDecimal> map_original = (Map<Long, BigDecimal>) paramMap.get("map_original");
                            Long mainid = null;
                            BigDecimal preemptAmountSum = null;
                            for (BizObject line : lines) {
                                // 付款单子表存的付款申请单子表的ID
                                Long srcbillitemid = Long.valueOf(line.get(SRCBILLITEMID).toString());
                                // 付款单子表的付款金额
                                BigDecimal oriSum = line.get(ORISUM);
                                log.info(" dealWith delete payment detail, Status = {}", line.getEntityStatus().name());
                                if (line.getEntityStatus().name().equals("Delete")) {
                                    PayApplicationBill_b payApplicationBill_b = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, srcbillitemid);
                                    mainid = payApplicationBill_b.getMainid();
                                    payApplicationBill_b.setPaymentPreemptAmount(BigDecimalUtils.safeSubtract(payApplicationBill_b.getPaymentPreemptAmount(), oriSum));
                                    EntityTool.setUpdateStatus(payApplicationBill_b);
                                    MetaDaoHelper.update(PayApplicationBill_b.ENTITY_NAME, payApplicationBill_b);
                                    preemptAmountSum = BigDecimalUtils.safeSubtract(preemptAmountSum, oriSum);
                                    continue;
                                }
                                BigDecimal subtract;
                                // 更新子表预占金额
                                PayApplicationBill_b payApplicationBill_b = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, srcbillitemid);
                                mainid = payApplicationBill_b.getMainid();
                                if (null != payApplicationBill_b.getPaymentPreemptAmount()) {
                                    BigDecimal bigDecimal = map_original.get(srcbillitemid);
                                    if (!(oriSum.compareTo(bigDecimal) == 0)) {
                                        subtract = BigDecimalUtils.safeSubtract(oriSum, bigDecimal);
                                        payApplicationBill_b.setPaymentPreemptAmount(BigDecimalUtils.safeAdd(payApplicationBill_b.getPaymentPreemptAmount(), subtract));
                                        EntityTool.setUpdateStatus(payApplicationBill_b);
                                        MetaDaoHelper.update(PayApplicationBill_b.ENTITY_NAME, payApplicationBill_b);
                                        // 更新主表预占金额
                                        preemptAmountSum = BigDecimalUtils.safeAdd(preemptAmountSum, subtract);
                                    }
                                }

                            }
                            if (ValueUtils.isNotEmptyObj(mainid)) {
                                updatePayApplyBillDetail(mainid, preemptAmountSum);
                            }
                        } catch (Exception exception) {
                            log.error("修改单据时，更新付款申请单预占金额异常", exception);
                        }

                    }
                }
            }


        });
        return new RuleExecuteResult();
    }


    private void insertPaymentByPayApplyBill(List<BizObject> lines) throws Exception {
        Long mainid = null;
        BigDecimal preemptAmountSum = null;
        List<PayApplicationBill_b> list = new ArrayList<>(CONSTANT_EIGHT);
        for (BizObject line : lines) {
            // 付款单子表存的付款申请单子表的ID
            Long srcbillitemid = Long.valueOf(line.get(SRCBILLITEMID).toString());
            // 付款单子表的付款金额
            BigDecimal oriSum = line.get(ORISUM);
            // 更新子表预占金额
            PayApplicationBill_b payApplicationBill_b = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, srcbillitemid);
            if (null != payApplicationBill_b.getPaymentPreemptAmount()) {
                payApplicationBill_b.setPaymentPreemptAmount(payApplicationBill_b.getPaymentPreemptAmount().add(oriSum));
            } else {
                payApplicationBill_b.setPaymentPreemptAmount(oriSum);
            }
            payApplicationBill_b.setEntityStatus(EntityStatus.Update);
            list.add(payApplicationBill_b);
            // 更新主表预占金额
            mainid = payApplicationBill_b.getMainid();
            preemptAmountSum = BigDecimalUtils.safeAdd(preemptAmountSum, oriSum);
        }
        MetaDaoHelper.update(PayApplicationBill_b.ENTITY_NAME, list);
        log.info("payment bill delete, pay apply bill data writeBack, mainid = {}, preemptAmountSum = {}", mainid, preemptAmountSum);
        if (ValueUtils.isNotEmptyObj(mainid)) {
            updatePayApplyBillDetail(mainid, preemptAmountSum);
        }
    }


    private void updatePayApplyBillDetail(Long mainid, BigDecimal preemptAmountSum) throws Exception {
        PayApplicationBill payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, mainid);
        if (null != payApplicationBill.getPaymentPreemptAmountSum()) {
            payApplicationBill.setPaymentPreemptAmountSum(BigDecimalUtils.safeAdd(payApplicationBill.getPaymentPreemptAmountSum(), preemptAmountSum));
        } else {
            payApplicationBill.setPaymentPreemptAmountSum(preemptAmountSum);
        }
        if (null == payApplicationBill.getPaymentPreemptAmountSum() || (payApplicationBill.getPaymentPreemptAmountSum().compareTo(BigDecimal.ZERO) == 0)) {
            payApplicationBill.setPayBillStatus(PayBillStatus.PendingApproval);
        }
        if (payApplicationBill.getPaymentPreemptAmountSum().equals(payApplicationBill.getPaymentApplyAmountSum())) {
            payApplicationBill.setPreemptAmountFull(CONSTANT_ONE);
        }
        if (!payApplicationBill.getPaymentPreemptAmountSum().equals(payApplicationBill.getPaymentApplyAmountSum())
                && payApplicationBill.getPreemptAmountFull().equals(CONSTANT_ONE)) {
            payApplicationBill.setPreemptAmountFull(CONSTANT_ZERO);
        }
        EntityTool.setUpdateStatus(payApplicationBill);
        MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplicationBill);
    }


}
