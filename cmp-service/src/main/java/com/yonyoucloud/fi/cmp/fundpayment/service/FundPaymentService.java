package com.yonyoucloud.fi.cmp.fundpayment.service;


import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.FundCommonQueryDataVo;
import org.imeta.orm.base.BizObject;

import java.util.Map;

/**
 * @time：2023/3/6--18:24
 * @author：yanglu
 *
 **/
public interface FundPaymentService {

    /**
     * 整单拒绝的接口
     */
    void entrustReject(CtmJSONObject jsonObject) throws Exception;

    /**
     * 子表拒绝的接口
     */
    void entrustRejectSub(CtmJSONObject jsonObject) throws Exception;

    /**
     * <h2>退票重付：生成退票的资金付款单</h2>
     *
     * @param json : 入参
     * @return java.lang.String
     * @author Sun GuoCai
     * @since 2023/11/18 8:12
     */
    String refundAndRepayment(CtmJSONObject json);

    /**
     * <h2>第三方服务更新结算状态以及过账</h2>
     *
     * @param jsonObject : 入参
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2024/1/11 16:52
     */
    CtmJSONObject updateThirdPartyBillSettlementStatus(CtmJSONObject jsonObject) throws Exception;

    /**
     * <h2>资金收付款单提供RPC查询接口</h2>
     *
     * @param fundCommonQueryDataVo : 入参实体
     * @return java.lang.Object
     * @author Sun GuoCai
     * @since 2024/1/20 22:13
     */
    Object queryFundBillDataByParams(FundCommonQueryDataVo fundCommonQueryDataVo) throws Exception;

    /**
     * 查询票据号
     */
    Map<String,Object> findBillNoById(CtmJSONObject jsonObject) throws Exception;

    String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception;

}
