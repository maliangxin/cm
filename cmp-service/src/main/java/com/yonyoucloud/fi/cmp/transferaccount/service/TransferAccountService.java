package com.yonyoucloud.fi.cmp.transferaccount.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;

/**
 * @InterfaceName TransferAccountService
 * @Desc 转账单服务
 * @Author tongyd
 * @Date 2019/9/9
 * @Version 1.0
 */
public interface TransferAccountService {
    /*
     * @Author tongyd
     * @Description 审核
     * @Date 2019/9/17
     * @Param [param]
     * @return java.lang.String
     **/
    CtmJSONObject audit(CtmJSONObject param) throws Exception;
    /**
     * @Author tongyd
     * @Description 弃审
     * @Date 2019/10/18
     * @Param [param]
     * @return java.lang.String
     **/
    CtmJSONObject unAudit(CtmJSONObject param) throws Exception;
    /**
     * @Author tongyd
     * @Description 线下支付
     * @Date 2019/10/10
     * @Param [param]
     * @return java.lang.String
     **/
    CtmJSONObject offLinePay(CtmJSONObject param) throws Exception;
    /**
     * @Author tongyd
     * @Description 取消线下支付
     * @Date 2019/10/10
     * @Param [param]
     * @return java.lang.String
     **/
    CtmJSONObject cancelOffLinePay(CtmJSONObject param) throws Exception;

    /**
     * 根据结算方式编码查询结算方式
     * @param param
     * @return
     * @throws Exception
     */
    CtmJSONObject findByCode(CtmJSONObject param) throws Exception;

    /**
     * 获取基准汇率
     * @param param
     * @return
     * @throws Exception
     */
    String queryExchangeRateRateTypeByCode(CtmJSONObject param) throws Exception;

    /**
     * 获取Swift码
     * @param param
     * @return
     * @throws Exception
     */
    String querySwiftCode(CtmJSONObject param) throws Exception;

    CtmJSONObject checkAddTransType(CtmJSONObject params) throws Exception;

    String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception;
}
