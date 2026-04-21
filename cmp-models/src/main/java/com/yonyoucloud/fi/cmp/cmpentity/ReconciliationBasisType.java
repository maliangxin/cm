
package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 银行对账依据类型
 *
 * @author wanxbo@yonyou.com
 * @version 1.0
 */
public enum ReconciliationBasisType {
    SmartCheckNoMatching("财资统一对账码勾对", (short)1),
    KeyElementMatching("关键要素匹配", (short)2),
    ManualMatching("手工对账", (short)3),
    NetAmountMatching("净额对账", (short)4),
    SingleSideMatching("单边对账", (short)5);

    private String name;
    private short value;

    private ReconciliationBasisType(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, ReconciliationBasisType> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, ReconciliationBasisType>();
        ReconciliationBasisType[] items = ReconciliationBasisType.values();
        for (ReconciliationBasisType item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static ReconciliationBasisType find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}