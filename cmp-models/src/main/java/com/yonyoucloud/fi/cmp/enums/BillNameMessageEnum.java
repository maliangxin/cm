package com.yonyoucloud.fi.cmp.enums;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

/**
 * <h1>BillNameMessageEnum</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-05-17 8:15
 */
public enum BillNameMessageEnum {

    /*
     * <h2>付款工作台</h2>
     */
    CMP_PAYMENT("cmp_payment", MessageUtils.getMessage("P_YS_PF_GZTSYS_0000013450") /* "付款工作台" */),
    /*
     * <h2>收款工作台</h2>
     */
    CMP_RECEIVE_BILL("cmp_receivebill", MessageUtils.getMessage("P_YS_PF_GZTSYS_0000013238") /* "收款工作台" */),
    /*
     * <h2>资金付款单</h2>
     */
    CMP_FUND_PAYMENT("cmp_fundpayment", MessageUtils.getMessage("P_YS_CTM_CM-UI_0001555889") /* "资金付款单" */),
    /*
     * <h2>资收付款单</h2>
     */
    CMP_FUND_COLLECTION("cmp_fundcollection", MessageUtils.getMessage("P_YS_CTM_CM-BE_1483318186127917060") /* "资收付款单" */),
    /*
     * <h2>薪资支付工作台</h2>
     */
    CMP_SALARY_PAY("cmp_salarypay", MessageUtils.getMessage("P_YS_PF_GZTSYS_0001106612") /* "薪资支付工作台" */),
    /*
     * <h2>转账工作台</h2>
     */
    CM_TRANSFER_ACCOUNT("cm_transfer_account", MessageUtils.getMessage("P_YS_PF_GZTSYS_0000096384") /* "转账工作台" */),
    /*
     * <h2>外币兑换工作台</h2>
     */
    CMP_CURRENCY_EXCHANGE("cmp_currencyexchange", MessageUtils.getMessage("P_YS_PF_GZTSYS_0000096387") /* "外币兑换工作台" */),
    /*
     * <h2>汇兑损益</h2>
     */
    CMP_EXCHANGE_GAINLOSS("cmp_exchangegainloss", MessageUtils.getMessage("P_YS_FI_YYFI-UI_0000161574") /* "汇兑损益" */);

    private final String billNum;
    private final String billName;
    private static HashMap<String, BillNameMessageEnum> valueMap = null;
    private static HashMap<String, BillNameMessageEnum> KeyMap = null;

    BillNameMessageEnum(String billNum, String billName) {
        this.billNum = billNum;
        this.billName = billName;
    }

    public String getBillNum() {
        return billNum;
    }

    public String getBillName() {
        return billName;
    }

    private static synchronized void initValueMap() {
        if (valueMap == null) {
            valueMap = new HashMap();
            BillNameMessageEnum[] items = values();
            BillNameMessageEnum[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                BillNameMessageEnum item = var1[var3];
                valueMap.put(item.getBillName(), item);
            }

        }
    }
    private static synchronized void initKeyMap() {
        if (KeyMap == null) {
            KeyMap = new HashMap();
            BillNameMessageEnum[] items = values();
            BillNameMessageEnum[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                BillNameMessageEnum item = var1[var3];
                KeyMap.put(item.getBillNum(), item);
            }

        }
    }

    public static BillNameMessageEnum findByValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        } else {
            if (valueMap == null) {
                initValueMap();
            }
            return valueMap.get(value);
        }
    }
    public static BillNameMessageEnum findByKey(String key) {
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
