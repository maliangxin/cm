package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JSONArray;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.https.utils.TransErrorInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author: liaojbo
 * @Date: 2025年03月14日 09:11
 * @Description:
 */
@Slf4j
public class YQLUtils {

    public static final String CUSTOM_NO = "customNo";
    public static final String CONTACT_YQL_TIP = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400424", "请联系银企联顾问处理！") /* "请联系银企联顾问处理！" */;
    //银企联报文字段对应的银行交易回单元数据字段名
    public static String YQL_UNIQUE_NO = "yqlUniqueNo";

    // 银企联报文字段
    public static String UNIQUE_NO = "unique_no";
    public static String CURR_CODE = "curr_code";
    public static String ACCT_NO = "acct_no";
    public static String BILL_NO = "bill_no";
    // 如果是其他返回的是back_num
    public static final String BACK_NUM = "back_num";
    // 如果是实时余额返回的是tot_num
    public static final String TOT_NUM = "tot_num";


    // 原有常量定义
    static String NO_DATAS_ERVICE_RESP_CODE = "350019";

    // 批量支付明细状态查询
    static String QUERY_BATCH_DETAIL_PAY_STATUS = "40B10";

    // 账户实时余额查询
    static String QUERY_ACCOUNT_BALANCE = "40T20";

    // 银行账户交易明细推送 - 银企联 到账通知
    static String RECEIVE_ACCOUNT_TRANSACTION_DETAIL = "40D22";
    // 银企联 到账通知 重试
    static String RECEIVE_ACCOUNT_TRANSACTION_DETAIL_CHECK = "60T20";
    // 账户交易明细查询
    static String QUERY_ACCOUNT_TRANSACTION_DETAIL = "40T22";

    // 查询内部账户交易明细
    static String QUERY_INNER_ACCOUNT_TRANSACTION_DETAIL = "INNER";
    // 账户电子回单查询
    static String QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL = "40T23";
    // 账户电子回单下载
    static String DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL = "48T23";

    // 批量预下单
    static String BATCH_PAY_PRE_ORDER = "11B10P";
    // 预下单交易确认
    static String PRE_ORDER_TRANSACTION_CONFIRM = "50C10";

    // 账户实时余额历史数据查询
    static String QUERY_HIS_ACCOUNT_BALANCE = "40T21";
    // 代发工资结果查询
    static String BATCH_PAY_DETAIL_STATUS_QUERY = "41B12";

    // 账户电子对账单确认查询
    static String QUERY_ELECTRONIC_STATEMENT_CONFIRM = "43T26";
    // 对账单文件下载
    static String DOWNLOAD_ELECTRONIC_STATEMENT_CONFIRM_FILE = "43T23";

    // 单笔支付预下单交易码
    static String SINGLE_PLACE_ORDER = "11T10P";
    // 单笔支付明细状态查询
    static String QUERY_SINGLE_DETAIL_PAY_STATUS = "41T10";

    // 批量支付预下单交易码
    static String BATCH_PLACE_ORDER = "11B10P";

    // 结售汇交易结果查询SSFE3001
    static String CURRENCY_EXCHANGE_RESULT_QUERY = "SSFE3001";
    // 即期结售汇交易提交SSFE1002
    static String CURRENCY_EXCHANGE_SUBMIT = "SSFE1002";
    // 即期结售汇交割SSFE1003
    static String CURRENCY_EXCHANGE_DELIVERY = "SSFE1003";
    // 即期结售汇询价SSFE1001
    static String CURRENCY_EXCHANGE_RATE_QUERY = "SSFE1001";
    // 即期外汇买卖SSFE1018(提交接口的copy)
    static String CURRENCY_EXCHANGE_SUBMIT_EACH = "SSFE1018";

    // 汇入汇款确认提交SSFE1004
    static String INWARD_REMITTANCE_SUBMIT = "SSFE1004";
    // 汇入汇款确认交易结果查询SSFE3004
    static String INWARD_REMITTANCE_RESULT_QUERY = "SSFE3004";
    // 汇入汇款待确认业务列表查询SSFE3005
    static String INWARD_REMITTANCE_LIST_QUERY = "SSFE3005";
    // 汇入汇款业务明细查询SSFE3006
    static String INWARD_REMITTANCE_DETAIL_QUERY = "SSFE3006";
    // 账户交易明细查询条数
    int QUERY_NUMBER_50 = 50;

    // 客户号查询
    static String CUST_NO_QUERY = "01a21a001";

    // 电子回单查询条数
    int QUERY_NUMBER_100 = 100;

    static String SERVICE_RESP_CODE = "000000";  // 服务响应码

    // 银企联银企对账信息查询
    static String QUERY_BANK_BILL_CHECK = "43T22";
    // 银企联对账信息提交
    static String SUBMIT_BANK_BILL_CHECK = "43T24";
    // 银企联对账结果查询
    static String SEARCH_BANK_BILL_RESULT = "43T25";

    // 新增常量映射表
    public static Map<String, String> TRANS_CODE_COMMENTS = new HashMap<String, String>() {{
        // 支付类
        put(QUERY_BATCH_DETAIL_PAY_STATUS, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400411", "批量支付明细状态查询") /* "批量支付明细状态查询" */);
        put(BATCH_PAY_DETAIL_STATUS_QUERY, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400412", "代发工资结果查询") /* "代发工资结果查询" */);
        put(SINGLE_PLACE_ORDER, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400413", "单笔支付预下单交易码") /* "单笔支付预下单交易码" */);
        put(QUERY_SINGLE_DETAIL_PAY_STATUS, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400414", "单笔支付明细状态查询") /* "单笔支付明细状态查询" */);
        put(BATCH_PLACE_ORDER, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400415", "批量支付预下单交易码") /* "批量支付预下单交易码" */);

        // 账户查询类
        put(QUERY_ACCOUNT_BALANCE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400416", "账户实时余额查询") /* "账户实时余额查询" */);
        put(QUERY_HIS_ACCOUNT_BALANCE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400417", "账户实时余额历史数据查询") /* "账户实时余额历史数据查询" */);
        put(QUERY_ACCOUNT_TRANSACTION_DETAIL, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400418", "账户交易明细查询") /* "账户交易明细查询" */);
        put(QUERY_INNER_ACCOUNT_TRANSACTION_DETAIL, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400419", "查询内部账户交易明细") /* "查询内部账户交易明细" */);

        // 电子回单类
        put(QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540041A", "账户电子回单查询") /* "账户电子回单查询" */);
        put(DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540041B", "账户电子回单下载") /* "账户电子回单下载" */);

        // 对账相关
        put(QUERY_ELECTRONIC_STATEMENT_CONFIRM, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540041C", "账户电子对账单确认查询") /* "账户电子对账单确认查询" */);
        put(DOWNLOAD_ELECTRONIC_STATEMENT_CONFIRM_FILE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540041D", "对账单文件下载") /* "对账单文件下载" */);
        put(QUERY_BANK_BILL_CHECK, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540041E", "银企联银企对账信息查询") /* "银企联银企对账信息查询" */);
        put(SUBMIT_BANK_BILL_CHECK, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540041F", "银企联对账信息提交") /* "银企联对账信息提交" */);
        put(SEARCH_BANK_BILL_RESULT, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400420", "银企联对账结果查询") /* "银企联对账结果查询" */);

        // 结售汇相关
        put(CURRENCY_EXCHANGE_RESULT_QUERY, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400421", "结售汇交易结果查询") /* "结售汇交易结果查询" */);
        put(CURRENCY_EXCHANGE_SUBMIT, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400422", "即期结售汇交易提交") /* "即期结售汇交易提交" */);
        put(CURRENCY_EXCHANGE_DELIVERY, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400423", "即期结售汇交割") /* "即期结售汇交割" */);
        put(CURRENCY_EXCHANGE_RATE_QUERY, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400425", "即期结售汇询价") /* "即期结售汇询价" */);
        put(CURRENCY_EXCHANGE_SUBMIT_EACH, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400426", "即期外汇买卖") /* "即期外汇买卖" */);

        // 汇款相关
        put(INWARD_REMITTANCE_SUBMIT, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400427", "汇入汇款确认提交") /* "汇入汇款确认提交" */);
        put(INWARD_REMITTANCE_RESULT_QUERY, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400428", "汇入汇款确认交易结果查询") /* "汇入汇款确认交易结果查询" */);
        put(INWARD_REMITTANCE_LIST_QUERY, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400429", "汇入汇款待确认业务列表查询") /* "汇入汇款待确认业务列表查询" */);
        put(INWARD_REMITTANCE_DETAIL_QUERY, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540042B", "汇入汇款业务明细查询") /* "汇入汇款业务明细查询" */);

        // 其他服务
        put(RECEIVE_ACCOUNT_TRANSACTION_DETAIL, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540042C", "银行账户交易明细推送 - 银企联到账通知") /* "银行账户交易明细推送 - 银企联到账通知" */);
        put(RECEIVE_ACCOUNT_TRANSACTION_DETAIL_CHECK, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540042E", "银企联到账通知重试") /* "银企联到账通知重试" */);
        put(SERVICE_RESP_CODE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540042F", "服务响应码") /* "服务响应码" */);
        put(CUST_NO_QUERY, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400430", "客户号查询") /* "客户号查询" */);
    }};
    public static String getYQLNoDataMsq(CtmJSONObject responseHead) {
        String service_resp_code = responseHead.getString("service_resp_code");
        String serviceRespDesc = responseHead.getString("service_resp_desc");
        String serviceSeqNo = responseHead.getString("service_seq_no");
        return String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400432", "银企联返回：[%s],service_seq_no:[%s]") /* "银企联返回：[%s],service_seq_no:[%s]" */, serviceRespDesc, serviceSeqNo);
    }

    public static String getYQLErrorMsq(CtmJSONObject responseHead) {
        String service_resp_code = responseHead.getString("service_resp_code");
        String serviceRespDesc = responseHead.getString("service_resp_desc");
        String serviceSeqNo = responseHead.getString("service_seq_no");
        return String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400435", "银企联返回报错：[%s],service_seq_no:[%s]。") /* "银企联返回报错：[%s],service_seq_no:[%s]。" */ + CONTACT_YQL_TIP, serviceRespDesc, serviceSeqNo);
    }


    public static String getYQLErrorMsq(CtmJSONObject responseHead, String errorMessages) {
        String serviceSeqNo = responseHead.getString("service_seq_no");
        return String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400435", "银企联返回报错：[%s],service_seq_no:[%s]。") /* "银企联返回报错：[%s],service_seq_no:[%s]。" */ + CONTACT_YQL_TIP, errorMessages, serviceSeqNo);

    }

    public static String getYQLErrorMsqWithBillno(CtmJSONObject responseHead, String billno) {
        String service_resp_code = responseHead.getString("service_resp_code");
        String serviceRespDesc = responseHead.getString("service_resp_desc");
        String serviceSeqNo = responseHead.getString("service_seq_no");
        return String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400410", "单号[%s]银企联返回报错：[%s],service_seq_no:[%s]。") /* "单号[%s]银企联返回报错：[%s],service_seq_no:[%s]。" */ + CONTACT_YQL_TIP, billno, serviceRespDesc, serviceSeqNo);
    }

    public static String getYQLErrorMsqForManual(Map<String, Object> params, CtmJSONObject responseHead) {
        String service_resp_code = responseHead.getString("service_resp_code");
        String serviceRespDesc = responseHead.getString("service_resp_desc");
        String serviceSeqNo = responseHead.getString("service_seq_no");
        Object acct_no = params.get("acct_no");
        Object date = null;
        Object startDate = Optional.ofNullable(params.get("startDate")).orElse(params.get("beg_date"));
        Object endDate = Optional.ofNullable(params.get("endDate")).orElse(params.get("end_date"));
        if (startDate == null) {
            //实时余额查询不传日期，查询的是今天的数据
            startDate = DateUtils.getTodayShort();
        }
        if (endDate != null) {
            //起始日期相同时，只提示一天
            if (startDate.toString().equals(endDate.toString())) {
                date = startDate;
            } else {
                date = startDate + "~" + endDate;
            }
        } else {
            date = startDate;
        }

        Object currencyCode = Optional.ofNullable(params.get("currencyCode")).orElse(params.get("curr_code"));
        String errorInfo = String.format("%s|%s|%s:%s[serviceSeqNo:%s]", acct_no, date, currencyCode, serviceRespDesc, serviceSeqNo);
        return errorInfo;
    }

    public static String getYQLErrorMsqOfNetWork(String message) {
        String errorInfo = String.format("%s[%s]", TransErrorInfo.ERROR_NET_ERROR.getErrorMessage(), message);
        return errorInfo;
    }

    public static String getErrorMsqWithAccount(Exception e, String accountInfo) {
        return String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540042A", "账户[%s]") /* "账户[%s]" */, accountInfo) + "[Error]" + e.getMessage();
    }

    public static String getErrorMsqWithAccountBillno(Exception e, String accountInfo, String billno) {
        return String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540042D", "账户[%s]单据号[%s]银企联返回报错：[%s]") /* "账户[%s]单据号[%s]银企联返回报错：[%s]" */, accountInfo, billno, e.getMessage());
    }


    public static String getYQLTestData(String transCode, String data, List<BasicNameValuePair> requestData) {
        //如果/data目录下有testYQL.txt文件，则读取该测试文件内容替代data。测试使用，正常环境没有该文件。
        try {
            CtmJSONObject  reqData =  new CtmJSONObject();
            for(BasicNameValuePair vo : requestData){
                if("reqData".equals(vo.getName())){
                    reqData = CtmJSONObject.parseObject(vo.getValue());
                }
            }
            Map requestBody = (Map) reqData.get("request_body");
            Object acctNo = requestBody.get("acct_no");
            Object currCode = Optional.ofNullable(requestBody.get("currencyCode")).orElse(requestBody.get("curr_code"));
            if (ITransCodeConstant.QUERY_ACCOUNT_BALANCE.equals(transCode)) {
                List<String> acctNoList = new ArrayList<>();
                List<String> currCodeList = new ArrayList<>();
                List<Map<String, Object>> recordList = (List<Map<String, Object>>) requestBody.get("record");
                for (int i = 0; i < recordList.size(); i++) {
                    Map<String, Object> record = recordList.get(i);
                    acctNoList.add(record.get("acct_no").toString());
                    currCodeList.add(record.get("curr_code").toString());
                }
                acctNo = formatListToFileName(acctNoList.toString());
                currCode = formatListToFileName(currCodeList.toString());
            }
            Object startDate = Optional.ofNullable(requestBody.get("startDate")).orElse(requestBody.get("beg_date"));
            Object endDate = Optional.ofNullable(requestBody.get("endDate")).orElse(requestBody.get("end_date"));
            String file_name = String.format("%s_%s_%s_%s_%s.txt", transCode, acctNo, currCode, startDate, endDate);
            String directoryPath = "/data/";
            String filePath = directoryPath + file_name;
            File file = new File(filePath);
            if (file.exists() && !file.isDirectory()) {
                try (Scanner scanner = new Scanner(file)) {
                    data = scanner.useDelimiter("\\A").next(); // 读取整个文件内容
                } catch (FileNotFoundException e) {
                    log.error("读取测试文件" + filePath + "失败FileNotFoundException:" + e.getMessage(), e);
                }
            } else {
                log.error("未找到测试文件:" + filePath);
            }
        } catch (Exception e) {
            log.error("读取测试文件失败");
        }
        return data;
    }


    public static void writeYQLTestDataFromRowList(String transCode,  List<Map<String, Object>> rowList) {
        for (int i = 0; i < rowList.size(); i++) {
            Map<String, Object> rowMap = rowList.get(i);
            CtmJSONObject directoryParam = new CtmJSONObject();
            // 从rowMap中取出directoryParam所需的四个值
            directoryParam.put("acct_no", rowMap.get("acct_no"));
            directoryParam.put("curr_code", rowMap.get("curr_code"));
            directoryParam.put("startDate", rowMap.get("tran_date"));
            directoryParam.put("endDate", rowMap.get("tran_date"));
            CtmJSONObject data  = getDataFromRowMap(rowMap, transCode);
            writeYQLTestData(transCode, data, directoryParam);

        }

    }

private static CtmJSONObject getDataFromRowMap(Map<String, Object> rowMap, String transCode) {
    if (transCode.equals(QUERY_ACCOUNT_TRANSACTION_DETAIL)) {
        return transBankDeal(rowMap);
    } else if (transCode.equals(QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL)) {
        return transReceipt(rowMap);
    } else if (transCode.equals(QUERY_HIS_ACCOUNT_BALANCE)) {
        return transHisBalance(rowMap);
    } else if (transCode.equals(QUERY_ACCOUNT_BALANCE)) {
        return transRealBalance(rowMap);
    } else if (transCode.equals(QUERY_ELECTRONIC_STATEMENT_CONFIRM)) {
        return transSatement(rowMap);
    } else {
        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400434", "不支持的交易类型") /* "不支持的交易类型" */);
    }
}

    private static CtmJSONObject transSatement(Map<String, Object> rowMap) {
    // 定义response_body字段常量数组
    String[] RESPONSE_BODY_FIELDS = {
        "next_page", "query_extend", "tot_num", "back_num"
    };

    // 定义record字段常量数组
    String[] RECORD_FIELDS = {
        "beg_date", "end_date", "statement_no", "statement_status", "unique_no"
    };

    // 定义response_head字段常量数组
    String[] RESPONSE_HEAD_FIELDS = {
        "service_busi_date", "service_finish_time", "service_recv_time",
        "service_resp_code", "service_resp_desc", "service_seq_no",
        "service_status"
    };

    // 构造testData部分
    CtmJSONObject testData = new CtmJSONObject();

    // 构造response_body
    CtmJSONObject responseBody = new CtmJSONObject();

    // 遍历赋值response_body字段
    for (String field : RESPONSE_BODY_FIELDS) {
        responseBody.put(field, rowMap.get(field));
    }

    // 构造record对象（注意这里是单个record对象而不是数组）
    CtmJSONObject record = new CtmJSONObject();

    // 遍历赋值record字段
    for (String field : RECORD_FIELDS) {
        record.put(field, rowMap.get(field));
    }

    // 将record对象直接放入responseBody（不是数组形式）
    responseBody.put("record", record);

    // 构造response_head
    CtmJSONObject responseHead = new CtmJSONObject();

    // 遍历赋值response_head字段
    for (String field : RESPONSE_HEAD_FIELDS) {
        responseHead.put(field, rowMap.get(field));
    }

    // 组装testData
    testData.put("response_body", responseBody);
    testData.put("response_head", responseHead);
    return testData;
}


    private static CtmJSONObject transRealBalance(Map<String, Object> rowMap) {
    // 定义response_body字段常量数组
    String[] RESPONSE_BODY_FIELDS = {
        "tot_num"
    };

    // 定义record字段常量数组
    String[] RECORD_FIELDS = {
        "acct_bal", "acct_name", "acct_no", "avl_bal",
        "cash_flag", "curr_code", "frz_bal", "yester_bal"
    };

    // 定义response_head字段常量数组
    String[] RESPONSE_HEAD_FIELDS = {
        "service_busi_date", "service_finish_time", "service_recv_time",
        "service_resp_code", "service_resp_desc", "service_seq_no",
        "service_status"
    };

    // 构造testData部分
    CtmJSONObject testData = new CtmJSONObject();

    // 构造response_body
    CtmJSONObject responseBody = new CtmJSONObject();

    // 遍历赋值response_body字段
    for (String field : RESPONSE_BODY_FIELDS) {
        responseBody.put(field, rowMap.get(field));
    }

    // 构造record数组
    JSONArray records = new JSONArray();
    CtmJSONObject record = new CtmJSONObject();

    // 遍历赋值record字段
    for (String field : RECORD_FIELDS) {
        record.put(field, rowMap.get(field));
    }

    records.add(record);
    responseBody.put("record", records);

    // 构造response_head
    CtmJSONObject responseHead = new CtmJSONObject();

    // 遍历赋值response_head字段
    for (String field : RESPONSE_HEAD_FIELDS) {
        responseHead.put(field, rowMap.get(field));
    }

    // 组装testData
    testData.put("response_body", responseBody);
    testData.put("response_head", responseHead);
    return testData;
}

    private static CtmJSONObject transHisBalance(Map<String, Object> rowMap) {
        // 定义response_body字段常量数组
        String[] RESPONSE_BODY_FIELDS = {
            "acct_name", "acct_no", "back_num"
        };

        // 定义record字段常量数组
        String[] RECORD_FIELDS = {
            "acct_bal", "acct_name", "acct_no", "avl_bal", "curr_code", "tran_date"
        };

        // 定义response_head字段常量数组
        String[] RESPONSE_HEAD_FIELDS = {
            "service_busi_date", "service_finish_time", "service_recv_time",
            "service_resp_code", "service_resp_desc", "service_seq_no",
            "service_status"
        };

        // 构造testData部分
        CtmJSONObject testData = new CtmJSONObject();

        // 构造response_body
        CtmJSONObject responseBody = new CtmJSONObject();

        // 遍历赋值response_body字段
        for (String field : RESPONSE_BODY_FIELDS) {
            responseBody.put(field, rowMap.get(field));
        }

        // 构造record数组
        JSONArray records = new JSONArray();
        CtmJSONObject record = new CtmJSONObject();

        // 遍历赋值record字段
        for (String field : RECORD_FIELDS) {
            record.put(field, rowMap.get(field));
        }

        records.add(record);
        responseBody.put("record", records);

        // 构造response_head
        CtmJSONObject responseHead = new CtmJSONObject();

        // 遍历赋值response_head字段
        for (String field : RESPONSE_HEAD_FIELDS) {
            responseHead.put(field, rowMap.get(field));
        }

        // 组装testData
        testData.put("response_body", responseBody);
        testData.put("response_head", responseHead);
        return testData;
    }

    private static CtmJSONObject transBankDeal(Map<String, Object> rowMap) {
        // 定义response_body字段常量数组
        String[] RESPONSE_BODY_FIELDS = {
            "acct_name", "acct_no", "back_num", "next_page",
            "query_extend", "tot_num"
        };

        // 定义record字段常量数组
        String[] RECORD_FIELDS = {
            "acct_bal", "bank_check_code", "bank_reconciliation_code",
            "bank_seq_no", "bank_temp_check_code", "curr_code",
            "dc_flag", "detail_check_id", "remark",
            "to_acct_bank_name", "to_acct_name", "to_acct_no",
            "tran_amt", "tran_date", "tran_time",
            "unique_no", "use_name"
        };

        // 定义response_head字段常量数组
        String[] RESPONSE_HEAD_FIELDS = {
            "service_busi_date", "service_finish_time", "service_recv_time",
            "service_resp_code", "service_resp_desc", "service_seq_no",
            "service_status"
        };

        // 构造testData部分
        CtmJSONObject testData = new CtmJSONObject();

        // 构造response_body
        CtmJSONObject responseBody = new CtmJSONObject();

        // 遍历赋值response_body字段
        for (String field : RESPONSE_BODY_FIELDS) {
            responseBody.put(field, rowMap.get(field));
        }

        // 构造record数组
        JSONArray records = new JSONArray();
        CtmJSONObject record = new CtmJSONObject();

        // 遍历赋值record字段
        for (String field : RECORD_FIELDS) {
            record.put(field, rowMap.get(field));
        }

        records.add(record);
        responseBody.put("record", records);

        // 构造response_head
        CtmJSONObject responseHead = new CtmJSONObject();

        // 遍历赋值response_head字段
        for (String field : RESPONSE_HEAD_FIELDS) {
            responseHead.put(field, rowMap.get(field));
        }

        // 组装testData
        testData.put("response_body", responseBody);
        testData.put("response_head", responseHead);
        return testData;
    }

    private static CtmJSONObject transReceipt(Map<String, Object> rowMap) {
    // 定义response_body字段常量数组
    String[] RESPONSE_BODY_FIELDS = {
        "acct_name", "acct_no", "tot_num", "back_num", "next_page"
    };

    // 定义record字段常量数组
    String[] RECORD_FIELDS = {
        "bill_extend", "bill_no", "curr_code", "detail_check_id",
        "to_acct_name", "to_acct_no", "tran_amt", "tran_date",
        "unique_no", "dc_flag"
    };

    // 定义response_head字段常量数组
    String[] RESPONSE_HEAD_FIELDS = {
        "service_busi_date", "service_finish_time", "service_recv_time",
        "service_resp_code", "service_resp_desc", "service_seq_no",
        "service_status"
    };

    // 构造testData部分
    CtmJSONObject testData = new CtmJSONObject();

    // 构造response_body
    CtmJSONObject responseBody = new CtmJSONObject();

    // 遍历赋值response_body字段
    for (String field : RESPONSE_BODY_FIELDS) {
        responseBody.put(field, rowMap.get(field));
    }

    // 构造record对象（注意这里是单个record对象而不是数组）
    CtmJSONObject record = new CtmJSONObject();

    // 遍历赋值record字段
    for (String field : RECORD_FIELDS) {
        record.put(field, rowMap.get(field));
    }

    // 将record对象直接放入responseBody（不是数组形式）
    responseBody.put("record", record);

    // 构造response_head
    CtmJSONObject responseHead = new CtmJSONObject();

    // 遍历赋值response_head字段
    for (String field : RESPONSE_HEAD_FIELDS) {
        responseHead.put(field, rowMap.get(field));
    }

    // 组装testData
    testData.put("response_body", responseBody);
    testData.put("response_head", responseHead);
    return testData;
}



    /**
     * 将测试数据写入指定文件（测试环境使用）
     * @param transCode 交易码，用于生成目录结构
     * @param data 要写入的测试数据内容
     */
    public static void writeYQLTestData(String transCode, CtmJSONObject data, CtmJSONObject directoryParam) {
        //从data中取出下面四个变量的值，其中data通过buildTestData函数生成
        Object acctNo = formatListToFileName(directoryParam.get("acct_no").toString());
        Object currCode = formatListToFileName(directoryParam.get("curr_code").toString());
        Object startDate = directoryParam.get("startDate");
        Object endDate = directoryParam.get("endDate");
        String file_name = String.format("%s_%s_%s_%s_%s.txt", transCode, acctNo, currCode, startDate, endDate);
        ///data/40T23/699973566/CNY/2025-02-14_2025-02-14/testYQL.txt
        writeYQLTestDataToFile(data, file_name);
    }

    private static Object formatListToFileName(String str) {
        return str.replace(',', '_').replaceAll("\\s+", "");
    }

    public static void writeYQLTestDataToFile(CtmJSONObject data, String file_name) {
        try {
            file_name.replace("'", "");
            String directoryPath = "/data/";
            String filePath = directoryPath + file_name;
            // 分离目录路径和文件路径
            Path fullPath = Paths.get(filePath);
            Path parentDir = fullPath.getParent();

            // 使用NIO API创建目录
            if (!Files.exists(parentDir)) {
                try {
                    Files.createDirectories(parentDir);
                } catch (IOException e) {
                    log.error("创建目录失败：{}，错误信息：{}", parentDir, e.getMessage(), e);
                    return;
                }
            }

            //// 校验目标路径类型
            //if (Files.exists(fullPath) && !Files.isRegularFile(fullPath)) {
            //    log.error("目标路径已被占用：{}", filePath);
            //    return;
            //}

            // 显式指定编码和写入模式
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
                writer.write(data.toString());
                log.info("测试数据已成功写入：{}", filePath);
            }
        } catch (IOException e) {
            log.error("写入测试文件失败，错误信息：{}",  e.getMessage(), e);
        }
    }


    public static CtmJSONObject buildTestData(CtmJSONObject directoryParam, String unique_no) {
//        Object accEntity = queryParam.get("accEntity");// 会计主体，可以是单个字符串或多个会计主体的列表
//        Object accountId = queryParam.get("accountId");// 银行账户ID，可以是单个字符串或多个银行账户ID的列表
//        Object startDate = queryParam.get("startDate");// 查询的开始日期，格式为 "yyyy-MM-dd"
//        Object endDate = queryParam.get("endDate");// 查询的结束日期，格式为 "yyyy-MM-dd"
        Object acctNo = directoryParam.get("acct_no");
        Object acctName = directoryParam.get("acct_name");
        Object currCode = directoryParam.get("curr_code");

        CtmJSONObject response = new CtmJSONObject();

// 构建response_body
        CtmJSONObject responseBody = new CtmJSONObject();
        responseBody.put("acct_name", acctName);
        responseBody.put("acct_no", acctNo);
        String tot_num = "1";
        responseBody.put("back_num", tot_num);
        responseBody.put("next_page", "0");

// 构建record数组
        JSONArray records = new JSONArray();
// 第一条记录
        CtmJSONObject record1 = new CtmJSONObject();
        record1.put("bank_check_code", "BEECC403E16C0F49A624E0196268AE42");
        record1.put("bank_seq_no", "11202502141540331067669502");
        record1.put("bill_extend", "");
        record1.put("bill_no", "12202502141540331299285132");
        record1.put("curr_code", currCode);
        record1.put("dc_flag", "d");
        record1.put("detail_check_id", "EB1D53E115E04D877D1C073CB5D10993");
        record1.put("to_acct_name", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400431", "wwz-供应商-银行账户-2") /* "wwz-供应商-银行账户-2" */);
        record1.put("to_acct_no", "wwz-gys-yhzh-2");
        record1.put("tran_amt", "0.02");
        record1.put("tran_date", "20250214");
        //record1.put("unique_no", "1297D1DF72945ADC17A189B63109D430");
        record1.put("unique_no", unique_no);
        records.add(record1);

//// 第二条记录
//        CtmJSONObject record2 = new CtmJSONObject();
//        record2.put("bank_check_code", "459048884B0AD334A191A5425DCBBBE2");
//        record2.put("bank_seq_no", "11202502141540331636592578");
//        record2.put("bill_extend", "");
//        record2.put("bill_no", "12202502141540331125824634");
//        record2.put("curr_code", currCode);
//        record2.put("dc_flag", "d");
//        record2.put("detail_check_id", "738E7344285E4657EAF7FD1263753A87");
//        record2.put("to_acct_name", "wwz-客户-银行账户-1");
//        record2.put("to_acct_no", "wwz-kh-yhzh-1");
//        record2.put("tran_amt", "0.01");
//        record2.put("tran_date", "20250214");
//        record2.put("unique_no", "ACAC587B126844A5F35300D3BD334314");
//        records.add(record2);
//
//// 第三条记录
//        CtmJSONObject record3 = new CtmJSONObject();
//        record3.put("bank_check_code", "E3F9542B1EDE79839A301CB2FEB3C662");
//        record3.put("bank_seq_no", "11202502141556180385589277");
//        record3.put("bill_extend", "");
//        record3.put("bill_no", "12202502141556180942613759");
//        record3.put("curr_code", currCode);
//        record3.put("dc_flag", "d");
//        record3.put("detail_check_id", "6EB252995F786E09FDD8594C43A638AA");
//        record3.put("remark", "92qj1i1");
//        record3.put("to_acct_name", "wwz-供应商-银行账户-2");
//        record3.put("to_acct_no", "wwz-gys-yhzh-2");
//        record3.put("tran_amt", "0.04");
//        record3.put("tran_date", "20250214");
//        record3.put("unique_no", "9FC09291327D8DD4F058399C4D31AB14");
//        records.add(record3);
//
//// 第四条记录
//        CtmJSONObject record4 = new CtmJSONObject();
//        record4.put("bank_check_code", "A1AA0852E5D05E854089A46C1C09B02C");
//        record4.put("bank_seq_no", "11202502141556180953127900");
//        record4.put("bill_extend", "");
//        record4.put("bill_no", "12202502141556180747286069");
//        record4.put("curr_code", currCode);
//        record4.put("dc_flag", "d");
//        record4.put("detail_check_id", "7260742D9347FE374ED0A3321F09AE27");
//        record4.put("remark", "92qj1i0");
//        record4.put("to_acct_name", "wwz-客户-银行账户-1");
//        record4.put("to_acct_no", "wwz-kh-yhzh-1");
//        record4.put("tran_amt", "0.03");
//        record4.put("tran_date", "20250214");
//        record4.put("unique_no", "D24E9F1329657FA760205EEAE539939E");
//        records.add(record4);

        responseBody.put("record", records);
        responseBody.put("tot_num", tot_num);

// 构建response_head
        CtmJSONObject responseHead = new CtmJSONObject();
        responseHead.put("service_busi_date", "20250407");
        responseHead.put("service_finish_time", "20250407193751062");
        responseHead.put("service_recv_time", "20250407193751005");
        responseHead.put("service_resp_code", "000000");
        responseHead.put("service_resp_desc", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400433", "成功") /* "成功" */);
        responseHead.put("service_seq_no", "RC000600000202504071938208500005");
        responseHead.put("service_status", "00");

// 组装完整响应
        response.put("response_body", responseBody);
        response.put("response_head", responseHead);
        return response;
    }

    public static String getCustomNoFromAccounts(List<EnterpriseBankAcctVO> netAccounts) throws Exception {
        if (CollectionUtils.isEmpty(netAccounts)) {
            return null;
        }

        List<String> netAccountsId = netAccounts.stream()
                .filter(Objects::nonNull)
                .map(EnterpriseBankAcctVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (netAccountsId.isEmpty()) {
            return null;
        }

        QuerySchema schema = QuerySchema.create().addSelect("accentity,enterpriseBankAccount,customNo");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(netAccountsId));
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq(true));
        conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
        conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
        schema.addCondition(conditionGroup);

        List<Map<String, Object>> accountSettings = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);

        if (accountSettings != null && !accountSettings.isEmpty()) {
            //同一租户只用一个客户号
            //todo 实际上是一个客户一个客户号
            Object customNoObj = accountSettings.get(0).get("customNo");
            return customNoObj != null ? customNoObj.toString() : null;
        }

        return null;
    }

    //旧方法比较和设置中间有其他线程操作时，可能导致数字大于4位，超过银企联位数限制，从而报错;CAS可能会有一定等待耗时
    public static String getSerialNumberCAS(AtomicInteger cardinalNumber) {
        int current;
        int next;
        do {
            current = cardinalNumber.get();
            // 如果当前值大于 9999，则重置为 1
            next = (current > 9999) ? 1 : current + 1;
        } while (!cardinalNumber.compareAndSet(current, next));

        return String.format("%04d", next);
    }

    //强制截取为4位，可能会跳号，影响不大
    public static String getSerialNumberNoCAS(AtomicInteger cardinalNumber) {
        if (cardinalNumber.intValue() > 9999) {
            cardinalNumber.set(1);
        }
        int increment = cardinalNumber.incrementAndGet();
        //高并发下可能超过4位
        return String.format("%04d", increment % 10000);
    }

    public static String getSerialNumberNoCASWithMaxNum5(AtomicInteger cardinalNumber) {
        if (cardinalNumber.intValue() > 99999) {
            cardinalNumber.set(1);
        }
        int increment = cardinalNumber.incrementAndGet();
        //高并发下可能超过5位
        return String.format("%05d", increment % 100000);
    }
}

