package com.yonyoucloud.fi.cmp.bankdealdetail;

import com.yonyoucloud.fi.cmp.bankunion.BankUnionRequest;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionRequestBodyRecord;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionResponse;

import java.util.List;

/**
 * desc:牧原交易明细接收service
 * author:wangqiangac
 * date:2023/12/5 16:41
 */
public interface BankUnionService {
    /**
     * 交易明细接收后发事件中心及批量入库
     * @param bankUnionRequests
     * @return
     * @throws Exception
     */
    BankUnionResponse insertTransactionDetail4BankUnion(List<BankUnionRequestBodyRecord> bankUnionRequests) throws Exception;
}