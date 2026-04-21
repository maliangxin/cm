package com.yonyoucloud.fi.cmp.currencyexchange.rule.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCurrencyExchangeManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("currencyexchangeAfterAuditRule")
public class CurrencyexchangeAfterAuditRule extends AbstractCommonRule {

    @Autowired
    private CmpBudgetCurrencyExchangeManagerService cmpBudgetCurrencyExchangeManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, bizObject.getId(), null);
            if (ObjectUtils.isEmpty(currencyExchange)) {
                return result;
            }
            if (!currencyExchange.getIsWfControlled()) {
                boolean budget = cmpBudgetCurrencyExchangeManagerService.budget(currencyExchange);
                if (budget) {
                    cmpBudgetCurrencyExchangeManagerService.updateOccupyBudget(currencyExchange, OccupyBudget.PreSuccess.getValue());
                    currencyExchange.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                }
            }
            if (currencyExchange.getSettlestatus() != null && currencyExchange.getSettlestatus().getValue() == DeliveryStatus.completeDelivery.getValue()) {
                boolean implement = cmpBudgetCurrencyExchangeManagerService.implement(currencyExchange);
                if (implement) {
                    cmpBudgetCurrencyExchangeManagerService.updateOccupyBudget(currencyExchange, OccupyBudget.ActualSuccess.getValue());
                }
            }
        }
        return new RuleExecuteResult();
    }
}
