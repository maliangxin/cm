package com.yonyoucloud.fi.cmp.currencyexchange.service;

import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface CurrencyExchangeManager {


    /**
     * 更新单据状态，独立事务
     * @param currencyExchange
     * @throws Exception
     */
    @Transactional(rollbackFor = RuntimeException.class, propagation = Propagation.REQUIRES_NEW)
    void updateStatus(CurrencyExchange currencyExchange) throws Exception;
}
