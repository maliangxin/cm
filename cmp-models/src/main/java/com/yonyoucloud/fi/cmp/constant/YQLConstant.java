package com.yonyoucloud.fi.cmp.constant;

/**
 * @author: liaojbo
 * @Date: 2025年05月29日 14:42
 * @Description:
 */
public class YQLConstant {

    //银企联返回报文通用字段
    public static final String DATA = "data";
    public static final String RESPONSE_BODY = "response_body";
    public static final String RECORD = "record";
    // 银企联返回报文字段
    public static String TO_ACCT_NO = "to_acct_no";
    public static String TO_ACCT_NAME = "to_acct_name";
    public static String REMARK = "remark";
    public static String UNIQUE_NO = "unique_no";
    public static String CURR_CODE = "curr_code";
    public static String ACCT_NO = "acct_no";
    //流水回单关联码
    public static String DETAIL_RECEIPT_RELATION_CODE = "detail_check_id";
    //回单编号
    public static String BILL_NO = "bill_no";
    //回单扩展信息
    public static String BILL_EXTEND = "bill_extend";
    // 如果是其他返回的是back_num
    public static final String BACK_NUM = "back_num";
    // 如果是实时余额返回的是tot_num
    public static final String TOT_NUM = "tot_num";
}
