
package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 银行对账依据类型
 *
 * @author wanxbo@yonyou.com
 * @version 1.0
 */
public enum ReconciliationMatchDataSource {
    Voucher("凭证", (short)1),
    Journal("银行日记账", (short)2),
    BankReconciliation("银行流水", (short)3),;

    private String name;
    private short value;

    private ReconciliationMatchDataSource(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, ReconciliationMatchDataSource> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, ReconciliationMatchDataSource>();
        ReconciliationMatchDataSource[] items = ReconciliationMatchDataSource.values();
        for (ReconciliationMatchDataSource item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static ReconciliationMatchDataSource find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}