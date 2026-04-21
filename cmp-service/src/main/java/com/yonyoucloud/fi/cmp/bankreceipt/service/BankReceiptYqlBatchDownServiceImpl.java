package com.yonyoucloud.fi.cmp.bankreceipt.service;

import cn.hutool.core.map.MapUtil;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.HttpsUtils;
import com.yonyoucloud.fi.cmp.util.YQLUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 银行交易回单-银企联批量下载数据接口
 *
 * @author zhuguangqian
 * @since 2024/12/6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankReceiptYqlBatchDownServiceImpl implements BankReceiptYqlBatchDownService {
    // 签名验签
    private final BankConnectionAdapterContext bankConnectionAdapterContext;

    @Override
    public CtmJSONObject batchDown(List<Map<String, Object>> bankelereceiptNew, String requestseqno, EnterpriseBankAcctVO enterpriseBankAcctVO) {
        try {
            //第一条数据
            Map<String, Object> bankelereceiptFirst = bankelereceiptNew.get(0);
            //组装下载信息
            CtmJSONArray param_down = new CtmJSONArray();
            for (Map<String, Object> newReceipt : bankelereceiptNew) {
                CtmJSONObject down = new CtmJSONObject();
                down.put("bill_no", newReceipt.get("receiptno"));
                down.put("bill_extend", newReceipt.get("bill_extend"));
                String uniqueNoStr = MapUtil.getStr(newReceipt, YQLUtils.YQL_UNIQUE_NO);
                if (StringUtils.isNotEmpty(uniqueNoStr)) {
                    down.put(YQLUtils.UNIQUE_NO, uniqueNoStr);
                }
                param_down.add(down);
            }

            CtmJSONObject postMsg_down = this.buildReceiptDownloadMsgBatch(param_down, enterpriseBankAcctVO, bankelereceiptFirst.get("custno").toString(), requestseqno);
            log.error("==========================回单下载参数=====================================>" + CtmJSONObject.toJSONString(postMsg_down));
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(postMsg_down.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", postMsg_down.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            CtmJSONObject postResult_down = HttpsUtils.doHttpsPostNew(ITransCodeConstant.DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL, requestData, bankConnectionAdapterContext.getChanPayUri());
            CtmJSONObject param = new CtmJSONObject();
            param.put("serviceCode", IServicecodeConstant.BANKRECEIPTMATCH);
            HttpsUtils.saveYQLBusinessLog(ITransCodeConstant.DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL, bankConnectionAdapterContext.getChanPayUri(), postMsg_down, postResult_down, param, Objects.toString(bankelereceiptFirst.get("account")));
            log.error("==========================回单下载结果=====================================>" + CtmJSONObject.toJSONString(postResult_down));
            return postResult_down;
        } catch (Exception e) {
            // 重新组装子线程全部失败的返回结果
            String failMsg = e.getMessage();
            CtmJSONObject error = new CtmJSONObject();
            error.put("code", 2);
            error.put("message", failMsg);
            return error;
        }
    }

    /**
     * 构建查电子回单批量下载报文 Copy by BankReceiptServiceImpl
     *
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author zhuguangqian
     * @since 2021/5/26
     **/
    private CtmJSONObject buildReceiptDownloadMsgBatch(CtmJSONArray params, EnterpriseBankAcctVO enterpriseBankAcctVO,
                                                       String customNo, String requestseqno) {
        Map<String, Object> requestHead = buildRequloadestHead(ITransCodeConstant.DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL,
                null,
                customNo,
                requestseqno,
                null,
                null,
                true);
        List<Map<String, String>> record = new ArrayList<>();
        Map<String, Object> request_body = new HashMap<>();
        request_body.put("acct_name", enterpriseBankAcctVO.getAcctName());
        request_body.put("acct_no", enterpriseBankAcctVO.getAccount());
        for (int i = 0; i < params.size(); i++) {
            CtmJSONObject param = params.getJSONObject(i);
            Map<String, String> recordOne = new HashMap<>();
            recordOne.put("bill_no", (String) param.get("bill_no"));
            recordOne.put("bill_extend", (String) param.get("bill_extend"));
            recordOne.put(YQLUtils.UNIQUE_NO, (String) param.get(YQLUtils.UNIQUE_NO));
            record.add(recordOne);
        }
        request_body.put("record", record);
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", request_body);
        return queryMsg;
    }

    private CtmJSONObject buildRequloadestHead(String transCode, String oper, String customNo, String requestseqno, String signature, String channel, Boolean isBatch) {
        CtmJSONObject requestHead = new CtmJSONObject();
        if (isBatch) {
            requestHead.put("version", "2.1.1");
        } else
            requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", requestseqno);
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", channel != null ? channel : AppContext.getBean(BankConnectionAdapterContext.class).getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper_sign", signature);
        requestHead.put("tran_code", transCode);
        requestHead.put("oper", oper);
        return requestHead;
    }
}
