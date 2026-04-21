package com.yonyoucloud.fi.cmp.currencyexchange.rule.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCurrencyExchangeManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("currencyexchangeAfterDeleteBpmRule")
public class CurrencyexchangeAfterDeleteBpmRule extends AbstractCommonRule {

    @Autowired
    private CmpBudgetCurrencyExchangeManagerService cmpBudgetCurrencyExchangeManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        String deleteReason = billContext.getDeleteReason();
        List<BizObject> bills = getBills(billContext, map);
        String fullName = billContext.getFullname();
        log.error("deleteReason:{}", deleteReason);
        for (BizObject bizobject : bills) {
            CurrencyExchange currencyExchange = (CurrencyExchange) MetaDaoHelper.findById(fullName, bizobject.getId(), ICmpConstant.CONSTANT_TWO);
            //驳回/撤回到发起人/审批终止
            //是否占预算为预占成功时，删除预占
            if (BooleanUtils.b(currencyExchange.get(ICmpConstant.IS_WFCONTROLLED)) && ("REJECTTOSTART".equals(deleteReason) || "ACTIVITI_DELETED".equals(deleteReason))) {
                boolean success = cmpBudgetCurrencyExchangeManagerService.releaseBudget(currencyExchange);
                if (success) {
                    cmpBudgetCurrencyExchangeManagerService.updateOccupyBudget(currencyExchange, OccupyBudget.UnOccupy.getValue());
                }
                //WITHDRAWREJECTTOSTART 撤销驳回制单 重新占用
            } else if (BooleanUtils.b(currencyExchange.get(ICmpConstant.IS_WFCONTROLLED)) && ("WITHDRAWREJECTTOSTART".equals(deleteReason))) {
                log.error("WITHDRAWREJECTTOSTART........");
                boolean success = cmpBudgetCurrencyExchangeManagerService.budget(currencyExchange);
                if (success) {
                    cmpBudgetCurrencyExchangeManagerService.updateOccupyBudget(currencyExchange, OccupyBudget.PreSuccess.getValue());
                }
            } else if (BooleanUtils.b(currencyExchange.get(ICmpConstant.IS_WFCONTROLLED)) && ("withdraw".equals(deleteReason))) {
                log.error("withdraw........");
            }
        }
        return new RuleExecuteResult();
    }
}
