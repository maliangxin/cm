package com.yonyoucloud.fi.cmp.openapi.service;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @Author: guanqg
 * @Date: 2020/8/25 16:51
 */
public interface OpenApiExternalService {

    String ReceiptDetail_ADDR = AppContext.getEnvConfig("ReceiptDetail_ADDR");

    String ReceiptDetail_ADDR_REQSEQ = AppContext.getEnvConfig("ReceiptDetail_ADDR_REQSEQ");

    CtmJSONObject queryBankelectronicreceiptPDFByBillBankSeq(String bankseq,String bankid,HttpServletResponse response);

    /**
     * 根据银行对账单ID获取交易回单
     */
    CtmJSONObject queryBankelectronicreceiptPDFByBillBillId(String bankBillId,HttpServletResponse response);

    CtmJSONObject queryBankelectronicreceiptPDFByReqSeq(String bankseq,String bankid,HttpServletResponse response);

    List queryBankTradeDetailElectron(CtmJSONObject param) throws Exception;

    Map<String, Object>  queryBankTradeDetailElectronList(CtmJSONObject param,Boolean isHistory) throws Exception;

    Map<String,Object>  ReceiptDetailListThread(CtmJSONObject param) throws Exception;

    List billStatus(CtmJSONObject param) throws Exception;

    List billTransferStatus(CtmJSONObject param) throws Exception;

    List<String> payStatusList(CtmJSONObject param) throws Exception;

    boolean isYonyouPay(String bankseq,String bankid) throws Exception;

    Object billReceiveStatus(CtmJSONObject param) throws Exception;

    CtmJSONObject bankTradeDetailElectronTask(CtmJSONObject param) throws Exception;

    Map<String, Object> bankElectronicReceiptFile(CtmJSONObject param) throws Exception;

    Map<String, Object> repairBankElectFile(CtmJSONObject param) throws Exception;

    Map<String, Object> balanceAdjustResultFile(CtmJSONObject param) throws Exception;

    Map<String, Object> electronicStatementConfirmFile(CtmJSONObject param) throws Exception;

}

