package com.yonyoucloud.fi.cmp.vo.migrade;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

public enum MigradeCheckResultEnum {

    CheckNotPass("1", "校验不通过"),
    MigradeFundPayBill("2", "升迁资金付款单"),
    MigradeFundReceiveBill("3", "升迁资金收款单"),
    DelJournal("4", "删除日记账"),
    CheckPass("5", "校验通过"),
    TipOperate("6", "建议处理");

    private final String CheckResult;
    private final String CheckResultName;
    private static HashMap<String, MigradeCheckResultEnum> valueMap = null;
    private static HashMap<String, MigradeCheckResultEnum> KeyMap = null;

    MigradeCheckResultEnum(String CheckResult, String CheckResultName) {
        this.CheckResult = CheckResult;
        this.CheckResultName = CheckResultName;
    }

    public String getCheckResult() {
        return CheckResult;
    }

    public String getCheckResultName() {
        return CheckResultName;
    }

    private static synchronized void initValueMap() {
        if (valueMap == null) {
            valueMap = new HashMap();
            MigradeCheckResultEnum[] items = values();
            MigradeCheckResultEnum[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                MigradeCheckResultEnum item = var1[var3];
                valueMap.put(item.getCheckResultName(), item);
            }

        }
    }
    private static synchronized void initKeyMap() {
        if (KeyMap == null) {
            KeyMap = new HashMap();
            MigradeCheckResultEnum[] items = values();
            MigradeCheckResultEnum[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                MigradeCheckResultEnum item = var1[var3];
                KeyMap.put(item.getCheckResult(), item);
            }

        }
    }

    public static MigradeCheckResultEnum findByValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        } else {
            if (valueMap == null) {
                initValueMap();
            }
            return valueMap.get(value);
        }
    }
    public static MigradeCheckResultEnum findByKey(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        } else {
            if (KeyMap == null) {
                initKeyMap();
            }
            return KeyMap.get(key);
        }
    }
}
