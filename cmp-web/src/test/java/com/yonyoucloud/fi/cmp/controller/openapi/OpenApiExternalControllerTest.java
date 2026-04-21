package com.yonyoucloud.fi.cmp.controller.openapi;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.paymargin.service.impl.PayMarginApiDto;

public class OpenApiExternalControllerTest {

    public static void main(String[] args) {
        String str = "{\n" +
                "    \"accentity\": \"1525652227072458756\",\n" +
                "    \"accentityRaw\": \"1525652227072458756\",\n" +
                "    \"accentity_name\": \"收入中台演示销售组织1\",\n" +
                "    \"accentityRaw_name\": \"收入中台演示销售组织1\",\n" +
                "    \"accentity_code\": \"pkm00\",\n" +
                "    \"accentityRaw_code\": \"pkm00\",\n" +
                "    \"code\": \"MDPT20241025000162\",\n" +
                "    \"vouchdate\": \"2024-10-25 00:00:00\",\n" +
                "    \"tradetype\": \"1784808303128215559\",\n" +
                "    \"tradetype_code\": \"cmp_paymargin_payment\",\n" +
                "    \"tradetype_name\": \"支付保证金\",\n" +
                "    \"initflag\": 0,\n" +
                "    \"marginbusinessno\": \"MDPT20241025000162\",\n" +
                "    \"currency\": \"1623643074168094746\",\n" +
                "    \"currency_name\": \"人民币\",\n" +
                "    \"currency_moneyDigit\": 2,\n" +
                "    \"currency_code\": \"CNY\",\n" +
                "    \"project\": \"1535890776273715201\",\n" +
                "    \"project_name\": \"000001\",\n" +
                "    \"project_code\": \"000002\",\n" +
                "    \"natCurrency\": \"1525643705855773169\",\n" +
                "    \"natCurrency_name\": \"人民币2jklifhd\",\n" +
                "    \"natCurrency_moneyDigit\": 8,\n" +
                "    \"exchangeratetype\": \"0000L6YQ8AVLFUZPXD0000\",\n" +
                "    \"exchangeratetype_code\": \"01\",\n" +
                "    \"exchangeratetype_digit\": 8,\n" +
                "    \"exchangeratetype_name\": \"基准汇率\",\n" +
                "    \"exchRate\": 0.5,\n" +
                "    \"srcitem\": 8,\n" +
                "    \"verifystate\": 0,\n" +
                "    \"isWfControlled\": false,\n" +
                "    \"voucherstatus\": 2,\n" +
                "    \"oppositetype\": \"1\",\n" +
                "    \"marginamount\": 11,\n" +
                "    \"natmarginamount\": \"5.50\",\n" +
                "    \"settleflag\": 0,\n" +
                "    \"paymenttype\": 1,\n" +
                "    \"enterprisebankaccount_name\": null,\n" +
                "    \"ourname_name\": null,\n" +
                "    \"ourbankaccount_name\": null,\n" +
                "    \"customer_name\": null,\n" +
                "    \"customerbankaccount_bankAccountName\": null,\n" +
                "    \"supplier_name\": null,\n" +
                "    \"supplierbankaccount_accountname\": null,\n" +
                "    \"capBizObj_name\": null,\n" +
                "    \"capBizObjbankaccount_accountname\": null,\n" +
                "    \"oppositebankNumber_name\": null,\n" +
                "    \"oppositebankType_name\": null,\n" +
                "    \"conversionmarginflag\": 0,\n" +
                "    \"isOccupyBudget\": 0\n" +
                "}";
        PayMarginApiDto dto = CtmJSONObject.parseObject(str, PayMarginApiDto.class);
        System.out.println(dto);
        PayMargin entity = new PayMargin();
        entity.toString();

    }

}