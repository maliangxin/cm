package com.yonyoucloud.fi.cmp.paymentbill.service;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PaymentBillSettleBO;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface PaymentSettleService {

    /**
     * 线下支付
     * @param rowData
     * @param batchProcess
     * @param date
     * @param userId
     * @param maxSettleDateMaps
     * @return
     * @throws Exception
     */
    PaymentBillSettleBO processOffLinePayItem(CtmJSONObject rowData, boolean batchProcess, Date date, long userId, Map<String, Date> maxSettleDateMaps, List<YmsLock> ymsLockList)throws Exception;

    /**
     * 线下支付
     * @param payBill
     * @param batchProcess
     * @param date
     * @param userId
     * @param maxSettleDateMaps
     * @return
     * @throws Exception
     */
    PaymentBillSettleBO processOffLinePayItem(PayBill payBill, boolean batchProcess, Date date, long userId, Map<String, Date> maxSettleDateMaps)throws Exception;

    /**
     * 取消线下支付
     * @param row
     * @param batchProcess
     * @param maxSettleDateMaps
     * @return
     * @throws Exception
     */
    PaymentBillSettleBO processCancelOffLinePayItem(CtmJSONObject row, boolean batchProcess, Map<String, Date> maxSettleDateMaps)throws Exception;


}
