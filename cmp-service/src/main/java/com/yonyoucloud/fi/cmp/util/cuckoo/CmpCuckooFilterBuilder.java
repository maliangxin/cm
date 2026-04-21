package com.yonyoucloud.fi.cmp.util.cuckoo;


import lombok.Data;
import lombok.experimental.Accessors;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Data
@Accessors(chain = true)
public class CmpCuckooFilterBuilder {

    /**
     * 布谷鸟过滤器初始容量,
     */
    private long cuckooFilterMaxSize = configValue("ctmcmp.bankDetail.cuckooFilterMaxSize", 1000000L, Long::parseLong);

    /**
     * 布谷鸟过滤器容误报率
     */
    private double cuckooFalsePositiveRate = configValue("ctmcmp.bankDetail.cuckooFalsePositiveRate", 0.0000001, Double::parseDouble);

    /**
     * 布谷鸟过滤器重建时间（默认分钟）
     */
    private int cuckooRebuildDuration = configValue("ctmcmp.bankDetail.cuckooRebuildDuration", 30 * 60, Integer::parseInt);

    /**
     * 布谷鸟过滤器重建时间单位（默认分钟）
     */
    private TimeUnit cuckooRebuildTimeUnit = configValue("ctmcmp.bankDetail.cuckooRebuildTimeUnit", TimeUnit.MINUTES, TimeUnit::valueOf);

    /**
     * 从系统属性》环境变量获取配置的值
     *
     * @param key       key
     * @param def       default value
     * @param converter 值转换
     * @param <V>       值类型
     * @return 配置值
     */
    private <V> V configValue(String key, Object def, Function<String, V> converter) {
        String val = System.getProperty(key, System.getenv(key));
        if (null == val || val.trim().length() == 0) {
            return (V) def;
        }
        return converter.apply(val.trim());
    }


    public CmpCuckooFilters build() {
        CmpCuckooFilters cuckooFilters = new CmpCuckooFilters(this);
        return cuckooFilters;
    }

}
