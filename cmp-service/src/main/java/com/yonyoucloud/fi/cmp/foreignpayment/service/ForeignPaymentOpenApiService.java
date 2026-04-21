package com.yonyoucloud.fi.cmp.foreignpayment.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import org.springframework.stereotype.Service;


public interface ForeignPaymentOpenApiService {

    String queryBillByIdOrCode(String billNum, Long id, String code) throws Exception;

    CtmJSONObject deleteBillByIds(CtmJSONObject param) throws Exception;
}
