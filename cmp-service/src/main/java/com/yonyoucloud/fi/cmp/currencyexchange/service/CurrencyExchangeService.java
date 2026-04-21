package com.yonyoucloud.fi.cmp.currencyexchange.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.currencyapply.CurrencyApply;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.journal.Journal;
import org.imeta.orm.base.BizObject;

import java.util.List;


public interface CurrencyExchangeService {

    // 结售汇交易结果查询SSFE3001
    String CURRENCY_EXCHANGE_RESULT_QUERY = "SSFE3001";
    // 即期结售汇交易提交SSFE1002
    String CURRENCY_EXCHANGE_SUBMIT = "SSFE1002";
    // 即期结售汇交割SSFE1003
    String CURRENCY_EXCHANGE_DELIVERY = "SSFE1003";
    // 即期结售汇询价SSFE1001
    String CURRENCY_EXCHANGE_RATE_QUERY = "SSFE1001";
    // 即期外汇买卖SSFE1018(提交接口的copy)
    String CURRENCY_EXCHANGE_SUBMIT_EACH = "SSFE1018";
    // 外币兑换工作台，银行类别为财务公司的银行账户，汇率查询接口
    String FINANCE_COMPANY_RATE_QUERY = "SSFE3012";
    // 附件上传银企联接口
    String UPLOAD_FILE = "11SC01";

	List<CurrencyExchange> queryByIds(Long[] ids) throws Exception;
    /**
     * 结算
     * @return CtmJSONObject 0:失败；1:成功；message：错误信息
     * @throws Exception
     */
    CtmJSONObject settle(List<CurrencyExchange> currencyExchangeList) throws Exception;
   /**
     * 结算
     * @return CtmJSONObject
     * @throws Exception
     */
   CtmJSONObject singleSettle(Long id) throws Exception;

    /**
     * 取消结算
     * @param id
     * @return
     * @throws Exception
     */
    CtmJSONObject singleUnSettle(Long id) throws Exception;

    /**
     * 取消结算
     * @param currencyExchangeList
     * @return
     * @throws Exception
     */
    CtmJSONObject unSettle(List<CurrencyExchange> currencyExchangeList) throws Exception;

    /**
     * 审批
     * @param currencyExchangeList
     * @throws Exception
     */
    CtmJSONObject audit(List<CurrencyExchange> currencyExchangeList) throws Exception;

    /**
     *取消审批
     * @param currencyExchangeList
     * @throws Exception
     */
    CtmJSONObject unAudit(List<CurrencyExchange> currencyExchangeList) throws Exception;

    /**
     * 获取用户自定义类型
     * @return
     */
    String getRateType(CtmJSONObject param) throws Exception;

    /**
     * 即期结售汇交易提交SSFE1002
     * @return
     */
    CtmJSONObject currencyExchangeSubmit(CtmJSONObject param) throws Exception;

    /**
     * 即期结售汇交割SSFE1003
     * @return
     */
    CtmJSONObject currencyExchangeDelivery(CtmJSONObject param) throws Exception;

    /**
     * 结售汇交易结果查询SSFE3001
     * @return
     */
    CtmJSONObject currencyExchangeResultQuery(CtmJSONObject param) throws Exception;

    /**
     * 即期结售汇询价SSFE1001
     * @return
     */
    CtmJSONObject currencyExchangeRateQuery(CtmJSONObject param) throws Exception;

    /**
     * 银行账户类别为财务公司，汇率查询接口，SSFE3012
     * @param param
     * @return
     * @throws Exception
     */
    CtmJSONObject financeCompanyRateQuery(CtmJSONObject param) throws Exception;

    String getRateTypeByFundBill(CtmJSONObject param) throws Exception;

    void insertCurrencyApply(CurrencyApply currencyApply) throws Exception;

    Boolean deleteCurrencyApply(Long currencyApplyId) throws Exception;

    CtmJSONObject checkAddTransType(CtmJSONObject params) throws Exception;

    String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception;

    Journal createJounal(BizObject bizObject, String billno, int data) throws Exception;

    /**
     * 货币兑换结果回传成功逻辑处理
     *
     * @param param
     * @return
     */
    CtmJSONObject updateCurrDataAndGeneratorVoucher(CtmJSONObject param) throws Exception;
}
