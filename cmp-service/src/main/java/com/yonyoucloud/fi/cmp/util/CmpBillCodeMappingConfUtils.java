package com.yonyoucloud.fi.cmp.util;

import com.yonyou.iuap.billcode.common.BillCodeMappingConfUtils;
import com.yonyou.ucf.mdd.ext.bill.common.BizObjCodeUtils;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author guoyangy
 * @Date 2024/4/20 23:45
 * @Description todo
 * @Version 1.0
 */
@Component
public class CmpBillCodeMappingConfUtils {
    private static BillCodeMappingConfUtils billCodeMappingConfUtils;
    @Autowired
    private void setBillCodeMappingConfUtils(BillCodeMappingConfUtils _billCodeMappingConfUtils) {
        billCodeMappingConfUtils = _billCodeMappingConfUtils;
    }

    public static String getBillCode(String billNum){
        //return billCodeMappingConfUtils.getBillCode(billNum);
        String bizcode = null;
        switch (billNum){
            case IBillNumConstant.RECEIVE_BILL:
                bizcode = "ctm-cmp.cmp_receivebill";
                break;
            case IBillNumConstant.PAYMENT:
                bizcode = "ctm-cmp.arap_paybill";
                break;
            case IBillNumConstant.PAYAPPLICATIONBILL:
                bizcode = "ctm-cmp.cmp_payapplicationbill";
                break;
            case IBillNumConstant.CMP_BILLCLAIM_CARD:
                bizcode = "ctm-cmp.cmp_billclaimcard";
                break;
            case IBillNumConstant.FUND_COLLECTION:
                bizcode = "ctm-cmp.cmp_fundcollection";
                break;
            case IBillNumConstant.FUND_PAYMENT:
                bizcode = "ctm-cmp.cmp_fundpayment";
                break;
            case IBillNumConstant.CMP_CHECKSTOCKAPPLY:
                bizcode = "ctm-cmp.cmp_checkStockApply";
                break;
            case IBillNumConstant.CMP_ACCOUNT_DETAIL_EXCLUSION:
                bizcode = "ctm-cmp.cmp_accountdetailexclusion";
                break;
            default:
                bizcode = null;
        }
        if(bizcode == null){
            return BizObjCodeUtils.getBizObjCodeByBillNumAndDomain(billNum, "ctm-ctmp");
        } else {
            return bizcode;
        }
    }
}
