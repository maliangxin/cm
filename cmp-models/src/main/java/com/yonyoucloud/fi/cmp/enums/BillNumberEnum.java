package com.yonyoucloud.fi.cmp.enums;

import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;

import java.util.HashMap;

import static com.yonyoucloud.fi.cmp.constant.MetadataURI.SalaryPay;

/**
 * <h1>BillNumberEnum</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-03-20 12:22
 */
public enum BillNumberEnum {
    FUND_PAYMENT("cmp_fundpayment", FundPayment.ENTITY_NAME),
    SALARY_PAY("cmp_salarypay", Salarypay.ENTITY_NAME ),
    PAY_MARGIN("cmp_paymargin", PayMargin.ENTITY_NAME),
    RECEIVE_MARGIN("cmp_receivemargin", ReceiveMargin.ENTITY_NAME);


    private String code;
    private String entityname;
    private static HashMap<String, BillNumberEnum> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        BillNumberEnum[] items = BillNumberEnum.values();
        for (BillNumberEnum item : items) {
            map.put(item.getCode(), item);
        }
    }

    public static String find(String code) {
        if (code == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(code).getEntityname();
    }

    BillNumberEnum(String code, String entityname){
        this.code = code;
        this.entityname = entityname;
    }

    public String getCode(){
        return code;
    }

    public String getEntityname(){
        return entityname;
    }
}
