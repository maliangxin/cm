package com.yonyoucloud.fi.cmp.common.service.cancelsettle;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;

import java.util.HashMap;

public enum CmpBusinessBillType {
    CashRecBill("UID:P_CM-BE_17FE975C041800A7", "2571640684663808", "资金收款单"),
    CashPayBill("UID:P_CM-BE_17FE975C041800A8", "2553141119111680", "资金付款单"),
    BatchTransferAccount("UID:P_STWB-BE_17FEC3DA041804F1", "2283268970412769285", "批量同名账户划转");

    private String name;
    private String value;
    private String defaultName;
    private static HashMap<String, CmpBusinessBillType> map = null;
    private static HashMap<String, CmpBusinessBillType> nameMap = null;

    CmpBusinessBillType(String name, String value, String defaultName) {
        this.name = name;
        this.value = value;
        this.defaultName = defaultName;
    }

    public String getName() {
        return InternationalUtils.getMessageWithDefault(this.name, this.defaultName);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private static synchronized void initMap() {
        if (map == null && nameMap == null) {
            map = new HashMap();
            nameMap = new HashMap();
            CmpBusinessBillType[] items = values();
            CmpBusinessBillType[] var1 = items;
            int var2 = items.length;

            for (int var3 = 0; var3 < var2; ++var3) {
                CmpBusinessBillType item = var1[var3];
                map.put(item.getValue(), item);
                nameMap.put(item.getName(), item);
            }

        }
    }

    public static CmpBusinessBillType find(String value) {
        if (value == null) {
            return null;
        } else {
            if (map == null) {
                initMap();
            }

            return map.get(value);
        }
    }

    public static CmpBusinessBillType findName(String name) {
        if (name == null) {
            return null;
        } else {
            if (nameMap == null) {
                initMap();
            }

            return nameMap.get(name);
        }
    }
}
