package com.yonyoucloud.fi.cmp.bankreceipt.service;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.List;
import java.util.Map;

/**
 * 银行交易回单-银企联批量下载数据接口
 *
 * @author zhuguangqian
 * @since  2024/12/6
 */
public interface BankReceiptYqlBatchDownService {
    CtmJSONObject batchDown(List<Map<String, Object>> bankelereceiptNew, String requestseqno, EnterpriseBankAcctVO enterpriseBankAcctVO);
}
