package com.yonyoucloud.fi.cmp.fundpayment.service;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;

import java.util.List;


public interface FundPaymentCommonService {

    void updateSubSettleStatusAndSettleAmountForFundPayment(CtmJSONArray array, List<FundPayment_b> fundPaymentBList) throws Exception;

    void updateSettleSuccessTimeAndGeneratorVoucherForFundPayment(List<FundPayment_b> fundPaymentBList) throws Exception;

    void updateSubSettleStatusAndSettleAmountForFundCollection(CtmJSONArray array, List<FundCollection_b> fundCollectionBList) throws Exception;

    void updateSettleSuccessTimeAndGeneratorVoucherForFundCollection(List<FundCollection_b> fundCollectionBList) throws Exception;
}
