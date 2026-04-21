package com.yonyoucloud.fi.cmp.currencyexchange.rule.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCurrencyExchangeManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("currencyexchangeAfterUnSubmitRule")
public class CurrencyexchangeAfterUnSubmitRule extends AbstractCommonRule {
    @Autowired
    private CmpBudgetCurrencyExchangeManagerService cmpBudgetCurrencyExchangeManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, bizObject.getId(), null);
            releaseBudget(currencyExchange);
        }
        return result;
    }

    /**
     * 是否占预算为实占成功时，删除实占；
     * 是否占预算为预占成功时，删除预占；
     *
     * @param currencyExchange
     * @throws Exception
     */
    private void releaseBudget(CurrencyExchange currencyExchange) throws Exception {
        Short budgeted = currencyExchange.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return;
        } else if (OccupyBudget.PreSuccess.getValue() == budgeted) {//是否占预算为预占成功时，删除预占；
            boolean releaseBudget = cmpBudgetCurrencyExchangeManagerService.releaseBudget(currencyExchange);
            if (releaseBudget) {
                cmpBudgetCurrencyExchangeManagerService.updateOccupyBudget(currencyExchange, OccupyBudget.UnOccupy.getValue());
            }
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            boolean releaseImplement = cmpBudgetCurrencyExchangeManagerService.releaseImplement(currencyExchange);
            if (releaseImplement) {
                cmpBudgetCurrencyExchangeManagerService.updateOccupyBudget(currencyExchange, OccupyBudget.UnOccupy.getValue());
            }
        }
    }
}
