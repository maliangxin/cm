package com.yonyoucloud.fi.cmp.util.cuckoo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmpCuckooFiltersNothing extends CmpCuckooFilters {

    private static final Logger log = LoggerFactory.getLogger(CmpCuckooFiltersNothing.class);

    public CmpCuckooFiltersNothing() {
        super(new CmpCuckooFilterBuilder());
    }

    @Override
    protected void init() {

    }

    @Override
    public void putValue(String value) {
    }

    @Override
    public void removeValue(String value) {
    }

    @Override
    public boolean mightContain(String value) {
        log.error("已关闭布谷鸟过滤器，返回false，使用数据查询!");
        return false;
    }

    @SuppressWarnings ("staticVariable")
    private static volatile CmpCuckooFilters SINGLETON = null;

    public static CmpCuckooFilters singleton() {
        if (null == SINGLETON) {
            synchronized (CmpCuckooFilters.class) {
                if (null == SINGLETON) {
                    SINGLETON = new CmpCuckooFiltersNothing();
                }
            }
        }
        return SINGLETON;
    }
}
