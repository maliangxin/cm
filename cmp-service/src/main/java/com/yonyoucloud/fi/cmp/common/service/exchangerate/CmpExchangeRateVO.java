package com.yonyoucloud.fi.cmp.common.service.exchangerate;

import java.math.BigDecimal;

public class CmpExchangeRateVO {
    /**
     * 汇率
     */
    private BigDecimal exchangeRate;
    /**
     * 汇率折算方式
     */
    private Short exchangeRateOps;

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public Short getExchangeRateOps() {
        return exchangeRateOps;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public void setExchangeRateOps(Short exchangeRateOps) {
        this.exchangeRateOps = exchangeRateOps;
    }
}
