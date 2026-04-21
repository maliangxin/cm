package com.yonyoucloud.fi.cmp.payapplicationbill.rule.check;

import com.google.gson.Gson;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.CheckItem;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.IBillNumConstant.PAYAPPLICATIONBILL_B;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;
import static com.yonyoucloud.fi.cmp.constant.IMsgConstant.PAY_APPLICATION;
import static com.yonyoucloud.fi.cmp.constant.IMsgConstant.PUSH_PAY_APPLICATION_BILL_SPLIT_AMOUNT;
import static com.yonyoucloud.fi.cmp.constant.IServicecodeConstant.PAYAPPLICATIONBILL;

/**
 * <h1>付款申请主表paymentApplyAmountSum字段的check规则应付事项推付款申请表头金额修改，平均到明细行</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-12-08 19:11
 */
@Slf4j
@Component("oapPushPayApplicationBillSplitAmountRule")
public class OapPushPayApplicationBillSplitAmountRule extends AbstractCommonRule {
    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    CTMCMPBusinessLogService ctmcmpBusinessLogService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        Map<String, Object> logMap = new HashMap<>();
        BillDataDto item = (BillDataDto) this.getParam(paramMap);
        logMap.put("billDataDto", JsonUtils.toJson(item));
        try {
            String childrenField = MetaDaoHelper.getChilrenField(billContext.getFullname());
            if (null == childrenField) {
                return new RuleExecuteResult();
            } else {
                CheckItem checkItem = (new Gson()).fromJson(item.getItem(), CheckItem.class);
                if (null == checkItem) {
                    assert false;
                    if (checkItem.getKey() == null) {
                        return new RuleExecuteResult();
                    }
                }
                //获取前端传输内容
                List<BizObject> bills = BillInfoUtils.decodeBills(billContext, item.getData());
                if (null == bills) {
                    assert false;
                    if (bills.size() == 0) {
                        return new RuleExecuteResult();
                    }
                }
                BizObject bill = bills.get(0);
                List<BizObject> lines = bill.get(childrenField);
                if (null == lines || lines.size() == 0) {
                    return new RuleExecuteResult();
                }
                boolean isSource = (StringUtils.equals(bill.get(SOURCE), FIARAP_ARAP_OAP) || StringUtils.equals(bill.get(SOURCE), "yonyoufi.arap_oap") ||
                        StringUtils.equals(bill.get(SOURCE), UPU_ST_PURCHASEORDER));
                if (PAYAPPLICATIONBILL_B.equals(childrenField) && PAYMENT_APPLY_AMOUNT_SUM.equals(checkItem.getKey())
                        && checkItem.getValue() != null && isSource) {
                    Map<String,Object> originalMap = new HashMap<>();
                    BigDecimal paymentApplyAmountSumNewValue = new BigDecimal(checkItem.getValue());
                    if (paymentApplyAmountSumNewValue.compareTo(BigDecimal.ZERO) <= 0) {
                        return new RuleExecuteResult();
                    }
                    originalMap.put(bill.getId() == null ? "head" : bill.getId().toString(), paymentApplyAmountSumNewValue);
                    BigDecimal childrenPaymentApplyAmountSum = BigDecimal.ZERO;
                    int index = 0;
                    for (BizObject line : lines) {
                        Object paymentApplyAmount = line.get(PAYMENT_APPLY_AMOUNT);
                        if (!ValueUtils.isNotEmptyObj(paymentApplyAmount)) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101744"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418008E","明细行付款申请金额不能为空！") /* "明细行付款申请金额不能为空！" */);
                        }
                        originalMap.put(line.getId() == null ? "" + index : line.get("id").toString(), paymentApplyAmount);
                        childrenPaymentApplyAmountSum = BigDecimalUtils.safeAdd(childrenPaymentApplyAmountSum, new BigDecimal(MapUtils.getString(line, PAYMENT_APPLY_AMOUNT)));
                        index ++;
                    }
                    if (paymentApplyAmountSumNewValue.compareTo(childrenPaymentApplyAmountSum) == 0) {
                        return new RuleExecuteResult();
                    }
                    Integer currencyMoneyDigit = MapUtils.getInteger(bill, IBussinessConstant.CURRENCY_MONEYDIGIT);
                    if (currencyMoneyDigit == null) {
                        CurrencyTenantDTO natCurrencyDTO = baseRefRpcService.queryCurrencyById(MapUtils.getString(bill, NATCURRENCY));
                        currencyMoneyDigit = natCurrencyDTO.getMoneydigit();
                    }
                    CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(MapUtils.getString(bill, IBussinessConstant.CURRENCY));
                    Integer moneyRunt = currencyDTO.getMoneyrount();
                    RoundingMode roundingMode = RoundingMode.HALF_UP;
                    switch (moneyRunt) {
                        case 0:
                            roundingMode = RoundingMode.UP;
                            break;
                        case 1:
                            roundingMode = RoundingMode.DOWN;
                            break;
                        default:
                            break;
                    }
                    Map<Long, BigDecimal> srcBillItemIdMap = new HashMap<>(16);
                    BigDecimal linesSum = BigDecimal.ZERO;
                    Set<Long> ids = new HashSet<>(16);
                    linesSum = getOapMainDetailBalance(lines, srcBillItemIdMap, linesSum, ids);
                    logMap.put("linesSum", linesSum);
                    logMap.put("originalMap", originalMap);
                    logMap.put("srcBillItemIdMap", srcBillItemIdMap);
                    log.error("OapPushPayApplicationBillSplitAmountRule, Query original data, linesSum = {}, srcBillItemIdMap = {}",
                            linesSum, srcBillItemIdMap);
                    BigDecimal rate = paymentApplyAmountSumNewValue.divide(linesSum, 16, roundingMode);
                    //算出为正数的行的已经分配过的总额
                    BigDecimal midAccount = new BigDecimal(0);
                    BigDecimal itemOriSumUpdate;
                    int quantity = lines.size();
                    for (BizObject line : lines) {
                        BigDecimal oriSum = srcBillItemIdMap.get(MapUtils.getLong(line, SRCBILLITEMID));
                        if (quantity != 1) {
                            itemOriSumUpdate = oriSum.multiply(rate)
                                    .setScale(currencyMoneyDigit, roundingMode);
                            midAccount = midAccount.add(itemOriSumUpdate);
                        } else {
                            itemOriSumUpdate = paymentApplyAmountSumNewValue.subtract(midAccount).setScale(currencyMoneyDigit, roundingMode);
                        }
                        quantity--;
                        line.put(PAYMENT_APPLY_AMOUNT, itemOriSumUpdate);
                        line.put(UNPAID_AMOUNT, itemOriSumUpdate);
                        logMap.put(line.get("srcbillitemid"), itemOriSumUpdate);
                    }
                    bill.put(UNPAID_AMOUNT_SUM, paymentApplyAmountSumNewValue);
                    recordBusinessLog(logMap, item.getCode());
                }
            }
        } catch (Exception e) {
            log.error("OapPushPayApplicationBillSplitAmountRule, billContext = {}, paramMap = {}",
                    JsonUtils.toJson(billContext), JsonUtils.toJson(paramMap));
            recordBusinessLog(logMap, item.getCode());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101745"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418008D","操作异常，请检查数据！") /* "操作异常，请检查数据！" */);
        }
        return new RuleExecuteResult();
    }

    private BigDecimal getOapMainDetailBalance(List<BizObject> lines, Map<Long, BigDecimal> srcBillItemIdMap, BigDecimal linesSum, Set<Long> ids) throws Exception {
        for (BizObject line : lines) {
            if (ValueUtils.isNotEmptyObj(line.get(SRCBILLITEMID))) {
                ids.add(Long.parseLong(line.get(SRCBILLITEMID)));
            }
        }
        String source = MapUtils.getString(lines.get(0), SOURCE);
        BillContext billContext1 = new BillContext();
        QuerySchema schema = QuerySchema.create();
        if (FIARAP_ARAP_OAP.equals(source) || StringUtils.equals(source, "yonyoufi.arap_oap")) {
            billContext1.setDomain(IDomainConstant.MDD_DOMAIN_FIARAP);
            billContext1.setFullname(OAPDETAIL);
            schema.addSelect(ARAP_OAP_SPLIT_AMOUNT_SQL);
        }
        if (UPU_ST_PURCHASEORDER.equals(source)) {
            billContext1.setDomain(UPU);
            billContext1.setFullname(PURCHASEORDERS);
            schema.addSelect(UPU_PURCHASEORDER_SPLIT_AMOUNT_SQL);
        }
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(PRIMARY_ID).in(ids));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> mapList = MetaDaoHelper.query(billContext1, schema);
        if (FIARAP_ARAP_OAP.equals(source) || StringUtils.equals(source, "yonyoufi.arap_oap")) {
            for (Map<String, Object> map : mapList) {
                if (ValueUtils.isNotEmptyObj(MapUtils.getString(map, IBussinessConstant.ORI_SUM_L)) && new BigDecimal(MapUtils.getString(map, IBussinessConstant.ORI_SUM_L)).compareTo(BigDecimal.ZERO) > 0) {
                    linesSum = BigDecimalUtils.safeAdd(linesSum, new BigDecimal(MapUtils.getString(map, IBussinessConstant.ORI_SUM_L)));
                    srcBillItemIdMap.put(MapUtils.getLong(map, PRIMARY_ID), new BigDecimal(MapUtils.getString(map, IBussinessConstant.ORI_SUM_L)));
                } else {
                    linesSum = BigDecimalUtils.safeAdd(linesSum, new BigDecimal(MapUtils.getString(map, OCCUPYAMOUNT)));
                    srcBillItemIdMap.put(MapUtils.getLong(map, PRIMARY_ID), new BigDecimal(MapUtils.getString(map, OCCUPYAMOUNT)));
                }
            }
        }
        if (UPU_ST_PURCHASEORDER.equals(source)) {
            for (Map<String, Object> map : mapList) {
                BigDecimal oriSum = BigDecimalUtils.computeShouldPay(map);
                BigDecimal totalPayApplyAmount = (BigDecimal) map.get(TOTAL_PAY_APPLY_AMOUNT);
                BigDecimal temp = BigDecimalUtils.safeSubtract(oriSum, totalPayApplyAmount);
                if (ValueUtils.isNotEmptyObj(temp) && temp.compareTo(BigDecimal.ZERO) <= 0) {
                    temp = totalPayApplyAmount;
                }
                linesSum = BigDecimalUtils.safeAdd(linesSum, temp);
                srcBillItemIdMap.put(MapUtils.getLong(map, PRIMARY_ID), temp);
            }
        }
        return linesSum;
    }

    private void recordBusinessLog(Map<String, Object> logMap, String code) {
        try {
            ctmcmpBusinessLogService.saveBusinessLog(logMap, code, "",
                    PAYAPPLICATIONBILL, PAY_APPLICATION, PUSH_PAY_APPLICATION_BILL_SPLIT_AMOUNT);
        } catch (Exception e) {
            log.error("UpuPurchaseOrderPlanLinPushPayApplyBillProposingRule, write Business Log, yTenantId = {}, code = {}, e = {}",
                    AppContext.getCurrentUser().getYTenantId(), code, e.getMessage());
        }
    }
}
