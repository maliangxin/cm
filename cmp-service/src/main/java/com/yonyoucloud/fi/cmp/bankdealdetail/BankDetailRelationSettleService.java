package com.yonyoucloud.fi.cmp.bankdealdetail;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;

import java.util.List;

/**
 * 银行交易明细拉取后关联结算实现
 * @author wq
 * @version 1.0.0
 * @date 2023年11月23日15:45:01
 */
public interface BankDetailRelationSettleService {
    /**
     * 银行对账单关联结算
     * @param bankRecords
     * @throws Exception
     */
    CtmJSONObject detailRelationSettle(List<BankReconciliation> bankRecords) throws  Exception;
}
