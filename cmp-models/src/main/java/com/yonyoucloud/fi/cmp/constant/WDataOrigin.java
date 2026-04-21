package com.yonyoucloud.fi.cmp.constant;

import java.util.HashMap;

/**
 * 待结算数据来源
 */
public enum WDataOrigin {
    SettleSystem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800AD","资金结算") /* "资金结算" */ , (short) 1),
    CapitalAllocation(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800AE","资金调度") /* "资金调度" */, (short) 3),
    DrftSystem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800AF","商业汇票") /* "商业汇票" */ , (short) 4),
    RecPaySystem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800B1","应收应付系统") /* "应收应付系统" */, (short) 6),
    CashSystem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800B2","现金管理") /* "现金管理" */, (short) 8),
    PaySystem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800B3","费用管理") /* "费用管理" */, (short) 10),
    SettleCenter(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800B4","结算中心") /* "结算中心" */, (short) 12),
    ARManage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800B5","应收管理") /* "应收管理" */, (short) 13),
    APManage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800B6","应付管理") /* "应付管理" */, (short) 14),
    TrsrSystem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800B7","投融资管理") /* "投融资管理" */, (short) 16),
    ProjectCloud(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800B8","项目管理") /* "项目管理" */ , (short) 18),
    Marketingexpense(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800B9","营销费用") /* "营销费用" */  , (short) 19),
    Other(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800BA","第三方") /* "第三方" */, (short) 20),
    CrossOperation(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800BB","跨境运营") /* "跨境运营" */, (short) 21),
    CrossCapitalAllocation(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800BC","跨境资金调度") /* "跨境资金调度" */, (short) 22),
    //汽车云
    CarSalesFinance (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800BD","汽车云-销售财务") /* "汽车云-销售财务" */, (short) 23),
    CarAfterSalesFinance(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800BE","汽车云-售后财务") /* "汽车云-售后财务" */, (short) 24),
    CarOtherSettle(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800BF","汽车云-其他结算") /* "汽车云-其他结算" */, (short) 25),
    // 授信管理   担保管理
    CreditManage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800C0","授信管理") /* "授信管理" */,(short)26),
    GuaranteeManage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800AB","担保管理") /* "担保管理" */,(short)27),

    LetterCreditManage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA041800AC","信用证管理") /* "信用证管理" */,(short)30),
    //，保函管理 ，衍生品管理
    GuaranteeHandlingFee(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-UI_189228AC04500012","保函管理") /* "保函管理"*/,(short) 31),
    DerivativesManagement(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-UI_18F6496004F8000A","衍生品管理") /* 衍生品管理 */,(short) 32),
    ConstructionCloud(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_19529B4004F00008","建筑云") /* 建筑云 */,(short) 33),
    RetailServices("零售服务",(short) 34),
    B2C("B2C订单中心",(short) 35);


    private String name;
    private short value;

    WDataOrigin(String name, short value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
    }

    public void setName(String name) {
        this.name = name;
    }

    public short getValue() {
        return value;
    }

    public void setValue(short value) {
        this.value = value;
    }

    private static HashMap<Short, WDataOrigin> map = null;
    private static HashMap<String, WDataOrigin> nameMap = null;

    private synchronized static void initMap() {
        if (map != null||nameMap!=null) {
            return;
        }
        map = new HashMap<Short, WDataOrigin>();
        nameMap = new HashMap<String, WDataOrigin>();
        WDataOrigin[] items = WDataOrigin.values();
        for (WDataOrigin item : items) {
            map.put(item.getValue(), item);
            nameMap.put(item.getName(),item);
        }
    }

    public static WDataOrigin find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }
    public static WDataOrigin findName(String name) {
        if (name == null) {
            return null;
        }
        if (nameMap == null) {
            initMap();
        }
        return nameMap.get(name);
    }
}
