package com.yonyoucloud.fi.cmp.constant;

/**
 * 银企联交易相关交易码常量类
 *
 * @author maliangn
 * @date 2023-04-17
 */
public interface ITransCodeConstant {

    static String NO_DATAS_ERVICE_RESP_CODE = "350019";

    //回单验签
    String VERIFY_SIGN = "48T25";//4.4.13 统一校验(电子凭证会计数据标准):48T25

    //批量支付明细状态查询
    String QUERY_BATCH_DETAIL_PAY_STATUS = "40B10";

    //账户实时余额查询
    String QUERY_ACCOUNT_BALANCE = "40T20";

    //银行账户交易明细推送 - 银企联 到账通知
    String RECEIVE_ACCOUNT_TRANSACTION_DETAIL = "40D22";
    //银企联 到账通知 重试
    String RECEIVE_ACCOUNT_TRANSACTION_DETAIL_CHECK = "60T20";
    //账户交易明细查询
    String QUERY_ACCOUNT_TRANSACTION_DETAIL = "40T22";

    //查询内部账户交易明细
    String QUERY_INNER_ACCOUNT_TRANSACTION_DETAIL = "INNER";
    //账户电子回单查询
    String QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL = "40T23";
    //账户电子回单下载
    String DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL = "48T23";

    //批量预下单
    String BATCH_PAY_PRE_ORDER = "11B10P";
    //预下单交易确认
    String PRE_ORDER_TRANSACTION_CONFIRM = "50C10";

    //账户实时余额历史数据查询
    String QUERY_HIS_ACCOUNT_BALANCE = "40T21";
    //代发工资结果查询
    String BATCH_PAY_DETAIL_STATUS_QUERY = "41B12";

    //账户电子对账单确认查询
    String QUERY_ELECTRONIC_STATEMENT_CONFIRM = "43T26";
    //对账单文件下载
    String DOWNLOAD_ELECTRONIC_STATEMENT_CONFIRM_FILE = "43T23";


    //单笔支付预下单交易码
    String SINGLE_PLACE_ORDER = "11T10P";
    //单笔支付明细状态查询
    String QUERY_SINGLE_DETAIL_PAY_STATUS = "41T10";

    //批量支付预下单交易码
    String BATCH_PLACE_ORDER = "11B10P";


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

    // 汇入汇款确认提交SSFE1004
    String INWARD_REMITTANCE_SUBMIT = "SSFE1004";
    // 汇入汇款确认交易结果查询SSFE3004
    String INWARD_REMITTANCE_RESULT_QUERY = "SSFE3004";
    // 汇入汇款待确认业务列表查询SSFE3005
    String INWARD_REMITTANCE_LIST_QUERY = "SSFE3005";
    // 汇入汇款业务明细查询SSFE3006
    String INWARD_REMITTANCE_DETAIL_QUERY = "SSFE3006";
    //账户交易明细查询条数
    int QUERY_NUMBER_50 = 50;
    //
    String UNIQUE_NO = "unique_no";
    //客户号查询
    String CUST_NO_QUERY =  "01a21a001";

    //电子回单查询条数
    int QUERY_NUMBER_100 = 100;


    String SERVICE_RESP_CODE = "000000";  //服务响应码   “000000”（6个0）代表成功，如果返回“000000”，则service_status的值一定是“00”

    //银企联银企对账信息查询
    String QUERY_BANK_BILL_CHECK = "43T22";
    //银企联对账信息提交
    String SUBMIT_BANK_BILL_CHECK = "43T24";
    //银企联对账结果查询
    String SEARCH_BANK_BILL_RESULT = "43T25";


}
