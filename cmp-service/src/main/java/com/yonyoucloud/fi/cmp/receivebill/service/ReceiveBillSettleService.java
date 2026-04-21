package com.yonyoucloud.fi.cmp.receivebill.service;

import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBillSettleBO;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBillUnsettleBO;

import java.util.Date;
import java.util.Map;

public interface ReceiveBillSettleService {

    /**
     * 结算
     * @param receiveBillId
     * @param batchProcess
     * @param codeMap
     * @param date
     * @param maxSettleDateMaps
     * @return
     * @throws Exception
     */
    ReceiveBillSettleBO processSettleItem(Long receiveBillId, boolean batchProcess, Map<Long, String> codeMap, Date date, Map<String, Date> maxSettleDateMaps) throws Exception;

    /**
     * 取消结算
     * @param receiveBill
     * @param batchProcess
     * @param maxSettleDateMaps
     * @return
     * @throws Exception
     */
    ReceiveBillUnsettleBO processUnsettleItem(ReceiveBill receiveBill, boolean batchProcess, Map<String, Date> maxSettleDateMaps) throws Exception;

    }
