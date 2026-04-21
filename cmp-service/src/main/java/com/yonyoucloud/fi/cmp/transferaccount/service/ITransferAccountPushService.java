package com.yonyoucloud.fi.cmp.transferaccount.service;

import org.imeta.orm.base.BizObject;

import java.util.List;

/**
 * @Description 转账单推送资金结算接口
 * @Author guanshaoting
 * @Date 2023/3/2
 **/
public interface ITransferAccountPushService {

    /**
     * 推送转账单数据
     * @param billList
     * @param bCheck
     * @throws Exception
     */
    void pushBill(List<BizObject> billList, boolean bCheck, int settlemode, String checkBillNo) throws Exception;


    /**
     * 删除转账单数据
     * @param billList
     * @throws Exception
     */
    void deleteBill(List<BizObject> billList) throws Exception;

}
