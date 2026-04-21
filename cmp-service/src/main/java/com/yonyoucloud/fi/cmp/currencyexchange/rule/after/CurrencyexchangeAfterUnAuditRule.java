package com.yonyoucloud.fi.cmp.currencyexchange.rule.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCurrencyExchangeManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("currencyexchangeAfterUnAuditRule")
public class CurrencyexchangeAfterUnAuditRule extends AbstractCommonRule {

    @Autowired
    private CmpBudgetCurrencyExchangeManagerService cmpBudgetCurrencyExchangeManagerService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            CurrencyExchange exchange = (CurrencyExchange) bizObject;
            CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, bizObject.getId(), null);
            if (ObjectUtils.isEmpty(currencyExchange)) {
                return result;
            }
            if (!currencyExchange.getIsWfControlled()) {//未开启审批流直接释放
                boolean releaseBudget = releaseBudget(currencyExchange);
                if (releaseBudget) {
                    cmpBudgetCurrencyExchangeManagerService.updateOccupyBudget(exchange, OccupyBudget.UnOccupy.getValue());
                }
            } else {//开启审批流释放重新预占
                Short occupyBudget = budgetAfterUnAudit(currencyExchange);
                if (occupyBudget != null) {
                    cmpBudgetCurrencyExchangeManagerService.updateOccupyBudget(exchange, occupyBudget);
                }
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 是否占预算为实占成功时，删除实占；
     * 是否占预算为预占成功时，删除预占；
     *
     * @param currencyExchange
     * @throws Exception
     */
    private boolean releaseBudget(CurrencyExchange currencyExchange) throws Exception {
        Short budgeted = currencyExchange.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return false;
        } else if (OccupyBudget.PreSuccess.getValue() == budgeted) {//是否占预算为预占成功时，删除预占；
            return cmpBudgetCurrencyExchangeManagerService.releaseBudget(currencyExchange);
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            return cmpBudgetCurrencyExchangeManagerService.releaseImplement(currencyExchange);
        }
        return false;
    }

    /**
     * 如果是预占就跳过，如果是实占，删除实占，重新预占
     *
     * @param currencyExchange
     * @throws Exception
     */
    private Short budgetAfterUnAudit(CurrencyExchange currencyExchange) throws Exception {
        Short budgeted = currencyExchange.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return null;
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            boolean releaseImplement = cmpBudgetCurrencyExchangeManagerService.releaseImplement(currencyExchange);
            if (releaseImplement) {
                //重新预占
                log.error("重新预占.....");
                //且结算状态应置为待结算、并清空结算成功时间
                currencyExchange.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                boolean budget = cmpBudgetCurrencyExchangeManagerService.budget(currencyExchange);
                if (budget) {//可能是没有匹配上规则，也可能是没有配置规则
                    return OccupyBudget.PreSuccess.getValue();
                } else {
                    return OccupyBudget.UnOccupy.getValue();
                }
            } else {
                log.error("释放实占失败,releaseImplement:{}", releaseImplement);
            }
        }
        return null;
    }
}
