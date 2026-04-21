package com.yonyoucloud.fi.cmp.api.openapi;

import com.yonyou.cloud.middleware.rpc.RemoteCall;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.List;
import java.util.Map;

/**
 * Created by sz on 2019/4/20 0020.
 */
@RemoteCall("yonbip-fi-ctmcmp@c87e2267-1001-4c70-bb2a-ab41f3b81aa3")
public interface OpenApiService {
    /**
     * 外部服务，生成付款单
     * @param bill
     * @return
     * @throws Exception
     */
    CtmJSONObject savePayment(BillDataDto bill) throws Exception;

    /**
     * 外部服务，生成付款单查询状态
     * @param param
     * @return
     * @throws Exception
     */
    List querystatusByIds(CtmJSONObject param) throws Exception;

    /**
     * 外部服务，删除付款单
     * @param param
     * @return
     * @throws Exception
     */
    List deleteByIds(CtmJSONObject param) throws Exception;

    /**
     * 外部服务  生成收款单
     * @param param
     * @return
     * @throws Exception
     */
    String insertReceiveBill(CtmJSONObject param) throws Exception;

    /**
     * 外部服务 查询收款单
     * @param param
     * @return
     * @throws Exception
     */
    String queryReceiveBillStatusByIds(CtmJSONObject param) throws Exception;

    /**
     * 外部服务  删除收款单
     * @param param
     * @return
     * @throws Exception
     */
    String deleteReceiveBillByIds(CtmJSONObject param) throws Exception;



    /**
     * 外部服务  删除转账单
     * @param param
     * @return
     * @throws Exception
     */
    String deleteTransfer(CtmJSONObject param) throws Exception;

    /**
     * 外部服务  生成薪资支付单
     * @param param
     * @return
     * @throws Exception
     */
    String insertSalaryPay(CtmJSONObject param) throws Exception;

    /**
     * 外部服务 查询薪资支付单
     * @param param
     * @return
     * @throws Exception
     */
    String querySalaryPayStatusByIds(CtmJSONObject param) throws Exception;

    /**
     * 外部服务  删除薪资支付单
     * @param param
     * @return
     * @throws Exception
     */
    String deleteSalaryPayByIds(CtmJSONObject param) throws Exception;

    /**
     * 外部服务  生成多笔薪资支付单
     * @param param
     * @return
     * @throws Exception
     */
	String salaryPayCreate(CtmJSONObject param) throws Exception;
    /**
     * 根据结算单生成日记账
     */

    /**
     * <h2>根据账号查询期初数据</h2>
     *
     * @param param: 入参
     * @return java.lang.String
     * @author Sun GuoCai
     * @since 2021/3/31 19:28
     */
    String queryInitDataByAccountNo(CtmJSONObject param) throws Exception;

    /**
     * <h2>根据传入付款工作台id修改单据凭证状态（报销凭证生成时调用）</h2>
     *
     * @param param: 入参
     * @return java.lang.String
     * @since 2021/4/26 19:28
     */
    String updatePayBillVoucherStatus(CtmJSONObject param) throws Exception;


    /**
     * 收款工作台中收款单审核
     *
     * @param param
     * @return java.lang.String
     * @throws Exception
     */
    CtmJSONObject auditReceiveBill(CtmJSONObject param) throws Exception;

    /**
     * 收款工作台中收款单弃审
     *
     * @param param 入参
     * @return java.lang.String
     * @throws Exception
     */
    String unauditReveiveBill(CtmJSONObject param) throws Exception;


    /**
     * 据单据编号:code or 单据id:id or 来源单据id：srcbillid 查询收款单详细信息
     *
     * @param billNum
     * @param id
     * @param code
     * @param srcbillid
     * @return
     * @throws Exception
     */
    String queryReceiveBillByIdOrCodeOrSrcbillid(String billNum, Long id, String code, String srcbillid) throws Exception;

    /**
     * 外部服务  根据id删除薪资支付单CtmcmpCommonQueryRpcService
     * @param param 入参
     * @return
     * @throws Exception
     */
    CtmJSONObject deleteSalaryPayById(CtmJSONObject param) throws Exception;

    List<Map<String, Object>> queryBatchInitData(List<Map<String, Object>> param) throws Exception;

    List<Map<String, Object>> queryRealtimeBalance(List<Map<String, Object>> param) throws Exception;



//    /**
//     * 支票锁定/解锁接口
//     * @param param
//     * @return
//     * @throws Exception
//     */
//    String checkLock(CtmJSONObject param) throws Exception;
//
//    /**
//     * 支票付票接口
//     * @param param
//     * @return
//     * @throws Exception
//     */
//    String checkPayTicket(CtmJSONObject param) throws Exception;
//
//    /**
//     * 支票兑付/背书接口
//     * @param param
//     * @return
//     *
//     * @throws Exception
//     */
//    String checkCashingAndendorsement(CtmJSONObject param) throws Exception;
//
//    /**
//     * 支票作废接口
//     * @param param
//     * @return
//     * @throws Exception
//     */
//    String checkCancel(CtmJSONObject param) throws Exception;




}
