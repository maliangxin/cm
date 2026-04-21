package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayBillStatus;
import com.yonyoucloud.fi.cmp.paybill.PayBillb;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>支付变更时，更新付款申请单对应的数据金额</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021/1/11 20:27
 */
@Slf4j
@Component
public class PaymentChangeUpdatePayApplyBillRule extends AbstractCommonRule {

    private static final String PAY_STATUS = "paystatus";
    private static final String BILL_TYPE = "billtype";

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103020"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C258B204E00006", "在财务新架构环境下，不允许在支付变更时，更新付款申请单对应的数据金额。")       /* "在财务新架构环境下，不允许在支付变更时，更新付款申请单对应的数据金额。" */);
        }
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bizObject : bills) {
            short payStatus = ValueUtils.isNotEmptyObj(bizObject.get(PAY_STATUS))
                    ? Short.parseShort(bizObject.get(PAY_STATUS).toString()) : null;
            short billType = ValueUtils.isNotEmptyObj(bizObject.get(BILL_TYPE))
                    ? Short.parseShort(bizObject.get(BILL_TYPE).toString()) : null;
            short srcitem = ValueUtils.isNotEmptyObj(bizObject.get(SRC_ITEM))
                    ? Short.parseShort(bizObject.get(SRC_ITEM).toString()) : null;
            String srcflag = ValueUtils.isNotEmptyObj(bizObject.get(SRC_FLAG))
                    ? bizObject.get(SRC_FLAG).toString() : null;
            boolean isPayApplyBill = EventType.PayApplyBill.getValue() == billType;
            boolean isManual = EventSource.Manual.getValue() == srcitem && CMP_PAY_APPLICATION.equals(srcflag);
            boolean isPaySuccess = payStatus == PayStatus.Success.getValue();
            if ((isPayApplyBill || isManual) && isPaySuccess) {
                QuerySchema querySchemaJ = QuerySchema.create().addSelect("*");
                querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name(MAINID).eq(bizObject.getId())));
                try {
                    List<Map<String, Object>> mapList = MetaDaoHelper.query(PayBillb.ENTITY_NAME, querySchemaJ);
                    mapList.forEach(e -> {
                        try {
                            Long id = Long.valueOf(e.get(SRCBILLITEMID).toString());
                            BigDecimal oriSum = (BigDecimal) e.get(ORISUM);
                            // 更新子表已付金额和未付金额
                            PayApplicationBill_b payApplicationBill_b = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, id);
                            if (null != payApplicationBill_b) {
                                if (null == payApplicationBill_b.getPaidAmount()) {
                                    payApplicationBill_b.setPaidAmount(oriSum);
                                } else {
                                    payApplicationBill_b.setPaidAmount(BigDecimalUtils.safeAdd(payApplicationBill_b.getPaidAmount(),oriSum));
                                }
                                payApplicationBill_b.setUnpaidAmount(BigDecimalUtils.safeSubtract(payApplicationBill_b.getUnpaidAmount(),oriSum));
                                EntityTool.setUpdateStatus(payApplicationBill_b);
                                MetaDaoHelper.update(PayApplicationBill_b.ENTITY_NAME, payApplicationBill_b);
                                // 更新主表已付金额总数和未付金额总数
                                Long mainId = payApplicationBill_b.getMainid();
                                PayApplicationBill payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, mainId);
                                if (null == payApplicationBill.getPaidAmountSum()) {
                                    payApplicationBill.setPaidAmountSum(oriSum);
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
                                MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplicationBill);
                            }
                        } catch (Exception exception) {
                            log.error("调整未付金额和已付金额数据失败!:" + exception.getMessage());
                        }
                    });
                } catch (Exception e)   {
                    log.error("查询付款申请单卡片失败:" + e.getMessage());
                }
            }
        }

        return new RuleExecuteResult();
    }
}
