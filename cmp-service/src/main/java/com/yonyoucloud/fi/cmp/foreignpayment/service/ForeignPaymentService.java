package com.yonyoucloud.fi.cmp.foreignpayment.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;

public interface ForeignPaymentService {

    /**
     * 查询银行类别编码*
     * @param param
     * @return
     * @throws Exception
     */
    CtmJSONObject queryBankCode(CtmJSONObject param) throws Exception;


    /**
     * 生成凭证
     * * *
     * @param foreignPayment
     * @throws Exception
     */
    void toVourcher(ForeignPayment foreignPayment) throws Exception;

    String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception;
}
