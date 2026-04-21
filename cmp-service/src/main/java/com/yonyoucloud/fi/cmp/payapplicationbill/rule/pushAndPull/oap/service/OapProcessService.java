package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <h1>应付事项推送过来的单据公共处理接口</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-11-19 15:57
 */
public interface OapProcessService {

    /**
     * <h2>处理子表采购发票id和子表供应商</h2>
     *
     * @param map : 表头数据
     * @param subList : 明细数据集合
     * @author Sun GuoCai
     * @since 2022/11/19 17:36
     */
    void invoiceAndSupplierCommonHandler(Map<String, Object> map, List<Map<String, Object>> subList) throws Exception;

    /**
     * <h2>处理源头是订单的主表id和子表id</h2>
     *
     * @param subList : 明细数据
     * @author Sun GuoCai
     * @since 2022/11/19 17:34
     */
    void sourceOrderDataHandle(List<Map<String, Object>> subList) throws Exception;

    /**
     * <h2>公共数据处理：供应商(默认银行账号，默认开户行地址 .etc),交易类型，款项类型，币种</h2>
     *
     * @param map : 表头数据
     * @param paramsCacheMap : 缓存Map
     * @author Sun GuoCai
     * @since 2022/11/19 17:31
     */
    void processOwnCommonData(Map<String, Object> map, Map<String, Map<String, Object>> paramsCacheMap, String tradeTypeFlag) throws Exception;

    /**
     * <h2>数据校验</h2>
     *
     * @param omakes : 应付事项推送过来的数据
     * @param messages : 数据校验错误提示信息集合
     * @author Sun GuoCai
     * @since 2022/11/19 17:29
     */
    void verificationParameters(List<Map<String, Object>> omakes, List<String> messages) throws Exception;

    /**
     * <h2>处理累计付款申请金额</h2>
     *
     * @param fromTopOutsourcingOrderList : 明细数据集合
     * @param map : 表头数据
     * @param paymentApplyAmountSumPurchaseAmount : 累计付款申请金额合计
     * @return boolean
     * @author Sun GuoCai
     * @since 2022/11/19 18:43
     */
    boolean paymentApplyAmountSumProcess(List<Map<String, Object>> fromTopOutsourcingOrderList,
                                         Map<String, Object> map, BigDecimal paymentApplyAmountSumPurchaseAmount) throws Exception;

    /**
     * <h2>设置相关金额字段的值和组织机构字段的值</h2>
     *
     * @param map : 表头数据
     * @param paymentApplyAmountSum : 累计付款申请金额字段
     * @author Sun GuoCai
     * @since 2022/11/19 19:54
     */
    void setAmountDefaultValueAndOrg(Map<String, Object> map, BigDecimal paymentApplyAmountSum) throws Exception;


    /**
     * <h2>老数据源头订单类型默认值的处理</h2>
     *
     * @param map : 表头数据
     * @param details : 子表明细数据集合
     * @author Sun GuoCai
     * @since 2022/11/19 19:59
     */
    void sourceOrderTypeValueProcess(Map<String, Object> map, List<Map<String, Object>> details) throws Exception;

    /**
     * <h2>请求采购订单提供的批量接口，获取可付款申请金额</h2>
     *
     * @param map : 表头数据
     * @param params : 批量入参参数
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2022/11/19 17:04
     */
    CtmJSONObject queryOutsourcingOrderRequestedAmount(Map<String, Object> map, Map<String, Object> params) throws Exception;

}
