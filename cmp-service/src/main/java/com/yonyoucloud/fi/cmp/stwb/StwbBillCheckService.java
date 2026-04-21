package com.yonyoucloud.fi.cmp.stwb;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;

/**
 * @description: 国机相关：结算检查，校验相关风险参数
 * @author: wanxbo@yonyou.com
 * @date: 2023/5/16 10:47
 */

public interface StwbBillCheckService {
    /**
     * 结算检查，校验风险参数
     * @param params
     * @return
     * @throws Exception
     */
    CtmJSONObject billCheck(CtmJSONObject params) throws Exception;

    /**
     * 资金付款单提交结算风险检查
     * @param fundPayment
     * @return
     * @throws Exception
     */
    CtmJSONObject fundSubmitBillCheck(FundPayment fundPayment) throws Exception;

    /**
     * 转账单提交结算风险检查
     * @param transferAccount
     * @return
     * @throws Exception
     */
    CtmJSONObject transferSubmitBillCheck(TransferAccount transferAccount) throws Exception;

    /**
     * 外汇付款 提交结算风险检查*
     * @param foreignPayment
     * @return
     * @throws Exception
     */
    CtmJSONObject foreignpaymentSubmitBillCheck(ForeignPayment foreignPayment) throws Exception;

}
