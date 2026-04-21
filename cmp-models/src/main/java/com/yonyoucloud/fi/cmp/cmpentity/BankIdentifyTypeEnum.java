package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * @description: 银行流水辨识匹配规则类型枚举
 * @author: wanxbo@yonyou.com
 * @date: 2024/6/12 16:11
 */

public enum BankIdentifyTypeEnum {
    BankReceipt("银行交易回单匹配", (short)1),
    Refund("银行退票匹配", (short)2),
    OurInfo("本方信息匹配", (short)3),
    PeerInfo("对方信息匹配", (short)4),
    PaymentsDocument("收付单据匹配",(short) 5),
    BusinessCredentials("业务凭据匹配",(short) 6),
    Generate("生单类型辨识",(short) 7),
    Publish("发布对象辨识",(short) 8),
    BuyingOnCredit("挂账辨识",(short) 9),
    OtherInfo("其他信息辨识",(short) 10),
    ;

    private String name;
    private short value;

    private BankIdentifyTypeEnum(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public short getValue() {
        return value;
    }

    private static HashMap<Short, BankIdentifyTypeEnum> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<Short, BankIdentifyTypeEnum>();
        BankIdentifyTypeEnum[] items = BankIdentifyTypeEnum.values();
        for (BankIdentifyTypeEnum item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static BankIdentifyTypeEnum find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
}
