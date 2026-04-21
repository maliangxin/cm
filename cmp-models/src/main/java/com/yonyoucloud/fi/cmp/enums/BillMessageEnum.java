package com.yonyoucloud.fi.cmp.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

/**
 * <h1>BillMessageEnum</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-05-17 8:15
 */
public enum BillMessageEnum {

    /*
     * <h2>付款工作台</h2>
     */
    CMP_PAYMENT("cmp_payment", "cmp.paybill.PayBill"),
    /*
     * <h2>收款工作台</h2>
     */
    CMP_RECEIVE_BILL("cmp_receivebill", "cmp.receivebill.ReceiveBill"),
    /*
     * <h2>资金付款单</h2>
     */
    CMP_FUND_PAYMENT("cmp_fundpayment", "cmp.fundpayment.FundPayment"),
    /*
     * <h2>资收付款单</h2>
     */
    CMP_FUND_COLLECTION("cmp_fundcollection", "cmp.fundcollection.FundCollection"),
    /*
     * <h2>薪资支付工作台</h2>
     */
    CMP_SALARY_PAY("cmp_salarypay", "cmp.salarypay.Salarypay"),
    /*
     * <h2>转账工作台</h2>
     */
    CM_TRANSFER_ACCOUNT("cm_transfer_account", "cm.transferaccount.TransferAccount"),
    /*
     * <h2>外币兑换工作台</h2>
     */
    CMP_CURRENCY_EXCHANGE("cmp_currencyexchange", "cmp.currencyexchange.CurrencyExchange"),
    /*
     * <h2>汇兑损益</h2>
     */
    CMP_EXCHANGE_GAINLOSS("cmp_exchangegainloss", "cmp.exchangegainloss.ExchangeGainLoss"),

    /*
     * <h2>利息预提实体</h2>
     */
    CMP_ACCRUALSWITHHOLDINGQUERY("cmp_accrualsWithholdingquery", "cmp.accruals.AccrualsWithholding"),

    /*
     * <h2>支付保证金实体</h2>
     */

    CMP_PAYMARGIN("cmp_paymargin", "cmp.paymargin.PayMargin"),


    /*
     * <h2>收到保证金实体</h2>
     */
    CMP_RECEIVEMARGIN("cmp_receivemargin", "cmp.receivemargin.ReceiveMargin"),

    CMP_FOREIGNPAYMENT("cmp_foreignpayment", "cmp.foreignpayment.ForeignPayment"),

    CMP_JOURNALBILL("cmp_journalbill", "cmp.journalbill.JournalBill"),

    /*
     * <h2>同名账户批量划转</h2>
     */
    CM_BATCH_TRANSFER_ACCOUNT("cmp_batchtransferaccount", "cmp.batchtransferaccount.BatchTransferAccount"),;

    private final String billNum;
    private final String billType;
    private static HashMap<String, BillMessageEnum> valueMap = null;
    private static HashMap<String, BillMessageEnum> KeyMap = null;

    BillMessageEnum(String billNum, String billType) {
        this.billNum = billNum;
        this.billType = billType;
    }

    public String getBillNum() {
        return billNum;
    }

    public String getBillType() {
        return billType;
    }

    private static synchronized void initValueMap() {
        if (valueMap == null) {
            valueMap = new HashMap();
            BillMessageEnum[] items = values();
            BillMessageEnum[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                BillMessageEnum item = var1[var3];
                valueMap.put(item.getBillType(), item);
            }

        }
    }
    private static synchronized void initKeyMap() {
        if (KeyMap == null) {
            KeyMap = new HashMap();
            BillMessageEnum[] items = values();
            BillMessageEnum[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                BillMessageEnum item = var1[var3];
                KeyMap.put(item.getBillNum(), item);
            }

        }
    }

    public static BillMessageEnum findByValue(String value) {
        if (org.apache.commons.lang3.StringUtils.isBlank(value)) {
            return null;
        } else {
            if (valueMap == null) {
                initValueMap();
            }
            return valueMap.get(value);
        }
    }
    public static BillMessageEnum findByKey(String key) {
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
