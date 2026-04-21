package com.yonyoucloud.fi.cmp.paymentbill.service;

import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.paybill.PayBill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @InterfaceName PaymentService
 * @Description 支付服务接口
 * @Author tongyd
 * @Date 2019/4/26 15:31
 * @Version 1.0
 **/
public interface PaymentService {
    // 回单地址 此处地址改为配置文件 放入yonbip-module-finmdd-url.properties中
//    String ReceiptDetail_ADDR = "https://open-api-pre.diwork.com/yonbip/fi/cmpBankElectronPdf/detail";
    // 回单地址 请求流水号
    String ReceiptDetail_ADDR_REQSEQ = AppContext.getEnvConfig("ReceiptDetail_ADDR_REQSEQ");
    /*
     *@Author tongyd
     *@Description 网银预下单
     *@Date 2019/6/24 15:42
     *@Param [params]
     *@Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    CtmJSONObject internetBankPlaceOrder(CtmJSONObject params) throws Exception;

    /*
     *@Author mln
     *@Description 取消网银预下单
     *@Date 2021/3/30 10:42
     *@Param [params]
     *@Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    CtmJSONObject internetBankPlaceOrderCancel(CtmJSONObject params) throws Exception;

    /*
     *@Author tongyd
     *@Description 预下单交易确认
     *@Date 2019/6/12 10:08
     *@Param [params]
     *@Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    CtmJSONObject confirmPlaceOrder(CtmJSONObject params) throws Exception;

    /*
     *@Author tongyd
     *@Description 更新批量网银支付结果
     *@Date 2019/6/13 9:20
     *@Param [requestData]
     *@Return java.lang.String
     **/
    String updateBatchPayStatus(String requestData) throws Exception;

    /*
     *@Author tongyd
     *@Description 批量支付明细状态查询
     *@Date 2019/6/13 20:41
     *@Param [params]
     *@Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    CtmJSONObject queryBatchDetailPayStatus(CtmJSONArray params) throws Exception;

    /*
     *@Author tongyd
     *@Description 线下支付
     *@Date 2019/6/4 20:46
     *@Param [params]
     *@Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    CtmJSONObject offLinePay(CtmJSONObject params) throws Exception;

    /*
     *@Author tongyd
     *@Description 取消线下支付
     *@Date 2019/6/4 20:46
     *@Param [params]
     *@Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    CtmJSONObject cancelOffLinePay(CtmJSONObject params) throws Exception;




    /**
     * @Author tongyd
     * @Description 获取银企联渠道号
     * @Date 2019/4/28 18:59
     * @Param []
     * @Return java.lang.String
     **/
    String getChanPayChanelNo();



    /**
     * 根据组织ID获取业务账簿
     * @param orgId
     * @return
     * @throws Exception
     */
//    Long getAccBookTypeByOrgId(String orgId) throws Exception;
    /**
     * 租户下有多个客户，每个客户有自己的客户号；调用银企联根据不同客户号做区分
     * @param bankAccountSettinges
     * @return
     */
    Map<String, List<Map<String, Object>>> customList(List<Map<String, Object>> bankAccountSettinges);

    /**
     * 构建请求流水号
     * @param customNo
     * @return
     */
    String buildRequestSeqNo(String customNo);

    /*
     *@Author tongyd
     *@Description 付款工作台审批
     *@Date 2019/6/24 18:51
     *@Param [params]
     *@Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    CtmJSONObject paymentAudit(CtmJSONObject param) throws Exception;

    /*
     *@Author tongyd
     *@Description 付款工作台弃审
     *@Date 2019/6/24 18:52
     *@Param [param]
     *@Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    CtmJSONObject paymentUnAudit(CtmJSONObject param) throws Exception;

    List<PayBill> queryAggvoByIds(Long[] ids) throws Exception;

    /**
     * 查询租户下开通银企联的银行账户信息
     * @param accentitys
     * @param banktypes
     * @param currencys
     * @return
     * @throws Exception
     */
    List<Map<String, Object>> queryBankAccountSetting(String accentitys, String banktypes, String currencys) throws Exception;

    CtmJSONObject queryBatchDetailPayStatusBySifang(CtmJSONArray params) throws Exception;


    /**
     * 网银单笔预下单
     * @param params
     * @param payBills 其实只有一条数据 为了适配之前的数据解析代码 这里传递list
     * @return
     * @throws Exception
     */
    CtmJSONObject internetBankPlaceOrderSingle(CtmJSONObject params,List<PayBill> payBills) throws Exception;


    /**
     * 单笔支付明细状态查询
     * @param params
     * @return
     * @throws Exception
     */
    CtmJSONObject querySingleDetailPayStatus(CtmJSONArray params) throws Exception;

    /**
     * 根据币种主键查询对应的编码
     * 如果缓存中存在的话取缓存 没有的话查询
     * @param currencyList
     * @return
     * @throws Exception
     */
    HashMap<String, String> queryCurrencyCode(List<BankAcctCurrencyVO> currencyList) throws Exception;

    /**
     * 根据币种编码获取币种
     *
     * @param currencyCode
     * @return
     * @throws Exception
     */
    Map<String, Object> getCurrencyByCode(String currencyCode) throws Exception;

    /**
     * 根据账户id获取币种信息
     *
     * @param currencyCode
     * @return
     * @throws Exception
     */
    Map<String, Object> getCurrencyByAccount(String currencyCode) throws Exception;

}
