package com.yonyoucloud.fi.cmp.budget;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;

import java.util.List;

public interface CmpBudgetCurrencyExchangeManagerService {

    /**
     * 查询符合规则设置的数据
     *
     * @param budgetEventBill
     * @return
     * @throws Exception
     */
    CtmJSONArray queryBillByRule(CmpBudgetEventBill budgetEventBill) throws Exception;

    void updateBillList(List<CurrencyExchange> currencyExchanges, Short isOccupyBudget) throws Exception;

    boolean budget(CurrencyExchange currencyExchange) throws Exception;

    boolean releaseBudget(CurrencyExchange currencyExchange) throws Exception;

    /**
     * 带判断的实占
     *
     * @param currencyExchange
     * @return
     * @throws Exception
     */
    boolean implement(CurrencyExchange currencyExchange) throws Exception;

    /**
     * 释放实占
     *
     * @param currencyExchange
     * @return
     * @throws Exception
     */
    boolean releaseImplement(CurrencyExchange currencyExchange) throws Exception;

    void updateOccupyBudget(CurrencyExchange currencyExchange, Short isOccupyBudget) throws Exception;


}
