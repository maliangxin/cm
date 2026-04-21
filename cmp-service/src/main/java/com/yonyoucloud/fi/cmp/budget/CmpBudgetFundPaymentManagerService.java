package com.yonyoucloud.fi.cmp.budget;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;

import java.util.List;

public interface CmpBudgetFundPaymentManagerService {
    /**
     * * 查询资金付款单
     * @return
     */
    CtmJSONArray queryFundPayment(CmpBudgetEventBill budgetEventBill) throws Exception;

    /**
     * * 更新
     * @param fundPayments
     * @return
     * @throws Exception
     */
    void updateFundPayment(List<FundPayment_b> fundPayments, Short isOccupyBudget) throws Exception;
    /**
     * * 查询资金收款单
     * @return
     */
    CtmJSONArray queryFundCollection(CmpBudgetEventBill budgetEventBill) throws Exception;

    /**
     * * 更新
     * @param fundPayments
     * @return
     * @throws Exception
     */
    void updateFundCollection(List<FundCollection_b> fundPayments, Short isOccupyBudget) throws Exception;

}
