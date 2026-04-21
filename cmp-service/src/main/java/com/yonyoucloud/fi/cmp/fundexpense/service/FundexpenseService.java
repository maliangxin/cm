package com.yonyoucloud.fi.cmp.fundexpense.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import org.imeta.orm.base.BizObject;

import java.util.List;

public interface FundexpenseService {

    /**
     * 根据费用币种、业务币种、汇率类型、费用日期查询汇率
     */
    CtmJSONObject queryExchangeRate(CtmJSONObject param) throws Exception;

    /**
     *  占预算
     * @param bizObject
     * @param fundexpense_bs
     * @param billCode
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget gcExecuteBatchSubmit(BizObject bizObject, List<Fundexpense_b> fundexpense_bs, String billCode, String billAction) throws Exception;

    /**
     *  释放预算
     * @param bizObject
     * @param fundexpense_bs
     * @param billCode
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget gcExecuteBatchUnSubmit(BizObject bizObject, List<Fundexpense_b> fundexpense_bs, String billCode,String billAction) throws Exception;

    /**
     *  实占预算
     * @param bizObject
     * @param fundexpense_b
     * @param billCode
     * @param billAction
     * @return
     */
    ResultBudget fundCollectionEmployActualOccupySuccessAudit(BizObject bizObject, Fundexpense_b fundexpense_b, String billCode, String billAction);
}
