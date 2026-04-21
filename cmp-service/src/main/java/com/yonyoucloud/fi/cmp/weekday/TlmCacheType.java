package com.yonyoucloud.fi.cmp.weekday;

import java.util.concurrent.TimeUnit;

public enum TlmCacheType {

    NORMAL(24, TimeUnit.HOURS),

    TEMPORARY(1, TimeUnit.MINUTES),

    TENANT(1, TimeUnit.HOURS),

    THREAD(1, TimeUnit.MINUTES),
    ;

    TlmCacheType(int duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
    }
    private int duration;

    private TimeUnit timeUnit;

    public int getDuration() {
        return this.duration;
    }

    public TimeUnit getTimeUnit() {
        return this.timeUnit;
    }


}
