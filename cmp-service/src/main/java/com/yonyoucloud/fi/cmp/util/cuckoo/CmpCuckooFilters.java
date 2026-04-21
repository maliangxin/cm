package com.yonyoucloud.fi.cmp.util.cuckoo;

import com.github.mgunlogson.cuckoofilter4j.CuckooFilter;
import com.google.common.base.Charsets;
import com.google.common.hash.Funnels;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CmpCuckooFilters {

    @SuppressWarnings ("staticVariable")
    private static Logger logger = LoggerFactory.getLogger(CmpCuckooFilters.class);
    private CmpCuckooFilterBuilder builder;
    private final ScheduledExecutorService filterExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().setNameFormat("ctmcmp_bankDetail_cuckoo_filter_renew-%d").setDaemon(true).build());
    private AtomicBoolean filter2IsMaster = new AtomicBoolean(false);
    private AtomicReference<CuckooFilter<String>> cuckooFilter = new AtomicReference<>();
    private AtomicReference<CuckooFilter<String>> cuckooFilter2 = new AtomicReference<>();
    public AtomicBoolean initDataState = new AtomicBoolean(false);

    public CmpCuckooFilters(CmpCuckooFilterBuilder builder) {
        this.builder = builder;

        init();
    }

    protected void init() {
        reNewFilter();
        reNewFilter2();

        filterExecutor.scheduleWithFixedDelay(() -> reNewFilter(), this.builder.getCuckooRebuildDuration(), this.builder.getCuckooRebuildDuration(), this.builder.getCuckooRebuildTimeUnit());
        filterExecutor.scheduleWithFixedDelay(() -> reNewFilter2(), this.builder.getCuckooRebuildDuration() / 2, this.builder.getCuckooRebuildDuration(), this.builder.getCuckooRebuildTimeUnit());
    }

    /**
     * 初始化一个CuckooFilter
     */
    private void reNewFilter() {
        logger.info("reNewFilter: " + LocalDateTime.now());
        cuckooFilter.set(new CuckooFilter.Builder<String>(Funnels.stringFunnel(Charsets.UTF_8), this.builder.getCuckooFilterMaxSize())
                .withFalsePositiveRate(this.builder.getCuckooFalsePositiveRate()).build());
        filter2IsMaster.getAndSet(false);
        initDataState.getAndSet(false);
    }

    /**
     * 初始化一个CuckooFilter2
     */
    private void reNewFilter2() {
        logger.info("reNewFilter2: " + LocalDateTime.now());
        cuckooFilter2.set(new CuckooFilter.Builder<String>(Funnels.stringFunnel(Charsets.UTF_8), this.builder.getCuckooFilterMaxSize())
                .withFalsePositiveRate(this.builder.getCuckooFalsePositiveRate()).build());
        filter2IsMaster.getAndSet(true);
        initDataState.getAndSet(false);
    }

    public void putValue(String value) {
        cuckooFilter.get().put(value);
        cuckooFilter2.get().put(value);
    }

    public void removeValue(String value) {
        cuckooFilter.get().delete(value);
        cuckooFilter2.get().delete(value);
    }


    public boolean mightContain(String value) {
        if (filter2IsMaster.get()) {
            return cuckooFilter2.get().mightContain(value) || cuckooFilter.get().mightContain(value);
        }
        return cuckooFilter.get().mightContain(value) || cuckooFilter2.get().mightContain(value);
    }

    @SuppressWarnings ("staticVariable")
    private static volatile CmpCuckooFilters SINGLETON = null;

    public static CmpCuckooFilters singleton() {
        if (null == SINGLETON) {
            synchronized (CmpCuckooFilters.class) {
                if (null == SINGLETON) {
                    SINGLETON = new CmpCuckooFilterBuilder().build();
                }
            }
        }
        return SINGLETON;
    }

}
