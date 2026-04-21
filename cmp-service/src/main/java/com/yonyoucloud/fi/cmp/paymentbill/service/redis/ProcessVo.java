package com.yonyoucloud.fi.cmp.paymentbill.service.redis;

/**
 * desc:
 * author:wangqiangac
 * date:2023/7/10 19:58
 */

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 百分比进度类
 */
public class ProcessVo implements Serializable {
    private static final long SerialVersionUID=7788999100L;
    int count;
    double percent;
    AtomicInteger executedCount;
    String uid;
    String message;
    int successCount;

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    public AtomicInteger getExecutedCount() {
        return this.executedCount;
    }

    public void setExecutedCount(AtomicInteger executedCount) {
        this.executedCount = executedCount;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
}
