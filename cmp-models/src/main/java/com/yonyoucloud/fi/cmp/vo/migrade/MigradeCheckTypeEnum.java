package com.yonyoucloud.fi.cmp.vo.migrade;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

/**
 * <h1>MigradeCheckTypeEnum</h1>
 *
 * @author majfd
 * @version 1.0
 * @since 2024-01-02 8:15
 */
public enum MigradeCheckTypeEnum {

    AuditAndUnSettle("1", "已审核未结算"),
    VoucherStatusReceived("2", "凭证已发送未生成-往期"),
    SourceFromDrftBill("3", "商业汇票推送数据"),
    SourceFromArapBill("4", "应收应付推送未审核数据"),
    CharantAllocation("5", "特征项待分配"),
    IntransitPayBill("6", "在途升迁付款工作台单据"),
    IntransitReceiveBill("7", "在途升迁收款工作台单据"),
    AuditAndSettleIng("8", "已审核结算中数据"),
    VoucherStatusReceivedThis("9", "凭证已发送未生成-本期"),
    SettlementPayBill("18", "结算完成升迁付款工作台单据"),
    SettlementReceiveBill("18", "结算完成升迁收款工作台单据"),
    PayapplicationBillApproveEdit("19", "付款申请单审批过程中的单据"),
    IsWfControlledApproveEdit("20", "启用审批流且审批状态为审批中"),
    FundPayBillIsExist("21", "存在资金付款单"),
    FundCollBillIsExist("22", "存在资金收款单");

    private final String CheckType;
    private final String CheckTypeName;
    private static HashMap<String, MigradeCheckTypeEnum> valueMap = null;
    private static HashMap<String, MigradeCheckTypeEnum> KeyMap = null;

    MigradeCheckTypeEnum(String CheckType, String CheckTypeName) {
        this.CheckType = CheckType;
        this.CheckTypeName = CheckTypeName;
    }

    public String getCheckType() {
        return CheckType;
    }

    public String getCheckTypeName() {
        return CheckTypeName;
    }

    private static synchronized void initValueMap() {
        if (valueMap == null) {
            valueMap = new HashMap();
            MigradeCheckTypeEnum[] items = values();
            MigradeCheckTypeEnum[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                MigradeCheckTypeEnum item = var1[var3];
                valueMap.put(item.getCheckTypeName(), item);
            }

        }
    }
    private static synchronized void initKeyMap() {
        if (KeyMap == null) {
            KeyMap = new HashMap();
            MigradeCheckTypeEnum[] items = values();
            MigradeCheckTypeEnum[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                MigradeCheckTypeEnum item = var1[var3];
                KeyMap.put(item.getCheckType(), item);
            }

        }
    }

    public static MigradeCheckTypeEnum findByValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        } else {
            if (valueMap == null) {
                initValueMap();
            }
            return valueMap.get(value);
        }
    }
    public static MigradeCheckTypeEnum findByKey(String key) {
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
