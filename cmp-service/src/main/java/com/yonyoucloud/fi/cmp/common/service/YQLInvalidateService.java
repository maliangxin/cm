package com.yonyoucloud.fi.cmp.common.service;

import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import org.imeta.orm.base.BizObject;

import java.util.List;

/**
 * RPT0234银行交易流水逆向处理
 * @author guoxh
 */
public interface YQLInvalidateService {
    /**
     * 银企联返回流水状态为 作废时(is_refund=3) 删除逻辑
     * @param list
     * @throws Exception
     */
    void refundDelete(List<BankDealDetail> list) throws Exception;
    /**
     * 银企联返回流水状态为 作废时(is_refund=3) 删除逻辑
     * @param list
     * @throws Exception
     */
    void refundDeleteByBizObj(List<? extends BizObject> list) throws Exception;
}
